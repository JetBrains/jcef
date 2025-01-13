package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift.transport.TServerSocket;
import com.jetbrains.cef.remote.thrift.transport.TSocket;
import com.jetbrains.cef.remote.thrift.transport.TTransportException;
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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class NativeServerManager {
    private static final Boolean DISABLE_GPU = Utils.getBoolean("JCEF_DISABLE_GPU");
    private static final Boolean KILL_SERVER_ON_SHUTDOWN = Utils.getBoolean("JCEF_KILL_SERVER_ON_SHUTDOWN");
    private static final String ALT_CEF_SERVER_PATH = Utils.getString("ALT_CEF_SERVER_PATH");
    private static final String ALT_SUBPROCESS_PATH = Utils.getString("ALT_SUBPROCESS_PATH");
    private static final boolean CHECK_PROCESS_ALIVE = Utils.getBoolean("JCEF_CHECK_PROCESS_ALIVE", true); // for debug, TODO: remove

    private static Map<String, Process> ourNativeServerProcesses = new HashMap<>();

    static {
        if (KILL_SERVER_ON_SHUTDOWN) {
            CefLog.Debug("All cef_server instances will be killed at JVM exit.");
            Thread task = new Thread(() -> {
                for (String servTransport: ourNativeServerProcesses.keySet()) {
                    Process p = ourNativeServerProcesses.get(servTransport);
                    if (p != null) {
                        p.destroyForcibly();
                        CefLog.Debug("Killed cef_server process [%s].", servTransport);
                    }
                }
            });
            Runtime.getRuntime().addShutdownHook(task);
        }
    }

    // Should be called in bg thread
    public static boolean startProcessAndWait(ThriftTransport thriftServer, CefAppHandler appHandler, CefSettings settings, long timeoutMs) {
        final long t0 = System.nanoTime();
        final Path settingsFileName = Path.of(System.getProperty("java.io.tmpdir")).resolve("cef_server_params.txt");
        File f = new File(settingsFileName.toString());
        PrintStream ps;
        try {
            new FileOutputStream(f).close(); // delete the content of the file
            f.createNewFile();
            ps = new PrintStream(new FileOutputStream(f, false));
        } catch (IOException e) {
            CefLog.Error("Can't create temp file with server params path=%s, msg=%s", settingsFileName.toString(), e.getMessage());
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

        CefLog.Debug("Settings were written to file, spent %d mcs", (System.nanoTime() - t0)/1000);

        // Select log path
        String serverLogPath = Utils.getString("CEF_SERVER_LOG_PATH");
        if (serverLogPath == null || serverLogPath.trim().isEmpty())
            serverLogPath = settings.log_file;

        // Select log level
        int serverLogLevel = Utils.getInteger("CEF_SERVER_LOG_LEVEL", -1);
        if (serverLogLevel == -1)
            serverLogLevel = ServerLogLevel.cef2native(settings.log_severity);

        return startProcessAndWait(thriftServer, f.getAbsolutePath(), timeoutMs, serverLogPath, serverLogLevel);
    }

    public static boolean isProcessAlive(ThriftTransport thriftServer) {
        Process p = ourNativeServerProcesses.get(thriftServer.toString());
        return p != null && p.isAlive();
    }

    private static boolean isConnectable(ThriftTransport thriftServer, boolean withDebug) {
        try {
            if (thriftServer.isTcp()) {
                try {
                    TSocket socket = new TSocket("localhost", thriftServer.getPort());
                    socket.open();
                    socket.close();
                    if (withDebug)
                        CefLog.Debug("isConnectable: tcp-port %d, opened and connected.", thriftServer.getPort());
                    return true;
                } catch (TTransportException e) {
                    if (withDebug)
                        CefLog.Debug("isConnectable: tcp-port %d, TTransportException occurred: %s", thriftServer.getPort(), e.getMessage());
                }
                return false;
            }
            try {
                if (OS.isWindows()) {
                    WindowsPipeSocket pipe = new WindowsPipeSocket(thriftServer.getPipe());
                    pipe.close();
                    if (withDebug)
                        CefLog.Debug("isConnectable: win-pipe '%s', opened and connected.", thriftServer.getPipe());
                    return true;
                }
                UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(thriftServer.getPipe());
                SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                channel.connect(socketAddress);
                channel.close();
                if (withDebug)
                    CefLog.Debug("isConnectable: pipe '%s', opened and connected.", thriftServer.getPipe());
                return true;
            } catch (IOException e) {
                if (withDebug)
                    CefLog.Debug("isConnectable: pipe '%s', IOException occurred: %s", thriftServer.getPipe(), e.getMessage());
            }
        } catch (Throwable e) {
            CefLog.Error("isConnectable: exception %s", e.getMessage());
        }
        return false;
    }

    private static boolean isServerSocketBusy(int port, boolean withDebug) {
        try {
            TServerSocket serverSocket = null;
            try {
                serverSocket = new TServerSocket(port);
            } catch (TTransportException e) {
                if (withDebug)
                    CefLog.Debug("isServerTransportBusy: tcp-port %d, TTransportException occurred: %s", port, e.getMessage());
                return true;
            }
            if (withDebug)
                CefLog.Debug("isServerTransportBusy: tcp-port %d, opened and connected.", port);
            serverSocket.close();
        } catch (Throwable e) {
            CefLog.Error("isServerSocketBusy: exception %s", e.getMessage());
        }
        return false;
    }

    public static String isRunning(ThriftTransport thriftServer) {
        return isRunning(thriftServer, false);
    }

    // returns root_cache_path of running server (or null if not running)
    public static String isRunning(ThriftTransport transport, boolean withDebug) {
        if (CHECK_PROCESS_ALIVE && ourNativeServerProcesses.get(transport.toString()) != null && !ourNativeServerProcesses.get(transport.toString()).isAlive()) {
            if (withDebug)
                CefLog.Debug("isRunning: server process is not alive.");
            return null;
        }
        try {
            if (transport.isTcp()) {
                // At first, we check whether the server socket is busy.
                if (!isServerSocketBusy(transport.getPort(), withDebug))
                    return null;
                // Well, socket is busy and server seems to be running. Let's try to connect to it.
            }

            if (!isConnectable(transport, withDebug))
                return null;

            // Successfully connected to server transport => server seems to be running. Let's connect and check an echo.
            RpcExecutor test;
            try {
                test = new RpcExecutor().openTransport(transport);
            } catch (TTransportException e) {
                if (withDebug)
                    CefLog.Debug("isRunning: TTransportException occurred when open server transport: %s", e.getMessage());
                return null;
            }
            String testMsg = "test_message786";
            String echoMsg = test.execObj(s -> s.echo(testMsg));
            String root = null;
            final boolean isEchoCorrect = echoMsg != null && echoMsg.equals(testMsg);
            if (!isEchoCorrect)
                CefLog.Error("isRunning: cef_server seems to be running, but echo is incorrect: '%s' (original '%s')", echoMsg, testMsg);
            else {
                root = test.execObj(s -> s.getServerInfo("root"));
                if (withDebug)
                    CefLog.Debug("isRunning: cef_server is running and echo is correct, root='%s'", root);
            }
            test.closeTransport();
            return isEchoCorrect ? root : null;
        } catch (Throwable e) {
            CefLog.Error("isRunning: exception %s", e.getMessage());
        }
        return null;
    }

    public static String getServerState() {
        try {
            RpcExecutor test = new RpcExecutor().openTransport(ThriftTransport.ourDefaultServer);
            String state = test.execObj(s -> s.getServerInfo("state"));
            test.closeTransport();
            return state;
        } catch (TTransportException e) {
            return "stopped";
        }
    }

    // returns true when server was stopped successfully
    public static boolean stopAndWait(ThriftTransport thriftServer, long timeoutMs) {
        CefLog.Debug("Stop running cef_server instance.");
        try {
            RpcExecutor test = new RpcExecutor().openTransport(thriftServer);
            String state = test.execObj(s -> s.getServerInfo("state"));
            CefLog.Debug("Server state before stop: %s", state);
            test.exec(s -> s.stop());
            test.closeTransport();
        } catch (TTransportException e) {
            CefLog.Debug("Exception when trying to stop server, err: %s", e.getMessage());
        }

        // Wait for stopping
        boolean stopped = waitForStopped(thriftServer, timeoutMs);
        if (!stopped) {
            CefLog.Error("Can't stop server in %d ms (process is %s)", timeoutMs, isProcessAlive(thriftServer) ? "alive" : "dead");
            CefLog.Debug("Server state: %s", getServerState());
            return false;
        }
        ourNativeServerProcesses.remove(thriftServer.toString());
        return true;
    }

    public static List<String> findRoots() {
        if (ThriftTransport.isTcpUsed()) {
            CefLog.Warn("Try implement findRoots for tcp transport.");
            return null;
        }
        List<String> existingRoots = new ArrayList<>();
        File[] pipes = ThriftTransport.findPipes();
        if (pipes != null && pipes.length > 0) {
            CefLog.Debug("Found %d pipes.", pipes.length);
            for (File pipe: pipes) {
                RpcExecutor exec = new RpcExecutor();
                try {
                    exec.openPipeTransport(new ThriftTransport(pipe));
                    String newRoot = exec.execObj(s -> s.getServerInfo("root"));
                    existingRoots.add(newRoot);
                    CefLog.Info("Found cef_server instance root_cache_path '%s' (pipe=%s).", newRoot, pipe.getName());
                    exec.closeTransport();
                } catch (TTransportException e) {
                    CefLog.Debug("getServerInfo (with pipe '%s') failed with exception: %s", pipe.getAbsolutePath(), e.getMessage());
                }
            }
        }

        return existingRoots.isEmpty() ? null : existingRoots;
    }

    private static boolean isDefaultRoot(String rootPath) {
        if (OS.isWindows())
            return rootPath.compareToIgnoreCase("~\\AppData\\Local\\CEF\\User Data") == 0;
        if (OS.isLinux())
            return rootPath.compareToIgnoreCase("~/.config/cef_user_data") == 0;
        return rootPath.compareToIgnoreCase("~/Library/Application Support/CEF/User Data") == 0;
    }

    public static void fixRootInSettings(CefSettings settings, String newRootDirName) {
        if (ThriftTransport.isTcpUsed()) {
            settings.cache_path = Path.of(System.getProperty("java.io.tmpdir")).resolve(newRootDirName).toString();
            CefLog.Info("settings.cache_path will be replaced with '%s' (because root search isn't implemented for TCP transport)", settings.cache_path);
            return;
        }
        List<String> existingRoots = NativeServerManager.findRoots();
        if (existingRoots == null || existingRoots.isEmpty())
            return;

        if (settings.cache_path != null && !settings.cache_path.isEmpty()) {
            Path settingsRoot;
            try {
                settingsRoot = Path.of(settings.cache_path);
            } catch (InvalidPathException e) {
                CefLog.Error("Can't find path '%s': %s", settings.cache_path, e.getMessage());
                return;
            }
            for (String sr : existingRoots) {
                Path r;
                try {
                    r = Path.of(sr);
                } catch (InvalidPathException e) {
                    CefLog.Error("Can't find path '%s': %s", sr, e.getMessage());
                    continue;
                }
                if (r.equals(settingsRoot)) {
                    settings.cache_path = Path.of(System.getProperty("java.io.tmpdir")).resolve(newRootDirName).toString();
                    CefLog.Info("Non-empty settings.cache_path='%s' conflicts with existing root_cache_path, will be replaced with '%s'.", r, settings.cache_path);
                    break;
                }
            }
        } else {
            // settings.cache_path == null
            for (String sr: existingRoots) {
                if (NativeServerManager.isDefaultRoot(sr)) {
                    settings.cache_path = Path.of(System.getProperty("java.io.tmpdir")).resolve(newRootDirName).toString();
                    CefLog.Info("Empty settings.cache_path will be replaced with '%s' (because found CEF instance with system-default root_cache_path '%s')", settings.cache_path, sr);
                    break;
                }
            }
        }
    }

    public static boolean waitForRunning(ThriftTransport thriftServer, long timeoutMs) {
        return waitFor(() -> isRunning(thriftServer) != null, timeoutMs, "starting");
    }

    public static boolean waitForStopped(ThriftTransport thriftServer, long timeoutMs) {
        return waitFor(() -> isRunning(thriftServer) == null, timeoutMs, "stopping");
    }

    private static boolean waitFor(BooleanSupplier checker, long timeoutMs, String hint) {
        final long startNs = System.nanoTime();
        boolean success;
        do {
            try {
                Thread.sleep(100);
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
    private static boolean startProcessAndWait(ThriftTransport thriftServer, String paramsPath, long timeoutMs, String logPath, int logLevel) {
        final long t0 = System.nanoTime();
        if (ourNativeServerProcesses.get(thriftServer.toString()) != null)
            CefLog.Debug("Handle of server process will be overwritten.");
        ourNativeServerProcesses.remove(thriftServer.toString());

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
        if (thriftServer.isTcp()) {
            CefLog.Debug("\tUse tcp-port %d", thriftServer.getPort());
            builder.command().add(String.format("--port=%d", thriftServer.getPort()));
        } else {
            CefLog.Debug("\tUse pipe %s", thriftServer.getPipe());
            builder.command().add(String.format("--pipe=%s", thriftServer.getPipe()));
        }
        if (logPath != null && !logPath.isEmpty()) {
            CefLog.Debug("\tLog file %s", logPath);
            builder.command().add(String.format("--logfile=%s", logPath.trim()));
        }

        CefLog.Debug("\tLog level %s [%d]", ServerLogLevel.nativeDesc(logLevel), logLevel);
        builder.command().add(String.format("--loglevel=%d", logLevel));

        if (System.getenv().containsKey("DEBUG_CEF_SERVER")) {
            builder.command().add("--cef-server-wait-debugger");
        }

        builder.command().add(String.format("--params=%s", paramsPath));
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process p;
        try {
            p = builder.start();
            ourNativeServerProcesses.put(thriftServer.toString(), p);
        } catch (IOException e) {
            CefLog.Error("Can't start native cef_server, exception: %s", e.getMessage());
            return false;
        }

        // Wait for native server
        final long t1 = System.nanoTime();
        boolean running = waitForRunning(thriftServer, timeoutMs);
        if (!running && !(running = (isRunning(thriftServer, true) != null))) {
            if (p.isAlive())
                CefLog.Error("Native cef_server was started but client can't connect.");
            else {
                CefLog.Error("Can't start native cef_server, process is dead.");
                ourNativeServerProcesses.remove(thriftServer.toString());
            }
        }
        CefLog.Debug("\t spent mcs: process starting %d, waiting %d", (t1 - t0)/1000, (System.nanoTime() - t1)/1000);
        return running;
    }

    private static class ServerLogLevel {
        final static int LEVEL_DISABLED = 100;
        final static int LEVEL_FATAL = 10;
        final static int LEVEL_ERROR = 9;
        final static int LEVEL_WARN = 8;
        final static int LEVEL_INFO = 7;
        final static int LEVEL_DEBUG = 6;
        final static int LEVEL_TRACE = 5;

        static int cef2native(CefSettings.LogSeverity severity) {
            if (severity == CefSettings.LogSeverity.LOGSEVERITY_DISABLE)
                return LEVEL_DISABLED;
            else if (severity == CefSettings.LogSeverity.LOGSEVERITY_DEFAULT)
                return LEVEL_INFO;
            else if (severity == CefSettings.LogSeverity.LOGSEVERITY_FATAL)
                return LEVEL_FATAL;
            else if (severity == CefSettings.LogSeverity.LOGSEVERITY_ERROR)
                return LEVEL_ERROR;
            else if (severity == CefSettings.LogSeverity.LOGSEVERITY_WARNING)
                return LEVEL_WARN;
            else if (severity == CefSettings.LogSeverity.LOGSEVERITY_INFO)
                return LEVEL_DEBUG;
            else if (severity == CefSettings.LogSeverity.LOGSEVERITY_VERBOSE)
                return LEVEL_TRACE;
            return LEVEL_DISABLED;
        }

        static String nativeDesc(int level) {
            if (level == LEVEL_DISABLED)
                return "disabled";
            if (level == LEVEL_FATAL)
                return "fatal";
            if (level == LEVEL_ERROR)
                return "error";
            if (level == LEVEL_WARN)
                return "warn";
            if (level == LEVEL_INFO)
                return "info";
            if (level == LEVEL_DEBUG)
                return "debug";
            if (level == LEVEL_TRACE)
                return "trace";
            return "unknown";
        }
    }
}
