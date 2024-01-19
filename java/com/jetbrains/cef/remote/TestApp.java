package com.jetbrains.cef.remote;

import com.jetbrains.cef.remote.thrift_codegen.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.*;
import org.cef.misc.CefLog;

import java.io.*;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.List;

public class TestApp {
    static class TestHandlers implements ClientHandlers.Iface {

        @Override
        public int connect() throws TException {
            return 0;
        }

        @Override
        public void log(String msg) throws TException {
            System.err.printf("Received message '%s'\n", msg);
        }

        @Override
        public List<CustomScheme> AppHandler_GetRegisteredCustomSchemes() throws TException {
            return null;
        }

        @Override
        public void AppHandler_OnContextInitialized() throws TException {

        }

        @Override
        public Rect RenderHandler_GetViewRect(int bid) throws TException {
            return null;
        }

        @Override
        public ScreenInfo RenderHandler_GetScreenInfo(int bid) throws TException {
            return null;
        }

        @Override
        public Point RenderHandler_GetScreenPoint(int bid, int viewX, int viewY) throws TException {
            return null;
        }

        @Override
        public void RenderHandler_OnPaint(int bid, boolean popup, int dirtyRectsCount, String sharedMemName, long sharedMemHandle, int width, int height) throws TException {

        }

        @Override
        public boolean LifeSpanHandler_OnBeforePopup(int bid, String url, String frameName, boolean gesture) throws TException {
            return false;
        }

        @Override
        public void LifeSpanHandler_OnAfterCreated(int bid) throws TException {

        }

        @Override
        public boolean LifeSpanHandler_DoClose(int bid) throws TException {
            return false;
        }

        @Override
        public void LifeSpanHandler_OnBeforeClose(int bid) throws TException {

        }

        @Override
        public void LoadHandler_OnLoadingStateChange(int bid, boolean isLoading, boolean canGoBack, boolean canGoForward) throws TException {

        }

        @Override
        public void LoadHandler_OnLoadStart(int bid, int transition_type) throws TException {

        }

        @Override
        public void LoadHandler_OnLoadEnd(int bid, int httpStatusCode) throws TException {

        }

        @Override
        public void LoadHandler_OnLoadError(int bid, int errorCode, String errorText, String failedUrl) throws TException {

        }

        @Override
        public void DisplayHandler_OnAddressChange(int bid, String url) throws TException {

        }

        @Override
        public void DisplayHandler_OnTitleChange(int bid, String title) throws TException {

        }

        @Override
        public boolean DisplayHandler_OnTooltip(int bid, String text) throws TException {
            return false;
        }

        @Override
        public void DisplayHandler_OnStatusMessage(int bid, String value) throws TException {

        }

        @Override
        public boolean DisplayHandler_OnConsoleMessage(int bid, int level, String message, String source, int line) throws TException {
            return false;
        }

        @Override
        public boolean RequestHandler_OnBeforeBrowse(int bid, RObject request, boolean user_gesture, boolean is_redirect) throws TException {
            return false;
        }

        @Override
        public boolean RequestHandler_OnOpenURLFromTab(int bid, String target_url, boolean user_gesture) throws TException {
            return false;
        }

        @Override
        public boolean RequestHandler_GetAuthCredentials(int bid, String origin_url, boolean isProxy, String host, int port, String realm, String scheme, RObject authCallback) throws TException {
            return false;
        }

        @Override
        public boolean RequestHandler_OnCertificateError(int bid, String cert_error, String request_url, RObject sslInfo, RObject callback) throws TException {
            return false;
        }

        @Override
        public void RequestHandler_OnRenderProcessTerminated(int bid, String status) throws TException {

        }

        @Override
        public RObject RequestHandler_GetResourceRequestHandler(int bid, RObject request, boolean isNavigation, boolean isDownload, String requestInitiator) throws TException {
            return null;
        }

        @Override
        public void ResourceRequestHandler_Dispose(int rrHandler) throws TException {

        }

        @Override
        public RObject ResourceRequestHandler_GetCookieAccessFilter(int rrHandler, int bid, RObject request) throws TException {
            return null;
        }

        @Override
        public void CookieAccessFilter_Dispose(int filter) throws TException {

        }

        @Override
        public boolean CookieAccessFilter_CanSendCookie(int filter, int bid, RObject request, List<String> cookie) throws TException {
            return false;
        }

        @Override
        public boolean CookieAccessFilter_CanSaveCookie(int filter, int bid, RObject request, RObject response, List<String> cookie) throws TException {
            return false;
        }

        @Override
        public boolean ResourceRequestHandler_OnBeforeResourceLoad(int rrHandler, int bid, RObject request) throws TException {
            return false;
        }

        @Override
        public RObject ResourceRequestHandler_GetResourceHandler(int rrHandler, int bid, RObject request) throws TException {
            return null;
        }

        @Override
        public void ResourceHandler_Dispose(int resourceHandler) throws TException {

        }

        @Override
        public String ResourceRequestHandler_OnResourceRedirect(int rrHandler, int bid, RObject request, RObject response, String new_url) throws TException {
            return null;
        }

        @Override
        public boolean ResourceRequestHandler_OnResourceResponse(int rrHandler, int bid, RObject request, RObject response) throws TException {
            return false;
        }

        @Override
        public void ResourceRequestHandler_OnResourceLoadComplete(int rrHandler, int bid, RObject request, RObject response, String status, long receivedContentLength) throws TException {

        }

        @Override
        public boolean ResourceRequestHandler_OnProtocolExecution(int rrHandler, int bid, RObject request, boolean allowOsExecution) throws TException {
            return false;
        }

        @Override
        public boolean MessageRouterHandler_onQuery(RObject handler, int bid, long queryId, String request, boolean persistent, RObject queryCallback) throws TException {
            return false;
        }

        @Override
        public void MessageRouterHandler_onQueryCanceled(RObject handler, int bid, long queryId) throws TException {

        }
    }
    private static void testJavaServer() throws IOException {
        // Start service for backward rpc calls (from native to java) over named pipes transport.
        final Path pipeName = Path.of(System.getProperty("java.io.tmpdir")).resolve("client_pipe");
        CefLog.Debug("Start test java server on pipe '%s'", pipeName.toString());

        new File(pipeName.toString()).delete(); // cleanup file remaining from prev process

        ServerSocketChannel clientChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        clientChannel.bind(UnixDomainSocketAddress.of(pipeName));

        ClientHandlers.Processor processor = new ClientHandlers.Processor(new TestHandlers());
        while(true) {
            SocketChannel channel = null;
            try {
                channel = clientChannel.accept();
            } catch (IOException e) {
                return;
            }

            if (channel == null)
                continue;

            TTransport client = null;
            InputStream is = new BufferedInputStream(Channels.newInputStream(channel));
            OutputStream os = new BufferedOutputStream(Channels.newOutputStream(channel));
            try {
                // NOTE: use the same transport as in native client
                // client = new TFramedTransport(new TIOStreamTransport(is, os));
                client = new TIOStreamTransport(is, os);
            } catch (TTransportException e) {
                return;
            }

            TProtocol inputProtocol = new TBinaryProtocol(client);
            TProtocol outputProtocol = new TBinaryProtocol(client);

            while(true) {
                try {
                    processor.process(inputProtocol, outputProtocol);
                } catch (TException e) {
                    break;
                }
            }
            client.close();
        }
    }

    private static void testJavaClient(boolean recursive) throws TException, IOException {
        final Path pipeName = Path.of(System.getProperty("java.io.tmpdir")).resolve("server_pipe");
        CefLog.Debug("Start test java client on pipe '%s'", pipeName.toString());

        SocketChannel channel;
        try {
            channel = SocketChannel.open(StandardProtocolFamily.UNIX);
            UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(pipeName);
            channel.connect(socketAddress);
        } catch (IOException e) {
            CefLog.Error(e.toString());
            throw e;
        }

        TTransport transport;
        try {
            InputStream is = new BufferedInputStream(Channels.newInputStream(channel));
            OutputStream os = new BufferedOutputStream(Channels.newOutputStream(channel));
            transport = new TIOStreamTransport(is, os);
        } catch (TTransportException e) {
            CefLog.Error(e.toString());
            throw e;
        }

        TProtocol protocol = new TBinaryProtocol(transport);
        Server.Iface serverIface = new Server.Client(protocol);
        try {
            if (recursive) // used to test several connections
                testJavaClient(false);
            System.out.println(serverIface.connect(recursive ? 77 : 66, null, null));
        } catch (TException e) {
            CefLog.Error(e.toString());
            throw e;
        }
    }

    private static void testJavaClientWin(boolean recursive) throws TException, IOException {
        final String pipeName = "\\\\.\\pipe\\server_pipe";
        CefLog.Debug("Start test java client on pipe '%s'", pipeName);

        Win32NamedPipeSocket pipe = new Win32NamedPipeSocket(pipeName);

        TTransport transport;
        try {
            InputStream is = new BufferedInputStream(pipe.getInputStream());
            OutputStream os = new BufferedOutputStream(pipe.getOutputStream());
            transport = new TIOStreamTransport(is, os);
        } catch (TTransportException e) {
            CefLog.Error(e.toString());
            throw e;
        }

        TProtocol protocol = new TBinaryProtocol(transport);
        Server.Iface serverIface = new Server.Client(protocol);
        try {
            if (recursive) // used to test several connections
                testJavaClient(false);
            System.out.println(serverIface.connect(recursive ? 77 : 66, null, null));
        } catch (TException e) {
            CefLog.Error(e.toString());
            throw e;
        }

        pipe.close();
    }

    private static void testJavaServerWin() throws IOException, TTransportException {
        // Start service for backward rpc calls (from native to java) over named pipes transport.
        final String pipeName = "\\\\.\\pipe\\client_pipe";
        CefLog.Debug("Start test java server on pipe '%s'", pipeName);

        ClientHandlers.Processor processor = new ClientHandlers.Processor(new TestHandlers());
        Win32NamedPipeServerSocket socketWrapper = new Win32NamedPipeServerSocket(pipeName);
        TServerTransport transport = new TServerSocket(socketWrapper);

        try {
            transport.listen();
        } catch (TTransportException var9) {
            CefLog.Error("Error occurred during listening: %s", var9);
            return;
        }

        while(true) {
            Socket client = null;
            TProtocol inputProtocol = null;
            TProtocol outputProtocol = null;

            try {
                client = socketWrapper.accept();
                if (client != null) {
                    TTransport t = new TIOStreamTransport(client.getInputStream(), client.getOutputStream());
                    inputProtocol = new TBinaryProtocol(t);
                    outputProtocol = new TBinaryProtocol(t);

                    while(true) {
                        processor.process(inputProtocol, outputProtocol);
                    }
                }
            } catch (TTransportException var10) {
                CefLog.Error("Client Transportation Exception: %s", var10);
            } catch (TException var11) {
                CefLog.Error("Thrift error occurred during processing of message: %s", var11);
            } catch (Exception var12) {
                CefLog.Error("Error occurred during processing of message: %s", var12);
            }
        }
    }

    public static void main(String[] args) throws IOException, TException {
//        System.load("C:\\Users\\bocha\\projects\\jcef\\cmake-build-debug\\remote\\Debug\\shared_mem_helper.dll");
        System.load("C:\\Users\\bocha\\projects\\jcef\\cmake-build-release\\remote\\Release\\shared_mem_helper.dll");
        //testJavaClient(true);
        testJavaServer();
    }

}
