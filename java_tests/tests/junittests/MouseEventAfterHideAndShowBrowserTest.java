package tests.junittests;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @key headful
 * @requires (os.arch == "amd64" | os.arch == "x86_64" | (os.arch == "aarch64" & os.family == "mac"))
 * @summary Regression test for JBR-2412. The test checks that mouse actions are handled on jcef browser after hide and show it.
 */
@ExtendWith(TestSetupExtension.class)
public class MouseEventAfterHideAndShowBrowserTest {
    @Test
    public void test() throws InvocationTargetException, InterruptedException {
        MouseEventScenario scenario = new MouseEventScenario();
        try {

            scenario.initUI();
            scenario.mouseMove(scenario.getBrowserFrame().getFrameCenter());

            MouseEventScenario.latch = new CountDownLatch(1);
            scenario.getBrowserFrame().hideAndShowBrowser();
            MouseEventScenario.latch.await(2, TimeUnit.SECONDS);

            //mouseEntered and mouseExited events work unstable. These actions are not tested.
            scenario.doMouseActions();

            System.out.println("Test PASSED");
        } catch (AWTException e) {
            e.printStackTrace();
        } finally {
            scenario.disposeBrowserFrame();
        }
    }
}