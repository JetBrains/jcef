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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
            "       let events_count = 0\n" +
            "       function on_touch_event(e) {\n" +
            "           console.log(`on_touch_event: ${e.type}`) \n" +
            "           last_touch_event = e\n" +
            "           events_count++ \n" +
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
        private CompletableFuture<String> myGetValueFuture = new CompletableFuture<>();
        private boolean ready = false;

        @Override
        protected void setupTest() {
            final String PAGE_URL = "https://some.url/";
            addResource(PAGE_URL, HTML_PAGE, "text/html");

            // add on touch event callback. it's used to wait for receiving the event on the browser side
            {
                CefMessageRouterConfig config = new CefMessageRouterConfig();
                config.jsQueryFunction = TOUCH_CALLBACK_NAME;
                var router = CefMessageRouter.create(config);
                router.addHandler(new CefMessageRouterHandlerAdapter() {
                    @Override
                    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                        myTouchCallbackLatch.get().countDown();
                        callback.success("ok");
                        return true;
                    }
                }, true);
                client_.addMessageRouter(router);
            }

            {
                CefMessageRouterConfig config = new CefMessageRouterConfig();
                config.jsQueryFunction = SEND_MESSAGE_NAME;
                var router = CefMessageRouter.create(config);
                router.addHandler(new CefMessageRouterHandlerAdapter() {
                    @Override
                    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                        request = request.replaceAll("^\"|\"$", "");
                        myGetValueFuture.complete(request);
                        callback.success("ok");
                        return true;
                    }
                }, true);
                client_.addMessageRouter(router);
            }

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
            Assertions.assertTrue(myTouchCallbackLatch.get().await(2, TimeUnit.SECONDS));
        }

        public String getValue(String /*language=javascript*/ expression) throws ExecutionException, InterruptedException {
            myGetValueFuture = new CompletableFuture<>();
            // language=javascript
            final String script =
                    "window." + SEND_MESSAGE_NAME + "({\n" +
                            "   request: JSON.stringify(" + expression + "),\n" +
                            "   persistent: true,\n" +
                            "   onSuccess: function(response) {},\n" +
                            "   onFailure: function(error_code, error_message) {}\n" +
                            "})\n";
            browser_.executeJavaScript(script, browser_.getURL(), 0);
            return myGetValueFuture.get();
        }

        void awaitLoad() throws InterruptedException {
            Assertions.assertTrue(myInitLatch.await(5, TimeUnit.SECONDS));
            var renderHandler = (JBCefOsrHandler) browser_.getRenderHandler();
            Assertions.assertNotNull(renderHandler);
            Assertions.assertTrue(renderHandler.awaitInit());
            ready = true;
        }

    }

    private CefTouchEvent createSimpleEvent(int id, float x, float y, CefTouchEvent.EventType type) {
        return new CefTouchEvent(id, x, y, 0, 0, 0, 0, type, 0, CefTouchEvent.PointerType.UNKNOWN);
    }

    @Test
    void eventType() throws InterruptedException, ExecutionException {
        var frame = new TouchTestFrame();
        frame.awaitLoad();

        frame.sendTouchEvent(createSimpleEvent(0, 10, 10, CefTouchEvent.EventType.PRESSED));
        Assertions.assertEquals("touchstart", frame.getValue("last_touch_event.type"));
        Assertions.assertEquals("1", frame.getValue("last_touch_event.touches.length"));
        Assertions.assertEquals("1", frame.getValue("events_count"));

        frame.sendTouchEvent(createSimpleEvent(0, 10, 100, CefTouchEvent.EventType.MOVED));
        Assertions.assertEquals("touchmove", frame.getValue("last_touch_event.type"));
        Assertions.assertEquals("1", frame.getValue("last_touch_event.touches.length"));
        Assertions.assertEquals("2", frame.getValue("events_count"));

        frame.sendTouchEvent(createSimpleEvent(1, 50, 10, CefTouchEvent.EventType.PRESSED));
        Assertions.assertEquals("touchstart", frame.getValue("last_touch_event.type"));
        Assertions.assertEquals("2", frame.getValue("last_touch_event.touches.length"));
        Assertions.assertEquals("3", frame.getValue("events_count"));

        frame.sendTouchEvent(createSimpleEvent(1, 50, 100, CefTouchEvent.EventType.MOVED));
        Assertions.assertEquals("touchmove", frame.getValue("last_touch_event.type"));
        Assertions.assertEquals("2", frame.getValue("last_touch_event.touches.length"));
        Assertions.assertEquals("4", frame.getValue("events_count"));

        frame.sendTouchEvent(createSimpleEvent(0, 10, 100, CefTouchEvent.EventType.RELEASED));
        Assertions.assertEquals("touchend", frame.getValue("last_touch_event.type"));
        Assertions.assertEquals("1", frame.getValue("last_touch_event.touches.length"));
        Assertions.assertEquals("5", frame.getValue("events_count"));

        frame.sendTouchEvent(createSimpleEvent(1, 50, 100, CefTouchEvent.EventType.CANCELLED));
        Assertions.assertEquals("touchcancel", frame.getValue("last_touch_event.type"));
        Assertions.assertEquals("0", frame.getValue("last_touch_event.touches.length"));
        Assertions.assertEquals("6", frame.getValue("events_count"));

        frame.terminateTest();
        frame.awaitCompletion();
    }

    @Test
    void numericValues() throws InterruptedException, ExecutionException {
        var frame = new TouchTestFrame();
        frame.awaitLoad();

        frame.sendTouchEvent(new CefTouchEvent(
                0, // ID
                10,  // x
                15,  // y
                20,  // radiusX
                25, // radiusX
                30,  // rotation angle
                0.5f, // pressure
                CefTouchEvent.EventType.PRESSED,
                0, // modifiers
                CefTouchEvent.PointerType.UNKNOWN)
        );

        Assertions.assertEquals("touchstart", frame.getValue("last_touch_event.type"));
        Assertions.assertEquals("1", frame.getValue("last_touch_event.touches.length"));
        Assertions.assertEquals("10", frame.getValue("last_touch_event.touches[0].screenX"));
        Assertions.assertEquals("15", frame.getValue("last_touch_event.touches[0].screenY"));
        Assertions.assertEquals("20", frame.getValue("last_touch_event.touches[0].radiusX"));
        Assertions.assertEquals("25", frame.getValue("last_touch_event.touches[0].radiusY"));
        Assertions.assertEquals("0.5", frame.getValue("last_touch_event.touches[0].force"));
        Assertions.assertEquals("30", frame.getValue("last_touch_event.touches[0].rotationAngle"));

        Assertions.assertEquals("1", frame.getValue("events_count"));

        frame.terminateTest();
        frame.awaitCompletion();
    }
}
