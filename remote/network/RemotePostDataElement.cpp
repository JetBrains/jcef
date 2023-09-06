#include "RemotePostDataElement.h"

RemotePostDataElement::RemotePostDataElement(const thrift_codegen::PostDataElement & element)
  : myElement(element) {}

bool RemotePostDataElement::IsReadOnly() {
  return myElement.isReadOnly;
}

void RemotePostDataElement::SetToEmpty() {
  myElement.bytes = "";
  myElement.file = "";
}

void RemotePostDataElement::SetToFile(const CefString& fileName) {
  myElement.file = fileName.ToString();
  myElement.bytes = "";
}

void RemotePostDataElement::SetToBytes(size_t size, const void* bytes) {
  myElement.file = "";
  myElement.bytes.assign((const char*)bytes, size);
}

CefPostDataElement::Type RemotePostDataElement::GetType() {
  if (myElement.file.empty() && myElement.bytes.empty())
    return PDE_TYPE_EMPTY;
  if (myElement.bytes.empty())
    return PDE_TYPE_FILE;
  return PDE_TYPE_BYTES;
}

CefString RemotePostDataElement::GetFile() {
  return CefString(myElement.file);
}

size_t RemotePostDataElement::GetBytesCount() {
  return myElement.bytes.size();
}

size_t RemotePostDataElement::GetBytes(size_t size, void* bytes) {
  if (size > myElement.bytes.size())
    size = myElement.bytes.size();
  memcpy(bytes, myElement.bytes.data(), size);
  return size;
}
