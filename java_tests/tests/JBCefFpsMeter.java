package tests;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


public abstract class JBCefFpsMeter {
    private static final Map<String, JBCefFpsMeter> INSTANCES = new HashMap<>(1);

    public abstract void paintFrameStarted();

    public abstract void paintFrameFinished(Graphics g);

    public abstract int getFps();

    public abstract void setActive(boolean active);

    public abstract boolean isActive();

    public abstract void registerComponent(Component component);

    public synchronized static JBCefFpsMeter register(String id) {
        JBCefFpsMeter instance = INSTANCES.get(id);
        if (instance != null) {
            return instance;
        }
        instance = new JBCefFpsMeterImpl(id);
        INSTANCES.put(id, instance);
        return instance;
    }

    public synchronized static JBCefFpsMeter get(String id) {
        JBCefFpsMeter instance = INSTANCES.get(id);
        if (instance == null) {
            //Logger.getInstance(JBCefFpsMeter.class).warn(JBCefFpsMeter.class + " not registered: " + id);
        }
        return instance;
    }
}

class JBCefFpsMeterImpl extends JBCefFpsMeter {
    private final AtomicInteger myFps = new AtomicInteger();
    private final AtomicInteger myFrameCount = new AtomicInteger();
    private final AtomicLong myStartMeasureTime = new AtomicLong();
    private final AtomicLong myMeasureDuration = new AtomicLong();
    private final AtomicBoolean myIsActive = new AtomicBoolean();
    private final AtomicReference<Rectangle> myFpsBarBounds = new AtomicReference<>(new Rectangle());
    private final AtomicReference<Font> myFont = new AtomicReference<>();
    private final AtomicReference<WeakReference<Component>> myComp = new AtomicReference<>(null);
    private final AtomicReference<Timer> myTimer = new AtomicReference<>();

    private static final int TICK_DELAY_MS = 1000;
    private static final int FPS_STR_OFFSET = 10;

    @SuppressWarnings("unused")
    JBCefFpsMeterImpl(String id) {
    }

    @Override
    public void paintFrameStarted() {
    }

    @Override
    public void paintFrameFinished(Graphics g) {
        if (isActive()) {
            myFrameCount.incrementAndGet();
            drawFps(g);
        }
    }

    private void tick() {
        if (myStartMeasureTime.get() > 0) {
            myMeasureDuration.set(System.nanoTime() - myStartMeasureTime.get());
            myFps.set((int)(myFrameCount.get() / ((float)myMeasureDuration.get() / 1000000000)));
        }
        myFrameCount.set(0);
        myStartMeasureTime.set(System.nanoTime());

        // during the measurement the component can be repainted partially in which case
        // the FPS bar may run out of the clip, so here we request repaint once per a tick
        requestFpsBarRepaint();
    }

    @Override
    public int getFps() {
        return Math.min(myFps.get(), 99);
    }

    @SuppressWarnings("UseJBColor")
    private void drawFps(Graphics g) {
        Graphics gr = g.create();
        try {
            gr.setColor(Color.blue);
            Rectangle r = myFpsBarBounds.get();
            gr.fillRect(r.x, r.y, r.width, r.height);
            gr.setColor(Color.green);
            gr.setFont(myFont.get());
            int fps = getFps();
            gr.drawString((fps == 0 ? "__" : fps) + " fps", FPS_STR_OFFSET, FPS_STR_OFFSET + myFont.get().getSize());
        } finally {
            gr.dispose();
        }
    }

    @Override
    public void setActive(boolean active) {
        boolean wasActive = myIsActive.getAndSet(active);
        if (active && !wasActive) {
            myTimer.set(new Timer(TICK_DELAY_MS, actionEvent -> tick()));
            myTimer.get().setRepeats(true);
            myTimer.get().start();
            reset();
        }
        else if (!active && wasActive) {
            myTimer.get().stop();
            myTimer.set(null);
            // clear the FPS bar
            requestFpsBarRepaint();
        }
    }

    @Override
    public boolean isActive() {
        return myIsActive.get();
    }

    @Override
    public void registerComponent(Component component) {
        myComp.set(new WeakReference<>(component));
    }

    private void requestFpsBarRepaint() {
        Rectangle r = myFpsBarBounds.get();
        getComponent().repaint(r.x, r.y, r.width, r.height);
    }

    private Component myDefault;

    private Component getComponent() {
        Component comp = null;
        WeakReference<Component> compRef = myComp.get();
        if (compRef != null) {
            comp = compRef.get();
        }
        if (comp != null)
            return comp;

        if (myDefault != null)
            return myDefault;

        myDefault = new JPanel();
//        try {
//            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
//            AWTAccessor.getComponentAccessor().setGraphicsConfiguration(myDefault, gc);
//        } catch (HeadlessException ignore) {
//        }
        return myDefault;
    }

    private void reset() {
        myFps.set(0);
        myFrameCount.set(0);
        myStartMeasureTime.set(0);
        myMeasureDuration.set(0);

        Component comp = getComponent();
        myFont.set(new Font("Sans", Font.BOLD, 16));
        comp.getFontMetrics(myFont.get());
        Rectangle strBounds = myFont.get().getStringBounds("00 fps", comp.getFontMetrics(myFont.get()).getFontRenderContext()).getBounds();
        myFpsBarBounds.get().setBounds(0, 0, strBounds.width + FPS_STR_OFFSET * 2, strBounds.height + FPS_STR_OFFSET * 2);
    }
}