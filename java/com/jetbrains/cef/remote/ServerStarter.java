package com.jetbrains.cef.remote;

import org.cef.CefSettings;
import org.cef.OS;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandler;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ServerStarter {
    private static final Boolean KILL_SERVER_ON_SHUTDOWN = Utils.getBoolean("JCEF_KILL_SERVER_ON_SHUTDOWN");
    private static final Boolean DISABLE_GPU = Utils.getBoolean("JCEF_DISABLE_GPU");
    private static final int WAIT_START_LOOP_SLEEP_MS = Utils.getInteger("JCEF_WAIT_START_LOOP_SLEEP_MS", 200);

    static Map<String, Process> ourNativeServerProcesses = new HashMap<>();

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
    public static boolean startProcessAndWait(ThriftTransport thriftServer, CefAppHandler appHandler, String[] args, CefSettings settings, boolean deleteRootDir, long timeoutMs) {
        // Select log path
        String serverLogPath = Utils.getString("CEF_SERVER_LOG_PATH");
        if (serverLogPath == null || serverLogPath.trim().isEmpty())
            serverLogPath = CefLog.GetFilePath();

        // Select log level
        String serverLogLevel = Utils.getString("CEF_SERVER_LOG_LEVEL");
        if (serverLogLevel == null)
            serverLogLevel = NativeServerManager.ServerLogLevel.cef2native_str(CefLog.GetLogLevel());
        else {
            try {
                final int nLogLevel = Integer.parseInt(serverLogLevel);
                serverLogLevel = NativeServerManager.ServerLogLevel.nativeDesc(nLogLevel);
            } catch (NumberFormatException e) {
            }
        }

        return startProcessAndWait(NativeServerManager.getServerExe(), thriftServer, appHandler, args, settings, serverLogPath, serverLogLevel, deleteRootDir, timeoutMs);
    }

    // Should be called in bg thread
    public static boolean startProcessAndWait(File serverExe, ThriftTransport thriftServer, CefAppHandler appHandler, String[] args, CefSettings settings, String logPath, String logLevel, boolean deleteRootDir, long timeoutMs) {
        Integer exitVal = startAndWait(serverExe, thriftServer, appHandler, args, settings, logPath, logLevel, deleteRootDir, timeoutMs);
        if (exitVal != null) {
            if (exitVal == 101) {
                // CefInitialize returns false. Probably, JCEF cache dir is locked.
                final SimpleDateFormat f = new SimpleDateFormat("hh_mm_ss_SSS");
                final String newCacheDir = Path.of(System.getProperty("java.io.tmpdir")).resolve("cef_cache_" + thriftServer.toStringShort() + "_" + f.format(new Date())).toString();
                CefLog.Info("Try to restart cef_server with another cache_dir '%s'.", newCacheDir);
                settings.cache_path = newCacheDir;
                exitVal = startAndWait(serverExe, thriftServer, appHandler, args, settings, logPath, logLevel, true, timeoutMs);
            }
        }

        return exitVal == null;
    }

    // Returns:
    // null when the process has been started successfully
    // Integer.MIN_VALUE when can't start process because of IO-errors
    // exit code, otherwise
    private static Integer startAndWait(File serverExe, ThriftTransport thriftServer, CefAppHandler appHandler, String[] args, CefSettings settings, String logPath, String logLevel, boolean deleteRootDir, long timeoutMs) {
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
            return Integer.MIN_VALUE;
        }

        // 1. command line args
        final String sectionCmdLine = "[COMMAND_LINE]:";
        ps.printf("%s\n", sectionCmdLine);
        if (args != null && args.length > 0)
            for (String arg: args) {
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

        if (OS.isMacintosh() && serverExe != null) {
            File subprocess = new File(serverExe.getParentFile().getParentFile(), "Frameworks/cef_server Helper.app/Contents/MacOS/cef_server Helper");
            ps.printf("browser_subprocess_path=%s\n", subprocess.getAbsolutePath());
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
        CefLog.Info("Start native cef_server with cache path: %s", settings.cache_path);

        return startAndWait(serverExe, thriftServer, f.getAbsolutePath(), timeoutMs, logPath, logLevel, deleteRootDir);
    }

    // Returns:
    // null when the process has been started successfully
    // Integer.MIN_VALUE when can't start process because of IO-errors
    // exit code, otherwise
    private static Integer startAndWait(File serverExe, ThriftTransport thriftServer, String paramsPath, long timeoutMs, String logPath, String logLevel, boolean deleteRootDir) {
        final long t0 = System.nanoTime();
        if (ourNativeServerProcesses.get(thriftServer.toString()) != null)
            CefLog.Debug("Handle of server process will be overwritten.");
        ourNativeServerProcesses.remove(thriftServer.toString());

        if (serverExe == null)
            return Integer.MIN_VALUE;

        CefLog.Debug("cef_server executable path='%s', params path='%s'", serverExe.getAbsolutePath(), paramsPath);
        if (!serverExe.exists()) {
            CefLog.Error("Can't start native cef_server, file doesn't exist: %s", serverExe.getAbsolutePath());
            return Integer.MIN_VALUE;
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
        String logStream = "stderr";
        if (logPath != null && !logPath.isEmpty()) {
            logStream = "file '" + logPath + "'";
            builder.command().add(String.format("--logfile=%s", logPath.trim()));
        }

        CefLog.Info("Native server logging: level '%s', stream: '%s'", logLevel, logStream);
        builder.command().add(String.format("--loglevel=%s", logLevel));

        if (System.getenv().containsKey("DEBUG_CEF_SERVER")) {
            builder.command().add("--cef-server-wait-debugger");
        }

        if (deleteRootDir)
            builder.command().add("--deleteRootCacheDir");

        builder.command().add(String.format("--params=%s", paramsPath));
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process p;
        try {
            p = builder.start();
            ourNativeServerProcesses.put(thriftServer.toString(), p);
        } catch (Throwable e) {
            CefLog.Error("Can't start native cef_server, exception: %s", e.getMessage());
            return Integer.MIN_VALUE;
        }

        // Wait for native server
        Integer exitVal = null;
        boolean running = false;
        final long t1 = System.nanoTime();
        do {
            try {
                Thread.sleep(WAIT_START_LOOP_SLEEP_MS);
            } catch (InterruptedException e) {
                CefLog.Error("Exception during waiting for native cef_server: %s", e.getMessage());
            }
            CefLog.Debug("Waiting for server %s starting...", thriftServer.toStringShort());
            // 1. Check process exit values.
            try {
                exitVal = p.exitValue();
            } catch (IllegalThreadStateException e) {}

            if (exitVal != null) {
                CefLog.Error("Native cef_server exited with code %d", exitVal);
                if (exitVal == 100) {
                    CefLog.Error("It means that cef_server can't load CEF framework library.");
                } else if (exitVal == 101) {
                    CefLog.Error("It means that CefInitialize returns false - probably, JCEF cache dir is locked.");
                    // TODO: search stdout for string 'Opening in existing browser session'
                }

                ourNativeServerProcesses.remove(thriftServer.toString());
                return exitVal;
            }

            // 2. Try to connect with cef_server.
            running = NativeServerManager.isRunning(thriftServer) != null;
        } while (!running && (System.nanoTime() - t1 < timeoutMs*1000000));

        // Check whether the server is running or not.
        if (!running && !(running = (NativeServerManager.isRunning(thriftServer, true) != null))) {
            if (p.isAlive())
                CefLog.Error("Native cef_server was started but client can't connect.");
            else {
                CefLog.Error("Can't start native cef_server, process is dead.");
                ourNativeServerProcesses.remove(thriftServer.toString());
            }
            try {
                exitVal = p.exitValue();
            } catch (IllegalThreadStateException e) {}
        } else
            CefLog.Debug("Server is started. Spent ms: process starting %d, waiting %d", (t1 - t0)/1000000, (System.nanoTime() - t1)/1000000);
        return running ? null : exitVal;
    }
}
