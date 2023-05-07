package org.cef.input;

import org.cef.misc.CefRange;

import java.awt.*;

public class CefCompositionUnderline {
    public CefCompositionUnderline(CefRange range, Color color, Color backgroundColor, int thick, Style style) {
        this.range = range;
        this.color = color;
        this.backgroundColor = backgroundColor;
        this.thick = thick;
        this.style = style;
    }

    public enum Style {
        SOLID,
        DOT,
        DASH,
        NONE
    }

    public CefRange getRange() {
        return range;
    }

    public Color getColor() {
        return color;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public int getThick() {
        return thick;
    }

    public Style getStyle() {
        return style;
    }

    private final CefRange range;
    private final Color color;
    private final Color backgroundColor;
    private final int thick;
    private final Style style;
}
