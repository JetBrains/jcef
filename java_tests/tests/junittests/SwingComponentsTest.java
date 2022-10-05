package tests.junittests;

import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SwingComponentsTest {
    public static CountDownLatch latch;

    static TestStage testStage;

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private Robot robot;
    private TestFrame testFrame;

    @Test
    public void testMouseListener() throws InvocationTargetException, InterruptedException {
        // reproducer for JBR-4884
        try {
            initUI();
            doMouseActions();
            System.err.println("Test PASSED");
        } catch (AWTException e) {
            e.printStackTrace();
        } finally {
            SwingUtilities.invokeAndWait(testFrame::dispose);
        }
    }

    public void initUI() throws AWTException, InvocationTargetException, InterruptedException {
        robot = new Robot();
        testFrame = new TestFrame(WIDTH, HEIGHT);
        SwingUtilities.invokeAndWait(testFrame::initUI);
        robot.waitForIdle();
    }

    public void doMouseActions() throws InterruptedException {
        Point frameCenter = new Point(testFrame.getLocationOnScreen().x + testFrame.getWidth() / 2,
                testFrame.getLocationOnScreen().y + testFrame.getHeight() / 2);
        robot.mouseMove(frameCenter.x, frameCenter.y);

        testStage = TestStage.MOUSE_PRESSED;
        System.err.println("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        latch.await(2, TimeUnit.SECONDS);
        if (latch.getCount() > 0) throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");

        testStage = TestStage.MOUSE_DRAGGED;
        System.err.println("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        // Empiric observation: robot.mouseMove with small shifts (1-3 pixels) doesn't produce real moves
        // So we must use quite large shifts
        robot.mouseMove(frameCenter.x + testFrame.getWidth() / 4, frameCenter.y);
        latch.await(2, TimeUnit.SECONDS);
        if (latch.getCount() > 0) throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");

        testStage = TestStage.MOUSE_RELEASED;
        System.err.println("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        latch.await(2, TimeUnit.SECONDS);
        if (latch.getCount() > 0) throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");

        testStage = TestStage.MOUSE_CLICKED;
        System.err.println("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        latch.await(2, TimeUnit.SECONDS);
        if (latch.getCount() > 0) throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");

        testStage = TestStage.MOUSE_MOVED;
        System.err.println("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mouseMove(frameCenter.x + 2, frameCenter.y);
        latch.await(2, TimeUnit.SECONDS);
        if (latch.getCount() > 0) throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");

        testStage = TestStage.MOUSE_WHEEL_MOVED;
        latch = new CountDownLatch(1);
        robot.mouseWheel(1);
        latch.await(2, TimeUnit.SECONDS);
        if (latch.getCount() > 0) throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");
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

    @Test
    public void testPaintOccured() {
        for (int c = 0; c < 10; ++c) {
            try {
                CountDownLatch paintLatch = new CountDownLatch(1);
                testFrame = new TestFrame(WIDTH, HEIGHT, paintLatch);
                SwingUtilities.invokeAndWait(testFrame::initUI);
                if(!paintLatch.await(5, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Paint wasn't occured in 5 seconds");
                }
                Assert.assertTrue(paintLatch.getCount() < 1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } finally {
                SwingUtilities.invokeLater(testFrame::dispose);
            }
        }
    }

    static class TestComponent extends JPanel {
        private Canvas canvas_ = null;
        private final CountDownLatch paintLatch;

        public TestComponent(CountDownLatch paintLatch) {
            super(new BorderLayout());
            this.paintLatch = paintLatch;
        }

        @Override
        public void addNotify() {
            super.addNotify();
            canvas_ = new Canvas();
            this.add(canvas_, BorderLayout.CENTER);
        }

        @Override
        public void removeNotify() {
            this.remove(canvas_);
            super.removeNotify();
        }

        @Override
        public void paint(Graphics g) {
            if (paintLatch != null) {
                paintLatch.countDown();
                Window frame = SwingUtilities.getWindowAncestor(this);
                if (frame != null)
                    SwingUtilities.invokeLater(()->frame.dispose());
            }
            super.paint(g);
        }
    }

    static class TestFrame extends JFrame {
        private final Component testComponent;
        private final int width, height;

        private MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                System.err.println("mouseDragged: " + e);
                if (testStage == TestStage.MOUSE_DRAGGED) {
                    latch.countDown();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                System.err.println("mouseMoved: " + e);
                if (testStage == TestStage.MOUSE_MOVED) {
                    
                    latch.countDown();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                System.err.println("mouseWheelMoved: " + e);
                if (testStage == TestStage.MOUSE_WHEEL_MOVED) {
                    
                    latch.countDown();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                System.err.println("mouseClicked: " + e);
                if (testStage == TestStage.MOUSE_CLICKED) {
                    
                    latch.countDown();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                System.err.println("mousePressed: " + e);
                if (testStage == TestStage.MOUSE_PRESSED) {
                    
                    latch.countDown();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                System.err.println("mouseReleased: " + e);
                if (testStage == TestStage.MOUSE_RELEASED) {
                    
                    latch.countDown();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (testStage == TestStage.MOUSE_ENTERED) {
                    System.err.println("mouseEntered");
                    
                    latch.countDown();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (testStage == TestStage.MOUSE_EXITED) {
                    System.err.println("mouseExited");
                    
                    latch.countDown();
                }
            }
        };

        public TestFrame(int width, int height) {
            this(width, height, null);
        }
        public TestFrame(int width, int height, CountDownLatch latch) {
            this.width = width;
            this.height = height;

            testComponent = new TestComponent(latch);
            testComponent.addMouseMotionListener(mouseAdapter);
            testComponent.addMouseListener(mouseAdapter);
            testComponent.addMouseWheelListener(mouseAdapter);
        }

        public void closeLater() {
            SwingUtilities.invokeLater(()->dispose());
        }

        public void initUI() {
            setResizable(false);
            getContentPane().add(testComponent);
            pack();
            setSize(width, height);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setVisible(true);
        }

        public void addremove() {
            SwingUtilities.invokeLater(() -> {
                Container parent = testComponent.getParent();
                parent.remove(testComponent);
                parent.add(testComponent);
            });
        }
    }
}