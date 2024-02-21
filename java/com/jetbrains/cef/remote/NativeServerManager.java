package com.jetbrains.cef.remote;

import org.apache.thrift.transport.TServerSocket;
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

public class NativeServerManager {
    private static final String ALT_CEF_SERVER_PATH = Utils.getString("ALT_CEF_SERVER_PATH");

    private static Process ourNativeServerProcess = null;

    // Should be called in bg thread
    public static boolean startIfNecessary(CefAppHandler appHandler, CefSettings settings, long timeoutNs) {
        // TODO: check that server runs with the same args and restart it if necessary.
        if (isRunning())
            return true;

        final Path pipeName = Path.of(System.getProperty("java.io.tmpdir")).resolve("cef_server_params.txt");
        File f = new File(pipeName.toString());
        f.delete();
        PrintStream ps;
        try {
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
                for (String arg: commandLineArgs)
                    ps.printf("%s\n", arg);
        } else if (appHandler != null)
            CefLog.Error("Unsupported class of CefAppHandler %s. Overridden command-line arguments will be ignored.", CefAppHandler.class);

        // 2. settings
        ps.printf("[SETTINGS]:\n");
        if (settings != null) {
            Map<String, String> settingsMap = settings.toMap();
            for (Map.Entry entry : settingsMap.entrySet())
                ps.printf("%s=%s\n", entry.getKey(), entry.getValue());
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

        return startNativeServer(f.getAbsolutePath(), timeoutNs);
    }

    public static boolean isRunning() {
        return isRunning(false);
    }

    public static boolean isRunning(boolean withDebug) {
        try {
            if (ThriftTransport.USE_TCP) {
                TServerSocket serverSocket = null;
                try {
                    serverSocket = new TServerSocket(ThriftTransport.PORT_CEF_SERVER);
                } catch (TTransportException e) {
                    if (withDebug)
                        CefLog.Debug("isRunning: tcp-port %d, TTransportException occurred: %s", ThriftTransport.PORT_CEF_SERVER, e.getMessage());
                    // Socket is busy => server is running
                    return true;
                }
                serverSocket.close();
                return false;
            }

            UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(ThriftTransport.getServerPipe());
            try {
                SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                channel.connect(socketAddress);
                channel.close();
                // Successfully connected to server pipe => server is running
                return true;
            } catch (IOException e) {
                if (withDebug)
                    CefLog.Debug("isRunning: pipe '%s', IOException occurred: %s", e.getMessage(), ThriftTransport.getServerPipe());
                return false;
            }
        } catch (Throwable e) {
            CefLog.Error("isRunning: exception %s", e.getMessage());
        }
        return false;
    }

    public static boolean isRunningOld(boolean withDebug) {
        try {
            RpcExecutor test = new RpcExecutor().openTransport();
            String testMsg = "test_message786";
            String echoMsg = test.execObj(s -> s.echo(testMsg));
            test.closeTransport();
            final boolean result = echoMsg != null && echoMsg.equals(testMsg);
            if (!result)
                CefLog.Debug("cef_server is running, but echo is incorrect: '%s' (original '%s')", echoMsg, testMsg);
            return result;
        } catch (TTransportException e) {
            if (withDebug)
                CefLog.Debug("isRunning: TTransportException occurred: %s", e.getMessage());
        }
        return false;
    }

    public static void stopRunning() {
        try {
            RpcExecutor test = new RpcExecutor().openTransport();
            test.openTransport();
            test.exec(s -> s.stop());
            test.closeTransport();
        } catch (TTransportException e) {
            CefLog.Debug("cef_server ins't running, exception: %s", e.getMessage());
        }
    }

    private static String getServerState() {
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
    public static boolean stopAndWait(long timeoutNs) {
        if (!isRunning()) {
            ourNativeServerProcess = null;
            return true;
        }

        CefLog.Debug("Stop running cef_server instance.");
        stopRunning();
        // Wait for stopping
        final long startNs = System.nanoTime();
        boolean success = false;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            CefLog.Debug("Waiting for server stopping... State: %s", getServerState());
            success = !isRunning();
        } while (!success && (System.nanoTime() - startNs < timeoutNs));
        if (!success) {
            CefLog.Error("Can't stop server in %d ms.", (System.nanoTime() - startNs)/1000000);
            return false;
        }
        ourNativeServerProcess = null;
        return true;
    }

    // returns true when server was started successfully
    private static boolean startNativeServer(String paramsPath, long timeoutNs) {
        if (ourNativeServerProcess != null)
            CefLog.Debug("Handle of server process will be overwritten.");
        ourNativeServerProcess = null;

        File serverExe;
        if (ALT_CEF_SERVER_PATH == null || ALT_CEF_SERVER_PATH.isEmpty()) {
            ProcessHandle.Info i = ProcessHandle.current().info();
            File javabin = new File(i.command().get());
            if (OS.isMacintosh()) {
                serverExe = new File(
                        new File(javabin.getParentFile().getParentFile().getParentFile(), "Frameworks"),
                        "cef_server.app/Contents/MacOS/cef_server");
            } else if (OS.isLinux()) {
                serverExe = new File(new File(javabin.getParentFile(), "lib"), "cef_server");
            } else {
                serverExe = new File(new File(javabin.getParentFile(), "bin"), "cef_server.exe");
            }
        } else
            serverExe = new File(ALT_CEF_SERVER_PATH);

        CefLog.Debug("Start native cef_server, path='%s', params path='%s'", serverExe.getAbsolutePath(), paramsPath);
        if (!serverExe.exists()) {
            CefLog.Error("Can't start native cef_server, file doesn't exist: %s", serverExe.getAbsolutePath());
            return false;
        }

        ProcessBuilder builder = new ProcessBuilder(serverExe.getAbsolutePath());
        builder.directory(serverExe.getParentFile());
        if (ThriftTransport.USE_TCP) {
            CefLog.Debug("\tUse tcp-port %d", ThriftTransport.PORT_CEF_SERVER);
            builder.command().add(String.format("--port=%d", ThriftTransport.PORT_CEF_SERVER));
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

        // Check native server
        final long startNs = System.nanoTime();
        boolean success = false;
        do {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            CefLog.Debug("Waiting for server...");
            success = isRunning();
        } while (!success && (System.nanoTime() - startNs < timeoutNs));

        if (!isRunning(true)) {
            if (ourNativeServerProcess.isAlive())
                CefLog.Error("Native cef_server was started but client can't connect.");
            else {
                CefLog.Error("Can't start native cef_server, process is dead.");
                ourNativeServerProcess = null;
            }
        }
        return success;
    }
}