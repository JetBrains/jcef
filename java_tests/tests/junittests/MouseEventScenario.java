package tests.junittests;// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandler;
import org.cef.handler.CefLifeSpanHandlerAdapter;
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
    private CefBrowserFrame browserFrame;

    public void initUI() throws AWTException, InvocationTargetException, InterruptedException {
        robot = new Robot();
        robot.setAutoDelay(100);
        SwingUtilities.invokeAndWait(()->{
            browserFrame = new CefBrowserFrame(WIDTH, HEIGHT);
        });
        robot.waitForIdle();
    }

    public void doMouseActions() {
        try {
            _doMouseActions();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    private void _doMouseActions() throws InterruptedException {


        Point frameCenter = browserFrame.getFrameCenter();

        mouseMove(frameCenter);

        //mouseEntered and mouseExited events work unstable. These actions are not tested.

        testStage = TestStage.MOUSE_PRESSED;
        CefLog.Info("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        latch.await(2, TimeUnit.SECONDS);
        checkActionHandler();
        robot.delay(150);

        testStage = TestStage.MOUSE_RELEASED;
        CefLog.Info("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        latch.await(2, TimeUnit.SECONDS);
        checkActionHandler();
        robot.delay(150);

        testStage = TestStage.MOUSE_CLICKED;
        CefLog.Info("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        latch.await(2, TimeUnit.SECONDS);
        checkActionHandler();
        robot.delay(150);

        testStage = TestStage.MOUSE_MOVED;
        CefLog.Info("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mouseMove(frameCenter.x + browserFrame.getWidth() / 4, frameCenter.y);
        latch.await(2, TimeUnit.SECONDS);
        checkActionHandler();
        robot.delay(150);

        testStage = TestStage.MOUSE_WHEEL_MOVED;
        latch = new CountDownLatch(1);
        robot.mouseWheel(1);
        latch.await(2, TimeUnit.SECONDS);
        checkActionHandler();
    }

    public void mouseMove(Point p) {
        testStage = TestStage.MOUSE_MOVED;
        latch = new CountDownLatch(1);
        robot.mouseMove(p.x, p.y);
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkActionHandler() {
        if (latch.getCount() > 0) {
            CefLog.Error("ERROR: " + testStage.name() + " action was not handled.");
            throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");
        }
    }

    public CefBrowserFrame getBrowserFrame() {
        return browserFrame;
    }

    // NOTE: skip testing MOUSE_DRAGGED event because it can't be emulated with robot under Ubuntu20.04
    enum TestStage {
        MOUSE_ENTERED,
        MOUSE_EXITED,
        MOUSE_MOVED,
        MOUSE_CLICKED,
        MOUSE_PRESSED,
        MOUSE_RELEASED,
        MOUSE_WHEEL_MOVED
    }

    static class CefBrowserFrame extends JFrame {
        private final JBCefBrowser browser;
        private AWTEventListener awtListener;

        private MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (testStage == TestStage.MOUSE_MOVED) {
                    CefLog.Info("mouseMoved, " + e);
                    latch.countDown();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (testStage == TestStage.MOUSE_WHEEL_MOVED) {
                    CefLog.Info("mouseWheelMoved, " + e);
                    latch.countDown();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (testStage == TestStage.MOUSE_CLICKED) {
                    CefLog.Info("mouseClicked, " + e);
                    latch.countDown();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (testStage == TestStage.MOUSE_PRESSED) {
                    CefLog.Info("mousePressed, " + e);
                    latch.countDown();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (testStage == TestStage.MOUSE_RELEASED) {
                    CefLog.Info("mouseReleased, " + e);
                    latch.countDown();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (testStage == TestStage.MOUSE_ENTERED) {
                    CefLog.Info("mouseEntered, " + e);
                    latch.countDown();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (testStage == TestStage.MOUSE_EXITED) {
                    CefLog.Info("mouseExited, " + e);
                    latch.countDown();
                }
            }
        };

        public CefBrowserFrame(int width, int height) {
            browser = new JBCefBrowser(null);
            browser.getComponent().addMouseMotionListener(mouseAdapter);
            browser.getComponent().addMouseListener(mouseAdapter);
            browser.getComponent().addMouseWheelListener(mouseAdapter);
            if (Boolean.getBoolean("jcef.trace.mouseeventscenario.all_awt_mouse_events")) {
                awtListener = new AWTEventListener() {
                    @Override
                    public void eventDispatched(AWTEvent event) {
                        CefLog.Debug("awt event: %s", event);
                    }
                };
                Toolkit.getDefaultToolkit().addAWTEventListener(awtListener, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
            }

            setResizable(false);
            getContentPane().add(browser.getComponent());
            setSize(width, height);
            setVisible(true);
        }

        public void closeWindow() {
            browser.dispose();
            browser.awaitClientDisposed();
            if (awtListener != null) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener);
            }
            super.dispose();
        }

        public void hideAndShowBrowser() {
            latch = new CountDownLatch(0);
            SwingUtilities.invokeLater(() -> {
                Container parent = browser.getComponent().getParent();
                parent.remove(browser.getComponent());
                parent.add(browser.getComponent());
                latch.countDown();
            });
            try {
                latch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public JBCefBrowser getBrowser() {
            return browser;
        }

        public Point getFrameCenter() {
            return new Point(getLocationOnScreen().x + getWidth() / 2,
                    getLocationOnScreen().y + getHeight() / 2);
        }
    }
}