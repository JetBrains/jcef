// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#include "request_handler.h"

#include "client_handler.h"
#include "jni_util.h"
#include "resource_handler.h"
#include "resource_request_handler.h"
#include "util.h"

#include "include/base/cef_logging.h"

namespace {

jobject NewCefX509Certificate(JNIEnv_* env,
                              CefRefPtr<CefX509Certificate> ssl_info) {
  ScopedJNIClass byteArrayCls(env, env->FindClass("[B"));
  if (!byteArrayCls) {
    if (env->ExceptionOccurred()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
      }
      return nullptr;
  }

  CefX509Certificate::IssuerChainBinaryList der_chain;
  ssl_info->GetDEREncodedIssuerChain(der_chain);
  der_chain.insert(der_chain.begin(), ssl_info->GetDEREncoded());

  ScopedJNIObjectLocal certificatesChain(
      env, env->NewObjectArray(static_cast<int>(der_chain.size()),
                               byteArrayCls, nullptr));

  for (size_t i = 0; i < der_chain.size(); ++i) {
    const auto& der_cert = der_chain[i];
    ScopedJNIObjectLocal derArray(
        env, env->NewByteArray((jsize)der_cert->GetSize()));
    {
      void* buf = env->GetPrimitiveArrayCritical((jarray)derArray.get(), 0);
      der_cert->GetData(buf, der_cert->GetSize(), 0);
      env->ReleasePrimitiveArrayCritical((jarray)derArray.get(), buf, 0);
    }

    env->SetObjectArrayElement((jobjectArray)certificatesChain.get(), (jsize)i,
                               derArray);
  }

  return NewJNIObject(env, "org/cef/security/CefX509Certificate", "([[B)V",
                      certificatesChain.get());
}

jobject NewCefSSLInfo(JNIEnv_* env, CefRefPtr<CefSSLInfo> ssl_info) {
  ScopedJNIObjectLocal certificate(
      env, NewCefX509Certificate(env, ssl_info->GetX509Certificate()));

  return NewJNIObject(env, "org/cef/security/CefSSLInfo",
                      "(ILorg/cef/security/CefX509Certificate;)V",
                      ssl_info.get()->GetCertStatus(), certificate.get());
}

}  // namespace

RequestHandler::RequestHandler(JNIEnv* env, jobject handler)
    : handle_(env, handler) {}

bool RequestHandler::OnBeforeBrowse(CefRefPtr<CefBrowser> browser,
                                    CefRefPtr<CefFrame> frame,
                                    CefRefPtr<CefRequest> request,
                                    bool user_gesture,
                                    bool is_redirect) {
  // Forward request to ClientHandler to make the message_router_ happy.
  CefRefPtr<ClientHandler> client =
      (ClientHandler*)browser->GetHost()->GetClient().get();
  client->OnBeforeBrowse(browser, frame);

  ScopedJNIEnv env;
  if (!env)
    return false;

  ScopedJNIBrowser jbrowser(env, browser);
  ScopedJNIFrame jframe(env, frame);
  jframe.SetTemporary();
  ScopedJNIRequest jrequest(env, request);
  jrequest.SetTemporary();
  jboolean jresult = JNI_FALSE;

  JNI_CALL_METHOD(env, handle_, "onBeforeBrowse",
                  "(Lorg/cef/browser/CefBrowser;Lorg/cef/browser/CefFrame;Lorg/"
                  "cef/network/CefRequest;ZZ)Z",
                  Boolean, jresult, jbrowser.get(), jframe.get(),
                  jrequest.get(), (user_gesture ? JNI_TRUE : JNI_FALSE),
                  (is_redirect ? JNI_TRUE : JNI_FALSE));

  return (jresult != JNI_FALSE);
}

bool RequestHandler::OnOpenURLFromTab(CefRefPtr<CefBrowser> browser,
                                      CefRefPtr<CefFrame> frame,
                                      const CefString& target_url,
                                      WindowOpenDisposition target_disposition,
                                      bool user_gesture) {
  ScopedJNIEnv env;
  if (!env)
    return false;

  ScopedJNIBrowser jbrowser(env, browser);
  ScopedJNIFrame jframe(env, frame);
  jframe.SetTemporary();
  ScopedJNIString jtargetUrl(env, target_url);
  jboolean jresult = JNI_FALSE;

  JNI_CALL_METHOD(env, handle_, "onOpenURLFromTab",
                  "(Lorg/cef/browser/CefBrowser;Lorg/cef/browser/CefFrame;"
                  "Ljava/lang/String;Z)Z",
                  Boolean, jresult, jbrowser.get(), jframe.get(),
                  jtargetUrl.get(), (user_gesture ? JNI_TRUE : JNI_FALSE));

  return (jresult != JNI_FALSE);
}

CefRefPtr<CefResourceRequestHandler> RequestHandler::GetResourceRequestHandler(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefRequest> request,
    bool is_navigation,
    bool is_download,
    const CefString& request_initiator,
    bool& disable_default_handling) {
  ScopedJNIEnv env;
  if (!env)
    return nullptr;

  ScopedJNIBrowser jbrowser(env, browser);
  ScopedJNIFrame jframe(env, frame);
  jframe.SetTemporary();
  ScopedJNIRequest jrequest(env, request);
  jrequest.SetTemporary();
  ScopedJNIString jrequestInitiator(env, request_initiator);
  ScopedJNIBoolRef jdisableDefaultHandling(env, disable_default_handling);
  ScopedJNIObjectResult jresult(env);

  JNI_CALL_METHOD(env, handle_, "getResourceRequestHandler",
                  "(Lorg/cef/browser/CefBrowser;Lorg/cef/browser/CefFrame;Lorg/"
                  "cef/network/CefRequest;ZZLjava/lang/String;Lorg/cef/misc/"
                  "BoolRef;)Lorg/cef/handler/CefResourceRequestHandler;",
                  Object, jresult, jbrowser.get(), jframe.get(), jrequest.get(),
                  is_navigation ? JNI_TRUE : JNI_FALSE,
                  is_download ? JNI_TRUE : JNI_FALSE, jrequestInitiator.get(),
                  jdisableDefaultHandling.get());

  disable_default_handling = jdisableDefaultHandling;

  if (jresult)
    return new ResourceRequestHandler(env, jresult);
  return nullptr;
}

bool RequestHandler::GetAuthCredentials(CefRefPtr<CefBrowser> browser,
                                        const CefString& origin_url,
                                        bool isProxy,
                                        const CefString& host,
                                        int port,
                                        const CefString& realm,
                                        const CefString& scheme,
                                        CefRefPtr<CefAuthCallback> callback) {
  ScopedJNIEnv env;
  if (!env)
    return false;

  ScopedJNIBrowser jbrowser(env, browser);
  ScopedJNIString joriginUrl(env, origin_url);
  ScopedJNIString jhost(env, host);
  ScopedJNIString jrealm(env, realm);
  ScopedJNIString jscheme(env, scheme);
  ScopedJNIAuthCallback jcallback(env, callback);
  jboolean jresult = JNI_FALSE;

  JNI_CALL_METHOD(
      env, handle_, "getAuthCredentials",
      "(Lorg/cef/browser/CefBrowser;Ljava/lang/String;ZLjava/lang/String;"
      "ILjava/lang/String;Ljava/lang/String;"
      "Lorg/cef/callback/CefAuthCallback;)Z",
      Boolean, jresult, jbrowser.get(), joriginUrl.get(),
      (isProxy ? JNI_TRUE : JNI_FALSE), jhost.get(), port, jrealm.get(),
      jscheme.get(), jcallback.get());

  if (jresult == JNI_FALSE) {
    // If the Java method returns "false" the callback won't be used and
    // the reference can therefore be removed.
    jcallback.SetTemporary();
  }

  return (jresult != JNI_FALSE);
}

bool RequestHandler::OnCertificateError(CefRefPtr<CefBrowser> browser,
                                        cef_errorcode_t cert_error,
                                        const CefString& request_url,
                                        CefRefPtr<CefSSLInfo> ssl_info,
                                        CefRefPtr<CefCallback> callback) {
  ScopedJNIEnv env;
  if (!env)
    return false;

  ScopedJNIBrowser jbrowser(env, browser);
  ScopedJNIObjectLocal jcertError(env, NewJNIErrorCode(env, cert_error));
  ScopedJNIString jrequestUrl(env, request_url);
  ScopedJNIObjectLocal jSSLInfo(env, NewCefSSLInfo(env, ssl_info));
  ScopedJNICallback jcallback(env, callback);
  jboolean jresult = JNI_FALSE;

  JNI_CALL_METHOD(
      env, handle_, "onCertificateError",
      "(Lorg/cef/browser/CefBrowser;Lorg/cef/handler/CefLoadHandler$ErrorCode;"
      "Ljava/lang/String;Lorg/cef/security/CefSSLInfo;Lorg/cef/callback/"
      "CefCallback;)Z",
      Boolean, jresult, jbrowser.get(), jcertError.get(), jrequestUrl.get(),
      jSSLInfo.get(), jcallback.get());

  if (jresult == JNI_FALSE) {
    // If the Java method returns "false" the callback won't be used and
    // the reference can therefore be removed.
    jcallback.SetTemporary();
  }

  return (jresult != JNI_FALSE);
}

void RequestHandler::OnRenderProcessTerminated(CefRefPtr<CefBrowser> browser,
                                               TerminationStatus status) {
  // Forward request to ClientHandler to make the message_router_ happy.
  CefRefPtr<ClientHandler> client =
      (ClientHandler*)browser->GetHost()->GetClient().get();
  client->OnRenderProcessTerminated(browser);

  ScopedJNIEnv env;
  if (!env)
    return;

  ScopedJNIBrowser jbrowser(env, browser);

  ScopedJNIObjectResult jstatus(env);
  switch (status) {
    JNI_CASE(env, "org/cef/handler/CefRequestHandler$TerminationStatus",
             TS_ABNORMAL_TERMINATION, jstatus);
    JNI_CASE(env, "org/cef/handler/CefRequestHandler$TerminationStatus",
             TS_PROCESS_WAS_KILLED, jstatus);
    JNI_CASE(env, "org/cef/handler/CefRequestHandler$TerminationStatus",
             TS_PROCESS_CRASHED, jstatus);
    JNI_CASE(env, "org/cef/handler/CefRequestHandler$TerminationStatus",
             TS_PROCESS_OOM, jstatus);
  }

  JNI_CALL_VOID_METHOD(
      env, handle_, "onRenderProcessTerminated",
      "(Lorg/cef/browser/CefBrowser;"
      "Lorg/cef/handler/CefRequestHandler$TerminationStatus;)V",
      jbrowser.get(), jstatus.get());
}
