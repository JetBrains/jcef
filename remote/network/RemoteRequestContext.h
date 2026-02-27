#ifndef JCEF_REMOTEREQUESTCONTEXT_H
#define JCEF_REMOTEREQUESTCONTEXT_H

#include "include/cef_request_context.h"
#include "../RemoteObjects.h"

class RemoteRequestContext : public RemoteServerObject<RemoteRequestContext, CefRequestContext> {
public:
  explicit RemoteRequestContext(int id, CefRefPtr<CefRequestContext> delegate) : RemoteServerObject(id, delegate) {}
};

#endif  // JCEF_REMOTEREQUESTCONTEXT_H
