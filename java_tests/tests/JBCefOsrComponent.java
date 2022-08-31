package tests;


import com.jetbrains.cef.remote.CefServer;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefNativeAdapter;
import tests.JBCefOsrHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


@SuppressWarnings("NotNullFieldNotInitialized")
public
class JBCefOsrComponent extends JPanel {
    private volatile JBCefOsrHandler myRenderHandler;
    private final MyScale myScale = new MyScale();
    
    private Timer myTimer;

    private CefBrowser myBrowser;
    private CefServer myCefServer;
    private int myBid;
    
    public JBCefOsrComponent() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.CYAN);
        addPropertyChangeListener("graphicsConfiguration",
                e -> myRenderHandler.updateScale(myScale.update(myRenderHandler.getDeviceScaleFactor(myBrowser))));

        enableEvents(AWTEvent.KEY_EVENT_MASK |
                AWTEvent.MOUSE_EVENT_MASK |
                AWTEvent.MOUSE_WHEEL_EVENT_MASK |
                AWTEvent.MOUSE_MOTION_EVENT_MASK);

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
    }

    public void setBrowser(CefBrowser browser) {
        myBrowser = browser;
    }

    public void setRemoteBid(CefServer server, int bid) {
        myCefServer = server;
        myBid = bid;
    }

    public void setRenderHandler(JBCefOsrHandler renderHandler) {
        myRenderHandler = renderHandler;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (myBrowser != null && ((CefNativeAdapter)myBrowser).getNativeRef("CefBrowser") == 0) {
            myBrowser.createImmediately();
        }
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
            else if (myCefServer != null) {
                int[] data = new int[]{sizeX, sizeY};
                ByteBuffer params = ByteBuffer.allocate(data.length*4);
                params.order(ByteOrder.nativeOrder());
                params.asIntBuffer().put(data);
                myCefServer.invoke(myBid, "wasresized", params);
            }
        });
        myTimer.setRepeats(false);
        myTimer.start();
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
        else if (myCefServer != null) {
            int[] data = new int[]{
                e.getID() == MouseEvent.MOUSE_RELEASED ? 0 : 1,
                e.getModifiersEx(),
                (int)Math.round(e.getX() / scale),
                (int)Math.round(e.getY() / scale),
                (int)Math.round(e.getXOnScreen() / scale),
                (int)Math.round(e.getYOnScreen() / scale),
                e.getClickCount(),
                e.isPopupTrigger() ? 1 : 0,
                e.getButton()
            };
            ByteBuffer params = ByteBuffer.allocate(data.length*4);
            params.order(ByteOrder.nativeOrder());
            params.asIntBuffer().put(data);
            myCefServer.invoke(myBid, "sendmouseevent", params);
        }

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
        else if (myCefServer != null) {
            int[] data = new int[]{
                    e.getID() == KeyEvent.KEY_RELEASED ? 0 : 1,
                    e.getKeyChar(),
                    e.getKeyCode(),
                    e.getModifiersEx()
            };
            ByteBuffer params = ByteBuffer.allocate(data.length*4);
            params.order(ByteOrder.nativeOrder());
            params.asIntBuffer().put(data);
            myCefServer.invoke(myBid, "sendkeyevent", params);
        }
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
