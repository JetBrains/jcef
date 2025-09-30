package org.cef.handler;

import java.awt.*;

public class CefAcceleratedPaintInfo {
    public enum ColorType {
        CEF_COLOR_TYPE_RGBA_8888,
        CEF_COLOR_TYPE_BGRA_8888
    }

    public ColorType format;

    public long handle;

    public Dimension codedSize;
}
