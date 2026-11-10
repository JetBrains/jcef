#include "RemoteDialogHandler.h"

#include "../RpcExecutor.h"
#include "../Utils.h"
#include "../browser/RemoteBrowser.h"
#include "../callback/RemoteFileDialogCallback.h"

namespace {

std::string fileDialogModeToString(CefDialogHandler::FileDialogMode mode) {
  switch (mode) {
    case FILE_DIALOG_OPEN: return "FILE_DIALOG_OPEN";
    case FILE_DIALOG_OPEN_MULTIPLE: return "FILE_DIALOG_OPEN_MULTIPLE";
    case FILE_DIALOG_OPEN_FOLDER: return "FILE_DIALOG_OPEN_FOLDER";
    case FILE_DIALOG_SAVE: return "FILE_DIALOG_SAVE";
    default: return string_format("FILE_DIALOG_MODE_%d", static_cast<int>(mode));
  }
}

std::vector<std::string> toStrings(const std::vector<CefString>& values) {
  std::vector<std::string> result;
  result.reserve(values.size());
  for (const auto& value : values)
    result.push_back(value.ToString());
  return result;
}

}  // namespace

RemoteDialogHandler::RemoteDialogHandler(std::shared_ptr<RpcExecutor> service) : myService(service) {}

bool RemoteDialogHandler::OnFileDialog(
    CefRefPtr<CefBrowser> browser,
    FileDialogMode mode,
    const CefString& title,
    const CefString& default_file_path,
    const std::vector<CefString>& accept_filters,
    const std::vector<CefString>& accept_extensions,
    const std::vector<CefString>& accept_descriptions,
    CefRefPtr<CefFileDialogCallback> callback) {
  FIND_BID_OR_RETURN_VAL(false);
  const std::vector<std::string> filters = toStrings(accept_filters);
  const std::vector<std::string> extensions = toStrings(accept_extensions);
  const std::vector<std::string> descriptions = toStrings(accept_descriptions);
  const auto fileDialogCallback = RemoteFileDialogCallback::wrapDelegate(callback);
  const bool handled = myService->exec<bool>([&](const JavaService& s) {
    return s->DialogHandler_OnFileDialog(
        bid, fileDialogModeToString(mode), title.ToString(), default_file_path.ToString(),
        filters, extensions, descriptions, fileDialogCallback->toRObject());
  }, false);
  if (!handled)
    RemoteFileDialogCallback::dispose(fileDialogCallback->getId());
  return handled;
}