// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#include "jni_util.h"

#include <jawt.h>
#include <algorithm>

#include "client_handler.h"
#include "util.h"

namespace {

JavaVM* g_jvm = NULL;

jobject g_javaClassLoader = NULL;

}  // namespace

void SetJVM(JavaVM* jvm) {
  ASSERT(!g_jvm);
  g_jvm = jvm;
}

JNIEnv* GetJNIEnv() {
  JNIEnv* env = NULL;
  if (g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_EDETACHED &&
      g_jvm->AttachCurrentThreadAsDaemon((void**)&env, NULL) != JNI_OK) {
    return NULL;
  }
  return env;
}

// Determines whether the current thread is already attached to the VM,
// and tells the caller if it needs to later DetachCurrentThread.
//
// CALL THIS ONCE WITHIN A FUNCTION SCOPE and use a local boolean
// for mustDetach; if you do not, the first call might attach, setting
// mustDetach to true, but the second will misleadingly set mustDetach
// to false, leaving a dangling JNIEnv.
jint GetJNIEnv(JNIEnv** env, bool* mustDetach) {
  jint getEnvErr = JNI_OK;
  *mustDetach = false;
  if (g_jvm) {
    getEnvErr = g_jvm->GetEnv((void**)env, JNI_VERSION_1_4);
    if (getEnvErr == JNI_EDETACHED) {
      getEnvErr = g_jvm->AttachCurrentThreadAsDaemon((void**)env, NULL);
      if (getEnvErr == JNI_OK) {
        *mustDetach = true;
      }
    }
  }
  return getEnvErr;
}

void DetachFromThread(bool* mustDetach) {
  if (!g_jvm) {
    return;
  }
  if (*mustDetach)
    g_jvm->DetachCurrentThread();
}

void SetJavaClassLoader(JNIEnv* env, jobject javaClassLoader) {
  ASSERT(!g_javaClassLoader);
  g_javaClassLoader = env->NewGlobalRef(javaClassLoader);
}

jclass FindClass(JNIEnv* env, const char* class_name) {
  ASSERT(g_javaClassLoader);

  std::string classNameSeparatedByDots(class_name);
  std::replace(classNameSeparatedByDots.begin(), classNameSeparatedByDots.end(),
               '/', '.');

  jstring classNameJString =
      env->NewStringUTF(classNameSeparatedByDots.c_str());
  jobject result = NULL;

  JNI_CALL_METHOD(env, g_javaClassLoader, "loadClass",
                  "(Ljava/lang/String;)Ljava/lang/Class;", Object, result,
                  classNameJString);

  env->DeleteLocalRef(classNameJString);

  return static_cast<jclass>(result);
}

jobject NewJNIObject(JNIEnv* env, jclass cls) {
  jmethodID initID = env->GetMethodID(cls, "<init>", "()V");
  if (initID == 0) {
    env->ExceptionClear();
    return NULL;
  }

  jobject obj = env->NewObject(cls, initID);
  if (obj == NULL) {
    env->ExceptionClear();
    return NULL;
  }

  return obj;
}

jobject NewJNIObject(JNIEnv* env, const char* class_name) {
  jclass cls = FindClass(env, class_name);
  if (!cls)
    return NULL;

  return NewJNIObject(env, cls);
}

jobject NewJNIObject(JNIEnv* env,
                     const char* class_name,
                     const char* sig,
                     ...) {
  jclass cls = FindClass(env, class_name);
  if (!cls)
    return NULL;

  jmethodID initID = env->GetMethodID(cls, "<init>", sig);
  if (initID == 0) {
    env->ExceptionClear();
    return NULL;
  }

  va_list ap;
  va_start(ap, sig);

  jobject obj = env->NewObjectV(cls, initID, ap);
  if (obj == NULL) {
    env->ExceptionClear();
    return NULL;
  }

  return obj;
}

jobject NewJNIBoolRef(JNIEnv* env, bool initValue) {
  jobject jboolRef = NewJNIObject(env, "org/cef/misc/BoolRef");
  if (!jboolRef)
    return NULL;
  SetJNIBoolRef(env, jboolRef, initValue);
  return jboolRef;
}

jobject NewJNIIntRef(JNIEnv* env, int initValue) {
  jobject jintRef = NewJNIObject(env, "org/cef/misc/IntRef");
  if (!jintRef)
    return NULL;
  SetJNIIntRef(env, jintRef, initValue);
  return jintRef;
}

jobject NewJNIStringRef(JNIEnv* env, const CefString& initValue) {
  jobject jstringRef = NewJNIObject(env, "org/cef/misc/StringRef");
  if (!jstringRef)
    return NULL;
  SetJNIStringRef(env, jstringRef, initValue);
  return jstringRef;
}

bool GetJNIBoolRef(JNIEnv* env, jobject jboolRef) {
  jboolean boolRefRes = JNI_FALSE;
  JNI_CALL_METHOD(env, jboolRef, "get", "()Z", Boolean, boolRefRes);
  return (boolRefRes != JNI_FALSE);
}

int GetJNIIntRef(JNIEnv* env, jobject jintRef) {
  jint intRefRes = -1;
  JNI_CALL_METHOD(env, jintRef, "get", "()I", Int, intRefRes);
  return intRefRes;
}

CefString GetJNIStringRef(JNIEnv* env, jobject jstringRef) {
  jobject jstr = NULL;
  JNI_CALL_METHOD(env, jstringRef, "get", "()Ljava/lang/String;", Object, jstr);
  return GetJNIString(env, (jstring)jstr);
}

void SetJNIBoolRef(JNIEnv* env, jobject jboolRef, bool boolValue) {
  JNI_CALL_VOID_METHOD(env, jboolRef, "set", "(Z)V",
                       (boolValue ? JNI_TRUE : JNI_FALSE));
}

void SetJNIIntRef(JNIEnv* env, jobject jintRef, int intValue) {
  JNI_CALL_VOID_METHOD(env, jintRef, "set", "(I)V", intValue);
}

void SetJNIStringRef(JNIEnv* env,
                     jobject jstringRef,
                     const CefString& stringValue) {
  JNI_CALL_VOID_METHOD(env, jstringRef, "set", "(Ljava/lang/String;)V",
                       NewJNIString(env, stringValue));
}

jobject NewJNIDate(JNIEnv* env, const CefTime& time) {
  jobject jdate = NewJNIObject(env, "java/util/Date");
  if (!jdate)
    return NULL;
  double timestamp = time.GetDoubleT() * 1000;
  JNI_CALL_VOID_METHOD(env, jdate, "setTime", "(J)V", (jlong)timestamp);
  return jdate;
}

jobject NewJNICookie(JNIEnv* env, const CefCookie& cookie) {
  bool hasExpires = (cookie.has_expires != 0);
  jobject jExpiresDate = hasExpires ? NewJNIDate(env, cookie.expires) : NULL;
  jobject jcookie = NewJNIObject(
      env, "org/cef/network/CefCookie",
      "(Ljava/lang/String;Ljava/lang/String;"
      "Ljava/lang/String;Ljava/lang/String;"
      "ZZLjava/util/Date;Ljava/util/Date;"
      "ZLjava/util/Date;)V",
      NewJNIString(env, CefString(&cookie.name)),
      NewJNIString(env, CefString(&cookie.value)),
      NewJNIString(env, CefString(&cookie.domain)),
      NewJNIString(env, CefString(&cookie.path)),
      (cookie.secure != 0 ? JNI_TRUE : JNI_FALSE),
      (cookie.httponly != 0 ? JNI_TRUE : JNI_FALSE),
      NewJNIDate(env, cookie.creation), NewJNIDate(env, cookie.last_access),
      (hasExpires ? JNI_TRUE : JNI_FALSE), jExpiresDate);
  return jcookie;
}

CefCookie GetJNICookie(JNIEnv* env, jobject jcookie) {
  CefCookie cookie;

  jclass cls = FindClass(env, "org/cef/network/CefCookie");
  if (!cls)
    return cookie;

  CefString name(&cookie.name);
  CefString value(&cookie.value);
  CefString domain(&cookie.domain);
  CefString path(&cookie.path);
  CefTime creation, lastAccess, expires;

  GetJNIFieldString(env, cls, jcookie, "name", &name);
  GetJNIFieldString(env, cls, jcookie, "value", &value);
  GetJNIFieldString(env, cls, jcookie, "domain", &domain);
  GetJNIFieldString(env, cls, jcookie, "path", &path);
  GetJNIFieldBoolean(env, cls, jcookie, "secure", &cookie.secure);
  GetJNIFieldBoolean(env, cls, jcookie, "httponly", &cookie.httponly);
  GetJNIFieldDate(env, cls, jcookie, "creation", &creation);
  cookie.creation = creation;
  GetJNIFieldDate(env, cls, jcookie, "lastAccess", &lastAccess);
  cookie.last_access = lastAccess;
  GetJNIFieldBoolean(env, cls, jcookie, "hasExpires", &cookie.has_expires);
  GetJNIFieldDate(env, cls, jcookie, "expires", &expires);
  cookie.expires = expires;

  return cookie;
}

CefString GetJNIString(JNIEnv* env, jstring jstr) {
  CefString cef_str;
  const char* chr = NULL;
  if (jstr)
    chr = env->GetStringUTFChars(jstr, NULL);
  if (chr)
    cef_str = chr;
  if (jstr)
    env->ReleaseStringUTFChars(jstr, chr);
  return cef_str;
}

void GetJNIStringArray(JNIEnv* env,
                       jobjectArray jarray,
                       std::vector<CefString>& vals) {
  jsize argc = env->GetArrayLength(jarray);
  for (jsize i = 0; i < argc; ++i) {
    jstring jstr = (jstring)env->GetObjectArrayElement(jarray, i);
    const char* cstr = env->GetStringUTFChars(jstr, NULL);
    CefString cef_str(cstr);
    vals.push_back(cef_str);
    env->ReleaseStringUTFChars(jstr, cstr);
  }
}

CefMessageRouterConfig GetJNIMessageRouterConfig(JNIEnv* env, jobject jConfig) {
  CefMessageRouterConfig config;

  if (jConfig == NULL)
    return config;
  jclass cls =
      FindClass(env, "org/cef/browser/CefMessageRouter$CefMessageRouterConfig");
  if (cls == NULL)
    return config;

  GetJNIFieldString(env, cls, jConfig, "jsQueryFunction",
                    &config.js_query_function);
  GetJNIFieldString(env, cls, jConfig, "jsCancelFunction",
                    &config.js_cancel_function);
  return config;
}

jobject NewJNIErrorCode(JNIEnv* env, cef_errorcode_t errorCode) {
  jobject jerrorCode = NULL;
  switch (errorCode) {
    default:
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_NONE,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_IO_PENDING,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_FAILED,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_ABORTED,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_INVALID_ARGUMENT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_INVALID_HANDLE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_FILE_NOT_FOUND, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_TIMED_OUT,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_FILE_TOO_BIG, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_UNEXPECTED,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_ACCESS_DENIED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_NOT_IMPLEMENTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_INSUFFICIENT_RESOURCES, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_OUT_OF_MEMORY, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_UPLOAD_FILE_CHANGED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SOCKET_NOT_CONNECTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_FILE_EXISTS,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_FILE_PATH_TOO_LONG, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_FILE_NO_SPACE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_FILE_VIRUS_INFECTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_BLOCKED_BY_CLIENT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_NETWORK_CHANGED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_BLOCKED_BY_ADMINISTRATOR, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SOCKET_IS_CONNECTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_BLOCKED_ENROLLMENT_CHECK_PENDING, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_UPLOAD_STREAM_REWIND_NOT_SUPPORTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CONTEXT_SHUT_DOWN, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_BLOCKED_BY_RESPONSE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_BLOCKED_BY_XSS_AUDITOR, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CLEARTEXT_NOT_PERMITTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CONNECTION_CLOSED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CONNECTION_RESET, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CONNECTION_REFUSED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CONNECTION_ABORTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CONNECTION_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_NAME_NOT_RESOLVED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_INTERNET_DISCONNECTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_PROTOCOL_ERROR, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_ADDRESS_INVALID, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_ADDRESS_UNREACHABLE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_CLIENT_AUTH_CERT_NEEDED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_TUNNEL_CONNECTION_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_NO_SSL_VERSIONS_ENABLED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_VERSION_OR_CIPHER_MISMATCH, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_RENEGOTIATION_REQUESTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PROXY_AUTH_UNSUPPORTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_ERROR_IN_SSL_RENEGOTIATION, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_BAD_SSL_CLIENT_AUTH_CERT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CONNECTION_TIMED_OUT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_HOST_RESOLVER_QUEUE_TOO_LARGE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SOCKS_CONNECTION_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SOCKS_CONNECTION_HOST_UNREACHABLE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_ALPN_NEGOTIATION_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_NO_RENEGOTIATION, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_WINSOCK_UNEXPECTED_WRITTEN_BYTES, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_DECOMPRESSION_FAILURE_ALERT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_BAD_RECORD_MAC_ALERT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PROXY_AUTH_REQUESTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_WEAK_SERVER_EPHEMERAL_DH_KEY, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PROXY_CONNECTION_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_MANDATORY_PROXY_CONFIGURATION_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PRECONNECT_MAX_SOCKET_LIMIT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_CLIENT_AUTH_PRIVATE_KEY_ACCESS_DENIED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_CLIENT_AUTH_CERT_NO_PRIVATE_KEY, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PROXY_CERTIFICATE_INVALID, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_NAME_RESOLUTION_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_NETWORK_ACCESS_DENIED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_TEMPORARILY_THROTTLED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_HTTPS_PROXY_TUNNEL_RESPONSE_REDIRECT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_CLIENT_AUTH_SIGNATURE_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_MSG_TOO_BIG,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_WS_PROTOCOL_ERROR, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_ADDRESS_IN_USE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_HANDSHAKE_NOT_COMPLETED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_BAD_PEER_PUBLIC_KEY, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_PINNED_KEY_NOT_IN_CERT_CHAIN, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CLIENT_AUTH_CERT_TYPE_UNSUPPORTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_ORIGIN_BOUND_CERT_GENERATION_TYPE_MISMATCH, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_DECRYPT_ERROR_ALERT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_WS_THROTTLE_QUEUE_TOO_LARGE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_SERVER_CERT_CHANGED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_UNRECOGNIZED_NAME_ALERT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SOCKET_SET_RECEIVE_BUFFER_SIZE_ERROR, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SOCKET_SET_SEND_BUFFER_SIZE_ERROR, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SOCKET_RECEIVE_BUFFER_SIZE_UNCHANGEABLE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SOCKET_SEND_BUFFER_SIZE_UNCHANGEABLE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_CLIENT_AUTH_CERT_BAD_FORMAT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_ICANN_NAME_COLLISION, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_SERVER_CERT_BAD_FORMAT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CT_STH_PARSING_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CT_STH_INCOMPLETE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_UNABLE_TO_REUSE_CONNECTION_FOR_PROXY_AUTH, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CT_CONSISTENCY_PROOF_PARSING_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_OBSOLETE_CIPHER, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_WS_UPGRADE,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_READ_IF_READY_NOT_IMPLEMENTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_NO_BUFFER_SPACE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_CLIENT_AUTH_NO_COMMON_ALGORITHMS, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_EARLY_DATA_REJECTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_WRONG_VERSION_ON_EARLY_DATA, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_TLS13_DOWNGRADE_DETECTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SSL_KEY_USAGE_INCOMPATIBLE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_COMMON_NAME_INVALID, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_DATE_INVALID, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_AUTHORITY_INVALID, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_CONTAINS_ERRORS, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_NO_REVOCATION_MECHANISM, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_UNABLE_TO_CHECK_REVOCATION, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_REVOKED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_INVALID, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_WEAK_SIGNATURE_ALGORITHM, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_NON_UNIQUE_NAME, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_WEAK_KEY, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_NAME_CONSTRAINT_VIOLATION, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_VALIDITY_TOO_LONG, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERTIFICATE_TRANSPARENCY_REQUIRED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_SYMANTEC_LEGACY, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_CERT_END,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_INVALID_URL,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_DISALLOWED_URL_SCHEME, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_UNKNOWN_URL_SCHEME, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_INVALID_REDIRECT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_TOO_MANY_REDIRECTS, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_UNSAFE_REDIRECT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_UNSAFE_PORT,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_INVALID_RESPONSE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_INVALID_CHUNKED_ENCODING, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_METHOD_NOT_SUPPORTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_UNEXPECTED_PROXY_AUTH, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_EMPTY_RESPONSE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_RESPONSE_HEADERS_TOO_BIG, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PAC_STATUS_NOT_OK, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PAC_SCRIPT_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_REQUEST_RANGE_NOT_SATISFIABLE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_MALFORMED_IDENTITY, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CONTENT_DECODING_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_NETWORK_IO_SUSPENDED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SYN_REPLY_NOT_RECEIVED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_ENCODING_CONVERSION_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_UNRECOGNIZED_FTP_DIRECTORY_LISTING_FORMAT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_NO_SUPPORTED_PROXIES, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SPDY_PROTOCOL_ERROR, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_INVALID_AUTH_CREDENTIALS, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_UNSUPPORTED_AUTH_SCHEME, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_ENCODING_DETECTION_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_MISSING_AUTH_CREDENTIALS, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_UNEXPECTED_SECURITY_LIBRARY_STATUS, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_MISCONFIGURED_AUTH_ENVIRONMENT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_UNDOCUMENTED_SECURITY_LIBRARY_STATUS, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_RESPONSE_BODY_TOO_BIG_TO_DRAIN, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_RESPONSE_HEADERS_MULTIPLE_CONTENT_LENGTH, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_INCOMPLETE_SPDY_HEADERS, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PAC_NOT_IN_DHCP, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_RESPONSE_HEADERS_MULTIPLE_CONTENT_DISPOSITION, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_RESPONSE_HEADERS_MULTIPLE_LOCATION, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SPDY_SERVER_REFUSED_STREAM, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SPDY_PING_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CONTENT_LENGTH_MISMATCH, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_INCOMPLETE_CHUNKED_ENCODING, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_QUIC_PROTOCOL_ERROR, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_RESPONSE_HEADERS_TRUNCATED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_QUIC_HANDSHAKE_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SPDY_INADEQUATE_TRANSPORT_SECURITY, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SPDY_FLOW_CONTROL_ERROR, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SPDY_FRAME_SIZE_ERROR, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SPDY_COMPRESSION_ERROR, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PROXY_AUTH_REQUESTED_WITH_NO_CONNECTION, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_HTTP_1_1_REQUIRED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PROXY_HTTP_1_1_REQUIRED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PAC_SCRIPT_TERMINATED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_INVALID_HTTP_RESPONSE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CONTENT_DECODING_INIT_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SPDY_RST_STREAM_NO_ERROR_RECEIVED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SPDY_PUSHED_STREAM_NOT_AVAILABLE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SPDY_CLAIMED_PUSHED_STREAM_RESET_BY_SERVER, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_TOO_MANY_RETRIES, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SPDY_STREAM_CLOSED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SPDY_CLIENT_REFUSED_STREAM, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SPDY_PUSHED_RESPONSE_DOES_NOT_MATCH, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_CACHE_MISS,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CACHE_READ_FAILURE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CACHE_WRITE_FAILURE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CACHE_OPERATION_NOT_SUPPORTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CACHE_OPEN_FAILURE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CACHE_CREATE_FAILURE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_CACHE_RACE,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CACHE_CHECKSUM_READ_FAILURE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CACHE_CHECKSUM_MISMATCH, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CACHE_LOCK_TIMEOUT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CACHE_AUTH_FAILURE_AFTER_READ, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CACHE_ENTRY_NOT_SUITABLE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CACHE_DOOM_FAILURE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CACHE_OPEN_OR_CREATE_FAILURE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_INSECURE_RESPONSE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_NO_PRIVATE_KEY_FOR_CERT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_ADD_USER_CERT_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_INVALID_SIGNED_EXCHANGE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode", ERR_FTP_FAILED,
               jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_FTP_SERVICE_UNAVAILABLE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_FTP_TRANSFER_ABORTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_FTP_FILE_BUSY, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_FTP_SYNTAX_ERROR, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_FTP_COMMAND_NOT_SUPPORTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_FTP_BAD_COMMAND_SEQUENCE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PKCS12_IMPORT_BAD_PASSWORD, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PKCS12_IMPORT_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_IMPORT_CA_CERT_NOT_CA, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_IMPORT_CERT_ALREADY_EXISTS, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_IMPORT_CA_CERT_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_IMPORT_SERVER_CERT_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PKCS12_IMPORT_INVALID_MAC, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PKCS12_IMPORT_INVALID_FILE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PKCS12_IMPORT_UNSUPPORTED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_KEY_GENERATION_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_PRIVATE_KEY_EXPORT_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_SELF_SIGNED_CERT_GENERATION_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_CERT_DATABASE_CHANGED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_DNS_MALFORMED_RESPONSE, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_DNS_SERVER_REQUIRES_TCP, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_DNS_SERVER_FAILED, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_DNS_TIMED_OUT, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_DNS_CACHE_MISS, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_DNS_SEARCH_EMPTY, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_DNS_SORT_ERROR, jerrorCode);
      JNI_CASE(env, "org/cef/handler/CefLoadHandler$ErrorCode",
               ERR_DNS_HTTP_FAILED, jerrorCode);
  }
  return jerrorCode;
}

cef_errorcode_t GetJNIErrorCode(JNIEnv* env, jobject jerrorCode) {
  cef_errorcode_t errorCode = ERR_NONE;

  if (jerrorCode) {
    jint jcode = 0;
    JNI_CALL_METHOD(env, jerrorCode, "getCode", "()I", Int, jcode);
    errorCode = static_cast<cef_errorcode_t>(jcode);
  }

  return errorCode;
}

jstring NewJNIString(JNIEnv* env, const CefString& str) {
  std::string cstr(str);
  return env->NewStringUTF(cstr.c_str());
}

jobject NewJNILong(JNIEnv* env, const int64& val) {
  return NewJNIObject(env, "java/lang/Long", "(J)V", (jlong)val);
}

jobjectArray NewJNIStringArray(JNIEnv* env,
                               const std::vector<CefString>& vals) {
  if (vals.empty())
    return NULL;

  jclass cls = FindClass(env, "java/lang/String");
  if (!cls)
    return NULL;

  jobjectArray arr =
      env->NewObjectArray(static_cast<jsize>(vals.size()), cls, NULL);

  for (jsize i = 0; i < static_cast<jsize>(vals.size()); i++) {
    jobject str_obj = NewJNIString(env, vals[i]);
    env->SetObjectArrayElement(arr, i, str_obj);
    env->DeleteLocalRef(str_obj);
  }

  return arr;
}

jobject NewJNIStringVector(JNIEnv* env, const std::vector<CefString>& vals) {
  jobject jvector = NewJNIObject(env, "java/util/Vector");
  if (!jvector)
    return NULL;

  std::vector<CefString>::const_iterator iter;
  for (iter = vals.begin(); iter != vals.end(); ++iter) {
    AddJNIStringToVector(env, jvector, *iter);
  }
  return jvector;
}

void AddJNIStringToVector(JNIEnv* env, jobject jvector, const CefString& str) {
  jstring argument = NewJNIString(env, str);
  JNI_CALL_VOID_METHOD(env, jvector, "addElement", "(Ljava/lang/Object;)V",
                       argument);
  env->DeleteLocalRef(argument);
}

jobject NewJNILongVector(JNIEnv* env, const std::vector<int64>& vals) {
  jobject jvector = NewJNIObject(env, "java/util/Vector");
  if (!jvector)
    return NULL;

  std::vector<int64>::const_iterator iter;
  for (iter = vals.begin(); iter != vals.end(); ++iter) {
    AddJNILongToVector(env, jvector, *iter);
  }
  return jvector;
}

void AddJNILongToVector(JNIEnv* env, jobject jvector, const int64& val) {
  jobject argument = NewJNILong(env, val);
  JNI_CALL_VOID_METHOD(env, jvector, "addElement", "(Ljava/lang/Object;)V",
                       argument);
  env->DeleteLocalRef(argument);
}

void GetJNIStringVector(JNIEnv* env,
                        jobject jvector,
                        std::vector<CefString>& vals) {
  if (!jvector)
    return;

  jint jsize = 0;
  JNI_CALL_METHOD(env, jvector, "size", "()I", Int, jsize);

  for (jint index = 0; index < jsize; index++) {
    ScopedJNIObjectResult jstr(env);
    JNI_CALL_METHOD(env, jvector, "get", "(I)Ljava/lang/Object;", Object, jstr,
                    index);
    vals.push_back(GetJNIString(env, (jstring)jstr.get()));
  }
}

void GetJNIStringMultiMap(JNIEnv* env,
                          jobject jheaderMap,
                          std::multimap<CefString, CefString>& vals) {
  if (!jheaderMap)
    return;

  // public abstract java.util.Set<java.util.Map$Entry<K, V>> entrySet();
  ScopedJNIObjectResult jentrySet(env);
  JNI_CALL_METHOD(env, jheaderMap, "entrySet", "()Ljava/util/Set;", Object,
                  jentrySet);
  if (!jentrySet)
    return;

  // public abstract java.lang.Object[] toArray();
  ScopedJNIObjectResult jentrySetValues(env);
  JNI_CALL_METHOD(env, jentrySet, "toArray", "()[Ljava/lang/Object;", Object,
                  jentrySetValues);
  if (!jentrySetValues)
    return;

  CefResponse::HeaderMap headerMap;
  jint length = env->GetArrayLength((jobjectArray)jentrySetValues.get());
  for (jint i = 0; i < length; i++) {
    ScopedJNIObjectLocal jmapEntry(
        env,
        env->GetObjectArrayElement((jobjectArray)jentrySetValues.get(), i));
    if (!jmapEntry)
      return;
    ScopedJNIObjectResult jkey(env);
    ScopedJNIObjectResult jvalue(env);
    JNI_CALL_METHOD(env, jmapEntry, "getKey", "()Ljava/lang/Object;", Object,
                    jkey);
    JNI_CALL_METHOD(env, jmapEntry, "getValue", "()Ljava/lang/Object;", Object,
                    jvalue);
    vals.insert(std::make_pair(GetJNIString(env, (jstring)jkey.get()),
                               GetJNIString(env, (jstring)jvalue.get())));
  }
}
void SetJNIStringMultiMap(JNIEnv* env,
                          jobject jheaderMap,
                          const std::multimap<CefString, CefString>& vals) {
  for (CefResponse::HeaderMap::const_iterator it = vals.begin();
       it != vals.end(); ++it) {
    ScopedJNIString jkey(env, it->first);
    ScopedJNIString jvalue(env, it->second);
    ScopedJNIObjectResult jresult(env);
    JNI_CALL_METHOD(env, jheaderMap, "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    Object, jresult, jkey.get(), jvalue.get());
  }
}

bool GetJNIFieldString(JNIEnv* env,
                       jclass cls,
                       jobject obj,
                       const char* field_name,
                       CefString* value) {
  jfieldID field = env->GetFieldID(cls, field_name, "Ljava/lang/String;");
  if (field) {
    jstring jstr = (jstring)env->GetObjectField(obj, field);
    const char* chr = NULL;
    if (jstr)
      chr = env->GetStringUTFChars(jstr, NULL);
    if (chr) {
      *value = chr;
      env->ReleaseStringUTFChars(jstr, chr);
    }
    return true;
  }
  env->ExceptionClear();
  return false;
}

bool GetJNIFieldDate(JNIEnv* env,
                     jclass cls,
                     jobject obj,
                     const char* field_name,
                     CefTime* value) {
  jfieldID field = env->GetFieldID(cls, field_name, "Ljava/util/Date;");
  if (field) {
    jobject jdate = env->GetObjectField(obj, field);
    long timestamp = 0;
    JNI_CALL_METHOD(env, jdate, "getTime", "()J", Long, timestamp);
    value->SetDoubleT((double)(timestamp / 1000));
    env->DeleteLocalRef(jdate);
    return true;
  }
  env->ExceptionClear();
  return false;
}

bool GetJNIFieldBoolean(JNIEnv* env,
                        jclass cls,
                        jobject obj,
                        const char* field_name,
                        int* value) {
  jfieldID field = env->GetFieldID(cls, field_name, "Z");
  if (field) {
    *value = env->GetBooleanField(obj, field) != JNI_FALSE ? 1 : 0;
    return true;
  }
  env->ExceptionClear();
  return false;
}

bool GetJNIFieldObject(JNIEnv* env,
                       jclass cls,
                       jobject obj,
                       const char* field_name,
                       jobject* value,
                       const char* object_type) {
  jfieldID field = env->GetFieldID(cls, field_name, object_type);
  if (field) {
    *value = env->GetObjectField(obj, field);
    return true;
  }
  env->ExceptionClear();
  return false;
}

bool GetJNIFieldDouble(JNIEnv* env,
                       jclass cls,
                       jobject obj,
                       const char* field_name,
                       double* value) {
  jfieldID field = env->GetFieldID(cls, field_name, "D");
  if (field) {
    *value = env->GetDoubleField(obj, field);
    return true;
  }
  env->ExceptionClear();
  return false;
}

bool GetJNIFieldInt(JNIEnv* env,
                    jclass cls,
                    jobject obj,
                    const char* field_name,
                    int* value) {
  jfieldID field = env->GetFieldID(cls, field_name, "I");
  if (field) {
    *value = env->GetIntField(obj, field);
    return true;
  }
  env->ExceptionClear();
  return false;
}

bool GetJNIFieldLong(JNIEnv* env,
                     jclass cls,
                     jobject obj,
                     const char* field_name,
                     jlong* value) {
  jfieldID field = env->GetFieldID(cls, field_name, "J");
  if (field) {
    *value = env->GetLongField(obj, field);
    return true;
  }
  env->ExceptionClear();
  return false;
}

bool SetJNIFieldInt(JNIEnv* env,
                    jclass cls,
                    jobject obj,
                    const char* field_name,
                    int value) {
  jfieldID field = env->GetFieldID(cls, field_name, "I");
  if (field) {
    env->SetIntField(obj, field, value);
    return true;
  }
  env->ExceptionClear();
  return false;
}

bool GetJNIFieldStaticInt(JNIEnv* env,
                          jclass cls,
                          const char* field_name,
                          int* value) {
  jfieldID field = env->GetStaticFieldID(cls, field_name, "I");
  if (field) {
    *value = env->GetStaticIntField(cls, field);
    return true;
  }
  env->ExceptionClear();
  return false;
}

bool CallJNIMethodI_V(JNIEnv* env,
                      jclass cls,
                      jobject obj,
                      const char* method_name,
                      int* value) {
  jmethodID methodID = env->GetMethodID(cls, method_name, "()I");
  if (methodID) {
    *value = env->CallIntMethod(obj, methodID);
    return true;
  }
  env->ExceptionClear();
  return false;
}

bool CallJNIMethodC_V(JNIEnv* env,
                      jclass cls,
                      jobject obj,
                      const char* method_name,
                      char16* value) {
  jmethodID methodID = env->GetMethodID(cls, method_name, "()C");
  if (methodID) {
    *value = env->CallCharMethod(obj, methodID);
    return true;
  }
  env->ExceptionClear();
  return false;
}

CefRange GetJNIPageRange(JNIEnv* env, jobject obj) {
  CefRange range;

  jclass cls = FindClass(env, "org/cef/misc/CefPageRange");
  if (!cls)
    return range;

  int from, to;
  if (GetJNIFieldInt(env, cls, obj, "from", &from) &&
      GetJNIFieldInt(env, cls, obj, "to", &to)) {
    range.Set(from, to);
  }
  return range;
}

jobject NewJNIPageRange(JNIEnv* env, const CefRange& range) {
  jclass cls = FindClass(env, "org/cef/misc/CefPageRange");
  if (!cls)
    return NULL;

  jobject obj = NewJNIObject(env, cls);
  if (!obj)
    return NULL;

  if (SetJNIFieldInt(env, cls, obj, "from", range.from) &&
      SetJNIFieldInt(env, cls, obj, "to", range.to)) {
    return obj;
  }

  env->DeleteLocalRef(obj);
  return NULL;
}

CefSize GetJNISize(JNIEnv* env, jobject obj) {
  CefSize size;

  jclass cls = FindClass(env, "java/awt/Dimension");
  if (!cls)
    return size;

  int width, height;
  if (GetJNIFieldInt(env, cls, obj, "width", &width) &&
      GetJNIFieldInt(env, cls, obj, "height", &height)) {
    size.Set(width, height);
  }
  return size;
}

CefRect GetJNIRect(JNIEnv* env, jobject obj) {
  CefRect rect;

  jclass cls = FindClass(env, "java/awt/Rectangle");
  if (!cls)
    return rect;

  int x, y, width, height;
  if (GetJNIFieldInt(env, cls, obj, "x", &x) &&
      GetJNIFieldInt(env, cls, obj, "y", &y) &&
      GetJNIFieldInt(env, cls, obj, "width", &width) &&
      GetJNIFieldInt(env, cls, obj, "height", &height)) {
    rect.Set(x, y, width, height);
    return rect;
  }

  return rect;
}

jobject NewJNIRect(JNIEnv* env, const CefRect& rect) {
  jclass cls = FindClass(env, "java/awt/Rectangle");
  if (!cls)
    return NULL;

  jobject obj = NewJNIObject(env, cls);
  if (!obj)
    return NULL;

  if (SetJNIFieldInt(env, cls, obj, "x", rect.x) &&
      SetJNIFieldInt(env, cls, obj, "y", rect.y) &&
      SetJNIFieldInt(env, cls, obj, "width", rect.width) &&
      SetJNIFieldInt(env, cls, obj, "height", rect.height)) {
    return obj;
  }

  env->DeleteLocalRef(obj);
  return NULL;
}

jobjectArray NewJNIRectArray(JNIEnv* env, const std::vector<CefRect>& vals) {
  if (vals.empty())
    return NULL;

  jclass cls = FindClass(env, "java/awt/Rectangle");
  if (!cls)
    return NULL;

  jobjectArray arr =
      env->NewObjectArray(static_cast<jsize>(vals.size()), cls, NULL);

  for (jsize i = 0; i < static_cast<jsize>(vals.size()); i++) {
    jobject rect_obj = NewJNIRect(env, vals[i]);
    env->SetObjectArrayElement(arr, i, rect_obj);
    env->DeleteLocalRef(rect_obj);
  }

  return arr;
}

bool GetJNIPoint(JNIEnv* env, jobject obj, int* x, int* y) {
  jclass cls = FindClass(env, "java/awt/Point");
  if (!cls)
    return false;

  if (GetJNIFieldInt(env, cls, obj, "x", x) &&
      GetJNIFieldInt(env, cls, obj, "y", y)) {
    return true;
  }

  return false;
}

// Create a new java.awt.Point.
jobject NewJNIPoint(JNIEnv* env, int x, int y) {
  jclass cls = FindClass(env, "java/awt/Point");
  if (!cls)
    return NULL;

  jobject obj = NewJNIObject(env, cls);
  if (!obj)
    return NULL;

  if (SetJNIFieldInt(env, cls, obj, "x", x) &&
      SetJNIFieldInt(env, cls, obj, "y", y)) {
    return obj;
  }

  env->DeleteLocalRef(obj);
  return NULL;
}

CefSettings GetJNISettings(JNIEnv* env, jobject obj) {
  CefString tmp;
  CefSettings settings;
  if (!obj)
    return settings;

  jclass cls = FindClass(env, "org/cef/CefSettings");
  if (!cls)
    return settings;

  if (GetJNIFieldString(env, cls, obj, "browser_subprocess_path", &tmp) &&
      !tmp.empty()) {
    CefString(&settings.browser_subprocess_path) = tmp;
    tmp.clear();
  }
  GetJNIFieldBoolean(env, cls, obj, "windowless_rendering_enabled",
                     &settings.windowless_rendering_enabled);
  GetJNIFieldBoolean(env, cls, obj, "command_line_args_disabled",
                     &settings.command_line_args_disabled);
  if (GetJNIFieldString(env, cls, obj, "cache_path", &tmp) && !tmp.empty()) {
    CefString(&settings.cache_path) = tmp;
    tmp.clear();
  }
  GetJNIFieldBoolean(env, cls, obj, "persist_session_cookies",
                     &settings.persist_session_cookies);
  if (GetJNIFieldString(env, cls, obj, "user_agent", &tmp) && !tmp.empty()) {
    CefString(&settings.user_agent) = tmp;
    tmp.clear();
  }
  if (GetJNIFieldString(env, cls, obj, "product_version", &tmp) &&
      !tmp.empty()) {
    CefString(&settings.product_version) = tmp;
    tmp.clear();
  }
  if (GetJNIFieldString(env, cls, obj, "locale", &tmp) && !tmp.empty()) {
    CefString(&settings.locale) = tmp;
    tmp.clear();
  }
  if (GetJNIFieldString(env, cls, obj, "log_file", &tmp) && !tmp.empty()) {
    CefString(&settings.log_file) = tmp;
    tmp.clear();
  }
  jobject obj_sev = NULL;
  if (GetJNIFieldObject(env, cls, obj, "log_severity", &obj_sev,
                        "Lorg/cef/CefSettings$LogSeverity;")) {
    if (obj_sev != NULL) {
      if (IsJNIEnumValue(env, obj_sev, "org/cef/CefSettings$LogSeverity",
                         "LOGSEVERITY_VERBOSE"))
        settings.log_severity = LOGSEVERITY_VERBOSE;
      else if (IsJNIEnumValue(env, obj_sev, "org/cef/CefSettings$LogSeverity",
                              "LOGSEVERITY_INFO"))
        settings.log_severity = LOGSEVERITY_INFO;
      else if (IsJNIEnumValue(env, obj_sev, "org/cef/CefSettings$LogSeverity",
                              "LOGSEVERITY_WARNING"))
        settings.log_severity = LOGSEVERITY_WARNING;
      else if (IsJNIEnumValue(env, obj_sev, "org/cef/CefSettings$LogSeverity",
                              "LOGSEVERITY_ERROR"))
        settings.log_severity = LOGSEVERITY_ERROR;
      else if (IsJNIEnumValue(env, obj_sev, "org/cef/CefSettings$LogSeverity",
                              "LOGSEVERITY_DISABLE"))
        settings.log_severity = LOGSEVERITY_DISABLE;
      else
        settings.log_severity = LOGSEVERITY_DEFAULT;
    }
  }
  if (GetJNIFieldString(env, cls, obj, "javascript_flags", &tmp) &&
      !tmp.empty()) {
    CefString(&settings.javascript_flags) = tmp;
    tmp.clear();
  }
  if (GetJNIFieldString(env, cls, obj, "resources_dir_path", &tmp) &&
      !tmp.empty()) {
    CefString(&settings.resources_dir_path) = tmp;
    tmp.clear();
  }
  if (GetJNIFieldString(env, cls, obj, "locales_dir_path", &tmp) &&
      !tmp.empty()) {
    CefString(&settings.locales_dir_path) = tmp;
    tmp.clear();
  }
  GetJNIFieldBoolean(env, cls, obj, "pack_loading_disabled",
                     &settings.pack_loading_disabled);
  GetJNIFieldInt(env, cls, obj, "remote_debugging_port",
                 &settings.remote_debugging_port);
  GetJNIFieldInt(env, cls, obj, "uncaught_exception_stack_size",
                 &settings.uncaught_exception_stack_size);
  GetJNIFieldBoolean(env, cls, obj, "ignore_certificate_errors",
                     &settings.ignore_certificate_errors);
  jobject obj_col = NULL;
  if (GetJNIFieldObject(env, cls, obj, "background_color", &obj_col,
                        "Lorg/cef/CefSettings$ColorType;")) {
    if (obj_col != NULL) {
      jlong jcolor = 0;
      JNI_CALL_METHOD(env, obj_col, "getColor", "()J", Long, jcolor);
      settings.background_color = (cef_color_t)jcolor;
    }
  }
  return settings;
}

CefPdfPrintSettings GetJNIPdfPrintSettings(JNIEnv* env, jobject obj) {
  CefString tmp;
  CefPdfPrintSettings settings;
  if (!obj)
    return settings;

  jclass cls = FindClass(env, "org/cef/misc/CefPdfPrintSettings");
  if (!cls)
    return settings;

  GetJNIFieldBoolean(env, cls, obj, "header_footer_enabled",
                     &settings.header_footer_enabled);

  if (GetJNIFieldString(env, cls, obj, "header_footer_title", &tmp) &&
      !tmp.empty()) {
    CefString(&settings.header_footer_title) = tmp;
    tmp.clear();
  }

  if (GetJNIFieldString(env, cls, obj, "header_footer_url", &tmp) &&
      !tmp.empty()) {
    CefString(&settings.header_footer_url) = tmp;
    tmp.clear();
  }

  GetJNIFieldBoolean(env, cls, obj, "landscape", &settings.landscape);

  GetJNIFieldBoolean(env, cls, obj, "backgrounds_enabled",
                     &settings.backgrounds_enabled);

  GetJNIFieldInt(env, cls, obj, "page_width", &settings.page_width);

  GetJNIFieldInt(env, cls, obj, "page_height", &settings.page_height);

  GetJNIFieldBoolean(env, cls, obj, "selection_only", &settings.selection_only);

  GetJNIFieldInt(env, cls, obj, "scale_factor", &settings.scale_factor);

  jobject obj_margin_type = NULL;
  if (GetJNIFieldObject(env, cls, obj, "margin_type", &obj_margin_type,
                        "Lorg/cef/misc/CefPdfPrintSettings$MarginType;")) {
    if (obj_margin_type != NULL) {
      if (IsJNIEnumValue(env, obj_margin_type,
                         "org/cef/misc/CefPdfPrintSettings$MarginType",
                         "DEFAULT"))
        settings.margin_type = PDF_PRINT_MARGIN_DEFAULT;
      else if (IsJNIEnumValue(env, obj_margin_type,
                              "org/cef/misc/CefPdfPrintSettings$MarginType",
                              "NONE"))
        settings.margin_type = PDF_PRINT_MARGIN_NONE;
      else if (IsJNIEnumValue(env, obj_margin_type,
                              "org/cef/misc/CefPdfPrintSettings$MarginType",
                              "MINIMUM"))
        settings.margin_type = PDF_PRINT_MARGIN_MINIMUM;
      else if (IsJNIEnumValue(env, obj_margin_type,
                              "org/cef/misc/CefPdfPrintSettings$MarginType",
                              "CUSTOM"))
        settings.margin_type = PDF_PRINT_MARGIN_CUSTOM;
    }
  }

  GetJNIFieldDouble(env, cls, obj, "margin_top", &settings.margin_top);

  GetJNIFieldDouble(env, cls, obj, "margin_bottom", &settings.margin_bottom);

  GetJNIFieldDouble(env, cls, obj, "margin_right", &settings.margin_right);

  GetJNIFieldDouble(env, cls, obj, "margin_left", &settings.margin_left);

  return settings;
}

jobject GetJNIBrowser(JNIEnv* env, CefRefPtr<CefBrowser> browser) {
  if (!browser)
    return NULL;
  CefRefPtr<ClientHandler> client =
      (ClientHandler*)browser->GetHost()->GetClient().get();
  return client->getBrowser(env, browser);
}

CefRefPtr<CefBrowser> GetCefBrowser(JNIEnv* env, jobject jbrowser) {
  return GetCefFromJNIObject<CefBrowser>(env, jbrowser, "CefBrowser");
}

jobject GetJNIBrowser(CefRefPtr<CefBrowser> browser) {
  if (!browser)
    return NULL;
  jobject jbrowser = NULL;
  BEGIN_ENV(env)
  jbrowser = GetJNIBrowser(env, browser);
  END_ENV(env)
  return jbrowser;
}

jobject NewJNITransitionType(JNIEnv* env,
                             CefRequest::TransitionType transitionType) {
  jobject result = NULL;
  switch (transitionType & TT_SOURCE_MASK) {
    default:
      JNI_CASE(env, "org/cef/network/CefRequest$TransitionType", TT_LINK,
               result);
      JNI_CASE(env, "org/cef/network/CefRequest$TransitionType", TT_EXPLICIT,
               result);
      JNI_CASE(env, "org/cef/network/CefRequest$TransitionType",
               TT_AUTO_SUBFRAME, result);
      JNI_CASE(env, "org/cef/network/CefRequest$TransitionType",
               TT_MANUAL_SUBFRAME, result);
      JNI_CASE(env, "org/cef/network/CefRequest$TransitionType", TT_FORM_SUBMIT,
               result);
      JNI_CASE(env, "org/cef/network/CefRequest$TransitionType", TT_RELOAD,
               result);
  }

  int qualifiers = (transitionType & TT_QUALIFIER_MASK);
  JNI_CALL_VOID_METHOD(env, result, "addQualifiers", "(I)V", qualifiers);

  return result;
}

jobject NewJNIURLRequestStatus(
    JNIEnv* env,
    CefResourceRequestHandler::URLRequestStatus status) {
  jobject result = GetJNIEnumValue(env, "org/cef/network/CefURLRequest$Status",
                                   "UR_UNKNOWN");

  switch (status) {
    default:
      JNI_CASE(env, "org/cef/network/CefURLRequest$Status", UR_UNKNOWN, result);
      JNI_CASE(env, "org/cef/network/CefURLRequest$Status", UR_SUCCESS, result);
      JNI_CASE(env, "org/cef/network/CefURLRequest$Status", UR_IO_PENDING,
               result);
      JNI_CASE(env, "org/cef/network/CefURLRequest$Status", UR_CANCELED,
               result);
      JNI_CASE(env, "org/cef/network/CefURLRequest$Status", UR_FAILED, result);
  }
  return result;
}

jobject GetJNIEnumValue(JNIEnv* env,
                        const char* class_name,
                        const char* enum_valname) {
  jclass sourceCls = FindClass(env, class_name);
  if (!sourceCls)
    return NULL;

  std::string tmp;
  tmp.append("L").append(class_name).append(";");

  jfieldID fieldId =
      env->GetStaticFieldID(sourceCls, enum_valname, tmp.c_str());
  if (!fieldId)
    return NULL;

  jobject jsource = env->GetStaticObjectField(sourceCls, fieldId);
  return jsource;
}

bool IsJNIEnumValue(JNIEnv* env,
                    jobject jenum,
                    const char* class_name,
                    const char* enum_valname) {
  if (!jenum)
    return false;

  jobject compareTo = GetJNIEnumValue(env, class_name, enum_valname);
  if (compareTo) {
    jboolean isEqual = JNI_FALSE;
    JNI_CALL_METHOD(env, jenum, "equals", "(Ljava/lang/Object;)Z", Boolean,
                    isEqual, compareTo);
    env->DeleteLocalRef(compareTo);
    return (isEqual != JNI_FALSE);
  }
  return false;
}
