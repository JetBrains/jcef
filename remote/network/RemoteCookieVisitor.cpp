#include "RemoteCookieVisitor.h"

RemoteCookieVisitor::RemoteCookieVisitor(std::shared_ptr<RpcExecutor> service, thrift_codegen::RObject peer) : RemoteJavaObject<RemoteCookieVisitor>(
          service,
          peer.objId,
          [=](std::shared_ptr<thrift_codegen::ClientHandlersClient> service) {
            service->CookieVisitor_Dispose(myPeerId);
          }) {}

static int64_t toTimestamp(const CefBaseTime & time) {
  CefTime cef_time;
  cef_time_from_basetime(time, &cef_time);
  double timestamp = cef_time.GetDoubleT()*1000;
  return (int64_t)timestamp;
}

static void fromTimestamp(int64_t timestamp, CefBaseTime* result) {
  CefTime cef_time;
  cef_time.SetDoubleT((double)(timestamp/1000));
  cef_time_to_basetime(&cef_time, result);
}

thrift_codegen::Cookie RemoteCookieVisitor::toThriftCookie(const CefCookie& c) {
  ::thrift_codegen::Cookie cookie;
  CefString tmp(&c.name);
  cookie.__set_name(CefString(&c.name).ToString());
  cookie.__set_value(CefString(&c.value).ToString());
  cookie.__set_domain(CefString(&c.domain).ToString());
  cookie.__set_path(CefString(&c.path).ToString());
  cookie.__set_secure(c.secure);
  cookie.__set_httponly(c.httponly);
  cookie.__set_creation(toTimestamp(c.creation));
  cookie.__set_lastAccess(toTimestamp(c.last_access));
  if (c.has_expires)
    cookie.__set_expires(toTimestamp(c.expires));
  return cookie;
}

void RemoteCookieVisitor::toCefCookie(const thrift_codegen::Cookie& cookie, CefCookie& out) {
  CefString(&out.name) = cookie.name;
  CefString(&out.value) = cookie.value;
  CefString(&out.domain) = cookie.domain;
  CefString(&out.path) = cookie.path;
  out.secure = cookie.secure;
  out.httponly = cookie.httponly;
  CefBaseTime creation, lastAccess, expires;
  fromTimestamp(cookie.creation, &creation);
  fromTimestamp(cookie.lastAccess, &lastAccess);
  out.creation = creation;
  out.last_access = lastAccess;
  if (cookie.__isset.expires) {
    fromTimestamp(cookie.expires, &expires);
    out.expires = expires;
  }
}

bool RemoteCookieVisitor::Visit(const CefCookie& c, int count, int total, bool& deleteCookie) {
  LNDCT();
  return myService->exec<bool>([&](RpcExecutor::Service s){
    return s->CookieVisitor_Visit(myPeerId, toThriftCookie(c), count, total);
  }, false);
}