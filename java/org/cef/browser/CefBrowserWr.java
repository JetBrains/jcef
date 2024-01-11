// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import com.jetbrains.cef.JCefAppConfig;
import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.OS;
import org.cef.handler.CefWindowHandler;
import org.cef.handler.CefWindowHandlerAdapter;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

import com.jetbrains.cef.JdkEx;
import org.cef.misc.CefLog;
import sun.awt.AWTAccessor;

/**
 * This class represents a windowed rendered browser.
 * The visibility of this class is "package". To create a new
 * CefBrowser instance, please use CefBrowserFactory.
 */
class CefBrowserWr extends CefBrowser_N {
    private static final boolean USE_CANVAS = OS.isWindows() || OS.isLinux();
    private Canvas canvas_ = null;
    private Component component_ = null;
    private Rectangle content_rect_ = new Rectangle(0, 0, 0, 0);
    private long window_handle_ = 0;
    private boolean justCreated_ = false;
    private double scaleFactor_ = 1.0;
    private long delayCreationUntilMs_ = 0; // used only for testing
    private Timer delayedUpdate_ = new Timer(100, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (isClosed()) return;

                    if (delayCreationUntilMs_ > 0 &&
                        System.currentTimeMillis() < delayCreationUntilMs_ &&
                        getNativeRef("CefBrowser") == 0
                    ) {
                        CefLog.Debug("delay native browser creation (need wait %d ms)", delayCreationUntilMs_ - System.currentTimeMillis());
                        delayedUpdate_.restart();
                        return;
                    }
                    if (AWTAccessor.getComponentAccessor().getPeer(component_) == null || // not in UI yet
                        createBrowserIfRequired(true)) // has just created UI
                    {
                        delayedUpdate_.restart();
                    } else {
                        // If on Mac, this is needed due to the quirk described below
                        // (in org.cef.browser.CefBrowserWr.CefBrowserWr(...).new JPanel()
                        // {...}.paint(Graphics)). If on Linux, this is needed to invoke an
                        // XMoveResizeWindow call shortly after the UI was created. That seems to be
                        // necessary to actually get a windowed renderer to display something.
                        if (OS.isMacintosh() || OS.isLinux()) doUpdate();
                    }
                }
            });
        }
    });

    private CefWindowHandlerAdapter win_handler_ = new CefWindowHandlerAdapter() {
        private Point lastPos = new Point(-1, -1);
        private long[] nextClick = new long[MouseInfo.getNumberOfButtons()];
        private int[] clickCnt = new int[MouseInfo.getNumberOfButtons()];

        @Override
        public Rectangle getRect(CefBrowser browser) {
            synchronized (content_rect_) {
                return content_rect_;
            }
        }

        @Override
        public void onMouseEvent(CefBrowser browser, int event, final int screenX,
                final int screenY, final int modifier, final int button) {
            final Point pt = new Point(screenX, screenY);
            if (event == MouseEvent.MOUSE_MOVED) {
                // Remove mouse-moved events if the position of the cursor hasn't
                // changed.
                if (pt.equals(lastPos)) return;
                lastPos = pt;

                // Change mouse-moved event to mouse-dragged event if the left mouse
                // button is pressed.
                if ((modifier & MouseEvent.BUTTON1_DOWN_MASK) != 0)
                    event = MouseEvent.MOUSE_DRAGGED;
            }

            final int finalEvent = event;

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Component parent = SwingUtilities.getRoot(component_);
                    if (parent == null) {
                        return;
                    }
                    double scaleX = parent.getGraphicsConfiguration().getDefaultTransform().getScaleX();
                    double scaleY = parent.getGraphicsConfiguration().getDefaultTransform().getScaleY();
                    if (JdkEx.isJetBrainsJDK() && OS.isWindows()) {
                        Point parentPt = parent.getLocationOnScreen();
                        // In JBR/win the device [x, y] is preserved in device space (width/height is scaled).
                        Rectangle devBounds = parent.getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();
                        int scaledScreenX = devBounds.x + (int) Math.round((screenX - devBounds.x) / scaleX);
                        int scaledScreenY = devBounds.y + (int) Math.round((screenY - devBounds.y) / scaleY);
                        pt.x = scaledScreenX - parentPt.x;
                        pt.y = scaledScreenY - parentPt.y;
                    }
                    else {
                        pt.x = (int)Math.round(pt.x / scaleX);
                        pt.y = (int)Math.round(pt.y / scaleY);
                        SwingUtilities.convertPointFromScreen(pt, parent);
                    }

                    int clickCnt = 0;
                    long now = new Date().getTime();
                    if (finalEvent == MouseEvent.MOUSE_WHEEL) {
                        int scrollType = MouseWheelEvent.WHEEL_UNIT_SCROLL;
                        int rotation = button > 0 ? 1 : -1;
                        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new MouseWheelEvent(parent, finalEvent, now,
                                modifier, pt.x, pt.y, 0, false, scrollType, 3, rotation));
                    } else {
                        clickCnt = getClickCount(finalEvent, button);
                        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new MouseEvent(parent, finalEvent, now, modifier,
                                pt.x, pt.y, screenX, screenY, clickCnt, false, button));
                    }

                    // Always fire a mouse-clicked event after a mouse-released event.
                    if (finalEvent == MouseEvent.MOUSE_RELEASED) {
                        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                                new MouseEvent(parent, MouseEvent.MOUSE_CLICKED, now, modifier,
                                        pt.x, pt.y, screenX, screenY, clickCnt, false, button));
                    }
                }
            });
        }

        public int getClickCount(int event, int button) {
            // avoid exceptions by using modulo
            int idx = button % nextClick.length;

            switch (event) {
                case MouseEvent.MOUSE_PRESSED:
                    long currTime = new Date().getTime();
                    if (currTime > nextClick[idx]) {
                        nextClick[idx] = currTime
                                + (Integer) Toolkit.getDefaultToolkit().getDesktopProperty(
                                        "awt.multiClickInterval");
                        clickCnt[idx] = 1;
                    } else {
                        clickCnt[idx]++;
                    }
                // FALL THRU
                case MouseEvent.MOUSE_RELEASED:
                    return clickCnt[idx];
                default:
                    return 0;
            }
        }
    };

    CefBrowserWr(
            CefClient client, String url, CefRequestContext context, CefBrowserSettings settings) {
        this(client, url, context, null, null, settings);
    }

    @SuppressWarnings("serial")
    private CefBrowserWr(CefClient client, String url, CefRequestContext context,
            CefBrowserWr parent, Point inspectAt, CefBrowserSettings settings) {
        super(client, url, context, parent, inspectAt, settings);
        delayedUpdate_.setRepeats(false);
        delayCreationUntilMs_ = Long.getLong("jcef.debug.cefbrowserwr.delay_creation", 0);
        if (delayCreationUntilMs_ > 0) delayCreationUntilMs_ += System.currentTimeMillis();

        // Disabling lightweight of popup menu is required because
        // otherwise it will be displayed behind the content of component_
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        // We're using a JComponent instead of a Canvas now because the
        // JComponent has clipping informations, which aren't accessible for Canvas.
        component_ = new JPanel(new BorderLayout()) {
            private MouseListener mouseListener;
            private MouseWheelListener mouseWheelListener;
            private MouseMotionListener mouseMotionListener;
            private boolean removed_ = true;

            {
                addPropertyChangeListener("graphicsConfiguration", e -> updateScale());
            }

            @Override
            public void addMouseListener(MouseListener l) {
                mouseListener = l;
                if (canvas_ != null)
                    canvas_.addMouseListener(l);
                super.addMouseListener(l);
            }
            @Override
            public void addMouseWheelListener(MouseWheelListener l) {
                mouseWheelListener = l;
                if (canvas_ != null)
                    canvas_.addMouseWheelListener(l);
                super.addMouseWheelListener(l);
            }
            @Override
            public void addMouseMotionListener(MouseMotionListener l) {
                mouseMotionListener = l;
                if (canvas_ != null)
                    canvas_.addMouseMotionListener(l);
                super.addMouseMotionListener(l);
            }

            @Override
            public void removeMouseListener(MouseListener l) {
                mouseListener = null;
                if (canvas_ != null)
                    canvas_.removeMouseListener(l);
                super.removeMouseListener(l);
            }

            @Override
            public void removeMouseMotionListener(MouseMotionListener l) {
                mouseMotionListener = null;
                if (canvas_ != null)
                    canvas_.removeMouseMotionListener(l);
                super.removeMouseMotionListener(l);
            }

            @Override
            public void removeMouseWheelListener(MouseWheelListener l) {
                mouseWheelListener = null;
                if (canvas_ != null)
                    canvas_.removeMouseWheelListener(l);
                super.removeMouseWheelListener(l);
            }

            private void addCanvas() {
                canvas_ = new BrowserCanvas();
                if (mouseListener != null) canvas_.addMouseListener(mouseListener);
                if (mouseWheelListener != null) canvas_.addMouseWheelListener(mouseWheelListener);
                if (mouseMotionListener != null) canvas_.addMouseMotionListener(mouseMotionListener);
                this.add(canvas_, BorderLayout.CENTER);
            }
            private void removeCanvas() {
                if (canvas_ == null) return;
                if (mouseListener != null) canvas_.removeMouseListener(mouseListener);
                if (mouseWheelListener != null) canvas_.removeMouseWheelListener(mouseWheelListener);
                if (mouseMotionListener != null) canvas_.removeMouseMotionListener(mouseMotionListener);
                this.remove(canvas_);
                canvas_ = null;
            }

            @Override
            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x, y, width, height);
                wasResized((int) (width * scaleFactor_), (int) (height * scaleFactor_));
            }

            @Override
            public void setBounds(Rectangle r) {
                setBounds(r.x, r.y, r.width, r.height);
            }

            @Override
            public void setSize(int width, int height) {
                super.setSize(width, height);
                wasResized((int) (width * scaleFactor_), (int) (height * scaleFactor_));
            }

            @Override
            public void setSize(Dimension d) {
                setSize(d.width, d.height);
            }

            @Override
            public void paint(Graphics g) {
                // If the user resizes the UI component, the new size and clipping
                // informations are forwarded to the native code.
                // But on Mac the last resize information doesn't resize the native UI
                // accurately (sometimes the native UI is too small). An easy way to
                // solve this, is to send the last Update-Information again. Therefore
                // we're setting up a delayedUpdate timer which is reset each time
                // paint is called. This prevents the us of sending the UI update too
                // often.
                doUpdate();
                delayedUpdate_.restart();
            }

            @Override
            public void addNotify() {
                super.addNotify();
                updateScale();
                if (removed_) {
                    if (USE_CANVAS) {
                        // Recreate canvas to prevent its blinking at toplevel's [0,0].
                        // NOTE: generally it's a bad idea to add components inside addNotify
                        // Also it'd be better to add component before super.addNotify (because it perfroms some processing with
                        // all children - send notifications, set listeners etc)
                        addCanvas();
                    }
                    setParent(getWindowHandle(this), canvas_);
                    removed_ = false;
                }
                delayedUpdate_.restart();
            }

            @Override
            public void removeNotify() {
                if (!removed_) {
                    if (!isClosed()) {
                        setParent(0, null);
                    }
                    removed_ = true;
                    if (USE_CANVAS) {
                        removeCanvas();
                    }
                }
                super.removeNotify();
            }
        };

        // Initial minimal size of the component. Otherwise the UI won't work
        // accordingly in panes like JSplitPane.
        component_.setMinimumSize(new Dimension(0, 0));
        component_.setFocusable(true);
        component_.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                setFocus(false);
            }

            @Override
            public void focusGained(FocusEvent e) {
                // Dismiss any Java menus that are currently displayed.
                MenuSelectionManager.defaultManager().clearSelectedPath();
                setFocus(true);
            }
        });
        component_.addHierarchyBoundsListener(new HierarchyBoundsListener() {
            @Override
            public void ancestorResized(HierarchyEvent e) {
                doUpdate();
            }
            @Override
            public void ancestorMoved(HierarchyEvent e) {
                doUpdate();
                notifyMoveOrResizeStarted();
            }
        });
        component_.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    setWindowVisibility(e.getChanged().isVisible());
                }
            }
        });
    }

    // On windows we have to use a Canvas because its a heavyweight component
    // and we need its native HWND as parent for the browser UI. The same
    // technique is used on Linux as well.
    private class BrowserCanvas extends Canvas {
        @Override
        public void paint(Graphics g) {
            g.setColor(component_.getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void updateScale() {
        if (!OS.isMacintosh()) scaleFactor_ = shouldUpscale() ? JCefAppConfig.getDeviceScaleFactor(component_) : 1;
    }

    @Override
    public void createImmediately() {
        justCreated_ = true;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Create the browser immediately. It will be parented to the Java
                // window once it becomes available.
                createBrowserIfRequired(false);
            }
        });
    }

    @Override
    public Component getUIComponent() {
        return component_;
    }

    @Override
    public CefWindowHandler getWindowHandler() {
        return win_handler_;
    }

    @Override
    protected CefBrowser createDevToolsBrowser(CefClient client, String url,
            CefRequestContext context, CefBrowser parent, Point inspectAt) {
        return new CefBrowserWr(client, url, context, (CefBrowserWr) this, inspectAt, null);
    }

    private synchronized long getWindowHandle() {
        if (window_handle_ == 0 && OS.isMacintosh()) {
            window_handle_ = getWindowHandle(component_);
        }
        return window_handle_;
    }

    static long getWindowHandle(Component component) {
        if (OS.isMacintosh()) {
            try {
                Class<?> cls = Class.forName("org.cef.browser.mac.CefBrowserWindowMac");
                CefBrowserWindow browserWindow = (CefBrowserWindow) cls.newInstance();
                if (browserWindow != null) {
                    return browserWindow.getWindowHandle(component);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    private void doUpdate() {
        if (isClosed()) return;

        Rectangle vr = ((JPanel) component_).getVisibleRect();
        Rectangle clipping = new Rectangle((int) (vr.getX() * scaleFactor_),
                (int) (vr.getY() * scaleFactor_), (int) (vr.getWidth() * scaleFactor_),
                (int) (vr.getHeight() * scaleFactor_));

        if (OS.isMacintosh()) {
            Container parent = component_.getParent();
            Point contentPos = component_.getLocation();
            while (parent != null) {
                Container next = parent.getParent();
                if (next != null && next instanceof Window) break;
                Point parentPos = parent.getLocation();
                contentPos.translate(parentPos.x, parentPos.y);
                parent = next;
            }
            contentPos.translate(clipping.x, clipping.y);

            Point browserPos = clipping.getLocation();
            browserPos.x *= -1;
            browserPos.y *= -1;

            synchronized (content_rect_) {
                content_rect_ = new Rectangle(contentPos, clipping.getSize());
                Rectangle browserRect = new Rectangle(browserPos, component_.getSize());
                updateUI(content_rect_, browserRect);
            }
        } else {
            synchronized (content_rect_) {
                Rectangle bounds = null != canvas_ ? canvas_.getBounds() : component_.getBounds();
                // On Linux, content_rect_ scaling downgrades, namely:
                // - should not be scaled in JRE-managed HiDPI mode
                // - should be downscaled in IDE-managed HiDPI mode
                double scale = OS.isLinux() ?
                    (shouldUpscale() ? 1 : 1 / JCefAppConfig.getDeviceScaleFactor(component_)) :
                    scaleFactor_;
                content_rect_ = new Rectangle(
                        (int) (bounds.getX() * scale),
                        (int) (bounds.getY() * scale),
                        (int) (bounds.getWidth() * scale),
                        (int) (bounds.getHeight() * scale));
                updateUI(clipping, content_rect_);
            }
        }
    }

    private boolean createBrowserIfRequired(boolean hasParent) {
        if (isClosed()) return false;

        long windowHandle = 0;
        Component canvas = null;
        if (hasParent) {
            windowHandle = getWindowHandle();
            canvas = (OS.isWindows() || OS.isLinux()) ? canvas_ : component_;
        }

        if (getNativeRef("CefBrowser") == 0) {
            if (getParentBrowser() != null) {
                createDevTools(getParentBrowser(), getClient(), windowHandle, false, false, canvas,
                        getInspectAt());
                return true;
            } else {
                createBrowser(getClient(), windowHandle, getUrl(), false, false, canvas);
                return true;
            }
        } else if (hasParent && justCreated_) {
            setParent(windowHandle, canvas);
            // setFocus(true); do not request focus on show
            justCreated_ = false;
        }

        return false;
    }

    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        throw new UnsupportedOperationException("Unsupported for windowed rendering");
    }

    private static boolean shouldUpscale() {
        return JCefAppConfig.getForceDeviceScaleFactor() == -1;
    }

    @Override
    public void setWindowlessFrameRate(int frameRate) {
        throw new UnsupportedOperationException(
                "You can only set windowless framerate on OSR browser");
    }

    @Override
    public CompletableFuture<Integer> getWindowlessFrameRate() {
        throw new UnsupportedOperationException(
                "You can only get windowless framerate on OSR browser");
    }
}
