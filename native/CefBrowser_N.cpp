// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#include "CefBrowser_N.h"

#include "include/base/cef_callback.h"
#include "include/cef_browser.h"
#include "include/cef_parser.h"
#include "include/cef_task.h"
#include "include/wrapper/cef_closure_task.h"

#include "browser_process_handler.h"
#include "client_handler.h"
#include "critical_wait.h"
#include "devtools_message_observer.h"
#include "int_callback.h"
#include "jni_util.h"
#include "keyboard_utils.h"
#include "life_span_handler.h"
#include "pdf_print_callback.h"
#include "run_file_dialog_callback.h"
#include "string_visitor.h"
#include "temp_window.h"
#include "window_handler.h"

#if defined(OS_LINUX)
#define XK_3270  // for XK_3270_BackTab
#include <X11/XF86keysym.h>
#include <X11/keysym.h>
#include <memory>
#endif

#if defined(OS_MAC)
#include "util_mac.h"
#endif

#if defined(OS_WIN)
#include <memory>
#include <synchapi.h>
#undef MOUSE_MOVED
#endif

namespace {

int GetCefModifiers(JNIEnv* env, jclass cls, int modifiers) {
  JNI_STATIC_DEFINE_INT_RV(env, cls, ALT_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, BUTTON1_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, BUTTON2_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, BUTTON3_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, CTRL_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, META_DOWN_MASK, 0);
  JNI_STATIC_DEFINE_INT_RV(env, cls, SHIFT_DOWN_MASK, 0);

  int cef_modifiers = 0;
  if (modifiers & JNI_STATIC(ALT_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_ALT_DOWN;
  if (modifiers & JNI_STATIC(BUTTON1_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_LEFT_MOUSE_BUTTON;
  if (modifiers & JNI_STATIC(BUTTON2_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_MIDDLE_MOUSE_BUTTON;
  if (modifiers & JNI_STATIC(BUTTON3_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_RIGHT_MOUSE_BUTTON;
  if (modifiers & JNI_STATIC(CTRL_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_CONTROL_DOWN;
  if (modifiers & JNI_STATIC(META_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_COMMAND_DOWN;
  if (modifiers & JNI_STATIC(SHIFT_DOWN_MASK))
    cef_modifiers |= EVENTFLAG_SHIFT_DOWN;

  return cef_modifiers;
}

struct JNIObjectsForCreate {
 public:
  ScopedJNIObjectGlobal jbrowser;
  ScopedJNIObjectGlobal jparentBrowser;
  ScopedJNIObjectGlobal jclientHandler;
  ScopedJNIObjectGlobal url;
  ScopedJNIObjectGlobal canvas;
  ScopedJNIObjectGlobal jcontext;
  ScopedJNIObjectGlobal jinspectAt;
  ScopedJNIObjectGlobal jbrowserSettings;

  JNIObjectsForCreate(JNIEnv* env,
                      jobject _jbrowser,
                      jobject _jparentBrowser,
                      jobject _jclientHandler,
                      jstring _url,
                      jobject _canvas,
                      jobject _jcontext,
                      jobject _jinspectAt,
                      jobject _browserSettings)
      :

        jbrowser(env, _jbrowser),
        jparentBrowser(env, _jparentBrowser),
        jclientHandler(env, _jclientHandler),
        url(env, _url),
        canvas(env, _canvas),
        jcontext(env, _jcontext),
        jinspectAt(env, _jinspectAt),
        jbrowserSettings(env, _browserSettings) {}
};

void create(std::shared_ptr<JNIObjectsForCreate> objs,
            jlong windowHandle,
            jboolean osr,
            jboolean transparent) {
  ScopedJNIEnv env;
  CefRefPtr<ClientHandler> clientHandler = GetCefFromJNIObject_sync<ClientHandler>(
      env, objs->jclientHandler, "CefClientHandler");
  if (!clientHandler.get())
    return;

  CefRefPtr<LifeSpanHandler> lifeSpanHandler =
      (LifeSpanHandler*)clientHandler->GetLifeSpanHandler().get();
  if (!lifeSpanHandler.get())
    return;

  CefWindowInfo windowInfo;
  if (osr == JNI_FALSE) {
    CefRect rect;
    CefRefPtr<WindowHandler> windowHandler =
        (WindowHandler*)clientHandler->GetWindowHandler().get();
    if (windowHandler.get()) {
      windowHandler->GetRect(objs->jbrowser, rect);
    }
#if defined(OS_WIN)
    CefWindowHandle parent = TempWindow::GetWindowHandle();
    if (objs->canvas != nullptr) {
      parent = GetHwndOfCanvas(objs->canvas, env);
    } else {
      // Do not activate hidden browser windows on creation.
      windowInfo.ex_style |= WS_EX_NOACTIVATE;
    }
    windowInfo.SetAsChild(parent, rect);
#elif defined(OS_MAC)
    NSWindow* parent = nullptr;
    if (windowHandle != 0) {
      parent = (NSWindow*)windowHandle;
    } else {
      parent = TempWindow::GetWindow();
    }
    CefWindowHandle browserContentView =
        util_mac::CreateBrowserContentView(parent, rect);
    windowInfo.SetAsChild(browserContentView, rect);
#elif defined(OS_LINUX)
    CefWindowHandle parent = TempWindow::GetWindowHandle();
    if (objs->canvas != nullptr) {
      parent = GetDrawableOfCanvas(objs->canvas, env);
    }
    windowInfo.SetAsChild(parent, rect);
#endif
  } else {
#if defined(OS_MAC)
    windowInfo.SetAsWindowless(
        (CefWindowHandle)util_mac::GetNSView((void*)windowHandle));
#else
    windowInfo.SetAsWindowless((CefWindowHandle)windowHandle);
#endif
  }

  CefBrowserSettings settings;

  /* [tav] do not override CefSettings.background_color
  if (transparent == JNI_FALSE) {
    // Specify an opaque background color (white) to disable transparency.
    settings.background_color = CefColorSetARGB(255, 255, 255, 255);
  }*/

  ScopedJNIClass cefBrowserSettings(env, "org/cef/CefBrowserSettings");
  if (cefBrowserSettings != nullptr &&
      objs->jbrowserSettings != nullptr) {  // Dev-tools settings are null
    GetJNIFieldInt(env, cefBrowserSettings, objs->jbrowserSettings,
                   "windowless_frame_rate", &settings.windowless_frame_rate);
  }

  CefRefPtr<CefBrowser> browserObj;
  CefString strUrl = GetJNIString(env, static_cast<jstring>(objs->url.get()));

  CefRefPtr<CefRequestContext> context = GetCefFromJNIObject_sync<CefRequestContext>(
      env, objs->jcontext, "CefRequestContext");

  CefRefPtr<CefBrowser> parentBrowser =
      GetCefFromJNIObject_sync<CefBrowser>(env, objs->jparentBrowser, "CefBrowser");

  // Add a global ref that will be released in LifeSpanHandler::OnAfterCreated.
  jobject globalRef = env->NewGlobalRef(objs->jbrowser);
  lifeSpanHandler->registerJBrowser(globalRef);

  // If parentBrowser is set, we want to show the DEV-Tools for that browser
  if (parentBrowser.get() != nullptr) {
    CefPoint inspectAt;
    if (objs->jinspectAt != nullptr) {
      int x, y;
      GetJNIPoint(env, objs->jinspectAt, &x, &y);
      inspectAt.Set(x, y);
    }
    parentBrowser->GetHost()->ShowDevTools(windowInfo, clientHandler.get(),
                                           settings, inspectAt);
    JNI_CALL_VOID_METHOD(env, objs->jbrowser, "notifyBrowserCreated", "()V");
    return;
  }

  CefRefPtr<CefDictionaryValue> extra_info;
  auto router_configs = BrowserProcessHandler::GetMessageRouterConfigs();
  if (router_configs) {
    // Send the message router config to CefHelperApp::OnBrowserCreated.
    extra_info = CefDictionaryValue::Create();
    extra_info->SetList("router_configs", router_configs);
  }

  static int testDelaySec = -1;
  if (testDelaySec < 0) {
    testDelaySec = GetJavaSystemPropertyLong("test.delay.create_browser2.seconds", env, 0);
    if (testDelaySec > 0) LOG(INFO) << "Use test.delay.create_browser2.seconds=" << testDelaySec;
  }
  if (testDelaySec > 0) {
#if defined(OS_WIN)
    Sleep(testDelaySec * 1000l);
#else
    sleep(testDelaySec*1000l);
#endif
  }

  bool result = CefBrowserHost::CreateBrowser(
      windowInfo, clientHandler.get(), strUrl, settings, extra_info, context);
  if (!result) {
    lifeSpanHandler->unregisterJBrowser(globalRef);
    env->DeleteGlobalRef(globalRef);
    return;
  }
  JNI_CALL_VOID_METHOD(env, objs->jbrowser, "notifyBrowserCreated", "()V");
}

static void getZoomLevel(CefRefPtr<CefBrowserHost> host, std::shared_ptr<double> result) {
  *result = host->GetZoomLevel();
}

void executeDevToolsMethod(CefRefPtr<CefBrowserHost> host,
                           const CefString& method,
                           const CefString& parametersAsJson,
                           CefRefPtr<IntCallback> callback) {
  CefRefPtr<CefDictionaryValue> parameters = nullptr;
  if (!parametersAsJson.empty()) {
    CefRefPtr<CefValue> value = CefParseJSON(
        parametersAsJson, cef_json_parser_options_t::JSON_PARSER_RFC);

    if (!value || value->GetType() != VTYPE_DICTIONARY) {
      callback->onComplete(0);
      return;
    }

    parameters = value->GetDictionary();
  }

  callback->onComplete(host->ExecuteDevToolsMethod(0, method, parameters));
}

void OnAfterParentChanged(CefRefPtr<CefBrowser> browser) {
  if (!CefCurrentlyOn(TID_UI)) {
    CefPostTask(TID_UI, base::BindOnce(&OnAfterParentChanged, browser));
    return;
  }

  if (browser->GetHost()->GetClient()) {
    CefRefPtr<LifeSpanHandler> lifeSpanHandler =
        (LifeSpanHandler*)browser->GetHost()
            ->GetClient()
            ->GetLifeSpanHandler()
            .get();
    if (lifeSpanHandler) {
      lifeSpanHandler->OnAfterParentChanged(browser);
    }
  }
}

CefPdfPrintSettings GetJNIPdfPrintSettings(JNIEnv* env, jobject obj) {
  CefString tmp;
  CefPdfPrintSettings settings;
  if (!obj)
    return settings;

  ScopedJNIClass cls(env, "org/cef/misc/CefPdfPrintSettings");
  if (!cls)
    return settings;

  GetJNIFieldBoolean(env, cls, obj, "landscape", &settings.landscape);

  GetJNIFieldBoolean(env, cls, obj, "print_background",
                     &settings.print_background);

  GetJNIFieldDouble(env, cls, obj, "scale", &settings.scale);

  GetJNIFieldDouble(env, cls, obj, "paper_width", &settings.paper_width);
  GetJNIFieldDouble(env, cls, obj, "paper_height", &settings.paper_height);

  GetJNIFieldBoolean(env, cls, obj, "prefer_css_page_size",
                     &settings.prefer_css_page_size);

  jobject obj_margin_type = nullptr;
  if (GetJNIFieldObject(env, cls, obj, "margin_type", &obj_margin_type,
                        "Lorg/cef/misc/CefPdfPrintSettings$MarginType;")) {
    ScopedJNIObjectLocal margin_type(env, obj_margin_type);
    if (IsJNIEnumValue(env, margin_type,
                       "org/cef/misc/CefPdfPrintSettings$MarginType",
                       "DEFAULT")) {
      settings.margin_type = PDF_PRINT_MARGIN_DEFAULT;
    } else if (IsJNIEnumValue(env, margin_type,
                              "org/cef/misc/CefPdfPrintSettings$MarginType",
                              "NONE")) {
      settings.margin_type = PDF_PRINT_MARGIN_NONE;
    } else if (IsJNIEnumValue(env, margin_type,
                              "org/cef/misc/CefPdfPrintSettings$MarginType",
                              "CUSTOM")) {
      settings.margin_type = PDF_PRINT_MARGIN_CUSTOM;
    }
  }

  GetJNIFieldDouble(env, cls, obj, "margin_top", &settings.margin_top);
  GetJNIFieldDouble(env, cls, obj, "margin_bottom", &settings.margin_bottom);
  GetJNIFieldDouble(env, cls, obj, "margin_right", &settings.margin_right);
  GetJNIFieldDouble(env, cls, obj, "margin_left", &settings.margin_left);

  if (GetJNIFieldString(env, cls, obj, "page_ranges", &tmp) && !tmp.empty()) {
    CefString(&settings.page_ranges) = tmp;
    tmp.clear();
  }

  GetJNIFieldBoolean(env, cls, obj, "display_header_footer",
                     &settings.display_header_footer);

  if (GetJNIFieldString(env, cls, obj, "header_template", &tmp) &&
      !tmp.empty()) {
    CefString(&settings.header_template) = tmp;
    tmp.clear();
  }

  if (GetJNIFieldString(env, cls, obj, "footer_template", &tmp) &&
      !tmp.empty()) {
    CefString(&settings.footer_template) = tmp;
    tmp.clear();
  }

  return settings;
}

// JNI CefRegistration object.
class ScopedJNIRegistration : public ScopedJNIObject<CefRegistration> {
 public:
  ScopedJNIRegistration(JNIEnv* env, CefRefPtr<CefRegistration> obj)
      : ScopedJNIObject<CefRegistration>(env,
                                         obj,
                                         "org/cef/browser/CefRegistration_N",
                                         "CefRegistration") {}
};

}  // namespace

JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1CreateBrowser(JNIEnv* env,
                                                    jobject jbrowser,
                                                    jobject jclientHandler,
                                                    jlong windowHandle,
                                                    jstring url,
                                                    jboolean osr,
                                                    jboolean transparent,
                                                    jobject canvas,
                                                    jobject jcontext,
                                                    jobject browserSettings) {
  std::shared_ptr<JNIObjectsForCreate> objs(
      new JNIObjectsForCreate(env, jbrowser, nullptr, jclientHandler, url,
                              canvas, jcontext, nullptr, browserSettings));

  static int testDelaySec = -1;
  if (testDelaySec < 0) {
    testDelaySec = GetJavaSystemPropertyLong("test.delay.create_browser.seconds", env, 0);
    if (testDelaySec > 0) LOG(INFO) << "Use test.delay.create_browser.seconds=" << testDelaySec;
  }

  if (testDelaySec > 0) {
    CefPostDelayedTask(TID_UI,
                base::BindOnce(&create, objs, windowHandle, osr, transparent), testDelaySec*1000l);
  } else if (CefCurrentlyOn(TID_UI)) {
    create(objs, windowHandle, osr, transparent);
  } else {
    CefPostTask(TID_UI,
                base::BindOnce(&create, objs, windowHandle, osr, transparent));
  }
  return JNI_FALSE;  // set asynchronously
}

JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1CreateDevTools(JNIEnv* env,
                                                     jobject jbrowser,
                                                     jobject jparent,
                                                     jobject jclientHandler,
                                                     jlong windowHandle,
                                                     jboolean osr,
                                                     jboolean transparent,
                                                     jobject canvas,
                                                     jobject inspect) {
  std::shared_ptr<JNIObjectsForCreate> objs(
      new JNIObjectsForCreate(env, jbrowser, jparent, jclientHandler, nullptr,
                              canvas, nullptr, inspect, nullptr));
  if (CefCurrentlyOn(TID_UI)) {
    create(objs, windowHandle, osr, transparent);
  } else {
    CefPostTask(TID_UI,
                base::BindOnce(&create, objs, windowHandle, osr, transparent));
  }
  return JNI_FALSE;  // set asynchronously
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1ExecuteDevToolsMethod(
    JNIEnv* env,
    jobject jbrowser,
    jstring method,
    jstring parametersAsJson,
    jobject jcallback) {
  CefRefPtr<IntCallback> callback = new IntCallback(env, jcallback);

  CefRefPtr<CefBrowser> browser = GetJNIBrowser(env, jbrowser);
  if (!browser.get()) {
    callback->onComplete(0);
    return;
  }

  CefString strMethod = GetJNIString(env, method);
  CefString strParametersAsJson = GetJNIString(env, parametersAsJson);

  if (CefCurrentlyOn(TID_UI)) {
    executeDevToolsMethod(browser->GetHost(), strMethod, strParametersAsJson,
                          callback);
  } else {
    CefPostTask(TID_UI,
                base::BindOnce(executeDevToolsMethod, browser->GetHost(),
                               strMethod, strParametersAsJson, callback));
  }
}

JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1AddDevToolsMessageObserver(
    JNIEnv* env,
    jobject jbrowser,
    jobject jobserver) {
  CefRefPtr<CefBrowser> browser =
      JNI_GET_BROWSER_OR_RETURN(env, jbrowser, NULL);

  CefRefPtr<DevToolsMessageObserver> observer =
      new DevToolsMessageObserver(env, jobserver);

  CefRefPtr<CefRegistration> registration =
      browser->GetHost()->AddDevToolsMessageObserver(observer);

  ScopedJNIRegistration jregistration(env, registration);
  return jregistration.Release();
}

JNIEXPORT jlong JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetWindowHandle(JNIEnv* env,
                                                      jobject obj,
                                                      jlong displayHandle) {
  CefWindowHandle windowHandle = kNullWindowHandle;
#if defined(OS_WIN)
  windowHandle = ::WindowFromDC((HDC)displayHandle);
#elif defined(OS_LINUX)
  return displayHandle;
#elif defined(OS_MAC)
  ASSERT(util_mac::IsNSView((void*)displayHandle));
#endif
  return (jlong)windowHandle;
}

JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1CanGoBack(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser =
      JNI_GET_BROWSER_OR_RETURN(env, obj, JNI_FALSE);
  return browser->CanGoBack() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GoBack(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GoBack();
}

JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1CanGoForward(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser =
      JNI_GET_BROWSER_OR_RETURN(env, obj, JNI_FALSE);
  return browser->CanGoForward() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GoForward(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GoForward();
}

JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1IsLoading(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser =
      JNI_GET_BROWSER_OR_RETURN(env, obj, JNI_FALSE);
  return browser->IsLoading() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1Reload(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->Reload();
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1ReloadIgnoreCache(JNIEnv* env,
                                                        jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->ReloadIgnoreCache();
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1StopLoad(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->StopLoad();
}

JNIEXPORT jint JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetIdentifier(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, -1);
  return browser->GetIdentifier();
}

JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetMainFrame(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, nullptr);
  CefRefPtr<CefFrame> frame = browser->GetMainFrame();
  if (!frame)
    return nullptr;
  ScopedJNIFrame jframe(env, frame);
  return jframe.Release();
}

JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetFocusedFrame(JNIEnv* env,
                                                      jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, nullptr);
  CefRefPtr<CefFrame> frame = browser->GetFocusedFrame();
  if (!frame)
    return nullptr;
  ScopedJNIFrame jframe(env, frame);
  return jframe.Release();
}

JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetFrame(JNIEnv* env,
                                               jobject obj,
                                               jstring identifier) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, nullptr);
  CefRefPtr<CefFrame> frame = browser->GetFrameByIdentifier(GetJNIString(env, identifier));
  if (!frame)
    return nullptr;
  ScopedJNIFrame jframe(env, frame);
  return jframe.Release();
}

JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetFrame2(JNIEnv* env,
                                                jobject obj,
                                                jstring name) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, nullptr);
  CefRefPtr<CefFrame> frame = browser->GetFrameByName(GetJNIString(env, name));
  if (!frame)
    return nullptr;
  ScopedJNIFrame jframe(env, frame);
  return jframe.Release();
}

JNIEXPORT jint JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetFrameCount(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, -1);
  return (jint)browser->GetFrameCount();
}

JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetFrameIdentifiers(JNIEnv* env,
                                                          jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, nullptr);
  std::vector<CefString> identifiers;
  browser->GetFrameIdentifiers(identifiers);
  return NewJNIStringVector(env, identifiers);
}

JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetFrameNames(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, nullptr);
  std::vector<CefString> names;
  browser->GetFrameNames(names);
  return NewJNIStringVector(env, names);
}

JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1IsPopup(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser =
      JNI_GET_BROWSER_OR_RETURN(env, obj, JNI_FALSE);
  return browser->IsPopup() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1HasDocument(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser =
      JNI_GET_BROWSER_OR_RETURN(env, obj, JNI_FALSE);
  return browser->HasDocument() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1ViewSource(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  CefRefPtr<CefFrame> mainFrame = browser->GetMainFrame();
  CefPostTask(TID_UI, base::BindOnce(&CefFrame::ViewSource, mainFrame.get()));
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetSource(JNIEnv* env,
                                                jobject obj,
                                                jobject jvisitor) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetMainFrame()->GetSource(new StringVisitor(env, jvisitor));
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetText(JNIEnv* env,
                                              jobject obj,
                                              jobject jvisitor) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetMainFrame()->GetText(new StringVisitor(env, jvisitor));
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1LoadRequest(JNIEnv* env,
                                                  jobject obj,
                                                  jobject jrequest) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  ScopedJNIRequest requestObj(env);
  requestObj.SetHandle(jrequest, false /* should_delete */);
  CefRefPtr<CefRequest> request = requestObj.GetCefObject();
  if (!request)
    return;
  browser->GetMainFrame()->LoadRequest(request);
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1LoadURL(JNIEnv* env,
                                              jobject obj,
                                              jstring url) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetMainFrame()->LoadURL(GetJNIString(env, url));
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1ExecuteJavaScript(JNIEnv* env,
                                                        jobject obj,
                                                        jstring code,
                                                        jstring url,
                                                        jint line) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetMainFrame()->ExecuteJavaScript(GetJNIString(env, code),
                                             GetJNIString(env, url), line);
}

JNIEXPORT jstring JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetURL(JNIEnv* env, jobject obj) {
  jstring tmp = env->NewStringUTF("");
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, tmp);
  return NewJNIString(env, browser->GetMainFrame()->GetURL());
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1Close(JNIEnv* env,
                                            jobject obj,
                                            jboolean force) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  if (force != JNI_FALSE) {
    if (browser->GetHost()->IsWindowRenderingDisabled()) {
      browser->GetHost()->CloseBrowser(true);
    } else {
      // Destroy the native window representation.
      if (CefCurrentlyOn(TID_UI))
        util::DestroyCefBrowser(browser);
      else
        CefPostTask(TID_UI, base::BindOnce(&util::DestroyCefBrowser, browser));
    }
  } else {
    browser->GetHost()->CloseBrowser(false);
  }
}

namespace {

void _runTaskAndWakeup(std::shared_ptr<CriticalWait> waitCond,
                       base::OnceClosure task) {
  waitCond->lock()->Lock();
  std::move(task).Run();
  waitCond->WakeUp();
  waitCond->lock()->Unlock();
}

void CefPostTaskAndWait(CefThreadId threadId,
                        base::OnceClosure task,
                        long waitMillis) {
  std::shared_ptr<CriticalLock> lock = std::make_shared<CriticalLock>();
  std::shared_ptr<CriticalWait> waitCond = std::make_shared<CriticalWait>(lock.get());
  lock.get()->Lock();
  CefPostTask(threadId, base::BindOnce(_runTaskAndWakeup, waitCond, std::move(task)));
  waitCond.get()->Wait(waitMillis);
  lock.get()->Unlock();
}

}


JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1SetFocus(JNIEnv* env,
                                               jobject obj,
                                               jboolean enable) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->SetFocus(enable != JNI_FALSE);
#if defined(OS_WIN)
  if (!browser->GetHost()->IsWindowRenderingDisabled()) {
    if (enable == JNI_FALSE) {
      if (CefCurrentlyOn(TID_UI)) {
        util::UnfocusCefBrowser(browser);
      } else {
        CefPostTaskAndWait(TID_UI, base::BindOnce(&util::UnfocusCefBrowser, browser), 1000);
      }
    }
  }
#endif
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1SetWindowVisibility(JNIEnv* env,
                                                          jobject obj,
                                                          jboolean visible) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);

#if defined(OS_MAC)
  if (!browser->GetHost()->IsWindowRenderingDisabled()) {
    util_mac::SetVisibility(browser->GetHost()->GetWindowHandle(),
                            visible != JNI_FALSE);
  }
#endif
}

JNIEXPORT jdouble JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetZoomLevel(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj, 0.0);
  CefRefPtr<CefBrowserHost> host = browser->GetHost();
  if (CefCurrentlyOn(TID_UI)) {
    return host->GetZoomLevel();
  }
  std::shared_ptr<double> result = std::make_shared<double>(0.0);
  CefPostTaskAndWait(TID_UI, base::BindOnce(getZoomLevel, host, result), 1000);
  return *result;
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1SetZoomLevel(JNIEnv* env,
                                                   jobject obj,
                                                   jdouble zoom) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->SetZoomLevel(zoom);
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1RunFileDialog(JNIEnv* env,
                                                    jobject obj,
                                                    jobject jmode,
                                                    jstring jtitle,
                                                    jstring jdefaultFilePath,
                                                    jobject jacceptFilters,
                                                    jobject jcallback) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);

  std::vector<CefString> accept_types;
  GetJNIStringVector(env, jacceptFilters, accept_types);

  CefBrowserHost::FileDialogMode mode;
  if (IsJNIEnumValue(env, jmode,
                     "org/cef/handler/CefDialogHandler$FileDialogMode",
                     "FILE_DIALOG_OPEN")) {
    mode = FILE_DIALOG_OPEN;
  } else if (IsJNIEnumValue(env, jmode,
                            "org/cef/handler/CefDialogHandler$FileDialogMode",
                            "FILE_DIALOG_OPEN_MULTIPLE")) {
    mode = FILE_DIALOG_OPEN_MULTIPLE;
  } else if (IsJNIEnumValue(env, jmode,
                            "org/cef/handler/CefDialogHandler$FileDialogMode",
                            "FILE_DIALOG_SAVE")) {
    mode = FILE_DIALOG_SAVE;
  } else {
    mode = FILE_DIALOG_OPEN;
  }

  browser->GetHost()->RunFileDialog(
      mode, GetJNIString(env, jtitle), GetJNIString(env, jdefaultFilePath),
      accept_types, new RunFileDialogCallback(env, jcallback));
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1StartDownload(JNIEnv* env,
                                                    jobject obj,
                                                    jstring url) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->StartDownload(GetJNIString(env, url));
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1Print(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->Print();
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1PrintToPDF(JNIEnv* env,
                                                 jobject obj,
                                                 jstring jpath,
                                                 jobject jsettings,
                                                 jobject jcallback) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);

  CefPdfPrintSettings settings = GetJNIPdfPrintSettings(env, jsettings);

  browser->GetHost()->PrintToPDF(GetJNIString(env, jpath), settings,
                                 new PdfPrintCallback(env, jcallback));
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1Find(JNIEnv* env,
                                           jobject obj,
                                           jstring searchText,
                                           jboolean forward,
                                           jboolean matchCase,
                                           jboolean findNext) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->Find(GetJNIString(env, searchText),
                           (forward != JNI_FALSE), (matchCase != JNI_FALSE),
                           (findNext != JNI_FALSE));
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1StopFinding(JNIEnv* env,
                                                  jobject obj,
                                                  jboolean clearSelection) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->StopFinding(clearSelection != JNI_FALSE);
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1CloseDevTools(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->CloseDevTools();
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1ReplaceMisspelling(JNIEnv* env,
                                                         jobject obj,
                                                         jstring jword) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->ReplaceMisspelling(GetJNIString(env, jword));
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1WasResized(JNIEnv* env,
                                                 jobject obj,
                                                 jint width,
                                                 jint height) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  if (browser->GetHost()->IsWindowRenderingDisabled()) {
    browser->GetHost()->WasResized();
  }
#if (defined(OS_WIN) || defined(OS_LINUX))
  else {
    CefWindowHandle browserHandle = browser->GetHost()->GetWindowHandle();
    if (CefCurrentlyOn(TID_UI)) {
      util::SetWindowSize(browserHandle, width, height);
    } else {
      CefPostTask(TID_UI, base::BindOnce(util::SetWindowSize, browserHandle,
                                         (int)width, (int)height));
    }
  }
#endif
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1Invalidate(JNIEnv* env, jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->Invalidate(PET_VIEW);
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1SendKeyEvent(JNIEnv* env,
                                                   jobject obj,
                                                   jobject key_event) {
  using namespace jcef_keyboard_utils;

  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);

  CefKeyEventAttributes eventAttributes{};

  if (!javaKeyEventToCef(env, key_event, &eventAttributes)) {
    LOG(ERROR) << "CefBrowser#SendKeyEvent: failed to convert the key event";
    return;
  }

  CefKeyEvent cef_key_event{};
  cef_key_event.type = static_cast<cef_key_event_type_t>(eventAttributes.type);
  cef_key_event.modifiers = eventAttributes.modifiers;
  cef_key_event.character = eventAttributes.character;
  cef_key_event.unmodified_character = eventAttributes.unmodified_character;
  cef_key_event.native_key_code = eventAttributes.native_key_code;
  cef_key_event.windows_key_code = eventAttributes.windows_key_code;
  cef_key_event.is_system_key = eventAttributes.is_system_key;
  browser->GetHost()->SendKeyEvent(cef_key_event);
}

namespace {

cef_touch_event_type_t GetTouchEventType(JNIEnv* env,
                                         const ScopedJNIObjectResult& jValue) {
  const char* CLASS_NAME = "org/cef/input/CefTouchEvent$EventType";
  if (IsJNIEnumValue(env, jValue, CLASS_NAME, "RELEASED")) {
    return CEF_TET_RELEASED;
  } else if (IsJNIEnumValue(env, jValue, CLASS_NAME, "PRESSED")) {
    return CEF_TET_PRESSED;
  } else if (IsJNIEnumValue(env, jValue, CLASS_NAME, "MOVED")) {
    return CEF_TET_MOVED;
  }

  return CEF_TET_CANCELLED;
}

cef_pointer_type_t GetPointerType(JNIEnv* env,
                                  const ScopedJNIObjectResult& jValue) {
  const char* CLASS_NAME = "org/cef/input/CefTouchEvent$PointerType";
  if (IsJNIEnumValue(env, jValue, CLASS_NAME, "TOUCH")) {
    return CEF_POINTER_TYPE_TOUCH;
  } else if (IsJNIEnumValue(env, jValue, CLASS_NAME, "MOUSE")) {
    return CEF_POINTER_TYPE_MOUSE;
  } else if (IsJNIEnumValue(env, jValue, CLASS_NAME, "PEN")) {
    return CEF_POINTER_TYPE_PEN;
  } else if (IsJNIEnumValue(env, jValue, CLASS_NAME, "ERASER")) {
    return CEF_POINTER_TYPE_ERASER;
  }

  return CEF_POINTER_TYPE_UNKNOWN;
}

}  // namespace

void Java_org_cef_browser_CefBrowser_1N_N_1SendTouchEvent(JNIEnv* env,
                                                          jobject obj,
                                                          jobject jEvent) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  ScopedJNIClass cls(env, env->GetObjectClass(jEvent));
  if (!cls)
    return;

  ScopedJNIObjectResult jEventType(env);
  int modifiers;
  ScopedJNIObjectResult jPointerType(env);

  cef_touch_event_t event = {};
  if (!CallJNIMethodI_V(env, cls, jEvent, "getId", &event.id) ||
      !CallJNIMethodF_V(env, cls, jEvent, "getX", &event.x) ||
      !CallJNIMethodF_V(env, cls, jEvent, "getY", &event.y) ||
      !CallJNIMethodF_V(env, cls, jEvent, "getRadiusX", &event.radius_x) ||
      !CallJNIMethodF_V(env, cls, jEvent, "getRadiusY", &event.radius_y) ||
      !CallJNIMethodF_V(env, cls, jEvent, "getRotationAngle", &event.rotation_angle) ||
      !CallJNIMethodObject_V(env, cls, jEvent, "getType", "()Lorg/cef/input/CefTouchEvent$EventType;", &jEventType) ||
      !CallJNIMethodF_V(env, cls, jEvent, "getPressure", &event.pressure) ||
      !CallJNIMethodI_V(env, cls, jEvent, "getModifiersEx", &modifiers) ||
      !CallJNIMethodObject_V(env, cls, jEvent, "getPointerType", "()Lorg/cef/input/CefTouchEvent$PointerType;", &jPointerType)
      ) {
    LOG(ERROR) << "SendTouchEvent: Failed to access touch event data";
    return;
  }

  event.type = GetTouchEventType(env, jEventType);
  event.modifiers = GetCefModifiers(env, cls, modifiers);
  event.pointer_type = GetPointerType(env, jPointerType);

  browser->GetHost()->SendTouchEvent(event);
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1SendMouseEvent(JNIEnv* env,
                                                     jobject obj,
                                                     jobject mouse_event) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  ScopedJNIClass cls(env, env->GetObjectClass(mouse_event));
  if (!cls)
    return;

  JNI_STATIC_DEFINE_INT(env, cls, BUTTON1);
  JNI_STATIC_DEFINE_INT(env, cls, BUTTON2);
  JNI_STATIC_DEFINE_INT(env, cls, BUTTON3);
  JNI_STATIC_DEFINE_INT(env, cls, MOUSE_DRAGGED);
  JNI_STATIC_DEFINE_INT(env, cls, MOUSE_ENTERED);
  JNI_STATIC_DEFINE_INT(env, cls, MOUSE_EXITED);
  JNI_STATIC_DEFINE_INT(env, cls, MOUSE_MOVED);
  JNI_STATIC_DEFINE_INT(env, cls, MOUSE_PRESSED);
  JNI_STATIC_DEFINE_INT(env, cls, MOUSE_RELEASED);

  int event_type, x, y, modifiers;
  if (!CallJNIMethodI_V(env, cls, mouse_event, "getID", &event_type) ||
      !CallJNIMethodI_V(env, cls, mouse_event, "getX", &x) ||
      !CallJNIMethodI_V(env, cls, mouse_event, "getY", &y) ||
      !CallJNIMethodI_V(env, cls, mouse_event, "getModifiersEx", &modifiers)) {
    return;
  }

  CefMouseEvent cef_event;
  cef_event.x = x;
  cef_event.y = y;

  cef_event.modifiers = GetCefModifiers(env, cls, modifiers);

  if (event_type == JNI_STATIC(MOUSE_PRESSED) ||
      event_type == JNI_STATIC(MOUSE_RELEASED)) {
    int click_count, button;
    if (!CallJNIMethodI_V(env, cls, mouse_event, "getClickCount",
                          &click_count) ||
        !CallJNIMethodI_V(env, cls, mouse_event, "getButton", &button)) {
      return;
    }

    CefBrowserHost::MouseButtonType cef_mbt;
    if (button == JNI_STATIC(BUTTON1))
      cef_mbt = MBT_LEFT;
    else if (button == JNI_STATIC(BUTTON2))
      cef_mbt = MBT_MIDDLE;
    else if (button == JNI_STATIC(BUTTON3))
      cef_mbt = MBT_RIGHT;
    else
      return;

    browser->GetHost()->SendMouseClickEvent(
        cef_event, cef_mbt, (event_type == JNI_STATIC(MOUSE_RELEASED)),
        click_count);
  } else if (event_type == JNI_STATIC(MOUSE_MOVED) ||
             event_type == JNI_STATIC(MOUSE_DRAGGED) ||
             event_type == JNI_STATIC(MOUSE_ENTERED) ||
             event_type == JNI_STATIC(MOUSE_EXITED)) {
    browser->GetHost()->SendMouseMoveEvent(
        cef_event, (event_type == JNI_STATIC(MOUSE_EXITED)));
  }
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1SendMouseWheelEvent(
    JNIEnv* env,
    jobject obj,
    jobject mouse_wheel_event) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  ScopedJNIClass cls(env, env->GetObjectClass(mouse_wheel_event));
  if (!cls)
    return;

  JNI_STATIC_DEFINE_INT(env, cls, WHEEL_UNIT_SCROLL);

  int scroll_type, delta, x, y, modifiers;
  if (!CallJNIMethodI_V(env, cls, mouse_wheel_event, "getScrollType",
                        &scroll_type) ||
      !CallJNIMethodI_V(env, cls, mouse_wheel_event, "getWheelRotation",
                        &delta) ||
      !CallJNIMethodI_V(env, cls, mouse_wheel_event, "getX", &x) ||
      !CallJNIMethodI_V(env, cls, mouse_wheel_event, "getY", &y) ||
      !CallJNIMethodI_V(env, cls, mouse_wheel_event, "getModifiersEx",
                        &modifiers)) {
    return;
  }

  CefMouseEvent cef_event;
  cef_event.x = x;
  cef_event.y = y;

  cef_event.modifiers = GetCefModifiers(env, cls, modifiers);

  if (scroll_type == JNI_STATIC(WHEEL_UNIT_SCROLL)) {
    // Use the smarter version that considers platform settings.
    CallJNIMethodI_V(env, cls, mouse_wheel_event, "getUnitsToScroll", &delta);
  }

  double deltaX = 0, deltaY = 0;
  if (cef_event.modifiers & EVENTFLAG_SHIFT_DOWN)
    deltaX = delta;
  else
#if defined(OS_WIN)
    deltaY = delta * (-1);
#else
    deltaY = delta;
#endif

  browser->GetHost()->SendMouseWheelEvent(cef_event, deltaX, deltaY);
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1DragTargetDragEnter(JNIEnv* env,
                                                          jobject obj,
                                                          jobject jdragData,
                                                          jobject pos,
                                                          jint jmodifiers,
                                                          jint allowedOps) {
  CefRefPtr<CefDragData> drag_data =
      GetCefFromJNIObject_sync<CefDragData>(env, jdragData, "CefDragData");
  if (!drag_data.get())
    return;
  ScopedJNIClass cls(env, "java/awt/event/MouseEvent");
  if (!cls)
    return;

  CefMouseEvent cef_event;
  GetJNIPoint(env, pos, &cef_event.x, &cef_event.y);
  cef_event.modifiers = GetCefModifiers(env, cls, jmodifiers);

  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->DragTargetDragEnter(
      drag_data, cef_event, (CefBrowserHost::DragOperationsMask)allowedOps);
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1DragTargetDragOver(JNIEnv* env,
                                                         jobject obj,
                                                         jobject pos,
                                                         jint jmodifiers,
                                                         jint allowedOps) {
  ScopedJNIClass cls(env, "java/awt/event/MouseEvent");
  if (!cls)
    return;

  CefMouseEvent cef_event;
  GetJNIPoint(env, pos, &cef_event.x, &cef_event.y);
  cef_event.modifiers = GetCefModifiers(env, cls, jmodifiers);

  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->DragTargetDragOver(
      cef_event, (CefBrowserHost::DragOperationsMask)allowedOps);
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1DragTargetDragLeave(JNIEnv* env,
                                                          jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->DragTargetDragLeave();
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1DragTargetDrop(JNIEnv* env,
                                                     jobject obj,
                                                     jobject pos,
                                                     jint jmodifiers) {
  ScopedJNIClass cls(env, "java/awt/event/MouseEvent");
  if (!cls)
    return;

  CefMouseEvent cef_event;
  GetJNIPoint(env, pos, &cef_event.x, &cef_event.y);
  cef_event.modifiers = GetCefModifiers(env, cls, jmodifiers);

  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->DragTargetDrop(cef_event);
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1DragSourceEndedAt(JNIEnv* env,
                                                        jobject obj,
                                                        jobject pos,
                                                        jint operation) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  int x, y;
  GetJNIPoint(env, pos, &x, &y);
  browser->GetHost()->DragSourceEndedAt(
      x, y, (CefBrowserHost::DragOperationsMask)operation);
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1DragSourceSystemDragEnded(JNIEnv* env,
                                                                jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->DragSourceSystemDragEnded();
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1UpdateUI(JNIEnv* env,
                                               jobject obj,
                                               jobject jcontentRect,
                                               jobject jbrowserRect) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  CefWindowHandle windowHandle = browser->GetHost()->GetWindowHandle();
  if (!windowHandle) // just for insurance
      return;

  CefRect contentRect = GetJNIRect(env, jcontentRect);
#if defined(OS_MAC)
  CefRect browserRect = GetJNIRect(env, jbrowserRect);
  util_mac::UpdateView(windowHandle, contentRect,
                       browserRect);
#else
  // TODO: check that browser extists
  if (CefCurrentlyOn(TID_UI)) {
    util::SetWindowBounds(windowHandle, contentRect);
  } else {
    CefPostTask(TID_UI, base::BindOnce(util::SetWindowBounds, windowHandle,
                                       contentRect));
  }
#endif
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1SetParent(JNIEnv* env,
                                                jobject obj,
                                                jlong windowHandle,
                                                jobject canvas) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  base::OnceClosure callback = base::BindOnce(&OnAfterParentChanged, browser);

#if defined(OS_MAC)
  util::SetParent(browser->GetHost()->GetWindowHandle(), windowHandle,
                  std::move(callback));
#else
  CefWindowHandle browserHandle = browser->GetHost()->GetWindowHandle();
  CefWindowHandle parentHandle =
      canvas ? util::GetWindowHandle(env, canvas) : kNullWindowHandle;
  if (CefCurrentlyOn(TID_UI)) {
    util::SetParent(browserHandle, parentHandle, std::move(callback));
  } else {
#if defined(OS_LINUX)
    CefPostTaskAndWait(TID_UI,
                base::BindOnce(util::SetParent, browserHandle, parentHandle,
                                   std::move(callback)), 1000);
#else
    CefPostTask(TID_UI, base::BindOnce(util::SetParent, browserHandle,
                                       parentHandle, std::move(callback)));
#endif
  }
#endif
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1NotifyMoveOrResizeStarted(JNIEnv* env,
                                                                jobject obj) {
#if (defined(OS_WIN) || defined(OS_LINUX))
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  if (!browser->GetHost()->IsWindowRenderingDisabled()) {
    browser->GetHost()->NotifyMoveOrResizeStarted();
  }
#endif
}

void Java_org_cef_browser_CefBrowser_1N_N_1NotifyScreenInfoChanged(JNIEnv* env,
                                                                   jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->NotifyScreenInfoChanged();
}

namespace {

bool GetJNIRange(JNIEnv* env, jobject obj, CefRange& range) {
  ScopedJNIClass cls(env, "org/cef/misc/CefRange");
  if (!cls) {
    LOG(ERROR) << "Failed to find org.cef.misc.CefRange";
    return false;
  }

  int from, to;
  if (!GetJNIFieldInt(env, cls, obj, "from", &from)) {
    LOG(ERROR) << "Failed to get org.cef.misc.CefRange#from";
    return false;
  }

  if (!GetJNIFieldInt(env, cls, obj, "to", &to)) {
    LOG(ERROR) << "Failed to get org.cef.misc.CefRange#to";
    return false;
  }

  if (from == -1 && to == -1) {
    range = CefRange::InvalidRange();
  } else {
    range.Set(from, to);
  }

  return true;
}

bool GetJNIColor(JNIEnv *env, jobject jColor, cef_color_t& color) {
  ScopedJNIClass cls(env, env->GetObjectClass(jColor));
  if (!cls) {
    LOG(ERROR) << "Failed to find java.awt.Color";
    return false;
  }

  int a, r, g, b;
  if (!CallJNIMethodI_V(env, cls, jColor, "getAlpha", &a)) {
    LOG(ERROR) << "Failed to call java.awt.Color#getAlpa";
    return false;
  }

  if (!CallJNIMethodI_V(env, cls, jColor, "getRed", &r)) {
    LOG(ERROR) << "Failed to call java.awt.Color#getRed";
    return false;
  }

  if (!CallJNIMethodI_V(env, cls, jColor, "getGreen", &g)) {
    LOG(ERROR) << "Failed to call java.awt.Color#getGreen";
    return false;
  }

  if (!CallJNIMethodI_V(env, cls, jColor, "getBlue", &b)) {
    LOG(ERROR) << "Failed to call java.awt.Color#getBlue";
    return false;
  }

  color = CefColorSetARGB(a, r, g, b);
  return true;
}

bool GetJNIUnderlineStyle(JNIEnv *env, jobject jStyle, cef_composition_underline_style_t& style) {
  if (IsJNIEnumValue(env, jStyle, "org/cef/input/CefCompositionUnderline$Style", "SOLID")) {
    style = CEF_CUS_SOLID;
  } else if (IsJNIEnumValue(env, jStyle, "org/cef/input/CefCompositionUnderline$Style", "DOT")) {
    style = CEF_CUS_DOT;
  } else if (IsJNIEnumValue(env, jStyle, "org/cef/input/CefCompositionUnderline$Style", "DASH")) {
    style = CEF_CUS_DASH;
  } else if (IsJNIEnumValue(env, jStyle, "org/cef/input/CefCompositionUnderline$Style", "NONE")) {
    style = CEF_CUS_NONE;
  } else {
    return false;
  }

  return true;
}

bool GetJNIUnderline(JNIEnv *env, jobject jUnderline, CefCompositionUnderline& underline) {
  ScopedJNIClass cls(env, env->GetObjectClass(jUnderline));
  if (!cls) {
    LOG(ERROR) << "Failed to find org.cef.input.CefCompositionUnderline";
    return false;
  }

  ScopedJNIObjectResult jRange(env);
  if (!CallJNIMethodObject_V(env, cls, jUnderline, "getRange", "()Lorg/cef/misc/CefRange;", &jRange)) {
    LOG(ERROR) << "Failed to call CefCompositionUnderline#getRange();";
    return false;
  }

  ScopedJNIObjectResult jColor(env);
  if (!CallJNIMethodObject_V(env, cls, jUnderline, "getColor", "()Ljava/awt/Color;", &jColor)) {
    LOG(ERROR) << "Failed to call CefCompositionUnderline#getColor();";
    return false;
  }

  ScopedJNIObjectResult jBackgroundColor(env);
  if (!CallJNIMethodObject_V(env, cls, jUnderline, "getBackgroundColor", "()Ljava/awt/Color;", &jBackgroundColor)) {
    LOG(ERROR) << "Failed to call CefCompositionUnderline#getBackgroundColor();";
    return false;
  }

  int thick;
  if (!CallJNIMethodI_V(env, cls, jUnderline, "getThick", &thick)) {
    LOG(ERROR) << "Failed to call CefCompositionUnderline#getThick();";
    return false;
  }

  ScopedJNIObjectResult jStyle(env);
  if (!CallJNIMethodObject_V(env, cls, jUnderline, "getStyle", "()Lorg/cef/input/CefCompositionUnderline$Style;", &jStyle)) {
    LOG(ERROR) << "Failed to call CefCompositionUnderline#getStyle();";
    return false;
  }

  CefRange range;
  if (!GetJNIRange(env, jRange, range)) {
    LOG(ERROR) << "Failed to convert org.cef.misc.CefRange";
    return false;
  }
  underline.range = range;

  if (!GetJNIColor(env, jColor, underline.color)) {
    LOG(ERROR) << "Failed to convert CefCompositionUnderline#getColor()";
    return false;
  }

  if (!GetJNIColor(env, jBackgroundColor, underline.background_color)) {
    LOG(ERROR) << "Failed to convert CefCompositionUnderline#getBackgroundColor()";
    return false;
  }

  underline.thick = thick;

  if (!GetJNIUnderlineStyle(env, jStyle, underline.style)) {
    LOG(ERROR) << "Failed to convert CefCompositionUnderline#getStyle()";
    return false;
  }

  return true;
}

bool GetJNIUnderlinesList(JNIEnv* env,
                          jobject jList,
                          std::vector<CefCompositionUnderline>& list) {
  std::vector<ScopedJNIObjectResult> jItems;
  if (!GetJNIListItems(env, jList, &jItems)) {
    LOG(ERROR) << "Failed to retrieve CefCompositionUnderline list";
    return false;
  }

  std::vector<CefCompositionUnderline> result;
  for (const auto& jItem: jItems) {
    result.emplace_back();
    if (!GetJNIUnderline(env, jItem, result.back())) {
      LOG(ERROR) << "Failed to convert CefCompositionUnderline list";
      return false;
    }
  }

  list = std::move(result);
  return true;
}

}  // namespace

void Java_org_cef_browser_CefBrowser_1N_N_1ImeSetComposition(
    JNIEnv* env,
    jobject obj,
    jstring jText,
    jobject jUnderlines,
    jobject jReplacementRange,
    jobject jSelectionRange) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  CefString text = GetJNIString(env, jText);

  std::vector<CefCompositionUnderline> underlines;
  GetJNIUnderlinesList(env, jUnderlines, underlines);

  CefRange replacement_range{};
  GetJNIRange(env, jReplacementRange, replacement_range);

  CefRange selection_range{};
  GetJNIRange(env, jSelectionRange, selection_range);

  browser->GetHost()->ImeSetComposition(text, underlines, replacement_range, selection_range);
}

void Java_org_cef_browser_CefBrowser_1N_N_1ImeCommitText(
    JNIEnv* env,
    jobject obj,
    jstring jText,
    jobject jReplacementRange,
    jint jRelativePos) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  CefString text = GetJNIString(env, jText);
  CefRange replacement_range;
  GetJNIRange(env, jReplacementRange, replacement_range);

  browser->GetHost()->ImeCommitText(text, replacement_range, jRelativePos);
}

void Java_org_cef_browser_CefBrowser_1N_N_1ImeFinishComposingText(
    JNIEnv* env,
    jobject obj,
    jboolean jKeepSelection) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->ImeFinishComposingText(jKeepSelection);
}

void Java_org_cef_browser_CefBrowser_1N_N_1ImeCancelComposing(JNIEnv* env,
                                                              jobject obj) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, obj);
  browser->GetHost()->ImeCancelComposition();
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1SetWindowlessFrameRate(JNIEnv* env,
                                                             jobject jbrowser,
                                                             jint frameRate) {
  CefRefPtr<CefBrowser> browser = JNI_GET_BROWSER_OR_RETURN(env, jbrowser);
  CefRefPtr<CefBrowserHost> host = browser->GetHost();
  host->SetWindowlessFrameRate(frameRate);
}

void getWindowlessFrameRate(CefRefPtr<CefBrowserHost> host,
                            CefRefPtr<IntCallback> callback) {
  callback->onComplete((jint)host->GetWindowlessFrameRate());
}

JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetWindowlessFrameRate(
    JNIEnv* env,
    jobject jbrowser,
    jobject jintCallback) {
  CefRefPtr<IntCallback> callback = new IntCallback(env, jintCallback);

  CefRefPtr<CefBrowser> browser = GetJNIBrowser(env, jbrowser);
  if (!browser.get()) {
    callback->onComplete(0);
    return;
  }

  CefRefPtr<CefBrowserHost> host = browser->GetHost();
  if (CefCurrentlyOn(TID_UI)) {
    getWindowlessFrameRate(host, callback);
  } else {
    CefPostTask(TID_UI, base::BindOnce(getWindowlessFrameRate, host, callback));
  }
}
