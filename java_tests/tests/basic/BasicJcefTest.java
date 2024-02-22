package tests.basic;

import com.jetbrains.cef.JCefAppConfig;
import com.jetbrains.cef.remote.*;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TTransportException;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefAppHandler;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.misc.CefLog;
import org.cef.misc.Utils;
import org.cef.network.CefRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tests.OsrSupport;
import tests.junittests.LoggingLifeSpanHandler;
import tests.junittests.LoggingLoadHandler;
import tests.junittests.TestSetupExtension;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BasicJcefTest {
    private static final boolean SKIP_BASIC_CHECK = Utils.getBoolean("JCEF_TESTS_SKIP_BASIC_CHECK");
    private static final boolean BASIC_CHECK_WITHOUT_UI = Utils.getBoolean("JCEF_TESTS_BASIC_CHECK_WITHOUT_UI");
    private static final long WAIT_TIMEOUT_MS = Utils.getInteger("WAIT_SERVER_TIMEOUT_MS", 25000); // 25 sec
    private static final String TCP_KEY = "CEF_SERVER_USE_TCP";

    static {
        CefLog.init(Utils.getString("JCEF_TESTS_LOG_FILE"), CefSettings.LogSeverity.LOGSEVERITY_VERBOSE);
    }

    @Test
    @Order(1)
    void testServerManagerPipe() {
        if (SKIP_BASIC_CHECK || !CefApp.isRemoteEnabled())
            return;

        final String isTcpPrev = System.getProperty(TCP_KEY);
        System.setProperty(TCP_KEY, "false");
        try {
            CefLog.Info("Test NativeServerManager with PIPE transport (timeout=%d ms).", WAIT_TIMEOUT_MS);
            testServerManagerImpl(WAIT_TIMEOUT_MS, true);
            testServerManagerImpl(WAIT_TIMEOUT_MS, false);
        } finally {
            if (isTcpPrev != null && !isTcpPrev.isEmpty())
                System.setProperty(TCP_KEY, isTcpPrev);
            else
                System.clearProperty(TCP_KEY);
        }
    }

    @Test
    @Order(2)
    void testServerManagerTcp() {
        if (SKIP_BASIC_CHECK || !CefApp.isRemoteEnabled())
            return;

        final String isTcpPrev = System.getProperty(TCP_KEY);
        System.setProperty(TCP_KEY, "true");
        try {
            CefLog.Info("Test NativeServerManager with TCP transport (timeout=%d ms).", WAIT_TIMEOUT_MS);
            testServerManagerImpl(WAIT_TIMEOUT_MS, true);
            testServerManagerImpl(WAIT_TIMEOUT_MS, false);
        } finally {
            if (isTcpPrev != null && !isTcpPrev.isEmpty())
                System.setProperty(TCP_KEY, isTcpPrev);
            else
                System.clearProperty(TCP_KEY);
        }
    }

    void testServerManagerImpl(long waitTimeoutMs, boolean testStopManually) {
        if (NativeServerManager.isRunning()) {
            CefLog.Info("Old cef_server instance is running, will stop.");
            boolean success = NativeServerManager.stopAndWait(waitTimeoutMs);
            if (!success)
                throw new AssertionError("Can't stop old server instance.");
        }

        CefLog.Info("Start new instance of cef_server");
        JCefAppConfig config = JCefAppConfig.getInstance();
        List<String> appArgs = config.getAppArgsAsList();
        if (OS.isLinux())
            appArgs.add("--password-store=basic");
        CefSettings settings = config.getCefSettings();
        settings.windowless_rendering_enabled = true;
        settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;
        settings.no_sandbox = true;
        CefAppHandler appHandler = new CefAppHandlerAdapter(appArgs.toArray(new String[0])){};
        boolean started = NativeServerManager.startProcessAndWait(appHandler, settings, waitTimeoutMs);
        if (!started)
            throw new AssertionError("Can't start server.");
        if (!NativeServerManager.isProcessAlive())
            throw new AssertionError("Server process is dead.");
        if (!NativeServerManager.isRunning(true))
            throw new AssertionError("Server isn't running.");

        //
        // Server is running now
        //
        if (testStopManually) {
            CefLog.Info("Server is running, try to stop it now (via rpc 'stop').");
            final boolean stopped = NativeServerManager.stopAndWait(waitTimeoutMs);
            if (!stopped) {
                CefLog.Debug("Can't stop server, additional debug:");
                if (NativeServerManager.isProcessAlive())
                    CefLog.Debug("\t server process is alive.");
                CefLog.Debug("\t isRunning returns %s.", String.valueOf(NativeServerManager.isRunning(true)));
                throw new AssertionError("Can't stop server.");
            }
        } else {
            CefLog.Info("Server is running, try to stop it now via master client.");
            CountDownLatch testServiceFinished = new CountDownLatch(1);
            TServer dummy = CefServer.startTestHandlersService(testServiceFinished);
            if (dummy == null)
                throw new AssertionError("Can't start test java-handlers service.");
            try {
                RpcExecutor test = new RpcExecutor();
                CefLog.Info("Test 'slave' connection.");
                try {
                    test.openTransport();
                    int cid = test.connect(false);
                    if (cid < 0)
                        throw new AssertionError("'connect' returns invalid cid=" + cid);
                } catch (TTransportException e) {
                    throw new AssertionError("Can't open transport for rpc-connection, err: " + e.getMessage());
                } finally {
                    test.closeTransport();
                }

                try { // Wait a little
                    Thread.sleep(500);
                } catch (InterruptedException e) {}

                boolean running = NativeServerManager.waitForRunning(2000);
                if (!running)
                    throw new AssertionError("Server was stopped after slave-client disconnected.");

                CefLog.Info("Test 'master' connection.");
                test = new RpcExecutor();
                try {
                    test.openTransport();
                    int cid = test.connect(true);
                    if (cid < 0)
                        throw new AssertionError("'connect' returns invalid cid=" + cid);
                } catch (TTransportException e) {
                    throw new AssertionError("Can't open transport for rpc-connection, err: " + e.getMessage());
                } finally {
                    test.closeTransport();
                }

                boolean stopped = NativeServerManager.waitForStopped(5000);
                if (!stopped) {
                    NativeServerManager.isRunning(true); // just for debug logging
                    throw new AssertionError("Server wasn't stopped after last master-client disconnected.");
                }
            } finally {
                dummy.stop();
                try {
                    if (!testServiceFinished.await(5, TimeUnit.SECONDS))
                        throw new AssertionError("Test java-handlers service wasn't stopped in 5 seconds.");
                } catch (InterruptedException e) {}
            }
        }

        //
        // Server was stopped
        //
        if (NativeServerManager.isProcessAlive())
            throw new AssertionError("Server process is alive.");
        if (NativeServerManager.isRunning(true))
            throw new AssertionError("Server is still running.");

        CefLog.Info("Server was successfully stopped.");
    }

    @Test
    @Order(3)
    void testBrowserCreation() {
        if (SKIP_BASIC_CHECK)
            return;

        final long start = System.currentTimeMillis();
        TestSetupExtension.initializeCef();

        //
        // 0. Wait CefApp intialization
        //
        CountDownLatch onAppInitialization_ = new CountDownLatch(1);
        CefApp.getInstance().onInitialization(state -> {
            if (state == CefApp.CefAppState.INITIALIZED) {
                onAppInitialization_.countDown();
                CefLog.Info("CefApp successfully initialized, spent %d ms", System.currentTimeMillis() - start);
            }
        });
        _wait(onAppInitialization_, 5, "CefApp wasn't initialized");

        CefLog.Info("Sequentially test basic JCEF functionality");
        final long time0 = System.currentTimeMillis();

        //
        // 1. Create client (CefApp.initialize will be invoked inside). Then setup client for testing (with use of
        //    latches in basic handlers)
        //
        CefClient client = CefApp.getInstance().createClient();
        final long time1 = System.currentTimeMillis();
        CefLog.Info("CefApp.getInstance().createClient() spent %d ms, created test client: %s", time1 - time0, client.getInfo());

        // Check correct disposing
        CountDownLatch clientDispose_ = new CountDownLatch(1);
        client.setOnDisposeCallback(()->clientDispose_.countDown());

        // Check CefLifeSpanHandler
        long[] onAfterCreatedTime = new long[]{-1};
        CountDownLatch onAfterCreated_ = new CountDownLatch(1);
        CountDownLatch onBeforeClose_ = new CountDownLatch(1);
        client.addLifeSpanHandler(new LoggingLifeSpanHandler(CefSettings.LogSeverity.LOGSEVERITY_INFO) {
            @Override
            public void onAfterCreated(CefBrowser browser) {
                super.onAfterCreated(browser);
                onAfterCreatedTime[0] = System.currentTimeMillis();
                onAfterCreated_.countDown();
            }
            @Override
            public void onBeforeClose(CefBrowser browser) {
                super.onBeforeClose(browser);
                onBeforeClose_.countDown();
            }
        });

        // Check CefLoadHandler
        CountDownLatch onLoadStart_ = new CountDownLatch(1);
        CountDownLatch onLoadEnd_ = new CountDownLatch(1);
        CountDownLatch onLoadErr_ = new CountDownLatch(1);
        client.addLoadHandler(new LoggingLoadHandler(CefSettings.LogSeverity.LOGSEVERITY_INFO) {
            @Override
            public void onLoadStart(CefBrowser browser, CefFrame cefFrame, CefRequest.TransitionType transitionType) {
                super.onLoadStart(browser, cefFrame, transitionType);
                onLoadStart_.countDown();
            }
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame cefFrame, int i) {
                super.onLoadEnd(browser, cefFrame, i);
                onLoadEnd_.countDown();
            }
            @Override
            public void onLoadError(CefBrowser browser, CefFrame cefFrame, ErrorCode errorCode, String errorText, String failedUrl) {
                super.onLoadError(browser, cefFrame, errorCode, errorText, failedUrl);
                onLoadErr_.countDown();
            }
        });

        //
        // 2. Create browser.
        //
        CefBrowser browser;
        if (OsrSupport.isEnabled()) {
            browser = OsrSupport.createBrowser(client, "about:blank");
        } else {
            browser = client.createBrowser("about:blank", false, false);
        }
        CefLog.Info("Created test browser with [native] id=" + browser.getIdentifier());

        //
        // 3. init UI
        //
        JFrame[] frame = new JFrame[1];
        if (!BASIC_CHECK_WITHOUT_UI && !GraphicsEnvironment.isHeadless()) {
            EventQueue.invokeLater(() -> {
                CefLog.Info("Start test UI initialization");
                frame[0] = new JFrame("JCEF basic test");
                frame[0].add(browser.getUIComponent());
                frame[0].setSize(640, 480);
                frame[0].setLocationRelativeTo(null);
                frame[0].setVisible(true);
                CefLog.Info("Test UI initialized");
            });
        }

        //
        // 4. Perform checks: onAfterCreated -> onLoadStart,onLoadEnd -> CefLifeSpanHandler.onBeforeClosed -> clientDispose_
        //
        _wait(onAfterCreated_, 5, "Native CefBrowser wasn't created");
        CefLog.Info("Native browser creation spent %d ms", onAfterCreatedTime[0] - time1);
        try {
            _wait(onLoadStart_, 5, "onLoadStart wasn't called, [native] id="+browser.getIdentifier());
        } catch (RuntimeException e) {
            if (onLoadErr_.getCount() <= 0) {
                // empiric observation: onLoadStart can be skipped when onLoadError occured.
                // see https://youtrack.jetbrains.com/issue/JBR-5192/Improve-JCEF-junit-tests#focus=Comments-27-6799179.0-0
                CefLog.Info("onLoadStart wasn't called and onLoadError was observed");
            } else throw e;
        }
        _wait(onLoadEnd_, 10, "onLoadEnd wasn't called");

        // dispose browser and client
        browser.setCloseAllowed(); // Cause browser.doClose() to return false so that OSR browser can close.
        browser.close(true);
        _wait(onBeforeClose_, 5, "onBeforeClose wasn't called");
        client.dispose();
        _wait(clientDispose_, 5, "CefClient wasn't completely disposed: " + client.getInfo());

        if (frame[0] != null)
            frame[0].dispose();

        // dispose CefApp
        TestSetupExtension.shutdonwCef();

        if (CefApp.isRemoteEnabled()) {
            // Ensure that server process is stopped
            boolean stopped = NativeServerManager.waitForStopped(WAIT_TIMEOUT_MS);
            if (!stopped)
                CefLog.Error("Can't stop server in %d ms.", WAIT_TIMEOUT_MS);
        }

        CefLog.Info("Basic checks spent %d ms", System.currentTimeMillis() - time0);
    }

    private static void _wait(CountDownLatch latch, int timeoutSec, String errorDesc) {
        try {
            if (!latch.await(timeoutSec, TimeUnit.SECONDS)) {
                CefLog.Error(errorDesc);
                throw new RuntimeException(errorDesc);
            }
        } catch (InterruptedException e) {
            CefLog.Error(e.getMessage());
        }
    }

    private static void testPipe() {
        final String testMsg = "TestPipe message 77";
        final String clientPrefix = "CLIENT23_";

        if (OS.isWindows()) {
            final String pipeName = "test_pipe";
            Thread threadServ = new Thread(()-> {
                try {
                    CefLog.Debug("Create server transport.");
                    WindowsPipeServerSocket pipeSocket = new WindowsPipeServerSocket(pipeName);
                    Socket client = pipeSocket.accept();
                    InputStream is = client.getInputStream();
                    OutputStream os = client.getOutputStream();
                    PrintStream ps = new PrintStream(os);
                    CefLog.Debug("Send message to client.");
                    ps.println(testMsg);
                    ps.flush();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    CefLog.Debug("Read response...");
                    String line = reader.readLine();
                    if (line != null && line.startsWith(clientPrefix) && line.endsWith(testMsg))
                        CefLog.Info("testPipe finished successfully: read expected line '%s'", line);
                    else
                        CefLog.Error("testPipe: read unexpected line '%s'", line);
                } catch (IOException e) {
                    CefLog.Error(e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }, "Serv");
            threadServ.start();

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Thread threadClient = new Thread(()-> {
                BufferedReader reader;
                PrintStream ps;
                try {
                    CefLog.Debug("Create client transport.");
                    WindowsPipeSocket pipe = new WindowsPipeSocket(pipeName);
                    InputStream is = pipe.getInputStream();
                    OutputStream os = pipe.getOutputStream();

                    reader = new BufferedReader(new InputStreamReader(is));
                    ps = new PrintStream(os);
                } catch (IOException e) {
                    CefLog.Error(e.getMessage());
                    throw new RuntimeException(e);
                }

                String line;
                try {
                    CefLog.Debug("Read message from server...");
                    line = reader.readLine();
                } catch (IOException e) {
                    CefLog.Error(e.getMessage());
                    throw new RuntimeException(e);
                }

                CefLog.Debug("Send response to server.");
                ps.println(clientPrefix + line);
                ps.flush();
            }, "Client");
            threadClient.start();
            try {
                threadServ.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        String pipeName = "test_pipe";
        new File(pipeName).delete(); // cleanup file remaining from prev process
        ServerSocketChannel serverChannel;
        try {
            serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            serverChannel.bind(UnixDomainSocketAddress.of(pipeName));
        } catch (IOException e) {
            CefLog.Error(e.getMessage());
            throw new RuntimeException(e);
        }

        Thread threadServ = new Thread(()-> {
            SocketChannel channel = null;
            try {
                channel = serverChannel.accept();
            } catch (IOException e) {
                CefLog.Error(e.getMessage());
                throw new RuntimeException(e);
            }
            InputStream is = new BufferedInputStream(Channels.newInputStream(channel));
            OutputStream os = new BufferedOutputStream(Channels.newOutputStream(channel));

            PrintStream ps = new PrintStream(os);
            ps.println(testMsg);
            ps.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            try {
                String line = reader.readLine();
                if (line != null && line.startsWith(clientPrefix) && line.endsWith(testMsg))
                    CefLog.Info("testPipe finished successfully: read expected line '%s'", line);
                else
                    CefLog.Error("testPipe: read unexpected line '%s'", line);
            } catch (IOException e) {
                CefLog.Error(e.getMessage());
                throw new RuntimeException(e);
            }
        }, "Serv");
        threadServ.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Thread threadClient = new Thread(()-> {
            BufferedReader reader;
            PrintStream ps;
            try {
                SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(pipeName);
                channel.connect(socketAddress);

                InputStream is = Channels.newInputStream(channel);
                OutputStream os = Channels.newOutputStream(channel);

                reader = new BufferedReader(new InputStreamReader(is));
                ps = new PrintStream(os);
            } catch (IOException e) {
                CefLog.Error(e.getMessage());
                throw new RuntimeException(e);
            }

            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                CefLog.Error(e.getMessage());
                throw new RuntimeException(e);
            }

            ps.println(clientPrefix + line);
            ps.flush();
        }, "Client");
        threadClient.start();
    }

    public static void main(String[] args) {
        new BasicJcefTest().testServerManagerPipe();
        new BasicJcefTest().testServerManagerTcp();
        new BasicJcefTest().testBrowserCreation();
    }
}
