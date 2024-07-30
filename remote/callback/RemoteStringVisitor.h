#ifndef JCEF_REMOTESTRINGVISITOR_H
#define JCEF_REMOTESTRINGVISITOR_H

#include "../RemoteObjects.h"
#include "include/cef_string_visitor.h"

class RemoteStringVisitor : public CefStringVisitor, public RemoteJavaObject<RemoteStringVisitor> {
 public:
  explicit RemoteStringVisitor(std::shared_ptr<RpcExecutor> service, thrift_codegen::RObject peer);
  void Visit(const CefString& string) override;

 private:
  IMPLEMENT_REFCOUNTING(RemoteStringVisitor);
};

#endif  // JCEF_REMOTESTRINGVISITOR_H
