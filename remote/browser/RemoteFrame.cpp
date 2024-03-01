#include "RemoteFrame.h"

#define GET_BOOL(map, key)                          \
  if ((*myDelegate).##key())                        \
    map[#key] = "true";                             \
  else                                              \
    map[#key] = "false";

static void setBool(std::map<std::string, std::string> & out, const std::string& key, bool val) {
  if (val)
    out[key] = "true";
  else
    out[key] = "false";
}

std::map<std::string, std::string> RemoteFrame::toMapImpl() {
  std::map<std::string, std::string> result;
  GET_STR(result, Identifier);
  GET_STR(result, URL);
  GET_STR(result, Name);
  setBool(result, "IsMain", myDelegate->IsMain());
  setBool(result, "IsValid", myDelegate->IsValid());
  setBool(result, "IsFocused", myDelegate->IsFocused());
  return result;
}
