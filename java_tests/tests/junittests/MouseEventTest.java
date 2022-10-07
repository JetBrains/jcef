package tests.junittests;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import org.cef.misc.CefLog;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @test
 * @key headful
 * @requires (os.arch == "amd64" | os.arch == "x86_64" | (os.arch == "aarch64" & os.family == "mac"))
 * @summary Regression test for JBR-2639. The test checks that mouse actions are handled on jcef browser.
 */
@ExtendWith(TestSetupExtension.class)
public class MouseEventTest {
    @Test
    public void test() throws InvocationTargetException, InterruptedException {
        CefLog.Info("Start basic mouse events test");
        doTest(scenario -> {
            scenario.doMouseActions();
        });
    }

    @Test
    public void testWithAwaitBrowserCreation() throws InvocationTargetException, InterruptedException {
        // debug helper for JBR-4649
        CefLog.Info("Start basic mouse events test");
        doTest(scenario -> {
            scenario.getBrowserFrame().getBrowser().awaitBrowserCreated();
            scenario.doMouseActions();
        });
    }

    @Test
    public void testWithDelayedCreation() throws InvocationTargetException, InterruptedException {
        // debug helper for JBR-4649
        CefLog.Info("Start testWithDelayedCreation");
        System.setProperty("jcef.debug.cefbrowserwr.delay_creation", "3000"); // 3 sec delay
        doTest(scenario -> {
            scenario.doMouseActions();
        });
        System.clearProperty("jcef.debug.cefbrowserwr.delay_creation");
    }


    @Test
    public void hideAndShowBrowserTest() throws InvocationTargetException, InterruptedException {
        CefLog.Info("Start hideAndShowBrowserTest");
        doTest(scenario -> {
            scenario.mouseMove(scenario.getBrowserFrame().getFrameCenter());
            scenario.getBrowserFrame().hideAndShowBrowser();
            scenario.doMouseActions();
        });
    }

    private void doTest(Consumer<MouseEventScenario> testTask) throws InterruptedException, InvocationTargetException {
        MouseEventScenario scenario = new MouseEventScenario();
        try {
            scenario.initUI();
            testTask.accept(scenario);
            CefLog.Info("Test PASSED");
        } catch (AWTException e) {
            e.printStackTrace();
        } finally {
            scenario.disposeBrowserFrame();
        }
    }
}