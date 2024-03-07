package tests.junittests;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.misc.BoolRef;
import org.cef.misc.CefLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(TestSetupExtension.class)
public class FrameIdTest {
    private final String testUrl_ = "http://test.com/test.html";
    private final String testContent_ = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "\n" +
            "<head>\n" +
            "    <title>FrameIdTest</title>\n" +
            "</head>\n" +
            "\n" +
            "<body>\n" +
            "<p>Document content goes here...</p>\n" +
            "\n" +
            "<iframe\n" +
            "        id=\"inlineFrameExample\"\n" +
            "        title=\"iframe Example 1\"\n" +
            "        width=\"300\"\n" +
            "        height=\"200\"\n" +
            "        src=\"https://www.openstreetmap.org/export/embed.html?bbox=-0.004017949104309083%2C51.47612752641776%2C0.00030577182769775396%2C51.478569861898606&layer=mapnik\">\n" +
            "</iframe>\n" +
            "\n" +
            "<iframe\n" +
            "        src=\"https://example.org\"\n" +
            "        title=\"iframe Example 2\"\n" +
            "        width=\"400\"\n" +
            "        height=\"300\">\n" +
            "</iframe>\n" +
            "\n" +
            "<iframe\n" +
            "        src=\"https://example.org\"\n" +
            "        title=\"iframe Example 3\"\n" +
            "</iframe>\n" +
            "\n" +
            "<p>Document content also go here...</p>\n" +
            "</body>\n";

    @Test
    void test1() {
        final Set<Long> loadedFrameIds = new HashSet<>();
        final BoolRef mainFrameLoaded = new BoolRef(false);

        TestFrame frame = new TestFrame() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                if (frame == null) {
                    CefLog.Debug("onLoadEnd: frame is null.");
                    return;
                }

                loadedFrameIds.add(frame.getIdentifier());

                CefFrame mainFrame = browser.getMainFrame();
                if (mainFrame.getIdentifier() != frame.getIdentifier()) {
                    CefLog.Debug("Sub-frame %d is loaded.", frame.getIdentifier());
                } else {
                    mainFrameLoaded.set(true);
                    assertEquals(loadedFrameIds.size(), browser_.getFrameCount());
                    Vector<Long> ids = browser_.getFrameIdentifiers();
                    assertEquals(loadedFrameIds.size(), ids.size());
                    for (Long id: ids)
                        assertTrue(loadedFrameIds.contains(id));
                    terminateTest();
                }
            }

            @Override
            protected void setupTest() {
                addResource(testUrl_, testContent_, "text/html");
                createBrowser(testUrl_);
            }
        };

        frame.awaitCompletion();
        assertTrue(mainFrameLoaded.get());
    }
}
