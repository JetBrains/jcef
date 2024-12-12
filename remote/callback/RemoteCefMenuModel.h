//
// Created by Vladimir.Kharitonov on 10/12/2024.
//

#ifndef REMOTECEFMENUMODEL_H
#define REMOTECEFMENUMODEL_H
#include "../RemoteObjects.h"
#include "include/cef_menu_model.h"

#include <set>

class RemoteCefMenuModel final
    : public CefBaseRefCounted,
      public RemoteServerObjectUpdatable<RemoteCefMenuModel, CefMenuModel> {
public:
  RemoteCefMenuModel(const CefRefPtr<CefMenuModel>& delegate, int id);
  RemoteCefMenuModel* wrap(const CefRefPtr<CefMenuModel>& other);
  ~RemoteCefMenuModel() override;

 private:
  std::set<int> myChildren;

  template <class T, class D>
  friend class ::RemoteServerObject;
  IMPLEMENT_REFCOUNTING(RemoteCefMenuModel);
};

#endif  // REMOTECEFMENUMODEL_H
