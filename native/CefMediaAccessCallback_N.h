#include <jni.h>
/* Header for class org_cef_callback_CefMediaAccessCallback_N */

#ifndef _Included_org_cef_callback_CefMediaAccessCallback_N
#define _Included_org_cef_callback_CefMediaAccessCallback_N
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_org_cef_callback_CefMediaAccessCallback_1N_N_1Cancel(JNIEnv* env,
                                                        jobject obj,
                                                        jlong self);

JNIEXPORT void JNICALL
Java_org_cef_callback_CefMediaAccessCallback_1N_N_1Continue(JNIEnv* env,
                                                       jobject obj,
                                                       jlong self,
                                                       jint allowed_permissions);
#ifdef __cplusplus
}
#endif
#endif
