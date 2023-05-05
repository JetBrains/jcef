
/**
 *  bool        Boolean, one byte
 *  i8 (byte)   Signed 8-bit integer
 *  i16         Signed 16-bit integer
 *  i32         Signed 32-bit integer
 *  i64         Signed 64-bit integer
 *  double      64-bit floating point value
 *  string      String
 *  binary      Blob (byte array)
 *  map<t1,t2>  Map from one type to another
 *  list<t1>    Ordered list of one type
 *  set<t1>     Set of unique elements of one type
 */

namespace cpp thrift_codegen
namespace java thrift_codegen

service Server {
   i32 connect(1:i32 backwardConnectionPort, 2:list<string> cmdLineArgs, 3:map<string,string> settings)
   oneway void log(1:string msg)

   i32 createBrowser(1:i32 cid)
   string closeBrowser(1:i32 bid)

   //
   // CefBrowser methods
   //
   oneway void invoke(1:i32 bid, 2:string method, 3:binary buffer)
}
