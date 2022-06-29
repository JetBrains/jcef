// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

#include <vector>
#include <malloc.h>

#include <tchar.h>

#include <Windows.h>
#include <ShellAPI.h>

#include <jni.h>

#include "resource.h"

#include "include/cef_command_line.h"
#include "include/cef_sandbox_win.h"
#include "include/cef_app.h"

namespace {
  const std::string JVM_PATH_PREFIX = "--jvm=";
  const std::string WORKING_DIR_PREFIX = "-Duser.dir=";
  const std::string CLASS_PATH_PREFIX = "-Djava.class.path=";
  const std::string MAIN_CLASS_PREFIX = "--main=";

  std::map<std::string, std::string> g_options {
      {JVM_PATH_PREFIX, ""},
      {WORKING_DIR_PREFIX, ""},
      {CLASS_PATH_PREFIX, ""},
      {MAIN_CLASS_PREFIX, ""},
  };

  bool FileExists(const std::string& path) {
    return GetFileAttributesA(path.c_str()) != INVALID_FILE_ATTRIBUTES;
  }
  bool IsValidJVM(const std::string& path) {
    return FileExists(path + "\\bin\\server\\jvm.dll") ||
           FileExists(path + "\\bin\\client\\jvm.dll");
  }
  jobjectArray ArgsToJavaArray(JNIEnv* jenv, std::vector<std::string> args) {
    jclass stringClass = jenv->FindClass("java/lang/String");
    jobjectArray result = jenv->NewObjectArray((jsize)args.size(), stringClass, NULL);
    for (int i = 0; i < args.size(); i++) {
      jenv->SetObjectArrayElement(result, i, jenv->NewString((const jchar *)args[i].c_str(), (jsize)args[i].size()));
    }
    return result;
  }
  bool RunMainClass(JNIEnv* jenv, const std::string & mainClassName, std::vector<std::string> args)
  {
    fprintf(stderr, "run main class\n");
    jclass mainClass = jenv->FindClass(mainClassName.c_str());
    jthrowable exc = jenv->ExceptionOccurred();
    if (exc)
    {
      jenv->ExceptionDescribe();
    }
    if (!mainClass) {
      fprintf(stderr, "ERROR: failed to load main class\n");
      return false;
    }

    jmethodID mainMethod = jenv->GetStaticMethodID(mainClass, "main", "([Ljava/lang/String;)V");
    if (!mainMethod) {
      fprintf(stderr, "ERROR: could not find main method\n");
      return false;
    }

    jenv->CallStaticVoidMethod(mainClass, mainMethod, ArgsToJavaArray(jenv, args));
    fprintf(stderr, "finished main class\n");
    exc = jenv->ExceptionOccurred();
    if (exc) {
      fprintf(stderr, "ERROR: exceprion occured:\n");
      jenv->ExceptionDescribe();
    }

    return true;
  }
}

typedef JNIIMPORT void(JNICALL *set_jcef_sandbox_info)(void *);
typedef JNIIMPORT jint(JNICALL *JNI_createJavaVM)(JavaVM **pvm, JNIEnv **env, void *args);

int APIENTRY main_launcher(HINSTANCE hInstance,
                       HINSTANCE hPrevInstance,
                       LPTSTR    lpCmdLine,
                       int       nCmdShow)
{
    UNREFERENCED_PARAMETER(hPrevInstance);

    fprintf(stderr, "start sandbox launcher\n");

    //
    // Create sandbox info
    //
    void* sandbox_info = nullptr;
    CefScopedSandboxInfo scoped_sandbox;
    sandbox_info = scoped_sandbox.sandbox_info();

    // Provide CEF with command-line arguments.
    CefMainArgs main_args(hInstance);

    int exit_code = CefExecuteProcess(main_args, nullptr, sandbox_info);
    if (exit_code >= 0) {
        // The sub-process has completed so return here.
        return exit_code;
    }

    //
    // Parse command line
    //
    std::vector<std::string> args;
    int numArgs;
    LPWSTR* argv = CommandLineToArgvW(GetCommandLineW(), &numArgs);

    for (int i = 0; i < numArgs; i++) {
      const std::wstring argw(argv[i]);
      std::string arg(argw.begin(), argw.end());
      args.push_back(arg);
      for (const auto& kv: g_options) {
        const size_t prefixPos = arg.rfind(kv.first.c_str(), 0);
        if (prefixPos == 0) {
          std::string optval = arg.substr(kv.first.length());
          g_options[kv.first] = optval;
          fprintf(stderr, "\t parsed option: [%s] = %s\n", kv.first.c_str(), optval.c_str());
          break;
        }
      }
    }

    //
    // Check jvm-path
    //
    std::string jvmPath = g_options[JVM_PATH_PREFIX];
    if (jvmPath.empty()) {
      fprintf(stderr, "ERROR: empty jvm path\n");
      return 1;
    }
    if (!IsValidJVM(jvmPath))
    {
      fprintf(stderr, "ERROR: invalid jvm by path: %s\n", jvmPath.c_str());
      return 1;
    }
    if (g_options[CLASS_PATH_PREFIX].empty()) {
      fprintf(stderr, "ERROR: empty class path\n");
      return 1;
    }
    if (g_options[MAIN_CLASS_PREFIX].empty()) {
      fprintf(stderr, "ERROR: empty main class option\n");
      return 1;
    }

    //
    // Load jcef.dll
    //
    std::string jcefDllPath = jvmPath + "\\bin\\jcef.dll";
    HMODULE hJCef = LoadLibraryA(jcefDllPath.c_str());
    fprintf(stderr, "\tjcef h: %p\n", hJCef);
    if (hJCef) {
      set_jcef_sandbox_info pSet = (set_jcef_sandbox_info) GetProcAddress(hJCef, "set_jcef_sandbox_info");
      if (pSet) pSet(sandbox_info);
      else {
        fprintf(stderr, "ERROR: can't find set_jcef_sandbox_info\n");
        return 1;
      }
    } else {
      fprintf(stderr, "ERROR: can't open %s\n", jcefDllPath.c_str());
      return 1;
    }

    //
    // Fill vmOptions
    //
    std::vector<std::string> vmOptionsLines;
    if (!g_options[WORKING_DIR_PREFIX].empty())
      vmOptionsLines.push_back(WORKING_DIR_PREFIX + g_options[WORKING_DIR_PREFIX]);
    vmOptionsLines.push_back(CLASS_PATH_PREFIX + g_options[CLASS_PATH_PREFIX]);
    vmOptionsLines.push_back("--add-exports=java.desktop/sun.awt=ALL-UNNAMED");
    vmOptionsLines.push_back("--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED");

    int vmOptionCount = (int)vmOptionsLines.size();
    JavaVMOption * vmOptions = (JavaVMOption *)calloc(vmOptionCount, sizeof(JavaVMOption));
    for (int i = 0; i < vmOptionsLines.size(); i++) {
      std::string opt = std::string(vmOptionsLines[i].begin(), vmOptionsLines[i].end());
      vmOptions[i].optionString = _strdup(opt.c_str());
      vmOptions[i].extraInfo = NULL;
    }

    //
    // Load jvm.dll
    //
    std::string binDir = std::string(jvmPath) + "\\bin";
    std::string dllName = binDir + "\\server\\jvm.dll";

    SetDllDirectoryA(nullptr);
    // Call SetCurrentDirectory to allow jvm.dll to load the corresponding runtime libraries.
    SetCurrentDirectoryA(binDir.c_str());
    HMODULE hJVM = LoadLibraryA(dllName.c_str());
    if (!hJVM) {
      fprintf(stderr, "ERROR: can't load jvm library: %s | directory: %s\n", dllName.c_str(), binDir.c_str());
      return 1;
    }

    JNI_createJavaVM pCreateJavaVM = (JNI_createJavaVM) GetProcAddress(hJVM, "JNI_CreateJavaVM");
    if (!pCreateJavaVM) {
      fprintf(stderr, "ERROR: can't find JNI_CreateJavaVM in library: %s | directory: %s\n", dllName.c_str(), binDir.c_str());
      return 1;
    }

    //
    // Create VM
    //
    JavaVMInitArgs initArgs;
    initArgs.version = JNI_VERSION_1_2;
    initArgs.options = vmOptions;
    initArgs.nOptions = vmOptionCount;
    initArgs.ignoreUnrecognized = JNI_FALSE;

    JNIEnv* jenv = NULL;
    JavaVM* jvm = NULL;
    int result = pCreateJavaVM(&jvm, &jenv, &initArgs);

    // free VM options
    for (int i = 1; i < vmOptionCount; i++) free(vmOptions[i].optionString);
    free(vmOptions);
    vmOptions = NULL;

    if (result != JNI_OK || jenv == NULL) {
      fprintf(stderr, "ERROR: can't create jvm\n");
      return 1;
    }

    if (!RunMainClass(jenv, g_options[MAIN_CLASS_PREFIX], args)) return 1;

    jvm->DestroyJavaVM();
    return 0;
}

//
// TODO: remove cefsimple launcher on finish
//

#include "simple_app.h"

#pragma comment(lib, "cef_sandbox.lib")

int APIENTRY main_original(HINSTANCE hInstance,
                      HINSTANCE hPrevInstance,
                      LPTSTR lpCmdLine,
                      int nCmdShow) {
    UNREFERENCED_PARAMETER(hPrevInstance);
    UNREFERENCED_PARAMETER(lpCmdLine);

    // Enable High-DPI support on Windows 7 or newer.
    CefEnableHighDPISupport();

    void* sandbox_info = nullptr;

    CefScopedSandboxInfo scoped_sandbox;
    sandbox_info = scoped_sandbox.sandbox_info();

    // Provide CEF with command-line arguments.
    CefMainArgs main_args(hInstance);

    // CEF applications have multiple sub-processes (render, plugin, GPU, etc)
    // that share the same executable. This function checks the command-line and,
    // if this is a sub-process, executes the appropriate logic.
    int exit_code = CefExecuteProcess(main_args, nullptr, sandbox_info);
    if (exit_code >= 0) {
        // The sub-process has completed so return here.
        return exit_code;
    }

    // Parse command-line arguments for use in this method.
    CefRefPtr<CefCommandLine> command_line = CefCommandLine::CreateCommandLine();
    command_line->InitFromString(::GetCommandLineW());

    // Specify CEF global settings here.
    CefSettings settings;

    if (command_line->HasSwitch("enable-chrome-runtime")) {
        // Enable experimental Chrome runtime. See issue #2969 for details.
        settings.chrome_runtime = true;
    }

    // SimpleApp implements application-level callbacks for the browser process.
    // It will create the first browser instance in OnContextInitialized() after
    // CEF has initialized.
    CefRefPtr<SimpleApp> app(new SimpleApp);

    // Initialize CEF.
    CefInitialize(main_args, settings, app.get(), sandbox_info);

    // Run the CEF message loop. This will block until CefQuitMessageLoop() is
    // called.
    CefRunMessageLoop();

    // Shut down CEF.
    CefShutdown();

    return 0;
}

int APIENTRY _tWinMain(HINSTANCE hInstance,
                       HINSTANCE hPrevInstance,
                       LPTSTR    lpCmdLine,
                       int       nCmdShow)
{
    const bool cefsimple = 0;
    if (cefsimple)
        main_original(hInstance, hPrevInstance, lpCmdLine, nCmdShow);
    else
        main_launcher(hInstance, hPrevInstance, lpCmdLine, nCmdShow);
}