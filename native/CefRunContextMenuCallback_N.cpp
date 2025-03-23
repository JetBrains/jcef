// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#include "CefRunContextMenuCallback_N.h"

#include "include/cef_context_menu_handler.h"
#include "include/internal/cef_ptr.h"
#include "jni_scoped_helpers.h"

namespace {

CefRefPtr<CefRunContextMenuCallback> GetSelf(jlong self) {
  return reinterpret_cast<CefRunContextMenuCallback*>(self);
}

void ClearSelf(JNIEnv* env, jobject obj) {
  // Clear the reference added in ContextMenuHandler::RunContextMenu.
  SetCefForJNIObject_sync<CefRunContextMenuCallback>(
      env, obj, nullptr, "CefRunContextMenuCallback");
}

}  // namespace

void Java_org_cef_callback_CefRunContextMenuCallback_1N_N_1Continue(
    JNIEnv* env,
    jobject object,
    jlong self,
    jint selected_command_id,
    jint event_flag) {
  CefRefPtr<CefRunContextMenuCallback> callback = GetSelf(self);
  if (!callback)
    return;
  callback->Continue(selected_command_id, static_cast<cef_event_flags_t>(event_flag));
  ClearSelf(env, object);
}

void Java_org_cef_callback_CefRunContextMenuCallback_1N_N_1Cancel(
    JNIEnv* env,
    jobject object,
    jlong self) {
  CefRefPtr<CefRunContextMenuCallback> callback = GetSelf(self);
  if (!callback)
    return;
  callback->Cancel();
  ClearSelf(env, object);
}
