// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#include "CefClientHandler.h"
#include "client_handler.h"
#include "jni_util.h"
#include "message_router_handler.h"

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1CefClientHandler_1CTOR(
    JNIEnv* env,
    jobject clientHandler) {
  CefRefPtr<ClientHandler> client = new ClientHandler(env, clientHandler);
  SetCefForJNIObject_sync(env, clientHandler, client.get(), "CefClientHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1addMessageRouter(
    JNIEnv* env,
    jobject clientHandler,
    jobject jmessageRouter) {
  CefRefPtr<ClientHandler> client = GetCefFromJNIObject_sync<ClientHandler>(
      env, clientHandler, "CefClientHandler");
  if (!client.get())
    return;
  client->AddMessageRouter(env, jmessageRouter);
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeContextMenuHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject contextMenuHandler) {
  SetCefForJNIObject_sync<CefContextMenuHandler>(env, contextMenuHandler, nullptr,
                                            "CefContextMenuHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeDialogHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject dialogHandler) {
  SetCefForJNIObject_sync<CefDialogHandler>(env, dialogHandler, nullptr,
                                       "CefDialogHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeDisplayHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject displayHandler) {
  SetCefForJNIObject_sync<CefDisplayHandler>(env, displayHandler, nullptr,
                                        "CefDisplayHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeDownloadHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject downloadHandler) {
  SetCefForJNIObject_sync<CefDownloadHandler>(env, downloadHandler, nullptr,
                                         "CefDownloadHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeDragHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject dragHandler) {
  SetCefForJNIObject_sync<CefDragHandler>(env, dragHandler, nullptr,
                                     "CefDragHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeFocusHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject focusHandler) {
  SetCefForJNIObject_sync<CefFocusHandler>(env, focusHandler, nullptr,
                                      "CefFocusHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeJSDialogHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject jsdialogHandler) {
  SetCefForJNIObject_sync<CefJSDialogHandler>(env, jsdialogHandler, nullptr,
                                         "CefJSDialogHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeKeyboardHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject keyboardHandler) {
  SetCefForJNIObject_sync<CefKeyboardHandler>(env, keyboardHandler, nullptr,
                                         "CefKeyboardHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeLifeSpanHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject lifeSpanHandler) {
  SetCefForJNIObject_sync<CefLifeSpanHandler>(env, lifeSpanHandler, nullptr,
                                         "CefLifeSpanHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeLoadHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject loadHandler) {
  SetCefForJNIObject_sync<CefLoadHandler>(env, loadHandler, nullptr,
                                     "CefLoadHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removePrintHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject printHandler) {
  SetCefForJNIObject_sync<CefPrintHandler>(env, printHandler, nullptr,
                                      "CefPrintHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeMessageRouter(
    JNIEnv* env,
    jobject clientHandler,
    jobject jmessageRouter) {
  CefRefPtr<ClientHandler> client = GetCefFromJNIObject_sync<ClientHandler>(
      env, clientHandler, "CefClientHandler");
  if (!client.get())
    return;
  client->RemoveMessageRouter(env, jmessageRouter);
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeRenderHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject renderHandler) {
  SetCefForJNIObject_sync<CefRenderHandler>(env, renderHandler, nullptr,
                                       "CefRenderHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeRequestHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject requestHandler) {
  SetCefForJNIObject_sync<CefRequestHandler>(env, requestHandler, nullptr,
                                        "CefRequestHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1removeWindowHandler(
    JNIEnv* env,
    jobject clientHandler,
    jobject windowHandler) {
  SetCefForJNIObject_sync<WindowHandler>(env, windowHandler, nullptr,
                                    "CefWindowHandler");
}

JNIEXPORT void JNICALL
Java_org_cef_handler_CefClientHandler_N_1CefClientHandler_1DTOR(
    JNIEnv* env,
    jobject clientHandler) {
  // delete reference to the native client handler
  SetCefForJNIObject_sync<ClientHandler>(env, clientHandler, nullptr,
                                    "CefClientHandler");
}
