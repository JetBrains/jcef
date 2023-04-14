// Copyright (c) 2022 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.input;

public class CefTouchEvent {
    public enum EventType {
        RELEASED(0),
        PRESSED(1),
        MOVED(2),
        CANCELLED(3);

        private final int value;

        EventType(int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }
    }

    public enum PointerType {
        TOUCH(0),
        MOUSE(1),
        PEN(2),
        ERASER(3),
        UNKNOWN(4);

        private final int value;

        PointerType(int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }
    }

    public CefTouchEvent(int id, float x, float y, float radiusX, float radiusY, float rotationAngle,
                         float pressure, EventType type, int modifiers, PointerType pointerType) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.rotationAngle = rotationAngle;
        this.pressure = pressure;
        this.type = type;
        this.modifiers = modifiers;
        this.pointerType = pointerType;
    }

    public CefTouchEvent(int id, float x, float y, EventType type) {
        this(id, x, y, 0, 0, 0, 0, type, 0, PointerType.UNKNOWN);
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

    public int getModifiers() {
        return modifiers;
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
     * The state of the touch point. Touches begin with one CEF_TET_PRESSED event followed by zero or more
     * CEF_TET_MOVED events and finally one CEF_TET_RELEASED or CEF_TET_CANCELLED event. Events not respecting
     * this order will be ignored.
     */
    final private EventType type;

    /**
     * Bit flags describing any pressed modifier keys. See cef_event_flags_t for values.
     */
    final private int modifiers;

    /**
     * The device type that caused the event.
     */
    final private PointerType pointerType;
}
