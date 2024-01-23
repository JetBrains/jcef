package com.jetbrains.cef.remote;

import org.apache.thrift.transport.TTransportException;
import org.cef.OS;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;

import java.io.File;
import java.io.IOException;

public class NativeServerManager {
    private static final String ALT_CEF_SERVER_PATH = Utils.getString("ALT_CEF_SERVER_PATH");
    private static final String CEF_SERVER_PIPE = Utils.getString("ALT_CEF_SERVER_PIPE", "cef_server_pipe");
    private static final long WAIT_SERVER_TIMEOUT = Utils.getInteger("WAIT_SERVER_TIMEOUT_MS", 5000)*1000000l;

    private static Process ourNativeServerProcess = null;

    // Should be called in bg thread
    public static boolean startIfNecessary() {
        return isRunning() ? true : startNativeServer();
    }

    private static boolean isRunning() {
        try {
            RpcExecutor test = new RpcExecutor(CEF_SERVER_PIPE);
            String testMsg = "test_message786";
            String echoMsg = test.execObj(s -> s.echo(testMsg));
            test.closeTransport();
            final boolean result = echoMsg != null && echoMsg.equals(testMsg);
            if (!result)
                CefLog.Debug("cef_server is running, but echo is incorrect: '%s' (original '%s')", echoMsg, testMsg);
            return result;
        } catch (TTransportException e) {
            CefLog.Debug("cef_server ins't running, exception: %s", e.getMessage());
        }
        return false;
    }

    private static boolean startNativeServer() {
        if (ourNativeServerProcess != null)
            return true;

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

        CefLog.Debug("Start native cef_server, path='%s'", serverExe.getAbsolutePath());
        if (!serverExe.exists()) {
            CefLog.Error("Can't start native cef_server, file doesn't exist: %s", serverExe.getAbsolutePath());
            return false;
        }

        ProcessBuilder builder = new ProcessBuilder(serverExe.getAbsolutePath());
        builder.directory(serverExe.getParentFile());
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
            success = isRunning();
        } while (!success && (System.nanoTime() - startNs < WAIT_SERVER_TIMEOUT));

        if (!success) {
            if (ourNativeServerProcess.isAlive())
                CefLog.Error("Native cef_server was started but client can't connect.");
            else
                CefLog.Error("Can't start native cef_server, process is dead.");
        }
        return success;
    }
}
