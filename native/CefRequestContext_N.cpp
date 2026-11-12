// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#include "CefRequestContext_N.h"
#include "completion_callback.h"
#include "include/base/cef_callback.h"
#include "include/cef_cookie.h"
#include "include/cef_request_context.h"
#include "jni_util.h"
#include "request_context_handler.h"

namespace {

CefRefPtr<CefRequestContext> GetSelf(jlong self) {
  return reinterpret_cast<CefRequestContext*>(self);
}

void GetJNIFieldCefString(JNIEnv* env,
                          jclass cls,
                          jobject obj,
                          const char* field_name,
                          cef_string_t* value) {
  CefString tmp;
  if (GetJNIFieldString(env, cls, obj, field_name, &tmp)) {
    CefString target(value);
    target = tmp;
  }
}

CefRequestContextSettings GetJNIRequestContextSettings(JNIEnv* env,
                                                       jobject obj) {
  CefRequestContextSettings settings;
  if (!obj)
    return settings;

  ScopedJNIClass cls(env, "org/cef/browser/CefRequestContextSettings");
  if (!cls)
    return settings;

  GetJNIFieldCefString(env, cls, obj, "cache_path", &settings.cache_path);
  GetJNIFieldBoolean(env, cls, obj, "persist_session_cookies",
                     &settings.persist_session_cookies);
  GetJNIFieldCefString(env, cls, obj, "accept_language_list",
                       &settings.accept_language_list);
  GetJNIFieldCefString(env, cls, obj, "cookieable_schemes_list",
                       &settings.cookieable_schemes_list);
  GetJNIFieldBoolean(env, cls, obj, "cookieable_schemes_exclude_defaults",
                     &settings.cookieable_schemes_exclude_defaults);
  return settings;
}

CefRefPtr<CefRequestContextHandler> GetHandler(JNIEnv* env, jobject jhandler) {
  if (!jhandler)
    return nullptr;
  return new RequestContextHandler(env, jhandler);
}

jobject NewJNIRequestContext(JNIEnv* env,
                             jclass cls,
                             CefRefPtr<CefRequestContext> context) {
  if (!context.get())
    return nullptr;

  ScopedJNIObjectLocal jContext(env, NewJNIObject(env, cls));
  if (!jContext)
    return nullptr;

  SetCefForJNIObject_sync(env, jContext, context.get(), "CefRequestContext");
  return jContext.Release();
}

}  // namespace

JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1GetGlobalContext(JNIEnv* env,
                                                              jclass cls) {
  return NewJNIRequestContext(env, cls, CefRequestContext::GetGlobalContext());
}

JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1CreateContext(JNIEnv* env,
                                                           jclass cls,
                                                           jobject jsettings,
                                                           jobject jhandler) {
  CefRequestContextSettings settings =
      GetJNIRequestContextSettings(env, jsettings);
  CefRefPtr<CefRequestContext> context = CefRequestContext::CreateContext(
      settings, GetHandler(env, jhandler));
  return NewJNIRequestContext(env, cls, context);
}

JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1CreateContextShared(
    JNIEnv* env,
    jclass cls,
    jobject jother,
    jobject jhandler) {
  CefRefPtr<CefRequestContext> other =
      GetCefFromJNIObject_sync<CefRequestContext>(env, jother,
                                                  "CefRequestContext");
  if (!other.get()) {
    LOG(ERROR) << "CefRequestContext_N: CreateContextShared: invalid 'other' "
                  "request context";
    return nullptr;
  }

  CefRefPtr<CefRequestContext> context =
      CefRequestContext::CreateContext(other, GetHandler(env, jhandler));
  return NewJNIRequestContext(env, cls, context);
}

JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1IsSame(JNIEnv* env,
                                                    jobject obj,
                                                    jobject jother) {
  CefRefPtr<CefRequestContext> context =
      GetCefFromJNIObject_sync<CefRequestContext>(env, obj,
                                                  "CefRequestContext");
  CefRefPtr<CefRequestContext> other =
      GetCefFromJNIObject_sync<CefRequestContext>(env, jother,
                                                  "CefRequestContext");
  if (!context.get() || !other.get())
    return JNI_FALSE;
  return context->IsSame(other) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1IsSharingWith(JNIEnv* env,
                                                           jobject obj,
                                                           jobject jother) {
  CefRefPtr<CefRequestContext> context =
      GetCefFromJNIObject_sync<CefRequestContext>(env, obj,
                                                  "CefRequestContext");
  CefRefPtr<CefRequestContext> other =
      GetCefFromJNIObject_sync<CefRequestContext>(env, jother,
                                                  "CefRequestContext");
  if (!context.get() || !other.get())
    return JNI_FALSE;
  return context->IsSharingWith(other) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1IsGlobal(JNIEnv* env,
                                                      jobject obj) {
  CefRefPtr<CefRequestContext> context =
      GetCefFromJNIObject_sync<CefRequestContext>(env, obj,
                                                  "CefRequestContext");
  if (!context.get())
    return JNI_FALSE;
  return context->IsGlobal() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1GetCachePath(JNIEnv* env,
                                                          jobject obj) {
  CefRefPtr<CefRequestContext> context =
      GetCefFromJNIObject_sync<CefRequestContext>(env, obj,
                                                  "CefRequestContext");
  if (!context.get())
    return nullptr;
  return NewJNIString(env, context->GetCachePath());
}

JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1GetCookieManager(JNIEnv* env,
                                                              jobject obj) {
  CefRefPtr<CefRequestContext> context =
      GetCefFromJNIObject_sync<CefRequestContext>(env, obj,
                                                  "CefRequestContext");
  if (!context.get())
    return nullptr;

  // TODO(JCEF): Expose the callback object.
  CefRefPtr<CefCookieManager> manager = context->GetCookieManager(nullptr);
  if (!manager)
    return nullptr;

  ScopedJNIObjectLocal jManager(
      env, NewJNIObject(env, "org/cef/network/CefCookieManager_N"));
  if (!jManager)
    return nullptr;

  SetCefForJNIObject_sync(env, jManager, manager.get(), "CefCookieManager");
  return jManager.Release();
}

JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1HasPreference(JNIEnv* env,
                                                           jobject obj,
                                                           jstring jname) {
  CefRefPtr<CefRequestContext> context =
      GetCefFromJNIObject<CefRequestContext>(env, obj, "CefRequestContext");
  if (!context.get())
    return JNI_FALSE;

  CefString name = GetJNIString(env, jname);
  return context->HasPreference(name) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1GetPreference(JNIEnv* env,
                                                           jobject obj,
                                                           jstring jname) {
  CefRefPtr<CefRequestContext> context =
      GetCefFromJNIObject<CefRequestContext>(env, obj, "CefRequestContext");
  if (!context.get())
    return nullptr;

  CefString name = GetJNIString(env, jname);
  CefRefPtr<CefValue> value = context->GetPreference(name);
  if (!value)
    return nullptr;

  return NewJNIObjectFromCefValue(env, value);
}

JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1GetAllPreferences(
    JNIEnv* env,
    jobject obj,
    jboolean includeDefaults) {
  CefRefPtr<CefRequestContext> context =
      GetCefFromJNIObject<CefRequestContext>(env, obj, "CefRequestContext");
  if (!context.get())
    return nullptr;

  CefRefPtr<CefDictionaryValue> value =
      context->GetAllPreferences(includeDefaults == JNI_TRUE);
  if (!value)
    return nullptr;

  jobject jmap = NewJNIHashMap(env);
  CefDictionaryValue::KeyList keys;
  value->GetKeys(keys);
  for (const CefString& key : keys) {
    jstring jkey = NewJNIString(env, key);
    jobject jvalue = NewJNIObjectFromCefValue(env, value->GetValue(key));
    JNI_CALL_VOID_METHOD(
        env, jmap, "put",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", jkey,
        jvalue);
  }
  return jmap;
}

JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1CanSetPreference(JNIEnv* env,
                                                              jobject obj,
                                                              jstring jname) {
  CefRefPtr<CefRequestContext> context =
      GetCefFromJNIObject<CefRequestContext>(env, obj, "CefRequestContext");
  if (!context.get())
    return JNI_FALSE;

  CefString name = GetJNIString(env, jname);
  return context->CanSetPreference(name) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1SetPreference(JNIEnv* env,
                                                           jobject obj,
                                                           jstring jname,
                                                           jobject jvalue) {
  if (!CefCurrentlyOn(TID_UI))
    return NewJNIString(env, "called on invalid thread");

  CefRefPtr<CefRequestContext> context =
      GetCefFromJNIObject<CefRequestContext>(env, obj, "CefRequestContext");
  if (!context.get())
    return NewJNIString(env, "no request context");

  CefString name = GetJNIString(env, jname);
  CefRefPtr<CefValue> value = GetCefValueFromJNIObject(env, jvalue);
  if (!value)
    return NewJNIString(env, "no value to set");

  CefString error;
  bool result = context->SetPreference(name, value, error);
  if (!result)
    return NewJNIString(env, error);

  return nullptr;
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1CefRequestContext_1DTOR(
    JNIEnv* env,
    jobject obj) {
  SetCefForJNIObject_sync<CefRequestContext>(env, obj, nullptr,
                                             "CefRequestContext");
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1ClearCertificateExceptions(
    JNIEnv* env,
    jobject,
    jlong self,
    jobject jcallback) {
  CefRefPtr<CefRequestContext> request_context = GetSelf(self);
  if (!request_context)
    return;
  CefRefPtr<CefCompletionCallback> callback =
      new CompletionCallback(env, jcallback);
  request_context->ClearCertificateExceptions(callback);
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1CloseAllConnections(
    JNIEnv* env,
    jobject,
    jlong self,
    jobject jcallback) {
  CefRefPtr<CefRequestContext> request_context = GetSelf(self);
  if (!request_context)
    return;
  CefRefPtr<CefCompletionCallback> callback =
      new CompletionCallback(env, jcallback);
  request_context->CloseAllConnections(callback);
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1ClearHttpCache(
    JNIEnv* env,
    jobject,
    jlong self,
    jobject jcallback) {
  CefRefPtr<CefRequestContext> request_context = GetSelf(self);
  if (!request_context)
    return;
  CefRefPtr<CefCompletionCallback> callback =
      new CompletionCallback(env, jcallback);
  request_context->ClearHttpCache(callback);
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefRequestContext_1N_N_1ClearHttpAuthCredentials(
    JNIEnv* env,
    jobject,
    jlong self,
    jobject jcallback) {
  CefRefPtr<CefRequestContext> request_context = GetSelf(self);
  if (!request_context)
    return;
  CefRefPtr<CefCompletionCallback> callback =
      new CompletionCallback(env, jcallback);
  request_context->ClearHttpAuthCredentials(callback);
}
