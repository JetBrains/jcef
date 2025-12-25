package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift.transport.TSocket;
import com.jetbrains.cef.remote.thrift.transport.TTransportException;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

public class NativeServerManager {
    public static final String ALT_CEF_SERVER_PATH = Utils.getString("ALT_CEF_SERVER_PATH");
    private static final boolean CHECK_PROCESS_ALIVE = Utils.getBoolean("JCEF_CHECK_PROCESS_ALIVE", true); // for debug, TODO: remove
    private static final int WAIT_LOOP_SLEEP_MS = Utils.getInteger("JCEF_WAIT_LOOP_SLEEP_MS", 200);

    public static boolean isProcessAlive(ThriftTransport thriftServer) {
        Process p = ServerStarter.ourNativeServerProcesses.get(thriftServer.toString());
        return p != null && p.isAlive();
    }

    public static boolean isConnectable(int port) {
        return isConnectable(new ThriftTransport(port), false);
    }

    public static boolean isConnectable(ThriftTransport thriftServer) {
        return isConnectable(thriftServer, false);
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
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(port, 1, InetAddress.getByName("localhost"));
            } catch (Throwable e) {
                if (withDebug)
                    CefLog.Debug("isServerSocketBusy: can't open tcp-port %d, exception occurred: %s", port, e.getMessage());
                return true;
            }
            if (withDebug)
                CefLog.Debug("isServerSocketBusy: tcp-port %d, successfully opened.", port);
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
        if (CHECK_PROCESS_ALIVE && ServerStarter.ourNativeServerProcesses.get(transport.toString()) != null && !ServerStarter.ourNativeServerProcesses.get(transport.toString()).isAlive()) {
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

    public static String getServerState(ThriftTransport thriftServer) {
        try {
            RpcExecutor test = new RpcExecutor().openTransport(thriftServer);
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
            CefLog.Debug("Server state: %s", getServerState(thriftServer));
            return false;
        }
        ServerStarter.ourNativeServerProcesses.remove(thriftServer.toString());
        return true;
    }

    public static class RunningServerInfo {
        public final ThriftTransport transport;
        public final int pid;
        public final int ppid; // parent process id

        public RunningServerInfo(ThriftTransport transport, int pid, int ppid) {
            this.transport = transport;
            this.pid = pid;
            this.ppid = ppid;
        }

        private static Map<Integer, String> ourPpid2Cmd = new ConcurrentHashMap<>();

        public String getParentProcessCmd() {
            if (ourPpid2Cmd.containsKey(ppid))
                return ourPpid2Cmd.get(ppid);

            final String cmd = "ps -o command -p " + ppid;
            try {
                Process process = new ProcessBuilder("bash", "-c", cmd).redirectErrorStream(true).start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("COMMAND"))
                            continue;
                        line = line.trim();
                        if (!line.isEmpty()) {
                            ourPpid2Cmd.put(ppid, line);
                            return line;
                        }
                    }
                }
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                System.err.println("Failed to execute command: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }
    public static List<RunningServerInfo> listRunningInstancesPorts() {
        if (OS.isLinux() || OS.isMacintosh()) {
            final ArrayList<RunningServerInfo> result = new ArrayList<>();
            final String cmd = "ps -Af | grep -E 'cef_server .*'";
            RunningServerInfo defaultArgsServer = null;

            try {
                Process process = new ProcessBuilder("bash", "-c", cmd).redirectErrorStream(true).start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    //   UID PID   PPID ....
                    //   501 80516 80489   0 Fri07AM ??         1:52.31 /Users/..../Contents/Frameworks/cef_server.app/Contents/MacOS/cef_server --port=6188 --logfile=/Users/.../jcef_80489.log --loglevel=5 --params=/var/folders/1k/hmmg06wx2bn4dwfq53wy6c_c0000gn/T/cef_server_params.txt
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("grep -E"))
                            continue;

                        line = line.trim();
                        final int posSp0 = line.indexOf(" ");
                        int posSp0End = posSp0 + 1;
                        while (posSp0End < line.length() && line.charAt(posSp0End) == ' ') ++posSp0End;
                        final int posSp1 = line.indexOf(" ", posSp0End);
                        int posSp1End = posSp1 + 1;
                        while (posSp1End < line.length() && line.charAt(posSp1End) == ' ') ++posSp1End;
                        final int posSp2 = line.indexOf(" ", posSp1End);
                        int posSp2End = posSp2 + 1;
                        while (posSp2End < line.length() && line.charAt(posSp2End) == ' ') ++posSp2End;

                        final int pid = Integer.parseInt(line.substring(posSp0End, posSp1));
                        final int ppid = Integer.parseInt(line.substring(posSp1End, posSp2));

                        final int pos0 = line.indexOf("--port=");
                        if (pos0 < 0) {
                            if (OS.isMacintosh() && line.contains("cef_server Helper"))
                                continue;
                            defaultArgsServer = new RunningServerInfo(new ThriftTransport(9999), pid, ppid);
                        } else {
                            final int pos1 = line.indexOf(" ", pos0 + 7);
                            String sport = line.substring(pos0 + 7, pos1);
                            try {
                                result.add(new RunningServerInfo(new ThriftTransport(Integer.parseInt(sport)), pid, ppid));
                            } catch (NumberFormatException e) {
                                System.out.println("Can't parse port number: " + sport);
                            }
                        }
                    }
                }

                process.waitFor();
            } catch (IOException | InterruptedException e) {
                System.err.println("Failed to execute command: " + e.getMessage());
                e.printStackTrace();
            }

            if (defaultArgsServer != null)
                result.add(defaultArgsServer);

            return result;
        }

        // Windows
        System.out.println("listAllRunningInstances: not implemented for Windows yet.");
        return new ArrayList<>();
    }

    public static List<RunningServerInfo> listRunningInstances() {
        final List<RunningServerInfo> running = new ArrayList<>();
        if (ThriftTransport.isTcpUsed()) {
            final List<RunningServerInfo> ports = listRunningInstancesPorts();
            if (!ports.isEmpty()) {
                CefLog.Debug("Found %d running instances (ports).", ports.size());
                for (RunningServerInfo s : ports)
                    running.add(s);
            }
        } else {
            File[] pipes = ThriftTransport.findPipes();
            if (pipes != null && pipes.length > 0) {
                CefLog.Debug("Found %d running instances (pipes).", pipes.length);
                for (File pipe : pipes)
                    running.add(new RunningServerInfo(new ThriftTransport(pipe), -1, -1));
            }
        }
        return running;
    }

    public static List<String> findRunningInstancesRoots() {
        final List<RunningServerInfo> running = listRunningInstances();
        if (running == null || running.isEmpty())
            return null;

        List<String> existingRoots = new ArrayList<>();
        for (RunningServerInfo server: running) {
            RpcExecutor exec = new RpcExecutor();
            try {
                exec.openPipeTransport(server.transport);
                String newRoot = exec.execObj(s -> s.getServerInfo("root"));
                if (newRoot != null) {
                    existingRoots.add(newRoot);
                    CefLog.Info("Found cef_server instance root_cache_path '%s' (transport=%s).", newRoot, server.transport);
                } else
                    CefLog.Debug("cef_server instance (transport=%s) returns null root", server.transport);
                exec.closeTransport();
            } catch (TTransportException e) {
                CefLog.Debug("getServerInfo (with transport '%s') failed with exception: %s", server.transport  , e.getMessage());
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

    public static boolean fixRootInSettings(CefSettings settings, String newRootDirName) {
        try {
            return fixRootInSettingsImpl(settings, newRootDirName);
        } catch (Throwable e) {
            CefLog.Error("Can't fix root_cache_path in settings: %s", e.getMessage());
        }
        return false;
    }

    private static boolean fixRootInSettingsImpl(CefSettings settings, String newRootDirName) {
        List<String> runningInstancesRoots = NativeServerManager.findRunningInstancesRoots();
        if (runningInstancesRoots == null || runningInstancesRoots.isEmpty())
            return false;

        if (settings.cache_path != null && !settings.cache_path.isEmpty()) {
            Path settingsRoot;
            try {
                settingsRoot = Path.of(settings.cache_path);
            } catch (InvalidPathException e) {
                CefLog.Error("Can't find path '%s': %s", settings.cache_path, e.getMessage());
                return false;
            }
            for (String sr : runningInstancesRoots) {
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
                    return true;
                }
            }
        } else {
            // settings.cache_path == null
            for (String sr: runningInstancesRoots) {
                if (NativeServerManager.isDefaultRoot(sr)) {
                    settings.cache_path = Path.of(System.getProperty("java.io.tmpdir")).resolve(newRootDirName).toString();
                    CefLog.Info("Empty settings.cache_path will be replaced with '%s' (because found CEF instance with system-default root_cache_path '%s')", settings.cache_path, sr);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean waitForRunning(ThriftTransport thriftServer, long timeoutMs) {
        return waitFor(() -> isRunning(thriftServer) != null, timeoutMs, thriftServer.toStringShort() + " starting");
    }

    public static boolean waitForStopped(ThriftTransport thriftServer, long timeoutMs) {
        return waitFor(() -> isRunning(thriftServer) == null, timeoutMs, thriftServer.toStringShort() + " stopping");
    }

    private static boolean waitFor(BooleanSupplier checker, long timeoutMs, String hint) {
        final long startNs = System.nanoTime();
        boolean success;
        do {
            try {
                Thread.sleep(WAIT_LOOP_SLEEP_MS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            CefLog.Debug("Waiting for server %s", hint);
            success = checker.getAsBoolean();
        } while (!success && (System.nanoTime() - startNs < timeoutMs*1000000));

        return success;
    }

    public static boolean isRemoteSupported() {
        File cef_server_exe = getServerExe();
        if (cef_server_exe == null) {
            // NOTE: cef_server can be manually started on custom port (for example, in debugging)
            return ThriftTransport.MANUAL_SERVER_SELECT || isRunning(ThriftTransport.ourDefaultServer) != null;
        }
        return cef_server_exe.exists() && !cef_server_exe.isDirectory();
    }

    static File getServerExe() {
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
                CefLog.Warn("Can't determine cef_server location via java-process path '%s' (because calculated path '%s' doesn't exist), cmd=%s", javabin.getAbsolutePath(), result.getAbsolutePath(), cmd);
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
            return null;
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

        File javaDir = new File(javaPath);
        if (!javaDir.exists() || !javaDir.isDirectory()) {
            CefLog.Error("Can't find cef_server binary via System.getProperty('java.home'): java directory doesn't exist, 'java.home'=%s", javaPath);
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
            CefLog.Debug("Can't find cef_server binary via System.getProperty('java.home'): file %s doesn't exist, 'java.home'=%s", result.getAbsolutePath(), javaPath);
            return null;
        }
        CefLog.Debug("Found cef_server binary '%s' via System.getProperty('java.home')=%s", result.getAbsolutePath(), javaPath);
        return result;
    }

    public static class ServerLogLevel {
        public final static int LEVEL_DISABLED = 100;
        public final static int LEVEL_FATAL = 10;
        public final static int LEVEL_ERROR = 9;
        public final static int LEVEL_WARN = 8;
        public final static int LEVEL_INFO = 7;
        public final static int LEVEL_DEBUG = 6;
        public final static int LEVEL_TRACE = 5;

        public static int cef2native(CefSettings.LogSeverity severity) {
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

        public static String nativeDesc(int level) {
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
            return "unknown_logging_level_" + level;
        }

        static String cef2native_str(CefSettings.LogSeverity severity) {
            if (severity == CefSettings.LogSeverity.LOGSEVERITY_DISABLE)
                return "disable";
            else if (severity == CefSettings.LogSeverity.LOGSEVERITY_DEFAULT)
                return "info";
            else if (severity == CefSettings.LogSeverity.LOGSEVERITY_FATAL)
                return "fatal";
            else if (severity == CefSettings.LogSeverity.LOGSEVERITY_ERROR)
                return "err";
            else if (severity == CefSettings.LogSeverity.LOGSEVERITY_WARNING)
                return "warn";
            else if (severity == CefSettings.LogSeverity.LOGSEVERITY_INFO)
                return "debug";
            else if (severity == CefSettings.LogSeverity.LOGSEVERITY_VERBOSE)
                return "verb";
            return "disable";
        }

        public static int str2native(String level) {
            if (level == null || level.isEmpty())
                return LEVEL_DISABLED;

            level = level.toLowerCase();
            if (level.contains("disable"))
                return LEVEL_DISABLED;
            if (level.contains("fatal"))
                return LEVEL_FATAL;
            if (level.contains("err"))
                return LEVEL_ERROR;
            if (level.contains("warn"))
                return LEVEL_WARN;
            if (level.contains("info"))
                return LEVEL_INFO;
            if (level.contains("debug"))
                return LEVEL_DEBUG;
            if (level.contains("trace"))
                return LEVEL_TRACE;

            return LEVEL_INFO;
        }
    }
}
