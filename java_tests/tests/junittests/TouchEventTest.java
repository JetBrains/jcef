package tests.junittests;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.input.CefTouchEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tests.JBCefOsrHandler;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(TestSetupExtension.class)
public class TouchEventTest {
    private final String TOUCH_CALLBACK_NAME = "_cef_on_touch";
    private final String SEND_MESSAGE_NAME = "_cef_send_message";

    // language=html
    private final String HTML_PAGE = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>" +
            "   <title>Touch event</title>" +
            "   <script>\n" +
            "       let last_touch_event = null\n" +
            "       let event_count = 0\n" +
            "       function on_touch_event(e) {\n" +
            "           console.log(`on_touch_event: ${e.type}`) \n" +
            "           last_touch_event = e\n" +
            "           event_count++ \n" +
            "           window." + TOUCH_CALLBACK_NAME + "({\n" +
            "              request: e.type,\n" +
            "              persistent: true,\n" +
            "              onSuccess: function(response) {},\n" +
            "              onFailure: function(error_code, error_message) {}\n" +
            "           })\n" +
            "       }\n" +
            "       document.addEventListener('touchstart', on_touch_event)\n" +
            "       document.addEventListener('touchmove', on_touch_event)\n" +
            "       document.addEventListener('touchend', on_touch_event)\n" +
            "       document.addEventListener('touchcancel', on_touch_event)\n" +
            "   </script>\n" +
            "</head>\n" +
            "<body/>\n" +
            "</html>";


    private class TouchTestFrame extends TestFrame {
        private final CountDownLatch myInitLatch = new CountDownLatch(1);
        private final AtomicReference<CountDownLatch> myTouchCallbackLatch = new AtomicReference<>();
        private boolean ready = false;

        @Override
        protected void setupTest() {
            final String PAGE_URL = "https://some.url/";
            addResource(PAGE_URL, HTML_PAGE, "text/html");

            CefMessageRouterConfig config = new CefMessageRouterConfig();
            config.jsQueryFunction = TOUCH_CALLBACK_NAME;
            var router = CefMessageRouter.create(config);
            router.addHandler(new CefMessageRouterHandlerAdapter() {
                @Override
                public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                    myTouchCallbackLatch.get().countDown();
                    return true;
                }
            }, true);
            client_.addMessageRouter(router);

            String isOsr = System.getProperty("jcef.tests.osr");
            System.setProperty("jcef.tests.osr", "true");
            createBrowser(PAGE_URL);
            System.setProperty("jcef.tests.osr", isOsr);
        }

        @Override
        public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
            Assertions.assertEquals(200, httpStatusCode);
            myInitLatch.countDown();
        }

        public void sendTouchEvent(CefTouchEvent e) throws InterruptedException {
            assert ready;
            myTouchCallbackLatch.set(new CountDownLatch(1));
            SwingUtilities.invokeLater(() -> browser_.sendTouchEvent(e));
            Assertions.assertTrue(myTouchCallbackLatch.get().await(2000, TimeUnit.SECONDS));
        }

        void awaitLoad() throws InterruptedException {
            Assertions.assertTrue(myInitLatch.await(5, TimeUnit.SECONDS));
            var renderHandler = (JBCefOsrHandler)browser_.getRenderHandler();
            Assertions.assertNotNull(renderHandler);
            Assertions.assertTrue(renderHandler.awaitInit());
            ready = true;
        }

    }

    @Test
    void eventType() throws InterruptedException {
        var frame = new TouchTestFrame();
        frame.awaitLoad();

        frame.sendTouchEvent(new CefTouchEvent(0, 10, 10, CefTouchEvent.EventType.PRESSED));
        frame.sendTouchEvent(new CefTouchEvent(0, 11, 10, CefTouchEvent.EventType.MOVED));
//        boolean a = true;
//        while (a);
        frame.sendTouchEvent(new CefTouchEvent(0, 30, 30, CefTouchEvent.EventType.RELEASED));
//        a = true;
//        while (a);


        frame.terminateTest();
        frame.awaitCompletion();
    }
}
