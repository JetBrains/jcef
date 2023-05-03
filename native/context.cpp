// Copyright (c) 2016 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#include "context.h"

#include "include/cef_app.h"

#include "client_app.h"
#include "jni_util.h"

#if defined(OS_MAC)
#include "util_mac.h"
#endif

#if defined(OS_POSIX)
#include "signal_restore_posix.h"
#endif

namespace {

Context* g_context = nullptr;

CefSettings GetJNISettings(JNIEnv* env, jobject obj) {
  CefString tmp;
  CefSettings settings;
  if (!obj)
    return settings;

  ScopedJNIClass cls(env, "org/cef/CefSettings");
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
  if (GetJNIFieldString(env, cls, obj, "user_agent_product", &tmp) &&
      !tmp.empty()) {
    CefString(&settings.user_agent_product) = tmp;
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
  jobject obj_sev = nullptr;
  if (GetJNIFieldObject(env, cls, obj, "log_severity", &obj_sev,
                        "Lorg/cef/CefSettings$LogSeverity;")) {
    ScopedJNIObjectLocal severity(env, obj_sev);
    if (IsJNIEnumValue(env, severity, "org/cef/CefSettings$LogSeverity",
                       "LOGSEVERITY_VERBOSE")) {
      settings.log_severity = LOGSEVERITY_VERBOSE;
    } else if (IsJNIEnumValue(env, severity, "org/cef/CefSettings$LogSeverity",
                              "LOGSEVERITY_INFO")) {
      settings.log_severity = LOGSEVERITY_INFO;
    } else if (IsJNIEnumValue(env, severity, "org/cef/CefSettings$LogSeverity",
                              "LOGSEVERITY_WARNING")) {
      settings.log_severity = LOGSEVERITY_WARNING;
    } else if (IsJNIEnumValue(env, severity, "org/cef/CefSettings$LogSeverity",
                              "LOGSEVERITY_ERROR")) {
      settings.log_severity = LOGSEVERITY_ERROR;
    } else if (IsJNIEnumValue(env, severity, "org/cef/CefSettings$LogSeverity",
                              "LOGSEVERITY_DISABLE")) {
      settings.log_severity = LOGSEVERITY_DISABLE;
    } else {
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
  jobject obj_col = nullptr;
  if (GetJNIFieldObject(env, cls, obj, "background_color", &obj_col,
                        "Lorg/cef/CefSettings$ColorType;")) {
    ScopedJNIObjectLocal color_type(env, obj_col);
    jlong jcolor = 0;
    JNI_CALL_METHOD(env, color_type, "getColor", "()J", Long, jcolor);
    settings.background_color = (cef_color_t)jcolor;
  }
  if (GetJNIFieldString(env, cls, obj, "cookieable_schemes_list", &tmp) &&
      !tmp.empty()) {
    CefString(&settings.cookieable_schemes_list) = tmp;
    tmp.clear();
  }
  GetJNIFieldBoolean(env, cls, obj, "cookieable_schemes_exclude_defaults",
                     &settings.cookieable_schemes_exclude_defaults);
  GetJNIFieldBoolean(env, cls, obj, "no_sandbox",
                     &settings.no_sandbox);

  return settings;
}

}  // namespace

// static
void Context::Create() {
  new Context();
}

// static
void Context::Destroy() {
  DCHECK(g_context);
  if (g_context)
    delete g_context;
}

// static
Context* Context::GetInstance() {
  return g_context;
}

bool Context::PreInitialize(JNIEnv* env, jobject c) {
  DCHECK(thread_checker_.CalledOnValidThread());

  JavaVM* jvm;
  jint rs = env->GetJavaVM(&jvm);
  DCHECK_EQ(rs, JNI_OK);
  if (rs != JNI_OK)
    return JNI_FALSE;
  SetJVM(jvm);

  ScopedJNIClass javaClass(env, env->GetObjectClass(c));
  ScopedJNIObjectResult javaClassLoader(env);
  JNI_CALL_METHOD(env, javaClass, "getClassLoader", "()Ljava/lang/ClassLoader;",
                  Object, javaClassLoader);
  ASSERT(javaClassLoader);
  if (!javaClassLoader)
    return false;
  SetJavaClassLoader(env, javaClassLoader);

  return true;
}

bool Context::Initialize(JNIEnv* env,
                         jobject c,
                         jobject appHandler,
                         jobject jsettings,
                         jboolean checkThread) {
  if (checkThread) DCHECK(thread_checker_.CalledOnValidThread());

#if defined(OS_WIN)
  CefMainArgs main_args(::GetModuleHandle(nullptr));
#else
  char *argv[] = {(char*)"jcef", (char*)"--disable-in-process-stack-traces"};
  CefMainArgs main_args(2, argv);
#endif

  CefSettings settings = GetJNISettings(env, jsettings);

#if defined(OS_WIN) || defined(OS_LINUX)
  // Use external message pump with OSR.
  // [tav] use the same settings for the windowed and OSR modes
  external_message_pump_ = false; //!!settings.windowless_rendering_enabled;
  if (!external_message_pump_) {
    // Windowed rendering on Windows requires multi-threaded message loop,
    // otherwise something eats the messages required by Java and the Java
    // window becomes unresponsive.
    //
    // Actually the same appears to be true for Linux, which is why we also
    // need multithreaded message loops there. Note that however, on Linux
    // it is more difficult to get this to work: it is necessary for the first
    // call to Xlib to be a call to XInitThreads! Since Java itself calls
    // Xlib when it initializes the first window, an application must make
    // sure to invoke this method before any other Xlib functions are called
    // - including by the Java runtime itself, which makes this feat a little
    // tricky. The CefApp class exposes a static method for this purpose,
    // initXlibForMultithreading(), but the host application must load the
    // jcef native lib by itself in order to use it, and it must invoke it
    // VERY early, ideally at the beginning of the main method.
    // Another neat trick to get this done is to create a special native lib
    // just for this purpose like described in this StackOverflow thread:
    // https://stackoverflow.com/questions/24559368
    settings.multi_threaded_message_loop = true;
  }
#endif

  // Use CefAppHandler.onScheduleMessagePumpWork to schedule calls to
  // DoMessageLoopWork.
  settings.external_message_pump = external_message_pump_;

  CefRefPtr<ClientApp> client_app(
      new ClientApp(CefString(&settings.cache_path), env, appHandler));
  bool res = false;

#if defined(OS_POSIX)
  // CefInitialize will reset signal handlers. Backup/restore the original
  // signal handlers to avoid crashes in the JVM (see issue #41).
  BackupSignalHandlers();
#endif

#if defined(OS_MAC)
  res = util_mac::CefInitializeOnMainThread(main_args, settings,
                                            client_app.get());
#else

void * p_sandbox_info = 0;
#if defined(OS_WIN)
  jclass jc_System = env->FindClass("java/lang/System");
  jmethodID jm_getProperty = env->GetStaticMethodID(jc_System, "getProperty", "(Ljava/lang/String;)Ljava/lang/String;");

  const char propName[] = "jcef.sandbox.ptr";
  jstring jKey = env->NewStringUTF(propName);
  jstring jValue = (jstring)env->CallStaticObjectMethod(jc_System, jm_getProperty, jKey);
  env->DeleteLocalRef(jKey);

  std::string propVal = "";
  if (jValue) {
    const char* chars = env->GetStringUTFChars(jValue, NULL);
    if (chars != NULL) {
      propVal.assign(chars);
      env->ReleaseStringUTFChars(jValue, chars);
    }
  }

  env->DeleteLocalRef(jValue);

  if (propVal.empty())
    fprintf(stdout, "Initialize JCEF context without sandbox\n");
  else
    fprintf(stdout, "Initialize JCEF context with sandbox=%s\n", propVal.c_str());

  sscanf(propVal.c_str(), "%p", &p_sandbox_info);
#endif // Windows

  res = CefInitialize(main_args, settings, client_app.get(), p_sandbox_info);
#endif

#if defined(OS_POSIX)
  RestoreSignalHandlers();
#endif

  return res;
}

void Context::OnContextInitialized() {
  REQUIRE_UI_THREAD();
  temp_window_.reset(new TempWindow());
}

void Context::DoMessageLoopWork() {
  DCHECK(thread_checker_.CalledOnValidThread());

#if defined(OS_MAC)
  util_mac::CefDoMessageLoopWorkOnMainThread();
#else
  CefDoMessageLoopWork();
#endif
}

void Context::Shutdown() {
  static bool shutdownWasCalled = false;
  if (shutdownWasCalled) return;
  shutdownWasCalled = true;

  // Clear scheme handler factories on shutdown to avoid refcount DCHECK.
  CefClearSchemeHandlerFactories();

  ClientApp::eraseTempFiles();
#if defined(OS_MAC)
  util_mac::CefShutdownOnMainThread();
#else

  // NOTE: external_message_pump_ == false in Windows and Linux (see Context::Initialize)
  // So don't pump CefDoMessageLoopWork (a few times before shutting down) here.
  temp_window_.reset(nullptr);

  CefShutdown();
#endif
}

Context::Context() : external_message_pump_(true) {
  DCHECK(!g_context);
  g_context = this;

#if defined(OS_MAC)
  // On macOS we create this object very early to allow LibraryLoader
  // assignment. However, we still want the PreInitialize() call to determine
  // thread ownership.
  thread_checker_.DetachFromThread();
#endif
}

Context::~Context() {
  DCHECK(thread_checker_.CalledOnValidThread());
  g_context = nullptr;

#if defined(OS_MAC)
  cef_unload_library();
#endif
}
