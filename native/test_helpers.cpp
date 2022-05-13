#include "jni_util.h"

#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jstring JNICALL
Java_tests_junittests_StringTest_convertString(JNIEnv* env,
                                               jclass obj,
                                               jstring jstr) {
  CefString cefString = GetJNIString(env, jstr);
  return NewJNIString(env, cefString);
}
#ifdef __cplusplus
}
#endif

