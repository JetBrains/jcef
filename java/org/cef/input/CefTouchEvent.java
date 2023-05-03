// Copyright (c) 2022 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.input;

public class CefTouchEvent {
    public enum EventType {
        RELEASED,
        PRESSED,
        MOVED,
        CANCELLED
    }

    public enum PointerType {
        TOUCH,
        MOUSE,
        PEN,
        ERASER,
        UNKNOWN
    }

    public CefTouchEvent(int id, float x, float y, float radiusX, float radiusY, float rotationAngle,
                         float pressure, EventType type, int modifiersEx, PointerType pointerType) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.rotationAngle = rotationAngle;
        this.pressure = pressure;
        this.type = type;
        this.modifiersEx = modifiersEx;
        this.pointerType = pointerType;
    }

    public int getId() {
        return id;
    }

    public float getX() {
        return x;
    }


    public float getY() {
        return y;
    }

    public float getRadiusX() {
        return radiusX;
    }

    public float getRadiusY() {
        return radiusY;
    }

    public float getRotationAngle() {
        return rotationAngle;
    }

    public float getPressure() {
        return pressure;
    }

    public EventType getType() {
        return type;
    }

    public int getModifiersEx() {
        return modifiersEx;
    }

    public PointerType getPointerType() {
        return pointerType;
    }

    /**
     * The id of a touch point. Must be unique per touch, can be any number except -1. Note that a maximum of 16
     * concurrent touches will be tracked; touches beyond that will be ignored.
     */
    private final int id;

    /**
     * X coordinate relative to the left side of the view.
     */
    final private float x;

    /**
     * Y coordinate relative to the top side of the view.
     */
    final private float y;

    /**
     * X radius in pixels. Set to 0 if not applicable.
     */
    final private float radiusX;

    /**
     * Y radius in pixels. Set to 0 if not applicable.
     */
    final private float radiusY;

    /**
     * Rotation angle in radians. Set to 0 if not applicable.
     */
    final private float rotationAngle;

    /**
     * The normalized pressure of the pointer input in the range of [0,1]. Set to 0 if not applicable.
     */
    final private float pressure;

    /**
     * The state of the touch point. Touches begin with one PRESSED event followed by zero or more
     * MOVED events and finally one RELEASED or CANCELLED event. Events not respecting
     * this order will be ignored.
     */
    final private EventType type;

    /**
     * Bit flags describing any pressed modifier keys.
     * See {@link java.awt.event.InputEvent#getModifiersEx()} for values.
     */
    final private int modifiersEx;

    /**
     * The device type that caused the event.
     */
    final private PointerType pointerType;
}
