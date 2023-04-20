#include "RemoteAppHandler.h"
#include "Log/Log.h"
#include "gen-cpp/cef_client_types.h"

using namespace thrift_codegen;

namespace {
  const std::string prefixArgument = "Argument_";
  const std::string prefixSwitch = "Switch_";
  const std::string prefixSwitchWithValue = "SwitchWithValue_";
}

RemoteAppHandler::RemoteAppHandler(std::shared_ptr<BackwardConnection> backwardConnection)
    : ConnectionUser(backwardConnection) {}

void RemoteAppHandler::OnBeforeCommandLineProcessing(
    const CefString& process_type,
    CefRefPtr<CefCommandLine> command_line) {
  LogNdc ndc("RemoteAppHandler::OnBeforeCommandLineProcessing");
  auto remoteService = getService();
  if (remoteService == nullptr) return;

  std::vector<std::string> result;
  std::vector<std::string> cmdline;
  {
    std::vector<CefString> cmdlineCef;
    command_line->GetArgv(cmdlineCef);
    for (auto cs : cmdlineCef)
      cmdline.push_back(cs.ToString());
  }

  try {
    remoteService->onBeforeCommandLineProcessing(result, process_type.ToString(), cmdline);
  } catch (apache::thrift::TException& tx) {
    onThriftException(tx);
    return;
  }

  Log::debug("Original command line:\n%s", command_line->GetCommandLineString().ToString().c_str());

  std::string additionalItems = "";
  for (auto s: result) {
    if (s.find(prefixArgument) == 0) {
      std::string item = s.substr(prefixArgument.size());
      command_line->AppendArgument(item);
      additionalItems += item + ", ";
    } else if (s.find(prefixSwitch) == 0) {
      std::string item = s.substr(prefixSwitch.size());
      command_line->AppendSwitch(item);
      additionalItems += item + ", ";
    } else if (s.find(prefixSwitchWithValue) == 0) {
      int separator = s.find("___");
      if (separator < 0) {
        Log::error("Unknown item in command line: %s", s.c_str());
      } else {
        std::string name = s.substr(prefixSwitchWithValue.size(), separator);
        std::string val = s.substr(separator + 3/*size of separator*/);
        command_line->AppendSwitchWithValue(name, val);
        additionalItems += name + "|" + val + ", ";
      }
    } else
      Log::error("Unknown prefix in command line item: %s", s.c_str());
  }

  Log::debug("Additional command line items:\n%s", additionalItems.c_str());
}

void RemoteAppHandler::OnRegisterCustomSchemes(
    CefRawPtr<CefSchemeRegistrar> registrar) {
  LogNdc ndc("RemoteAppHandler::OnRegisterCustomSchemes");
  auto remoteService = getService();
  if (remoteService == nullptr) return;

  std::vector<CustomScheme> result;
  try {
    remoteService->onRegisterCustomSchemes(result);
  } catch (apache::thrift::TException& tx) {
    onThriftException(tx);
    return;
  }

  Log::debug("Additional schemes:");
  for (auto cs: result) {
    registrar->AddCustomScheme(cs.schemeName, cs.options);
    Log::debug("%s [%d]", cs.schemeName.c_str(), cs.options);
  }
}

CefRefPtr<CefBrowserProcessHandler> RemoteAppHandler::GetBrowserProcessHandler() {
  Log::error("RemoteAppHandler::GetBrowserProcessHandler: unimplemented");
  return nullptr;
}
