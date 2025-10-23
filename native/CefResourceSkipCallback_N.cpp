// Copyright (c) 2024 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#include "CefResourceSkipCallback_N.h"
#include "include/cef_resource_handler.h"
#include "jni_scoped_helpers.h"

namespace {

CefRefPtr<CefResourceSkipCallback> GetSelf(jlong self) {
  return reinterpret_cast<CefResourceSkipCallback*>(self);
}

void ClearSelf(JNIEnv* env, jobject obj) {
  // Clear the reference added in ResourceHandler.
  SetCefForJNIObject<CefResourceSkipCallback>(env, obj, nullptr,
                                              "CefResourceSkipCallback");
}

}  // namespace

JNIEXPORT void JNICALL
Java_org_cef_callback_CefResourceSkipCallback_1N_N_1Continue(
    JNIEnv* env,
    jobject obj,
    jlong self,
    jlong bytes_skipped) {
  CefRefPtr<CefResourceSkipCallback> callback = GetSelf(self);
  if (!callback)
    return;
  callback->Continue(bytes_skipped);
  ClearSelf(env, obj);
}
