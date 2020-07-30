// Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.detailed.util;

import javax.xml.bind.DatatypeConverter;

/**
 * Utility class for creating data: URIs that can be passed to CefBrowser.loadURL.
 */
public class DataUri {
    public static String create(String mimeType, String contents) {
        return "data:" + mimeType + ";base64,"
                + DatatypeConverter.printBase64Binary(contents.getBytes());
    }
};
