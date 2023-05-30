#include "RemotePostData.h"
#include "RemotePostDataElement.h"

RemotePostData::RemotePostData(const thrift_codegen::PostData & postData) : myData(postData) {
  for (auto e: myData.elements) {
    myElements.push_back(CefRefPtr<RemotePostDataElement>(new RemotePostDataElement(e)));
  }
}

bool RemotePostData::IsReadOnly() {
  return myData.isReadOnly;
}

bool RemotePostData::HasExcludedElements() {
  return myData.hasExcludedElements;
}

size_t RemotePostData::GetElementCount() {
  return myData.elements.size();
}

void RemotePostData::GetElements(CefPostData::ElementVector& elements) {
  elements.assign(myElements.begin(), myElements.end());
}

bool RemotePostData::RemoveElement(CefRefPtr<CefPostDataElement> element) {
  std::remove(myElements.begin(), myElements.end(), element);
  return false;
}

bool RemotePostData::AddElement(CefRefPtr<CefPostDataElement> element) {
  myElements.push_back(element);
  return false;
}

void RemotePostData::RemoveElements() {
  myElements.clear();
}