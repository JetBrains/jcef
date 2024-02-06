#include "RemoteResponse.h"

extern std::string err2str(cef_errorcode_t errorcode);
extern cef_errorcode_t str2err(std::string err);

void RemoteResponse::updateImpl(const std::map<std::string, std::string>& requestInfo) {
  SET_INT(requestInfo, Status);
  SET_STR(requestInfo, StatusText);
  SET_STR(requestInfo, MimeType);

  if (requestInfo.count("Error") > 0) {
    myDelegate->SetError(str2err(requestInfo.at("Error")));
  }
}

std::map<std::string, std::string> RemoteResponse::toMapImpl() {
  std::map<std::string, std::string> result;
  result["IsReadOnly"] = std::to_string(myDelegate->IsReadOnly());

  GET_INT(result, Status);
  GET_STR(result, StatusText);
  GET_STR(result, MimeType);

  result["Error"] = err2str(myDelegate->GetError());
  return result;
}
