package com.jetbrains.cef.remote;

import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandler;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.io.*;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class NativeServerManager {
    private static final Boolean DISABLE_GPU = Utils.getBoolean("JCEF_DISABLE_GPU");
    private static final String ALT_CEF_SERVER_PATH = Utils.getString("ALT_CEF_SERVER_PATH");
    private static final String ALT_SUBPROCESS_PATH = Utils.getString("ALT_SUBPROCESS_PATH");
    private static final boolean CHECK_PROCESS_ALIVE = Utils.getBoolean("JCEF_CHECK_PROCESS_ALIVE", true); // for debug, TODO: remove

    private static Process ourNativeServerProcess = null;

    // Should be called in bg thread
    public static boolean startProcessAndWait(CefAppHandler appHandler, CefSettings settings, long timeoutMs) {
        final Path pipeName = Path.of(System.getProperty("java.io.tmpdir")).resolve("cef_server_params.txt");
        File f = new File(pipeName.toString());
        PrintStream ps;
        try {
            new FileOutputStream(f).close(); // delete the content of the file
            f.createNewFile();
            ps = new PrintStream(new FileOutputStream(f, false));
        } catch (IOException e) {
            CefLog.Error("Can't create temp file with server params path=%s, msg=%s", pipeName.toString(), e.getMessage());
            return false;
        }

        // 1. command line args
        final String sectionCmdLine = "[COMMAND_LINE]:";
        ps.printf("%s\n", sectionCmdLine);
        if (appHandler instanceof CefAppHandlerAdapter) {
            CefAppHandlerAdapter h = (CefAppHandlerAdapter)appHandler;
            String[] commandLineArgs = h.getArgs();
            if (commandLineArgs != null && commandLineArgs.length > 0)
                for (String arg: commandLineArgs) {
                    boolean skip = arg.startsWith("--browser-subprocess-path=")
                            || arg.startsWith("--main-bundle-path=")
                            || arg.startsWith("--framework-dir-path=");
                    if (skip)
                        CefLog.Debug("Skip cmdline swintch '%s'", arg);
                    else
                        ps.printf("%s\n", arg);
                }
            if (DISABLE_GPU) {
                ps.println("--disable-gpu");
                ps.println("--disable-gpu-compositing");
                ps.println("--disable-gpu-vsync");
                ps.println("--disable-software-rasterizer");
                ps.println("--disable-extensions");
            }

        } else if (appHandler != null)
            CefLog.Error("Unsupported class of CefAppHandler %s. Overridden command-line arguments will be ignored.", CefAppHandler.class);

        // 2. settings
        ps.printf("[SETTINGS]:\n");
        if (settings != null) {
            Map<String, String> settingsMap = settings.toMap();
            for (Map.Entry entry : settingsMap.entrySet()) {
                boolean skip = "browser_subprocess_path".equals(entry.getKey())
                        || "resources_dir_path".equals(entry.getKey())
                        || "locales_dir_path".equals(entry.getKey());
                if (skip)
                    CefLog.Debug("Skip setting %s=%s", entry.getKey(), entry.getValue());
                else
                    ps.printf("%s=%s\n", entry.getKey(), entry.getValue());
            }
        }

        if (ALT_SUBPROCESS_PATH != null && !ALT_SUBPROCESS_PATH.trim().isEmpty())
            ps.printf("browser_subprocess_path=%s\n", ALT_SUBPROCESS_PATH);
        else if (OS.isMacintosh()) {
            File serverExe = getServerExe();
            if (serverExe != null) {
                File subprocess = new File(serverExe.getParentFile().getParentFile(), "Frameworks/cef_server Helper.app/Contents/MacOS/cef_server Helper");
                ps.printf("browser_subprocess_path=%s\n", subprocess.getAbsolutePath());
            }
        }

        // 3. custom schemes
        ps.printf("[CUSTOM_SCHEMES]:\n");
        if (appHandler != null) {
            CefSchemeRegistrar collector = new CefSchemeRegistrar() {
                @Override
                public boolean addCustomScheme(String schemeName, boolean isStandard, boolean isLocal, boolean isDisplayIsolated, boolean isSecure, boolean isCorsEnabled, boolean isCspBypassing, boolean isFetchEnabled) {
                    int options = 0;
                    if (isStandard) options |= 1 << 0;
                    if (isLocal) options |= 1 << 1;
                    if (isDisplayIsolated) options |= 1 << 2;
                    if (isSecure) options |= 1 << 3;
                    if (isCorsEnabled) options |= 1 << 4;
                    if (isCspBypassing) options |= 1 << 5;
                    if (isFetchEnabled) options |= 1 << 6;
                    ps.printf("%s|%d\n", schemeName, options);
                    return false;
                }
            };
            appHandler.onRegisterCustomSchemes(collector);
        }

        ps.flush();
        ps.close();

        // Ensure file written
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(f));
            String line = reader.readLine();
            if (line == null || line.isEmpty() || !line.contains(sectionCmdLine)) {
                CefLog.Error("Write errors (when write temp file with server params), was written:");
                while (line != null) {
                    CefLog.Error("\t%s", line);
                    line = reader.readLine();
                }
            }
            reader.close();
        } catch (IOException e) {
            CefLog.Error("Can't read temp file with server params: %s", e.getMessage());
        }

        return startProcessAndWait(f.getAbsolutePath(), timeoutMs);
    }

    public static boolean isProcessAlive() {
        Process p = ourNativeServerProcess;
        return p != null && p.isAlive();
    }

    private static boolean isConnectable(boolean withDebug) {
        try {
            if (ThriftTransport.isTcp()) {
                try {
                    TSocket socket = new TSocket("localhost", ThriftTransport.getServerPort());
                    socket.open();
                    socket.close();
                    if (withDebug)
                        CefLog.Debug("isConnectable: tcp-port %d, opened and connected.", ThriftTransport.getServerPort());
                    return true;
                } catch (TTransportException e) {
                    if (withDebug)
                        CefLog.Debug("isConnectable: tcp-port %d, TTransportException occurred: %s", ThriftTransport.getServerPort(), e.getMessage());
                }
                return false;
            }
            try {
                if (OS.isWindows()) {
                    WindowsPipeSocket pipe = new WindowsPipeSocket(ThriftTransport.getServerPipe());
                    pipe.close();
                    if (withDebug)
                        CefLog.Debug("isConnectable: win-pipe '%s', opened and connected.", ThriftTransport.getServerPipe());
                    return true;
                }
                UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(ThriftTransport.getServerPipe());
                SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                channel.connect(socketAddress);
                channel.close();
                if (withDebug)
                    CefLog.Debug("isConnectable: pipe '%s', opened and connected.", ThriftTransport.getServerPipe());
                return true;
            } catch (IOException e) {
                if (withDebug)
                    CefLog.Debug("isConnectable: pipe '%s', IOException occurred: %s", ThriftTransport.getServerPipe(), e.getMessage());
            }
        } catch (Throwable e) {
            CefLog.Error("isConnectable: exception %s", e.getMessage());
        }
        return false;
    }

    private static boolean isServerSocketBusy(boolean withDebug) {
        try {
            if (ThriftTransport.isTcp()) {
                TServerSocket serverSocket = null;
                try {
                    serverSocket = new TServerSocket(ThriftTransport.getServerPort());
                } catch (TTransportException e) {
                    if (withDebug)
                        CefLog.Debug("isServerTransportBusy: tcp-port %d, TTransportException occurred: %s", ThriftTransport.getServerPort(), e.getMessage());
                    return true;
                }
                if (withDebug)
                    CefLog.Debug("isServerTransportBusy: tcp-port %d, opened and connected.", ThriftTransport.getServerPort());
                serverSocket.close();
            }
        } catch (Throwable e) {
            CefLog.Error("isServerSocketBusy: exception %s", e.getMessage());
        }
        return false;
    }

    public static boolean isRunning() {
        return isRunning(false);
    }

    public static boolean isRunning(boolean withDebug) {
        if (CHECK_PROCESS_ALIVE && ourNativeServerProcess != null && !ourNativeServerProcess.isAlive()) {
            if (withDebug)
                CefLog.Debug("isRunning: server process is not alive.");
            return false;
        }
        try {
            if (ThriftTransport.isTcp()) {
                // At first, we check whether the server socket is busy.
                if (!isServerSocketBusy(withDebug))
                    return false;
                // Well, socket is busy and server seems to be running. Let's try to connect to it.
            }

            if (!isConnectable(withDebug))
                return false;

            // Successfully connected to server transport => server seems to be running. Let's connect and check an echo.
            RpcExecutor test;
            try {
                test = new RpcExecutor().openTransport();
            } catch (TTransportException e) {
                if (withDebug)
                    CefLog.Debug("isRunning: TTransportException occurred when open server transport: %s", e.getMessage());
                return false;
            }
            String testMsg = "test_message786";
            String echoMsg = test.execObj(s -> s.echo(testMsg));
            test.closeTransport();
            final boolean result = echoMsg != null && echoMsg.equals(testMsg);
            if (!result)
                CefLog.Error("isRunning: cef_server seems to be running, but echo is incorrect: '%s' (original '%s')", echoMsg, testMsg);
            else if (withDebug)
                CefLog.Debug("isRunning: cef_server is running and echo is correct.");
            return result;
        } catch (Throwable e) {
            CefLog.Error("isRunning: exception %s", e.getMessage());
        }
        return false;
    }

    public static String getServerState() {
        try {
            RpcExecutor test = new RpcExecutor().openTransport();
            String state = test.execObj(s -> s.state());
            test.closeTransport();
            return state;
        } catch (TTransportException e) {
            return "stopped";
        }
    }

    // returns true when server was stopped successfully
    public static boolean stopAndWait(long timeoutMs) {
        CefLog.Debug("Stop running cef_server instance.");
        try {
            RpcExecutor test = new RpcExecutor().openTransport();
            String state = test.execObj(s -> s.state());
            CefLog.Debug("Server state before stop: %s", state);
            test.exec(s -> s.stop());
            test.closeTransport();
        } catch (TTransportException e) {
            CefLog.Debug("Exception when trying to stop server, err: %s", e.getMessage());
        }

        // Wait for stopping
        boolean stopped = waitForStopped(timeoutMs);
        if (!stopped) {
            CefLog.Error("Can't stop server in %d ms (process is %s)", timeoutMs, isProcessAlive() ? "alive" : "dead");
            CefLog.Debug("Server state: %s", getServerState());
            return false;
        }
        ourNativeServerProcess = null;
        return true;
    }

    public static boolean waitForRunning(long timeoutMs) {
        return waitFor(NativeServerManager::isRunning, timeoutMs, "starting");
    }

    public static boolean waitForStopped(long timeoutMs) {
        return waitFor(()->!isRunning(), timeoutMs, "stopping");
    }

    private static boolean waitFor(BooleanSupplier checker, long timeoutMs, String hint) {
        final long startNs = System.nanoTime();
        boolean success;
        do {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            CefLog.Debug("Waiting for server %s", hint);
            success = checker.getAsBoolean();
        } while (!success && (System.nanoTime() - startNs < timeoutMs*1000000));

        return success;
    }

    private static File getServerExe() {
        if (ALT_CEF_SERVER_PATH != null && !ALT_CEF_SERVER_PATH.trim().isEmpty())
            return new File(ALT_CEF_SERVER_PATH);

        ProcessHandle.Info i = ProcessHandle.current().info();
        String cmd = i.command().get();
        if (cmd == null || cmd.isEmpty()) {
            CefLog.Warn("Can't determine cef_server location via ProcessHandle (because the command is empty).");
            return findExeViaSystemProperty();
        }

        final boolean isJava = OS.isWindows() ? cmd.endsWith("java.exe") : cmd.endsWith("java");
        if (isJava) {
            File javabin = new File(cmd);
            if (!javabin.exists() || javabin.isDirectory()) {
                CefLog.Warn("Can't determine cef_server location via ProcessHandle (because calculated java.exe doesn't exist), cmd=%s");
                return findExeViaSystemProperty();
            }
            File result;
            if (OS.isMacintosh())
                result = new File(javabin.getParentFile().getParentFile().getParentFile(), "Frameworks/cef_server.app/Contents/MacOS/cef_server");
            else if (OS.isLinux())
                result = new File(javabin.getParentFile().getParentFile(), "lib/cef_server");
            else
                result = new File(javabin.getParentFile(), "cef_server.exe");
            if (!result.exists()) {
                CefLog.Warn("Can't determine cef_server location via ProcessHandle (because calculated path '%s' doesn't exist), cmd=%s", result.getAbsolutePath(), cmd);
                return findExeViaSystemProperty();
            }

            return result;
        }

        //
        // It seems that JVM is started via the native launcher.
        //

        File result = findExeViaSystemProperty();
        if (result != null) {
            CefLog.Debug("Java is started via native launcher. Found cef_server path %s (via system propety)", result.getAbsolutePath());
            return result;
        }

        // TODO: get path of loaded libjvm and calculate relative server path
        File launcher = new File(cmd);
        if (!launcher.exists()) {
            CefLog.Warn("Can't find cef_server in bundled jbr (launcher '%s' doesn't exist), cmd=%s", launcher.getAbsolutePath(), cmd);
            return null;
        }

        if (OS.isMacintosh())
            result = new File(launcher.getParentFile().getParentFile(), "jbr/Contents/Frameworks/cef_server.app/Contents/MacOS/cef_server");
        else if (OS.isLinux())
            result = new File(launcher.getParentFile().getParentFile(), "jbr/lib/cef_server");
        else
            result = new File(new File(new File(launcher.getParentFile().getParentFile(), "jbr"), "bin"), "cef_server.exe");

        if (!result.exists()) {
            CefLog.Warn("Can't find cef_server in bundled jbr (calculated path '%s' doesn't exist), cmd=%s", result.getAbsolutePath(), cmd);
            result = null;
        }
        CefLog.Debug("Java is started via native launcher. Found cef_server path %s (in bundled jbr)", result.getAbsolutePath());
        return result;

    }

    private static File findExeViaSystemProperty() {
        String javaPath = System.getProperty("java.home");
        if (javaPath == null || javaPath.isEmpty()) {
            CefLog.Error("Can't find cef_server binary: system property 'java.home' is empty.");
            return null;
        }
        CefLog.Debug("Find cef_server binary via system property 'java.home'=%s", javaPath);

        File javaDir = new File(javaPath);
        if (!javaDir.exists() || !javaDir.isDirectory()) {
            CefLog.Error("Can't find cef_server binary: java directory doesn't exist, 'java.home'=%s", javaPath);
            return null;
        }

        File result;
        if (OS.isMacintosh()) // javaPath points to Home: /Applications/IntelliJ IDEA Ultimate 2024.3 Nightly.app/Contents/jbr/Contents/Home
            result = new File(javaDir.getParentFile(), "Frameworks/cef_server.app/Contents/MacOS/cef_server");
        else if (OS.isLinux())
            result = new File(javaDir, "lib/cef_server");
        else
            result = new File(new File(javaDir, "bin"), "cef_server.exe");

        if (!result.exists()) {
            CefLog.Error("Can't find cef_server binary: file %s doesn't exist, 'java.home'=%s", result.getAbsolutePath(), javaPath);
            return null;
        }
        return result;
    }

    // returns true when server was started successfully
    private static boolean startProcessAndWait(String paramsPath, long timeoutMs) {
        if (ourNativeServerProcess != null)
            CefLog.Debug("Handle of server process will be overwritten.");
        ourNativeServerProcess = null;

        File serverExe = getServerExe();
        if (serverExe == null)
            return false;

        CefLog.Debug("Start native cef_server, path='%s', params path='%s'", serverExe.getAbsolutePath(), paramsPath);
        if (!serverExe.exists()) {
            CefLog.Error("Can't start native cef_server, file doesn't exist: %s", serverExe.getAbsolutePath());
            return false;
        }

        ProcessBuilder builder = new ProcessBuilder(serverExe.getAbsolutePath());
        CefLog.Debug("\tWorking dir %s", serverExe.getParentFile());
        builder.directory(serverExe.getParentFile());
        if (ThriftTransport.isTcp()) {
            CefLog.Debug("\tUse tcp-port %d", ThriftTransport.getServerPort());
            builder.command().add(String.format("--port=%d", ThriftTransport.getServerPort()));
        } else {
            CefLog.Debug("\tUse pipe %s", ThriftTransport.getServerPipe());
            builder.command().add(String.format("--pipe=%s", ThriftTransport.getServerPipe()));
        }
        final String serverLog = Utils.getString("CEF_SERVER_LOG_PATH");
        if (serverLog != null && !serverLog.isEmpty()) {
            CefLog.Debug("\tLog file %s", serverLog);
            builder.command().add(String.format("--logfile=%s", serverLog.trim()));
        }
        builder.command().add(String.format("--params=%s", paramsPath));
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
            ourNativeServerProcess = builder.start();
        } catch (IOException e) {
            CefLog.Error("Can't start native cef_server, exception: %s", e.getMessage());
            return false;
        }

        // Wait for native server
        boolean running = waitForRunning(timeoutMs);
        if (!running && !(running = isRunning(true))) {
            if (ourNativeServerProcess.isAlive())
                CefLog.Error("Native cef_server was started but client can't connect.");
            else {
                CefLog.Error("Can't start native cef_server, process is dead.");
                ourNativeServerProcess = null;
            }
        }
        return running;
    }
}
