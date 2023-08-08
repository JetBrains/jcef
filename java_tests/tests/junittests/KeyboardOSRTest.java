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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@ExtendWith(TestSetupExtension.class)
public class KeyboardOSRTest {
    static final String PAGE_URL = "https://some.url/";
    private static final String KEY_EVENT_CALLBACK_NAME = "_cef_on_key_event";
    private static final List<Scenario> outputScenarios = new ArrayList<>();
    private static final String KEYBOARD_TEST_OUTPUT_FILE = System.getenv("KEYBOARD_TEST_OUTPUT_FILE");
    private static final boolean UPDATE_REFERENCE = KEYBOARD_TEST_OUTPUT_FILE != null && !KEYBOARD_TEST_OUTPUT_FILE.isEmpty();

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
    private static final EventsWaiter eventsWaiter = new EventsWaiter();
    private static CountDownLatch callbackLatch = new CountDownLatch(1);

    @BeforeAll
    public static void before() throws InterruptedException {
        myFrame = new MyFrame();
        myFrame.awaitLoad();
    }

    @AfterAll
    public static void after() throws IOException {
        myFrame.closeBrowser();
        myFrame.awaitCompletion();
        File file = getScenarioFile();
        if (UPDATE_REFERENCE && file != null) {
            String jsonString = new GsonBuilder()
                    .setPrettyPrinting()
                    .create()
                    .toJson(outputScenarios.toArray());

            Files.write(file.toPath(), jsonString.getBytes());
        }
    }

    private static Stream<Scenario> getScenarios() throws IOException {
        File file = getScenarioFile();
        if (file == null) {
            return Stream.empty();
        }
        String jsonText = Files.readString(file.toPath());

        Type typeToken = new TypeToken<ArrayList<Scenario>>() {
        }.getType();
        ArrayList<Scenario> scenarios = new Gson().fromJson(jsonText, typeToken);
        return scenarios.stream().filter(scenario -> !Objects.requireNonNullElse(scenario.comments, "").toLowerCase().contains("disable"));
    }

    private static File getScenarioFile() {
        if (KEYBOARD_TEST_OUTPUT_FILE != null) {
            return new File(KEYBOARD_TEST_OUTPUT_FILE);
        }
        String scenarioPath;
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.startsWith("mac")) {
            scenarioPath = "data/keyboard_scenario_mac.json";
        } else {
            return null;
        }

        try {
            return new File(Objects.requireNonNull(KeyboardOSRTest.class.getResource(scenarioPath)).toURI());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @ParameterizedTest
    @MethodSource("getScenarios")
    void doTest(Scenario scenario) throws InterruptedException {
        System.err.println("Testing '" + scenario.name + "'");
        eventsWaiter.setup();
        for (Scenario.EventDataJava data : scenario.eventsJava) {
            callbackLatch = new CountDownLatch(1);
            myFrame.browser_.sendKeyEvent(data.makeKeyEvent(myFrame.browser_.getUIComponent()));
            callbackLatch.await(200, TimeUnit.MILLISECONDS);
        }
        List<Scenario.EventDataJS> eventsJS = eventsWaiter.get();

        if (!UPDATE_REFERENCE) {
            Assertions.assertFalse(eventsJS.isEmpty());
            Assertions.assertEquals(scenario.eventsJSExpected, eventsJS);
        }
        outputScenarios.add(new Scenario(scenario.name, scenario.comments, scenario.eventsJava, eventsJS));
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
                    System.err.println("Received scenario: " + request);
                    eventsWaiter.addEvent(Scenario.EventDataJS.fromJson(request));
                    callbackLatch.countDown();
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

    private static class EventsWaiter {
        List<Scenario.EventDataJS> events;
        final Lock lock = new ReentrantLock();
        final Condition scenarioFinished = lock.newCondition();
        private void setup() {
            events = new ArrayList<>();
        }

        public void addEvent(Scenario.EventDataJS event) {
            lock.lock();
            events.add(event);
            scenarioFinished.signal();
            lock.unlock();
        }

        public List<Scenario.EventDataJS> get() throws InterruptedException {
            lock.lock();
            while (!isReady()) {
                if (!scenarioFinished.await(500, TimeUnit.MILLISECONDS)) {
                    if (UPDATE_REFERENCE) {
                        System.err.println("The received scenario events are incomplete: " + events);
                    } else {
                        Assertions.fail("The received scenario events are incomplete: " + events);
                    }
                    break;
                }
            }
            var result = events;
            setup();
            lock.unlock();
            return result;
        }

        public boolean isReady() {
            if (events.isEmpty()) {
                return false;
            }

            Scenario.EventDataJS first = events.get(0);
            Scenario.EventDataJS last = events.get(events.size() - 1);

            return "keydown".equals(first.type) && "keyup".equals(last.type) && Objects.equals(first.code, last.code);
        }
    }
}
