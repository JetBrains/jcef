// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#include "render_handler.h"

#include "client_handler.h"
#include "jni_util.h"

namespace {

// Create a new java.awt.Rectangle.
jobject NewJNIRect(JNIEnv* env, const CefRect& rect) {
  ScopedJNIClass cls(env, "java/awt/Rectangle");
  if (!cls)
    return NULL;

  ScopedJNIObjectLocal obj(env, NewJNIObject(env, cls));
  if (!obj)
    return NULL;

  if (SetJNIFieldInt(env, cls, obj, "x", rect.x) &&
      SetJNIFieldInt(env, cls, obj, "y", rect.y) &&
      SetJNIFieldInt(env, cls, obj, "width", rect.width) &&
      SetJNIFieldInt(env, cls, obj, "height", rect.height)) {
    return obj.Release();
  }

  return NULL;
}

// create a new array of java.awt.Rectangle.
jobjectArray NewJNIRectArray(JNIEnv* env, const std::vector<CefRect>& vals) {
  if (vals.empty())
    return NULL;

  ScopedJNIClass cls(env, "java/awt/Rectangle");
  if (!cls)
    return NULL;

  const jsize size = static_cast<jsize>(vals.size());
  jobjectArray arr = env->NewObjectArray(size, cls, NULL);

  for (jsize i = 0; i < size; i++) {
    ScopedJNIObjectLocal rect_obj(env, NewJNIRect(env, vals[i]));
    env->SetObjectArrayElement(arr, i, rect_obj);
  }

  return arr;
}

// Create a new java.awt.Point.
jobject NewJNIPoint(JNIEnv* env, int x, int y) {
  ScopedJNIClass cls(env, "java/awt/Point");
  if (!cls)
    return NULL;

  ScopedJNIObjectLocal obj(env, NewJNIObject(env, cls));
  if (!obj)
    return NULL;

  if (SetJNIFieldInt(env, cls, obj, "x", x) &&
      SetJNIFieldInt(env, cls, obj, "y", y)) {
    return obj.Release();
  }

  return NULL;
}

int GetCursorId(cef_cursor_type_t type) {
  ScopedJNIEnv env;
  if (!env)
    return 0;

  ScopedJNIClass cls(env, "java/awt/Cursor");
  if (!cls)
    return 0;

  JNI_STATIC_DEFINE_INT_RV(env, cls, CROSSHAIR_CURSOR, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, DEFAULT_CURSOR, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, E_RESIZE_CURSOR, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, HAND_CURSOR, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, MOVE_CURSOR, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, N_RESIZE_CURSOR, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, NE_RESIZE_CURSOR, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, NW_RESIZE_CURSOR, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, S_RESIZE_CURSOR, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, SE_RESIZE_CURSOR, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, SW_RESIZE_CURSOR, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, TEXT_CURSOR, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, W_RESIZE_CURSOR, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, WAIT_CURSOR, 0);

  switch (type) {
    case CT_CROSS:
      return JNI_STATIC(CROSSHAIR_CURSOR);
    case CT_HAND:
      return JNI_STATIC(HAND_CURSOR);
    case CT_IBEAM:
      return JNI_STATIC(TEXT_CURSOR);
    case CT_WAIT:
      return JNI_STATIC(WAIT_CURSOR);
    case CT_EASTRESIZE:
      return JNI_STATIC(E_RESIZE_CURSOR);
    case CT_NORTHRESIZE:
      return JNI_STATIC(N_RESIZE_CURSOR);
    case CT_NORTHEASTRESIZE:
      return JNI_STATIC(NE_RESIZE_CURSOR);
    case CT_NORTHWESTRESIZE:
      return JNI_STATIC(NW_RESIZE_CURSOR);
    case CT_SOUTHRESIZE:
      return JNI_STATIC(S_RESIZE_CURSOR);
    case CT_SOUTHEASTRESIZE:
      return JNI_STATIC(SE_RESIZE_CURSOR);
    case CT_SOUTHWESTRESIZE:
      return JNI_STATIC(SW_RESIZE_CURSOR);
    case CT_WESTRESIZE:
      return JNI_STATIC(W_RESIZE_CURSOR);
    case CT_MOVE:
      return JNI_STATIC(MOVE_CURSOR);
    default:
      return JNI_STATIC(DEFAULT_CURSOR);
  }
}

}  // namespace

RenderHandler::RenderHandler(JNIEnv* env, jobject handler)
    : handle_(env, handler) {}

bool RenderHandler::GetRootScreenRect(CefRefPtr<CefBrowser> browser,
                                      CefRect& rect) {
  ScopedJNIEnv env;
  if (!env)
    return false;

  ScopedJNIBrowser jbrowser(env, browser);
  bool result = GetViewRect(jbrowser, rect);
  return result;
}

void RenderHandler::GetViewRect(CefRefPtr<CefBrowser> browser, CefRect& rect) {
  ScopedJNIEnv env;
  if (!env)
    return;

  ScopedJNIBrowser jbrowser(env, browser);
  if (!GetViewRect(jbrowser, rect)) {
    rect = CefRect(0, 0, 1, 1);
  }
}

bool RenderHandler::GetScreenPoint(CefRefPtr<CefBrowser> browser,
                                   int viewX,
                                   int viewY,
                                   int& screenX,
                                   int& screenY) {
  ScopedJNIEnv env;
  if (!env)
    return false;

  ScopedJNIBrowser jbrowser(env, browser);
  return GetScreenPoint(jbrowser, viewX, viewY, screenX, screenY);
}

void RenderHandler::OnPopupShow(CefRefPtr<CefBrowser> browser, bool show) {
  ScopedJNIEnv env;
  if (!env)
    return;

  ScopedJNIBrowser jbrowser(env, browser);
  JNI_CALL_VOID_METHOD(env, handle_, "onPopupShow",
                       "(Lorg/cef/browser/CefBrowser;Z)V", jbrowser.get(),
                       (jboolean)show);
}

void RenderHandler::OnPopupSize(CefRefPtr<CefBrowser> browser,
                                const CefRect& rect) {
  ScopedJNIEnv env;
  if (!env)
    return;

  ScopedJNIObjectLocal jrect(env, NewJNIRect(env, rect));
  if (!jrect)
    return;

  ScopedJNIBrowser jbrowser(env, browser);
  JNI_CALL_VOID_METHOD(env, handle_, "onPopupSize",
                       "(Lorg/cef/browser/CefBrowser;Ljava/awt/Rectangle;)V",
                       jbrowser.get(), jrect.get());
}

void RenderHandler::OnPaint(CefRefPtr<CefBrowser> browser,
                            PaintElementType type,
                            const RectList& dirtyRects,
                            const void* buffer,
                            int width,
                            int height) {
  ScopedJNIEnv env;
  if (!env)
    return;

  ScopedJNIBrowser jbrowser(env, browser);
  jboolean jtype = type == PET_VIEW ? JNI_FALSE : JNI_TRUE;
  ScopedJNIObjectLocal jrectArray(env, NewJNIRectArray(env, dirtyRects));
  ScopedJNIObjectLocal jdirectBuffer(
      env,
      env->NewDirectByteBuffer(const_cast<void*>(buffer), width * height * 4));
  JNI_CALL_VOID_METHOD(env, handle_, "onPaint",
                       "(Lorg/cef/browser/CefBrowser;Z[Ljava/awt/"
                       "Rectangle;Ljava/nio/ByteBuffer;II)V",
                       jbrowser.get(), jtype, jrectArray.get(),
                       jdirectBuffer.get(), width, height);
}

// TODO(JCEF): Expose all parameters.
void RenderHandler::OnCursorChange(CefRefPtr<CefBrowser> browser,
                                   CefCursorHandle cursor,
                                   CursorType type,
                                   const CefCursorInfo& custom_cursor_info) {
  ScopedJNIEnv env;
  if (!env)
    return;

  ScopedJNIBrowser jbrowser(env, browser);
  const int cursorId = GetCursorId(type);
  JNI_CALL_VOID_METHOD(env, handle_, "onCursorChange",
                       "(Lorg/cef/browser/CefBrowser;I)V", jbrowser.get(),
                       cursorId);
}

bool RenderHandler::StartDragging(CefRefPtr<CefBrowser> browser,
                                  CefRefPtr<CefDragData> drag_data,
                                  DragOperationsMask allowed_ops,
                                  int x,
                                  int y) {
  ScopedJNIEnv env;
  if (!env)
    return false;

  ScopedJNIBrowser jbrowser(env, browser);
  ScopedJNIDragData jdragdata(env, drag_data);
  jdragdata.SetTemporary();
  jboolean jresult = JNI_FALSE;
  JNI_CALL_METHOD(
      env, handle_, "startDragging",
      "(Lorg/cef/browser/CefBrowser;Lorg/cef/callback/CefDragData;III)Z",
      Boolean, jresult, jbrowser.get(), jdragdata.get(), (jint)allowed_ops,
      (jint)x, (jint)y);

  return (jresult != JNI_FALSE);
}

void RenderHandler::UpdateDragCursor(CefRefPtr<CefBrowser> browser,
                                     DragOperation operation) {
  ScopedJNIEnv env;
  if (!env)
    return;

  ScopedJNIBrowser jbrowser(env, browser);
  JNI_CALL_VOID_METHOD(env, handle_, "updateDragCursor",
                       "(Lorg/cef/browser/CefBrowser;I)V", jbrowser.get(),
                       (jint)operation);
}

bool RenderHandler::GetViewRect(jobject browser, CefRect& rect) {
  ScopedJNIEnv env;
  if (!env)
    return false;

  ScopedJNIObjectResult jreturn(env);
  JNI_CALL_METHOD(env, handle_, "getViewRect",
                  "(Lorg/cef/browser/CefBrowser;)Ljava/awt/Rectangle;", Object,
                  jreturn, browser);
  if (jreturn) {
    rect = GetJNIRect(env, jreturn);
    return true;
  }
  return false;
}

bool RenderHandler::GetScreenPoint(jobject browser,
                                   int viewX,
                                   int viewY,
                                   int& screenX,
                                   int& screenY) {
  ScopedJNIEnv env;
  if (!env)
    return false;

  ScopedJNIObjectLocal jpoint(env, NewJNIPoint(env, viewX, viewY));
  if (!jpoint)
    return false;

  ScopedJNIObjectResult jreturn(env);
  JNI_CALL_METHOD(
      env, handle_, "getScreenPoint",
      "(Lorg/cef/browser/CefBrowser;Ljava/awt/Point;)Ljava/awt/Point;", Object,
      jreturn, browser, jpoint.get());

  if (jreturn) {
    GetJNIPoint(env, jreturn, &screenX, &screenY);
    return true;
  }
  return false;
}

bool RenderHandler::GetScreenInfo(CefRefPtr<CefBrowser> browser,
                                  CefScreenInfo& screen_info) {
  screen_info.device_scale_factor = 1.0; // [tav] todo: provide actual scale factor
  return true;
}
