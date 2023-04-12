// Copyright (c) 2023 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.input;

public final class CefEventsFlags {
    public static final int EVENTFLAG_NONE = 0;
    public static final int EVENTFLAG_CAPS_LOCK_ON = 1;
    public static final int EVENTFLAG_SHIFT_DOWN = 1 << 1;
    public static final int EVENTFLAG_CONTROL_DOWN = 1 << 2;
    public static final int EVENTFLAG_ALT_DOWN = 1 << 3;
    public static final int EVENTFLAG_LEFT_MOUSE_BUTTON = 1 << 4;
    public static final int EVENTFLAG_MIDDLE_MOUSE_BUTTON = 1 << 5;
    public static final int EVENTFLAG_RIGHT_MOUSE_BUTTON = 1 << 6;
    // Mac OS-X command key.
    public static final int EVENTFLAG_COMMAND_DOWN = 1 << 7;
    public static final int EVENTFLAG_NUM_LOCK_ON = 1 << 8;
    public static final int EVENTFLAG_IS_KEY_PAD = 1 << 9;
    public static final int EVENTFLAG_IS_LEFT = 1 << 10;
    public static final int EVENTFLAG_IS_RIGHT = 1 << 11;
    public static final int EVENTFLAG_ALTGR_DOWN = 1 << 12;
    public static final int EVENTFLAG_IS_REPEAT = 1 << 13;
}