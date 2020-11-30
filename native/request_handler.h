// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#ifndef JCEF_NATIVE_REQUEST_HANDLER_H_
#define JCEF_NATIVE_REQUEST_HANDLER_H_
#pragma once

#include <jni.h>

#include "include/cef_request_handler.h"

#include "jni_scoped_helpers.h"

// RequestHandler implementation.
class RequestHandler : public CefRequestHandler {
 public:
  RequestHandler(JNIEnv* env, jobject handler);

  // CefRequestHandler methods:
  bool OnBeforeBrowse(CefRefPtr<CefBrowser> browser,
                      CefRefPtr<CefFrame> frame,
                      CefRefPtr<CefRequest> request,
                      bool user_gesture,
                      bool is_redirect) OVERRIDE;
  bool OnOpenURLFromTab(CefRefPtr<CefBrowser> browser,
                        CefRefPtr<CefFrame> frame,
                        const CefString& target_url,
                        WindowOpenDisposition target_disposition,
                        bool user_gesture) OVERRIDE;
  CefRefPtr<CefResourceRequestHandler> GetResourceRequestHandler(
      CefRefPtr<CefBrowser> browser,
      CefRefPtr<CefFrame> frame,
      CefRefPtr<CefRequest> request,
      bool is_navigation,
      bool is_download,
      const CefString& request_initiator,
      bool& disable_default_handling) OVERRIDE;
  bool GetAuthCredentials(CefRefPtr<CefBrowser> browser,
                          const CefString& origin_url,
                          bool isProxy,
                          const CefString& host,
                          int port,
                          const CefString& realm,
                          const CefString& scheme,
                          CefRefPtr<CefAuthCallback> callback) OVERRIDE;
  bool OnQuotaRequest(CefRefPtr<CefBrowser> browser,
                      const CefString& origin_url,
                      int64 new_size,
                      CefRefPtr<CefRequestCallback> callback) OVERRIDE;
  bool OnCertificateError(CefRefPtr<CefBrowser> browser,
                          cef_errorcode_t cert_error,
                          const CefString& request_url,
                          CefRefPtr<CefSSLInfo> ssl_info,
                          CefRefPtr<CefRequestCallback> callback) OVERRIDE;
  void OnPluginCrashed(CefRefPtr<CefBrowser> browser,
                       const CefString& plugin_path) OVERRIDE;
  void OnRenderProcessTerminated(CefRefPtr<CefBrowser> browser,
                                 TerminationStatus status) OVERRIDE;

 protected:
  ScopedJNIObjectGlobal handle_;

  // Include the default reference counting implementation.
  IMPLEMENT_REFCOUNTING(RequestHandler);
};

#endif  // JCEF_NATIVE_REQUEST_HANDLER_H_
