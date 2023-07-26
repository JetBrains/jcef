package tests.junittests;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tests.JBCefOsrHandler;
import tests.keyboard.Scenario;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@ExtendWith(TestSetupExtension.class)
public class KeyboardOSRTest {
    static final String PAGE_URL = "https://some.url/";
    private static final String KEY_EVENT_CALLBACK_NAME = "_cef_on_key_event";
//    private static String outputFileName = "/Users/Vladimir.Kharitonov/develop/jcef-kb/java_tests/tests/junittests/data/keyboard_events_scenario_mac.json";
    private static String outputFileName = System.getProperty("jcef.tests.osr.keyboard.output");
    private static List<Scenario> outputScenarios = new ArrayList<>();

    // language=HTML
    final static String PAGE_HTML = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <script>\n" +
            "        /**\n" +
            "         * @param {KeyboardEvent}e\n" +
            "         * @returns {string}\n" +
            "         */\n" +
            "        function eventToString(e) {\n" +
            "            return JSON.stringify({\n" +
            "                'type': e.type,\n" +
            "                'key': e.key,\n" +
            "                'code': e.code,\n" +
            "                'location': e.location,\n" +
            "                'altKey': e.altKey,\n" +
            "                'ctrlKey': e.ctrlKey,\n" +
            "                'metaKay': e.metaKey,\n" +
            "                'shiftKey': e.shiftKey,\n" +
            "            })\n" +
            "        }\n" +
            "        function sendEvent(e) {\n" +
            "            const msg = eventToString(e)\n" +
            "            console.log(`New event: ${msg}`)\n" +
            "            window." + KEY_EVENT_CALLBACK_NAME + "({\n" +
            "               request: msg,\n" +
            "               persistent: true,\n" +
            "               onSuccess: function(response) {},\n" +
            "               onFailure: function(error_code, error_message) {}\n" +
            "            })\n" +
            "        }\n" +
            "        document.addEventListener('keydown', sendEvent, false);\n" +
            "        document.addEventListener('keypress', sendEvent, false);\n" +
            "        document.addEventListener('keyup', sendEvent, false);\n" +
            "    </script>\n" +
            "    <title>keyboard test</title>\n" +
            "</head>\n" +
            "<body></body>\n" +
            "</html>";
    private static MyFrame myFrame = null;
    private static final EventWaiter eventWaiter = new EventWaiter();

    @BeforeAll
    public static void before() throws InterruptedException, InvocationTargetException {
        myFrame = new MyFrame();
        myFrame.awaitLoad();
    }

    @AfterAll
    public static void after() throws IOException {
        myFrame.closeBrowser();
        myFrame.awaitCompletion();
        if (outputFileName != null) {
            String jsonString = new GsonBuilder()
                    .setPrettyPrinting()
                    .create()
                    .toJson(outputScenarios.toArray());

            File file = new File(outputFileName);
            Files.write(file.toPath(), jsonString.getBytes());
        }
    }

    private static Stream<Scenario> getScenarios() throws URISyntaxException, IOException {
        Path path = new File(KeyboardOSRTest.class.getResource("data/keyboard_events_scenario_mac.json").toURI()).toPath();
        String jsonText = Files.readString(path);

        Type typeToken = new TypeToken<ArrayList<Scenario>>() {
        }.getType();
        ArrayList<Scenario> scenarios = new Gson().fromJson(jsonText, typeToken);
        return scenarios.stream().filter(scenario -> !"disabled".equals(scenario.comments));
    }

    private static KeyEvent makeKeyEvent(Scenario.EventDataJava eventData) {
        return new KeyEvent(
                myFrame.browser_.getUIComponent(),
                eventData.id,
                0,
                eventData.modifiers,
                eventData.keyCode,
                eventData.keyChar,
                eventData.keyLocation
        );
    }

    @ParameterizedTest
    @MethodSource("getScenarios")
    void doTest(Scenario scenario) throws InterruptedException {
        List<Scenario.EventDataJS> callbacks = new ArrayList<>();
        for (Scenario.EventDataJava data: scenario.eventsJava) {
            eventWaiter.setup();
            myFrame.browser_.sendKeyEvent(makeKeyEvent(data));
            eventWaiter.await();
            String response = eventWaiter.event;
            Type typeToken = new TypeToken<Scenario.EventDataJS>() {}.getType();
            callbacks.add(new Gson().fromJson(response, typeToken));
        }
        if (outputFileName == null) {
            Assertions.assertEquals(scenario.eventsJSExpected, callbacks);
        }
        outputScenarios.add(new Scenario(scenario.name, scenario.comments, scenario.eventsJava, callbacks));
    }

    static class MyFrame extends TestFrame {
        private final CountDownLatch myInitLatch = new CountDownLatch(1);

        @Override
        protected void setupTest() {
            CefMessageRouter.CefMessageRouterConfig config = new CefMessageRouter.CefMessageRouterConfig();
            config.jsQueryFunction = KEY_EVENT_CALLBACK_NAME;
            var router = CefMessageRouter.create(config);
            router.addHandler(new CefMessageRouterHandlerAdapter() {
                @Override
                public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                    eventWaiter.set(request);
                    return true;
                }
            }, true);
            client_.addMessageRouter(router);


            addResource(PAGE_URL, PAGE_HTML, "text/html");
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

        public void closeBrowser() {
            terminateTest();
        }

        void awaitLoad() throws InterruptedException {
            Assertions.assertTrue(myInitLatch.await(5, TimeUnit.SECONDS));
            var renderHandler = (JBCefOsrHandler) browser_.getRenderHandler();
            Assertions.assertNotNull(renderHandler);
            Assertions.assertTrue(renderHandler.awaitInit());
        }
    }

    private static class EventWaiter {
        private CountDownLatch latch;
        private String event;

        public void setup() {
            latch = new CountDownLatch(1);
            event = null;
        }

        public void await() throws InterruptedException {
            latch.await(2, TimeUnit.SECONDS);
        }

        public String get() {
            return event;
        }

        public void set(String event) {
            this.event = event;
            latch.countDown();
        }
    }
}
