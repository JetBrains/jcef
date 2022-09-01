package tests.junittests;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import org.cef.CefApp;
import org.cef.misc.CefLog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class MouseEventScenario {
    public static CountDownLatch latch;

    static TestStage testStage;

    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;
    private Robot robot;
    private CefBrowserFrame browserFrame = new CefBrowserFrame(WIDTH, HEIGHT);

    public void initUI() throws AWTException, InvocationTargetException, InterruptedException {
        robot = new Robot();
        SwingUtilities.invokeAndWait(browserFrame::initUI);
        robot.waitForIdle();
    }

    public void doMouseActions() throws InterruptedException {
        Point frameCenter = browserFrame.getFrameCenter();

        mouseMove(frameCenter);

        //mouseEntered and mouseExited events work unstable. These actions are not tested.

        testStage = TestStage.MOUSE_PRESSED;
        CefLog.Info("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        latch.await(2, TimeUnit.SECONDS);
        checkActionHandler();

        testStage = TestStage.MOUSE_DRAGGED;
        CefLog.Info("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        // Empiric observation: robot.mouseMove with small shifts (1-3 pixels) doesn't produce real moves
        // So we must use quite large shifts
        robot.mouseMove(frameCenter.x + browserFrame.getWidth() / 4, frameCenter.y);
        latch.await(2, TimeUnit.SECONDS);
        checkActionHandler();

        testStage = TestStage.MOUSE_RELEASED;
        CefLog.Info("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        latch.await(2, TimeUnit.SECONDS);
        checkActionHandler();

        testStage = TestStage.MOUSE_CLICKED;
        CefLog.Info("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        latch.await(2, TimeUnit.SECONDS);
        checkActionHandler();

        testStage = TestStage.MOUSE_MOVED;
        CefLog.Info("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mouseMove(frameCenter.x + 2, frameCenter.y);
        latch.await(2, TimeUnit.SECONDS);
        checkActionHandler();

        testStage = TestStage.MOUSE_WHEEL_MOVED;
        latch = new CountDownLatch(1);
        robot.mouseWheel(1);
        latch.await(2, TimeUnit.SECONDS);
        checkActionHandler();

    }

    public void mouseMove(Point p) throws InterruptedException {
        testStage = TestStage.MOUSE_MOVED;
        latch = new CountDownLatch(1);
        robot.mouseMove(p.x, p.y);
        latch.await(2, TimeUnit.SECONDS);
        browserFrame.resetMouseActionPerformedFlag();
    }

    public void disposeBrowserFrame() throws InvocationTargetException, InterruptedException {
        getBrowserFrame().getBrowser().dispose();
        SwingUtilities.invokeAndWait(getBrowserFrame()::dispose);
    }

    private void checkActionHandler() {
        if (!browserFrame.isMouseActionPerformed()) {
            throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");
        }
        browserFrame.resetMouseActionPerformedFlag();
    }

    public CefBrowserFrame getBrowserFrame() {
        return browserFrame;
    }

    enum TestStage {
        MOUSE_ENTERED,
        MOUSE_EXITED,
        MOUSE_MOVED,
        MOUSE_DRAGGED,
        MOUSE_CLICKED,
        MOUSE_PRESSED,
        MOUSE_RELEASED,
        MOUSE_WHEEL_MOVED
    }

    static class CefBrowserFrame extends JFrame {
        private final JBCefBrowser browser;
        private final int width, height;
        private volatile boolean mouseActionPerformed = false;

        private MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (MouseEventScenario.testStage == TestStage.MOUSE_DRAGGED) {
                    CefLog.Info("mouseDragged");
                    mouseActionPerformed = true;
                    MouseEventScenario.latch.countDown();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (MouseEventScenario.testStage == TestStage.MOUSE_MOVED) {
                    CefLog.Info("mouseMoved");
                    mouseActionPerformed = true;
                    MouseEventScenario.latch.countDown();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (MouseEventScenario.testStage == TestStage.MOUSE_WHEEL_MOVED) {
                    CefLog.Info("mouseWheelMoved");
                    mouseActionPerformed = true;
                    MouseEventScenario.latch.countDown();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (MouseEventScenario.testStage == TestStage.MOUSE_CLICKED) {
                    CefLog.Info("mouseClicked");
                    mouseActionPerformed = true;
                    MouseEventScenario.latch.countDown();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (MouseEventScenario.testStage == TestStage.MOUSE_PRESSED) {
                    CefLog.Info("mousePressed");
                    mouseActionPerformed = true;
                    MouseEventScenario.latch.countDown();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (MouseEventScenario.testStage == TestStage.MOUSE_RELEASED) {
                    CefLog.Info("mouseReleased");
                    mouseActionPerformed = true;
                    MouseEventScenario.latch.countDown();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (MouseEventScenario.testStage == TestStage.MOUSE_ENTERED) {
                    CefLog.Info("mouseEntered");
                    mouseActionPerformed = true;
                    MouseEventScenario.latch.countDown();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (MouseEventScenario.testStage == TestStage.MOUSE_EXITED) {
                    CefLog.Info("mouseExited");
                    mouseActionPerformed = true;
                    MouseEventScenario.latch.countDown();
                }
            }
        };

        public CefBrowserFrame(int width, int height) {
            this.width = width;
            this.height = height;

            browser = new JBCefBrowser(null);
            browser.getComponent().addMouseMotionListener(mouseAdapter);
            browser.getComponent().addMouseListener(mouseAdapter);
            browser.getComponent().addMouseWheelListener(mouseAdapter);
        }

        public void initUI() {
            setResizable(false);
            getContentPane().add(browser.getComponent());
            setSize(width, height);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    browser.dispose();
                }
            });
            setVisible(true);
        }

        public void hideAndShowBrowser() {
            SwingUtilities.invokeLater(() -> {
                Container parent = browser.getComponent().getParent();
                parent.remove(browser.getComponent());
                parent.add(browser.getComponent());
                MouseEventScenario.latch.countDown();
            });
        }

        public JBCefBrowser getBrowser() {
            return browser;
        }

        public void resetMouseActionPerformedFlag() {
            mouseActionPerformed = false;
        }

        public boolean isMouseActionPerformed() {
            return mouseActionPerformed;
        }

        public Point getFrameCenter() {
            return new Point(getLocationOnScreen().x + getWidth() / 2,
                    getLocationOnScreen().y + getHeight() / 2);
        }
    }
}