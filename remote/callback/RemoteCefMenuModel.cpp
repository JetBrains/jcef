#include "RemoteCefMenuModel.h"

RemoteCefMenuModel::RemoteCefMenuModel(
  const CefRefPtr<CefMenuModel>& delegate,
  int id)
    : RemoteServerObjectUpdatable(id, delegate) {}

RemoteCefMenuModel* RemoteCefMenuModel::wrap(
    const CefRefPtr<CefMenuModel>& other) {
  RemoteCefMenuModel* remote_cef_menu_model = wrapDelegate(other.get());
  if (remote_cef_menu_model) {
    myChildren.insert(remote_cef_menu_model->getId());
  }
  return remote_cef_menu_model;
}

RemoteCefMenuModel::~RemoteCefMenuModel() {
  for (const auto p: myChildren) {
    dispose(p);
  }
}
