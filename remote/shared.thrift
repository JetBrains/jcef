namespace cpp thrift_codegen
namespace java thrift_codegen

struct RObject {
    1: required i32 objId;
    2: optional bool isPersistent;
    3: optional bool isDisableDefaultHandling;
    4: optional map<string,string> objInfo;
}

struct PostDataElement {
    1: required bool isReadOnly;
    2: optional string file;
    3: optional binary bytes;
}

struct PostData {
    1: required bool isReadOnly;
    2: required bool hasExcludedElements;
    3: optional list<PostDataElement> elements;
}