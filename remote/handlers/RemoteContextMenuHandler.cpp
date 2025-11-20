#include "RemoteContextMenuHandler.h"

#include "../browser/RemoteFrame.h"
#include "../callback/RemoteCefRunContextMenuCallback.h"

#include "../browser/RemoteBrowser.h"
#include <vector>

namespace {

thrift_codegen::ContextMenuParams convertParams(
    const CefRefPtr<CefContextMenuParams>& params) {
  thrift_codegen::ContextMenuParams thriftParams;

  if (!params) {
    return thriftParams;
  }

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

thrift_codegen::MenuItemType::type convert_menu_type(
    CefMenuModel::MenuItemType type) {
  switch (type) {
    case CefMenuModel::MenuItemType::MENUITEMTYPE_NONE:
      return thrift_codegen::MenuItemType::MENUITEMTYPE_NONE;

    case CefMenuModel::MenuItemType::MENUITEMTYPE_COMMAND:
      return thrift_codegen::MenuItemType::MENUITEMTYPE_COMMAND;

    case CefMenuModel::MenuItemType::MENUITEMTYPE_CHECK:
      return thrift_codegen::MenuItemType::MENUITEMTYPE_CHECK;

    case CefMenuModel::MenuItemType::MENUITEMTYPE_RADIO:
      return thrift_codegen::MenuItemType::MENUITEMTYPE_RADIO;

    case CefMenuModel::MenuItemType::MENUITEMTYPE_SEPARATOR:
      return thrift_codegen::MenuItemType::MENUITEMTYPE_SEPARATOR;

    case CefMenuModel::MenuItemType::MENUITEMTYPE_SUBMENU:
      return thrift_codegen::MenuItemType::MENUITEMTYPE_SUBMENU;

    default:
      LOG(ERROR) << "RemoteContextMenuHandler: Unknown cef menu item type: "
                 << type;
      return thrift_codegen::MenuItemType::MENUITEMTYPE_NONE;
  }
}

/* TODO: Non-recursive DFS
std::vector<thrift_codegen::MenuItem> to_thrift(CefRefPtr<CefMenuModel> model) {
  std::vector<thrift_codegen::MenuItem> result;
  std::stack<std::pair<CefRefPtr<CefMenuModel>,
std::vector<thrift_codegen::MenuItem>*>> stack;

  // Initialize stack with the root menu model and output list
  stack.push({model, &result});

  while (!stack.empty()) {
    auto [current_model, current_result] = stack.top();
    stack.pop();

    for (int i = 0; i < current_model->GetCount(); ++i) {
      thrift_codegen::MenuItem item;
      item.label = current_model->GetLabelAt(i).ToString();
      item.type = convert_menu_type(current_model->GetTypeAt(i));
      item.command_id = current_model->GetCommandIdAt(i);
      item.group_id = current_model->GetGroupIdAt(i);
      item.visible = current_model->IsVisibleAt(i);
      item.enabled = current_model->IsEnabledAt(i);
      item.checked = current_model->IsCheckedAt(i);

      // If the current item is a submenu, prepare to process it later
      if (item.type == thrift_codegen::MenuItemType::MENUITEMTYPE_SUBMENU) {
        // Submenu items are placed in `item.sub_menu`, which is processed
separately item.sub_menu.emplace(); // Initialize sub_menu
        stack.push({current_model->GetSubMenuAt(i), &item.sub_menu.value()});
      }

      current_result->push_back(std::move(item));
    }
  }

  return result;
}
 */

std::vector<thrift_codegen::MenuItem> to_thrift(CefRefPtr<CefMenuModel> model) {
  std::vector<thrift_codegen::MenuItem> result;
  for (int i = 0; i < model->GetCount(); ++i) {
    result.emplace_back();
    result.back().label = model->GetLabelAt(i).ToString();
    result.back().type = convert_menu_type(model->GetTypeAt(i));
    result.back().command_id = model->GetCommandIdAt(i);
    result.back().group_id = model->GetGroupIdAt(i);
    result.back().visible = model->IsVisibleAt(i);
    result.back().enabled = model->IsEnabledAt(i);
    result.back().checked = model->IsCheckedAt(i);
    result.back().command_id = model->GetCommandIdAt(i);
    if (result.back().type ==
        thrift_codegen::MenuItemType::MENUITEMTYPE_SUBMENU) {
      result.back().sub_menu = to_thrift(model->GetSubMenuAt(i));
    }
  }

  return result;
}

void to_cef(CefRefPtr<CefMenuModel> out,
            const std::vector<thrift_codegen::MenuItem>& in) {
  out->Clear();
  for (size_t i = 0; i < in.size(); ++i) {
    switch (in[i].type) {
      case thrift_codegen::MenuItemType::MENUITEMTYPE_COMMAND:
        out->AddItem(in[i].command_id, in[i].label);
        break;
      case thrift_codegen::MenuItemType::MENUITEMTYPE_CHECK:
        out->AddCheckItem(in[i].command_id, in[i].label);
        out->SetCheckedAt(i, in[i].checked);
        break;
      case thrift_codegen::MenuItemType::MENUITEMTYPE_RADIO:
        out->AddRadioItem(in[i].command_id, in[i].label, in[i].group_id);
        out->SetCheckedAt(i, in[i].checked);
        break;
      case thrift_codegen::MenuItemType::MENUITEMTYPE_SEPARATOR:
        out->AddSeparator();
        break;
      case thrift_codegen::MenuItemType::MENUITEMTYPE_SUBMENU: {
        auto sub_menu = out->AddSubMenu(in[i].command_id, in[i].label);
        to_cef(sub_menu, in[i].sub_menu);
        break;
      }
      case thrift_codegen::MenuItemType::MENUITEMTYPE_NONE:
        continue;
    }

    out->SetEnabledAt(i, in[i].enabled);
    out->SetVisibleAt(i, in[i].visible);
  }
}

}  // namespace

RemoteContextMenuHandler::RemoteContextMenuHandler(const std::shared_ptr<RpcExecutor>& my_service) : myService(my_service) {}

RemoteContextMenuHandler::~RemoteContextMenuHandler() = default;

void RemoteContextMenuHandler::OnBeforeContextMenu(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefContextMenuParams> params,
    CefRefPtr<CefMenuModel> model) {
  FIND_BID_OR_RETURN();
  RemoteFrame::Holder frm(frame);
  const auto thriftParams = convertParams(params);
  const std::vector<thrift_codegen::MenuItem> menu_model = to_thrift(model);

  std::vector<thrift_codegen::MenuItem> result;
  myService->exec([&](const JavaService& s) {
    s->ContextMenuHandler_OnBeforeContextMenu(
        result, bid, frm.serverId(), thriftParams, menu_model);
  });

  to_cef(model, result);
}

bool RemoteContextMenuHandler::RunContextMenu(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefContextMenuParams> params,
    CefRefPtr<CefMenuModel> model,
    CefRefPtr<CefRunContextMenuCallback> callback) {
  FIND_BID_OR_RETURN_VAL(false);
  RemoteFrame::Holder frm(frame);
  const auto thriftParams = convertParams(params);
  const std::vector<thrift_codegen::MenuItem> menu_model = to_thrift(model);
  std::shared_ptr<RemoteCefRunContextMenuCallback> callback_wrapper =
      RemoteCefRunContextMenuCallback::wrapDelegate(callback);
  bool result = myService->exec<bool>(
      [&](const JavaService& s) -> bool {
        return s->ContextMenuHandler_RunContextMenu(
            bid, frm.serverId(), thriftParams, menu_model,
            callback_wrapper->serverId());
      },
      false);

  if (!result) {
    RemoteCefRunContextMenuCallback::dispose(callback_wrapper->getId());
  }
  return result;
}

bool RemoteContextMenuHandler::OnContextMenuCommand(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame,
    CefRefPtr<CefContextMenuParams> params,
    int command_id,
    EventFlags event_flags) {
  FIND_BID_OR_RETURN_VAL(false);
  RemoteFrame::Holder frm(frame);
  const auto thriftParams = convertParams(params);
  return myService->exec<bool>([&](const JavaService& s) {
    return s->ContextMenuHandler_OnContextMenuCommand(
        bid, frm.serverId(), thriftParams, command_id,
        event_flags);
  }, false);
}

void RemoteContextMenuHandler::OnContextMenuDismissed(
    CefRefPtr<CefBrowser> browser,
    CefRefPtr<CefFrame> frame) {
  FIND_BID_OR_RETURN();
  RemoteFrame::Holder frm(frame);
  myService->exec([&](const JavaService& s) {
    s->ContextMenuHandler_OnContextMenuDismissed(bid, frm.serverId());
  });
}