#include "RemoteContextMenuHandler.h"

#include "../browser/RemoteFrame.h"
#include "../callback/RemoteCallback.h"
#include "../callback/RemoteCefMenuModel.h"
#include "../callback/RemoteCefRunContextMenuCallback.h"

namespace {

thrift_codegen::ContextMenuParams convertParams(
    const CefRefPtr<CefContextMenuParams>& params) {
  thrift_codegen::ContextMenuParams thriftParams;

  if (!params) {
    // Handle the case where params is null
    return thriftParams;
  }

  // Map the values from CefContextMenuParams to
  // thrift_codegen::ContextMenuParams
  thriftParams.x = params->GetXCoord();
  thriftParams.y = params->GetYCoord();
  thriftParams.link_url = params->GetLinkUrl().ToString();
  thriftParams.unfiltered_link_url = params->GetUnfilteredLinkUrl().ToString();
  thriftParams.source_url = params->GetSourceUrl().ToString();
  thriftParams.page_url = params->GetPageUrl().ToString();
  thriftParams.frame_url = params->GetFrameUrl().ToString();
  thriftParams.frame_charset = params->GetFrameCharset();
  thriftParams.media_type = static_cast<int>(params->GetMediaType());
  thriftParams.media_state_flags = params->GetMediaStateFlags();
  thriftParams.selected_text = params->GetSelectionText().ToString();
  thriftParams.misspelled_word = params->GetMisspelledWord().ToString();
  // not supported: params->GetDictionarySuggestions()
  thriftParams.is_editable = params->IsEditable();
  thriftParams.edit_state_flags = params->GetEditStateFlags();
  thriftParams.is_custom_menu = params->IsCustomMenu();
  thriftParams.type_flags = params->GetTypeFlags();
  thriftParams.has_image_contents = params->HasImageContents();

  return thriftParams;
}

}  // namespace

RemoteContextMenuHandler::RemoteContextMenuHandler(
    const int my_bid,
    const std::shared_ptr<RpcExecutor>& my_service)
    : myBid(my_bid), myService(my_service) {}

RemoteContextMenuHandler::~RemoteContextMenuHandler() {
  for (const auto c : myCallbacks) {
    RemoteCefRunContextMenuCallback::dispose(c);
  }
}

void RemoteContextMenuHandler::OnBeforeContextMenu(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefContextMenuParams> params,
    CefRefPtr<CefMenuModel> model) {
  LNDC();


  auto b = model->AddItem(MENU_ID_USER_FIRST + 5, "test");
  b = model->AddSeparator();
  b = model->AddItem(MENU_ID_USER_FIRST + 6, "test");
  // RemoteFrame::Holder frm(frame);
  // const auto thriftParams = convertParams(params);
  // RemoteCefMenuModel::Holder modelHolder(model);
  //
  // myService->exec([&](const RpcExecutor::Service& s) {
  //   s->ContextMenuHandler_OnBeforeContextMenu(
  //       myBid, frm.get()->serverIdWithMap(), thriftParams, modelHolder.get()->serverIdWithMap());
  // });
}

bool RemoteContextMenuHandler::RunContextMenu(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefContextMenuParams> params,
    CefRefPtr<CefMenuModel> model,
    CefRefPtr<CefRunContextMenuCallback> callback) {
  // TODO
  return CefContextMenuHandler::RunContextMenu(browser, frame, params, model,
                                               callback);
}

bool RemoteContextMenuHandler::OnContextMenuCommand(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefContextMenuParams> params,
    int command_id,
    EventFlags event_flags) {
  // TODO
  return CefContextMenuHandler::OnContextMenuCommand(browser, frame, params,
                                                     command_id, event_flags);
}

void RemoteContextMenuHandler::OnContextMenuDismissed(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame) {
  // TODO
  CefContextMenuHandler::OnContextMenuDismissed(browser, frame);
}