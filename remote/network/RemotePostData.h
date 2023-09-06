#ifndef JCEF_REMOTEPOSTDATA_H
#define JCEF_REMOTEPOSTDATA_H

#include "../gen-cpp/shared_types.h"
#include "include/cef_request.h"

class RemotePostData : public CefPostData {
 public:
  RemotePostData(const thrift_codegen::PostData & postData);

  bool IsReadOnly() override;
  bool HasExcludedElements() override;
  size_t GetElementCount() override;
  void GetElements(ElementVector& elements) override;
  bool RemoveElement(CefRefPtr<CefPostDataElement> element) override;
  bool AddElement(CefRefPtr<CefPostDataElement> element) override;
  void RemoveElements() override;

 private:
  thrift_codegen::PostData myData;
  CefPostData::ElementVector myElements;

  IMPLEMENT_REFCOUNTING(RemotePostData);
};

#endif  // JCEF_REMOTEPOSTDATA_H
