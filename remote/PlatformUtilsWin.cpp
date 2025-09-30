#include "PlatformUtils.h"

#include <jni.h>

namespace {

void throwException(JNIEnv* env, const char* message) {
  jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
  env->ThrowNew(exceptionClass, message);
}

}  // namespace

extern "C" {

jlong Java_com_jetbrains_cef_remote_PlatformUtils_getMtlTexture(JNIEnv* env,
                                                                jclass,
                                                                jlong) {
  throwException(env, "Not supported on Windows");
  return 0;
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_PlatformUtils_releaseMtlTextureRef(JNIEnv* env,
                                                                 jclass,
                                                                 jlong) {
  throwException(env, "Not supported on Windows");
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_PlatformUtils_retainIOSurfaceRef(JNIEnv* env,
                                                               jclass,
                                                               jlong) {
  throwException(env, "Not supported on Windows");
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_PlatformUtils_releaseIOSurfaceRef(JNIEnv* env,
                                                                jclass,
                                                                jlong) {
  throwException(env, "Not supported on Windows");
}

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_PlatformUtils_getIOSurfaceRef(JNIEnv* env,
                                                            jclass,
                                                            jlong) {
  throwException(env, "Not supported on Windows");
  return 0;
}
}  // extern "C"
