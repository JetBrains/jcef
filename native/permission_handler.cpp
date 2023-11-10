// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#include "permission_handler.h"

#include "include/base/cef_bind.h"
#include "include/wrapper/cef_closure_task.h"

#include "client_handler.h"
#include "jni_util.h"

namespace {

// JNI CefJSDialogCallback object.
class ScopedJNIMediaAccessCallback : public ScopedJNIObject<CefMediaAccessCallback> {
 public:
  ScopedJNIMediaAccessCallback(JNIEnv* env, CefRefPtr<CefMediaAccessCallback> obj)
      : ScopedJNIObject<CefMediaAccessCallback>(
            env,
            obj,
            "org/cef/callback/CefMediaAccessCallback_N",
            "CefMediaAccessCallback") {}
};

}  // namespace

PermissionHandler::PermissionHandler(JNIEnv* env, jobject handler)
        : handle_(env, handler) {}

bool PermissionHandler::OnRequestMediaAccessPermission(
        CefRefPtr<CefBrowser> browser,
        CefRefPtr<CefFrame> frame,
        const CefString& requesting_url,
        uint32_t requested_permissions,
        CefRefPtr<CefMediaAccessCallback> callback) {
    ScopedJNIEnv env;
    if (!env)
        return false;

    ScopedJNIBrowser jbrowser(env, browser);
    ScopedJNIFrame jframe(env, frame);
    ScopedJNIString jrequestUrl(env, requesting_url);
    ScopedJNIMediaAccessCallback jcallback(env, callback);

    jboolean jresult = JNI_FALSE;
    JNI_CALL_METHOD(env, handle_, "onRequestMediaAccessPermission",
                         "(Lorg/cef/browser/CefBrowser;"
                         "Lorg/cef/browser/CefFrame;"
                         "Ljava/lang/String;"
                         "I"
                         "Lorg/cef/callback/CefMediaAccessCallback;)Z",
                         Boolean, jresult,
                         jbrowser.get(), jframe.get(),
                         jrequestUrl.get(), (jint)requested_permissions,
                         jcallback.get());
    return (jresult != JNI_FALSE);
}
