#ifndef JCEF_REMOTECOOKIEVISITOR_H
#define JCEF_REMOTECOOKIEVISITOR_H

#include "../RemoteObjects.h"
#include "include/cef_cookie.h"

// Created before cookie visiting, lives in CefCookieVisitor.VisitXXXCookies.
// Deleted in CefRefPrt dtor (when visiting finished)
class RemoteCookieVisitor : public CefCookieVisitor, public RemoteJavaObject<RemoteCookieVisitor> {
 public:
  explicit RemoteCookieVisitor(std::shared_ptr<RpcExecutor> service, thrift_codegen::RObject peer);
  ///
  /// Method that will be called once for each cookie. |count| is the 0-based
  /// index for the current cookie. |total| is the total number of cookies.
  /// Set |deleteCookie| to true to delete the cookie currently being visited.
  /// Return false to stop visiting cookies. This method may never be called if
  /// no cookies are found.
  ///
  /*--cef()--*/
  bool Visit(const CefCookie& cookie,
                     int count,
                     int total,
                     bool& deleteCookie) override;

  static thrift_codegen::Cookie toThriftCookie(const CefCookie& cookie);
  static void toCefCookie(const thrift_codegen::Cookie& cookie, CefCookie& out);
 private:
  IMPLEMENT_REFCOUNTING(RemoteCookieVisitor);
};


#endif  // JCEF_REMOTECOOKIEVISITOR_H
