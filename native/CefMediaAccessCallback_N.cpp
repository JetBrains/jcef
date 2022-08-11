// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#include "CefMediaAccessCallback_N.h"
#include "include/cef_permission_handler.h"
#include "jni_scoped_helpers.h"

namespace {

CefRefPtr<CefMediaAccessCallback> GetSelf(jlong self) {
  return reinterpret_cast<CefMediaAccessCallback*>(self);
}

}  // namespace

JNIEXPORT void JNICALL
Java_org_cef_callback_CefMediaAccessCallback_1N_N_1Cancel(JNIEnv* env,
                                                           jobject obj,
                                                           jlong self) {
  CefRefPtr<CefMediaAccessCallback> callback = GetSelf(self);
  if (!callback)
    return;
  callback->Cancel();
}

JNIEXPORT void JNICALL
Java_org_cef_callback_CefMediaAccessCallback_1N_N_1Continue(JNIEnv* env,
                                                          jobject obj,
                                                          jlong self,
                                                          jint allowed_permissions) {
  CefRefPtr<CefMediaAccessCallback> callback = GetSelf(self);
  if (!callback)
    return;
  callback->Continue(allowed_permissions);
}

