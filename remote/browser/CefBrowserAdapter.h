#ifndef JCEF_CEFBROWSERADAPTER_H
#define JCEF_CEFBROWSERADAPTER_H

#include "include/cef_base.h"
#include "include/cef_browser.h"

class CefBrowserAdapter {
 private:
  CefRefPtr<CefBrowser> myBrowser;
  int myBid = -1; // only for logging

 public:
  explicit CefBrowserAdapter(CefRefPtr<CefBrowser> browser);
  void setBid(int bid) { myBid = bid; } // only for logging

  void invoke(const std::string& method, const std::string& buffer);

};

#endif  // JCEF_CEFBROWSERADAPTER_H
