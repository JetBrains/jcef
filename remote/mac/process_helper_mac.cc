// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that can
// be found in the LICENSE file.

#include "include/wrapper/cef_library_loader.h"
#include "../handlers/app/HelperApp.h"

#include "../gen-cpp/Server.h"

// When generating projects with CMake the CEF_USE_SANDBOX value will be defined
// automatically. Pass -DUSE_SANDBOX=OFF to the CMake command-line to disable
// use of the sandbox.
#if defined(CEF_USE_SANDBOX)
#include "include/cef_sandbox_mac.h"
#endif

#include <thrift/transport/TSocket.h>
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/transport/TTransportUtils.h>

#include "../CefUtils.h"

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;

using namespace thrift_codegen;

// Entry point function for sub-processes.
int main(int argc, char* argv[]) {
#if defined(CEF_USE_SANDBOX)
  // Initialize the macOS sandbox for this helper process.
  CefScopedSandboxContext sandbox_context;
  if (!sandbox_context.Initialize(argc, argv))
    return 1;
#endif
  std::string framework_path;
  const std::string switchPrefix = "--framework-dir-path=";
  for (int i = 0; i < argc; ++i) {
    std::string arg = argv[i];
    if (arg.find(switchPrefix) == 0) {
      framework_path = arg.substr(switchPrefix.length());
      break;
    }
  }

  // Load the CEF framework library at runtime instead of linking directly
  // as required by the macOS sandbox implementation.
  CefScopedLibraryLoader library_loader;
  if (!framework_path.empty()) {
    framework_path += "/Chromium Embedded Framework";
    if (!cef_load_library(framework_path.c_str()))
      return 1;
  } else {
    return 1;
  }

  // Provide CEF with command-line arguments.
  CefMainArgs main_args(argc, argv);
  CefRefPtr<HelperApp> app = new HelperApp();

  // Execute the sub-process.
  return CefExecuteProcess(main_args, app, nullptr);
}
