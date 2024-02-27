package tests;
// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import com.jetbrains.JBR;
import com.jetbrains.cef.JCefAppConfig;
import com.jetbrains.cef.SharedMemory;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefDragData;
import org.cef.handler.CefNativeRenderHandler;
import org.cef.handler.CefScreenInfo;
import org.cef.misc.CefRange;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.VolatileImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/**
 * A render handler for an off-screen browser.
 *
 * @see JBCefOsrComponent
 * @author tav
 */
public class JBCefOsrHandler implements CefNativeRenderHandler {
    private static final int CLEAN_CACHE_SIZE = Integer.getInteger("jcef.remote.tests.clean_cache_size", 4);
    private static final int CLEAN_CACHE_TIME_MS = Integer.getInteger("jcef.remote.tests.clean_cache_time_ms", 10*1000); // 10 sec

    interface ScreenBoundsProvider {
        Rectangle fun(JComponent param);
    }
    public static final ScreenBoundsProvider ourDefaultScreenBoundsProvider = new ScreenBoundsProvider() {
        @Override
        public Rectangle fun(JComponent comp) {
            if (comp != null && !GraphicsEnvironment.isHeadless()) {
                try {
                    return comp.isShowing() ?
                            comp.getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds() :
                            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return new Rectangle(0, 0, 0, 0);
        }
    };

    private final JBCefOsrComponent myComponent;
    private final ScreenBoundsProvider myScreenBoundsProvider;
    private final AtomicReference<Point> myLocationOnScreenRef = new AtomicReference<>(new Point());
    private final JBCefOsrComponent.MyScale myScale = new JBCefOsrComponent.MyScale();

    private volatile BufferedImage myImage;
    private volatile VolatileImage myVolatileImage;

    private final static Point ZERO_POINT = new Point();
    private final static Rectangle ZERO_RECT = new Rectangle();

    // jcef thread only
    private Rectangle myPopupBounds = ZERO_RECT;
    private boolean myPopupShown;
    private final CountDownLatch initLatch = new CountDownLatch(1);

    private final Map<String, SharedMemory.WithRaster> mySharedMemCache = new ConcurrentHashMap<>();
    private boolean myIsDisposed = false;

    private JBCefFpsMeter myFpsMeter;

    public JBCefOsrHandler(JBCefOsrComponent component, ScreenBoundsProvider screenBoundsProvider) {
        myComponent = component;
        component.setRenderHandler(this);
        myScreenBoundsProvider = screenBoundsProvider != null ? screenBoundsProvider : ourDefaultScreenBoundsProvider;

        myComponent.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                updateLocation();
            }
        });

        myComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                updateLocation();
            }
        });

        myFpsMeter = JBCefFpsMeter.register(this.toString());
        myFpsMeter.registerComponent(myComponent);
        myFpsMeter.setActive(true);
    }

    @Override
    public void disposeNativeResources() {
        if (myIsDisposed)
            return;
        myIsDisposed = true;
        for (SharedMemory.WithRaster mem: mySharedMemCache.values())
            mem.close();
        mySharedMemCache.clear();
    }

    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        double scale = myScale.getIdeBiased();
        return new Rectangle(0, 0, (int)Math.ceil(myComponent.getWidth() / scale), (int)Math.ceil(myComponent.getHeight() / scale));
    }

    @Override
    public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
        Rectangle rect = myScreenBoundsProvider.fun(myComponent);
        screenInfo.Set(getDeviceScaleFactor(browser), 32, 4, false, rect, rect);
        return true;
    }

    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        Point pt = viewPoint.getLocation();
        Point loc = getLocation();
        Rectangle rect = myScreenBoundsProvider.fun(myComponent);
        pt.setLocation(loc.x + pt.x, rect.height - loc.y - pt.y);
        return pt;
    }

    @Override
    public double getDeviceScaleFactor(CefBrowser browser) {
        return JCefAppConfig.getDeviceScaleFactor(myComponent);
    }

    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {
        myPopupShown = show;
    }

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {
        myPopupBounds = scaleUp(size);
    }

    private void cleanCacheIfNecessary() {
        final long timeMs = System.currentTimeMillis();
        if (mySharedMemCache.size() < CLEAN_CACHE_SIZE)
            return;

        ArrayList<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, SharedMemory.WithRaster> item: mySharedMemCache.entrySet()) {
            if (timeMs - item.getValue().lasUsedMs > CLEAN_CACHE_TIME_MS) {
                toRemove.add(item.getKey());
            }
        }
        for (String name: toRemove) {
            SharedMemory.WithRaster removed = mySharedMemCache.remove(name);
            if (removed != null)
                removed.close();
        }

    }

    @Override
    public void onPaintWithSharedMem(CefBrowser browser, boolean popup, int dirtyRectsCount, String sharedMemName, long sharedMemHandle, int width, int height) {
        // TODO: support popups
        long startMs = System.currentTimeMillis();

        SharedMemory.WithRaster mem = mySharedMemCache.get(sharedMemName);
        if (mem == null) {
            cleanCacheIfNecessary();
            mem = new SharedMemory.WithRaster(sharedMemName, sharedMemHandle);
            mySharedMemCache.put(sharedMemName, mem);
        }

        mem.setWidth(width);
        mem.setHeight(height);
        mem.setDirtyRectsCount(dirtyRectsCount);
        mem.lasUsedMs = startMs;

        BufferedImage bufImage = myImage;
        VolatileImage volatileImage = myVolatileImage;
        final double jreScale = myScale.getJreBiased();
        final int scaledW = (int)(width / jreScale);
        final int scaledH = (int)(height / jreScale);
        if (volatileImage == null || volatileImage.getWidth() != scaledW || volatileImage.getHeight() != scaledH) {
            try {
                if (myComponent.getGraphicsConfiguration() == null)
                    return; // TODO: hold more carefully

                volatileImage = myComponent.getGraphicsConfiguration().createCompatibleVolatileImage(scaledW, scaledH, null, Transparency.TRANSLUCENT);
                if (!JBR.isNativeRasterLoaderSupported())
                    bufImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
            mem.setDirtyRectsCount(0);// will cause full raster loading
        }
        long midMs = System.currentTimeMillis();

        if (JBR.isNativeRasterLoaderSupported()) {
            JBR.getNativeRasterLoader().loadNativeRaster(volatileImage, mem.getPtr(), mem.getWidth(), mem.getHeight(), mem.getPtr() + mem.getWidth()*mem.getHeight()*4, mem.getDirtyRectsCount());
        } else {
            // load buffered
            loadBuffered(bufImage, mem);

            // draw buffered onto volatile
            Graphics2D viGr = (Graphics2D)volatileImage.getGraphics().create();
            try {
                double sx = viGr.getTransform().getScaleX();
                double sy = viGr.getTransform().getScaleY();
                viGr.scale(1 / sx, 1 / sy);
                viGr.drawImage(bufImage,
                        0, 0, width, height,
                        0, 0, width, height,
                        null);
            }
            finally {
                viGr.dispose();
            }
        }
        myVolatileImage = volatileImage;
        myImage = bufImage;

        // TODO: calculate outerRect
        //Rectangle outerRect = findOuterRect(dirtyRects);
        //SwingUtilities.invokeLater(() -> myComponent.repaint(scaleDown(outerRect)));
        SwingUtilities.invokeLater(() -> myComponent.repaint());

        long endMs = System.currentTimeMillis();
        System.err.println("onPaintWithSharedMem spent " + (endMs - startMs) + " ms, load spent " + (endMs - midMs));
    }

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        long startMs = System.currentTimeMillis();
        initLatch.countDown();
        BufferedImage image = myImage;
        VolatileImage volatileImage = myVolatileImage;

        //
        // Recreate images when necessary
        //
        if (!popup) {
            Dimension size = getDevImageSize();
            if (size.width != width || size.height != height) {
                image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
                volatileImage = myComponent.createVolatileImage(width, height);
                dirtyRects = new Rectangle[]{new Rectangle(0, 0, width, height)};
            }
        }
        assert image != null;

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        // {volatileImage} can be null if myComponent is not yet displayed, in that case we will use {myImage} in {paint(Graphics)} as
        // it can be called (asynchronously) when {myComponent} has already been displayed - in order not to skip the {onPaint} request
        if (volatileImage != null && volatileImage.contentsLost()) {
            int result = volatileImage.validate(myComponent.getGraphicsConfiguration());
            if (result != VolatileImage.IMAGE_OK) {
                dirtyRects = new Rectangle[]{ new Rectangle(0, 0, width, height) };
            }
            if (result == VolatileImage.IMAGE_INCOMPATIBLE) {
                volatileImage = myComponent.createVolatileImage(imageWidth, imageHeight);
            }
        }

        int[] dst = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        IntBuffer src = buffer.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();

        //
        // Adjust the dirty rects for a popup case (a workaround for not enough erase rect after popup close)
        //
        if (!popup && !myPopupShown && myPopupBounds != ZERO_RECT) {
            // first repaint after popup close
            var rects = new ArrayList<>(Arrays.asList(dirtyRects));
            rects.add(myPopupBounds);
            Rectangle outerRect = findOuterRect(rects.toArray(new Rectangle[0]));
            // mind the bounds of the {buffer}
            outerRect = outerRect.intersection(new Rectangle(0, 0, width, height));
            dirtyRects = new Rectangle[]{ outerRect };
            myPopupBounds = ZERO_RECT;
        }

        //
        // Copy pixels into the BufferedImage
        //
        Point popupLoc = popup ? myPopupBounds.getLocation() : ZERO_POINT;

        for (Rectangle rect : dirtyRects) {
            if (rect.width < imageWidth) {
                for (int line = rect.y; line < rect.y + rect.height; line++) {
                    int srcOffset = line * width + rect.x;
                    int dstOffset = (line + popupLoc.y) * imageWidth + (rect.x + popupLoc.x);
                    src.position(srcOffset).get(dst, dstOffset, Math.min(rect.width, src.capacity() - srcOffset));
                }
            }
            else { // optimized for a buffer wide dirty rect
                int srcOffset = rect.y * width;
                int dstOffset = (rect.y + popupLoc.y) * imageWidth;
                src.position(srcOffset).get(dst, dstOffset, Math.min(rect.height * width, src.capacity() - srcOffset));
            }
        }

        long midMs = System.currentTimeMillis();

        //
        // Draw the BufferedImage into the VolatileImage
        //
        Rectangle outerRect = findOuterRect(dirtyRects);
        if (popup) outerRect.translate(popupLoc.x, popupLoc.y);

        if (volatileImage != null) {
            Graphics2D viGr = (Graphics2D)volatileImage.getGraphics().create();
            try {
                double sx = viGr.getTransform().getScaleX();
                double sy = viGr.getTransform().getScaleY();
                viGr.scale(1 / sx, 1 / sy);
                viGr.drawImage(image,
                        outerRect.x, outerRect.y, outerRect.x + outerRect.width, outerRect.y + outerRect.height,
                        outerRect.x, outerRect.y, outerRect.x + outerRect.width, outerRect.y + outerRect.height,
                        null);
            }
            finally {
                viGr.dispose();
            }
        }
        myImage = image;
        myVolatileImage = volatileImage;
        SwingUtilities.invokeLater(() -> myComponent.repaint(popup ? scaleDown(new Rectangle(0, 0, imageWidth, imageHeight)) : scaleDown(outerRect)));

        long endMs = System.currentTimeMillis();
        System.err.println("onPaint spent " + (endMs - startMs) + " ms, draw buffered spent " + (endMs - midMs));
    }

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        SwingUtilities.invokeLater(() -> myComponent.setCursor(new Cursor(cursorType)));
        return true;
    }

    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        return false;
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {
    }

    @Override
    public void OnImeCompositionRangeChanged(CefBrowser browser, CefRange selectionRange, Rectangle[] characterBounds) {
        myComponent.getCefInputMethodAdapter().OnImeCompositionRangeChanged(selectionRange, characterBounds);
    }

    @Override
    public void OnTextSelectionChanged(CefBrowser browser, String selectedText, CefRange selectionRange) {
        myComponent.getCefInputMethodAdapter().OnTextSelectionChanged(selectedText, selectionRange);
    }

    public void paint(Graphics2D g) {
        // The dirty rects passed to onPaint are set as the clip on the graphics, so here we draw the whole image.
        Image volatileImage = myVolatileImage;
        Image image = myImage;
        if (volatileImage != null) {
            g.drawImage(volatileImage, 0, 0, null );
        } else if (image != null) {
            System.err.println("ERROR!!! Unimplemented drawing of JBHiDPIScaledImage");
            g.drawImage(image, 0, 0, null );
            //UIUtil.drawImage(g, image, 0, 0, null);
        }

        myFpsMeter.paintFrameFinished(g);
    }

    private static Rectangle findOuterRect(Rectangle[] rects) {
        if (rects.length == 1) return rects[0];

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = 0;
        int maxY = 0;
        for (Rectangle rect : rects) {
            if (rect.x < minX) minX = rect.x;
            if (rect.y < minY) minY = rect.y;
            int rX = rect.x + rect.width;
            if (rX > maxX) maxX = rX;
            int rY = rect.y + rect.height;
            if (rY > maxY) maxY = rY;
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    public void updateScale(JBCefOsrComponent.MyScale scale) {
        myScale.update(scale);
    }

    private void updateLocation() {
        // getLocationOnScreen() is an expensive op, so do not request it on every mouse move, but cache
        myLocationOnScreenRef.set(myComponent.getLocationOnScreen());
    }

    private Point getLocation() {
        return myLocationOnScreenRef.get().getLocation();
    }

    private Dimension getDevImageSize() {
        BufferedImage image = myImage;
        if (image == null) return new Dimension(0, 0);

        return new Dimension(image.getWidth(), image.getHeight());
    }

    private Rectangle scaleDown(Rectangle rect) {
        double scale = myScale.getJreBiased();
        return new Rectangle((int)Math.floor(rect.x / scale), (int)Math.floor(rect.y / scale),
                (int)Math.ceil(rect.width / scale), (int)Math.ceil(rect.height / scale));
    }

    private Rectangle scaleUp(Rectangle rect) {
        double scale = myScale.getJreBiased();
        return new Rectangle((int)Math.floor(rect.x * scale), (int)Math.floor(rect.y * scale),
                (int)Math.ceil(rect.width * scale), (int)Math.ceil(rect.height * scale));
    }

    private Point scaleUp(Point pt) {
        double scale = myScale.getJreBiased();
        return new Point((int)Math.round(pt.x * scale), (int)Math.round(pt.y * scale));
    }

    public boolean awaitInit() throws InterruptedException {
        return initLatch.await(5, TimeUnit.SECONDS);
    }

    public static void loadBuffered(BufferedImage bufImage, SharedMemory.WithRaster mem) {
        final int width = mem.getWidth();
        final int height = mem.getHeight();
        final int rectsCount = mem.getDirtyRectsCount();

        ByteBuffer buffer = mem.wrapRaster();
        int[] dst = ((DataBufferInt)bufImage.getRaster().getDataBuffer()).getData();
        IntBuffer src = buffer.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();

        Rectangle[] dirtyRects = new Rectangle[]{new Rectangle(0, 0, width, height)};
        if (rectsCount > 0) {
            ByteBuffer rectsMem = mem.wrapRects();
            IntBuffer rects = rectsMem.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
            for (int c = 0; c < rectsCount; ++c) {
                int pos = c*4;
                Rectangle r = new Rectangle();
                r.x = rects.get(pos++);
                r.y = rects.get(pos++);
                r.width = rects.get(pos++);
                r.height = rects.get(pos++);
                dirtyRects[c] = r;
            }
        }

        for (Rectangle rect : dirtyRects) {
            if (rect.width < width) {
                for (int line = rect.y; line < rect.y + rect.height; line++) {
                    int offset = line*width + rect.x;
                    src.position(offset).get(dst, offset, rect.width);
                }
            }
            else { // optimized for a buffer wide dirty rect
                int offset = rect.y*width;
                src.position(offset).get(dst, offset, width*rect.height);
            }
        }

//         // draw debug
//         Graphics2D g = bufImage.createGraphics();
//         g.setColor(Color.RED);
//         for (Rectangle r : dirtyRects)
//             g.drawRect(r.x, r.y, r.width, r.height);
//         g.dispose();
    }
}
