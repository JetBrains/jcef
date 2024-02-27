package tests;


import org.cef.CefClient;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.im.InputMethodRequests;

@SuppressWarnings("NotNullFieldNotInitialized")
public
class JBCefOsrComponent extends JPanel {
    private volatile JBCefOsrHandler myRenderHandler;
    private volatile CefBrowser myBrowser;
    private final MyScale myScale = new MyScale();
    
    private Timer myTimer;

    private final CefInputMethodAdapter myCefInputMethodAdapter;

    public JBCefOsrComponent() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.CYAN);
        addPropertyChangeListener("graphicsConfiguration",
                e -> {
                    myRenderHandler.updateScale(myScale.update(myRenderHandler.getDeviceScaleFactor(myBrowser)));
                    myBrowser.notifyScreenInfoChanged();
                });

        enableEvents(AWTEvent.KEY_EVENT_MASK |
                AWTEvent.MOUSE_EVENT_MASK |
                AWTEvent.MOUSE_WHEEL_EVENT_MASK |
                AWTEvent.MOUSE_MOTION_EVENT_MASK |
                AWTEvent.INPUT_METHOD_EVENT_MASK);
        enableInputMethods(true);

        setFocusable(true);
        setRequestFocusEnabled(true);
        // [tav] todo: so far the browser component can not be traversed out
        setFocusTraversalKeysEnabled(false);
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (myBrowser != null)
                    myBrowser.setFocus(true);
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (myBrowser != null)
                    myBrowser.setFocus(false);
            }
        });

        enableInputMethods(true);
        myCefInputMethodAdapter = new CefInputMethodAdapter(myBrowser, this);
        addInputMethodListener(myCefInputMethodAdapter);
    }

    public void setBrowser(CefBrowser browser) {
        myBrowser = browser;
        myCefInputMethodAdapter.setBrowser(browser);
    }

    public void setRenderHandler(JBCefOsrHandler renderHandler) {
        myRenderHandler = renderHandler;
    }

    @Override
    public void addNotify() {
        super.addNotify();

        if (!CefClient.isNativeBrowserCreationStarted(myBrowser))
            myBrowser.createImmediately();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        myRenderHandler.paint((Graphics2D)g);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void reshape(int x, int y, int w, int h) {
        super.reshape(x, y, w, h);
        if (myTimer != null) {
            myTimer.stop();
        }

        double scale = myScale.getInverted();
        myTimer = new Timer(100, e -> {
        int sizeX = (int) Math.ceil(w * scale);
        int sizeY = (int) Math.ceil(h * scale);
        if (myBrowser != null)
            myBrowser.wasResized(sizeX, sizeY);
        });
        myTimer.setRepeats(false);
        myTimer.start();
    }

    @Override
    public InputMethodRequests getInputMethodRequests() {
        return myCefInputMethodAdapter;
    }

    public CefInputMethodAdapter getCefInputMethodAdapter() {
        return myCefInputMethodAdapter;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);

        double scale = myScale.getIdeBiased();
        if (myBrowser != null)
            myBrowser.sendMouseEvent(new MouseEvent(
                e.getComponent(),
                e.getID(),
                e.getWhen(),
                e.getModifiersEx(),
                (int)Math.round(e.getX() / scale),
                (int)Math.round(e.getY() / scale),
                (int)Math.round(e.getXOnScreen() / scale),
                (int)Math.round(e.getYOnScreen() / scale),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getButton()));

        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            requestFocusInWindow();
        }
    }

    @Override
    protected void processMouseWheelEvent(MouseWheelEvent e) {
        super.processMouseWheelEvent(e);

        double val = e.getPreciseWheelRotation() * Integer.getInteger("ide.browser.jcef.osr.wheelRotation.factor", 1) * (-1);
        double scale = myScale.getIdeBiased();
        if (myBrowser != null)
            myBrowser.sendMouseWheelEvent(new MouseWheelEvent(
                e.getComponent(),
                e.getID(),
                e.getWhen(),
                e.getModifiersEx(),
                (int)Math.round(e.getX() / scale),
                (int)Math.round(e.getY() / scale),
                (int)Math.round(e.getXOnScreen() / scale),
                (int)Math.round(e.getYOnScreen() / scale),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getScrollType(),
                e.getScrollAmount(),
                (int)val,
                val));
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        super.processMouseMotionEvent(e);

        double scale = myScale.getIdeBiased();
        if (myBrowser != null)
            myBrowser.sendMouseEvent(new MouseEvent(
                e.getComponent(),
                e.getID(),
                e.getWhen(),
                e.getModifiersEx(),
                (int)Math.round(e.getX() / scale),
                (int)Math.round(e.getY() / scale),
                (int)Math.round(e.getXOnScreen() / scale),
                (int)Math.round(e.getYOnScreen() / scale),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getButton()));
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
        if (myBrowser != null)
            myBrowser.sendKeyEvent(e);
    }

    static class MyScale {
        private volatile double myScale = 1;
        private volatile double myInvertedScale = 1;

        public MyScale update(double scale) {
            myScale = scale;
            return this;
        }

        public MyScale update(MyScale scale) {
            myScale = scale.myScale;
            myInvertedScale = scale.myInvertedScale;
            return this;
        }

        public double get() {
            return myScale;
        }

        public double getInverted() {
            return myScale;
        }

        public double getIdeBiased() {
            // IDE-managed HiDPI
            return 1;
        }

        public double getJreBiased() {
            // JRE-managed HiDPI
            return myScale;
        }
    }
}
