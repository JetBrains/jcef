#ifndef JCEF_REMOTEPOSTDATAELEMENT_H
#define JCEF_REMOTEPOSTDATAELEMENT_H

#include "../gen-cpp/shared_types.h"
#include "include/cef_request.h"

class RemotePostDataElement : public CefPostDataElement {
 public:
  RemotePostDataElement(const thrift_codegen::PostDataElement & element);

  bool IsReadOnly() override;
  void SetToEmpty() override;
  void SetToFile(const CefString& fileName) override;
  void SetToBytes(size_t size, const void* bytes) override;
  Type GetType() override;
  CefString GetFile() override;
  size_t GetBytesCount() override;
  size_t GetBytes(size_t size, void* bytes) override;

 private:
  thrift_codegen::PostDataElement myElement;

  IMPLEMENT_REFCOUNTING(RemotePostDataElement);
};

#endif  // JCEF_REMOTEPOSTDATAELEMENT_H
