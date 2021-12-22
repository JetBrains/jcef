// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#ifndef JCEF_NATIVE_MEDIA_ACCESS_HANDLER_H_
#define JCEF_NATIVE_MEDIA_ACCESS_HANDLER_H_
#pragma once

#include <jni.h>

#include "include/cef_media_access_handler.h"

#include "jni_scoped_helpers.h"

// MediaAccessHandler implementation.
class MediaAccessHandler : public CefMediaAccessHandler {
public:
    MediaAccessHandler(JNIEnv* env, jobject handler);

    virtual bool OnRequestMediaAccessPermission(
            CefRefPtr<CefBrowser> browser,
            CefRefPtr<CefFrame> frame,
            const CefString& requesting_url,
            int32_t requested_permissions,
            CefRefPtr<CefMediaAccessCallback> callback) override;

protected:
    ScopedJNIObjectGlobal handle_;

    // Include the default reference counting implementation.
    IMPLEMENT_REFCOUNTING(MediaAccessHandler);
};

#endif  // JCEF_NATIVE_MEDIA_ACCESS_HANDLER_H_
