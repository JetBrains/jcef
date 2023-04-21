#include <jni.h>

#include <boost/interprocess/managed_shared_memory.hpp>

using namespace boost::interprocess;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_SharedMemory_openSharedSegment(JNIEnv* env,
                                               jclass clazz,
                                               jstring sid) {
  if (!sid)
    return 0;

  managed_shared_memory * segment = nullptr;
  const char* strSid = env->GetStringUTFChars(sid, nullptr);
  if (strSid)
    segment = new managed_shared_memory(open_only, strSid);
  if (sid)
    env->ReleaseStringUTFChars(sid, strSid);

  //fprintf(stderr, "opened segment '%s': %p\n", strSid, segment);
  return (jlong)segment;
}

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_SharedMemory_getPointer(JNIEnv* env,
                                                            jclass clazz,
                                                            jlong segment,
                                                            jlong handle) {
  managed_shared_memory * segm = (managed_shared_memory*)segment;
  return (jlong)segm->get_address_from_handle(handle);
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_SharedMemory_closeSharedSegment(JNIEnv* env,
                                                            jclass clazz,
                                                            jlong segment) {
  managed_shared_memory * segm = (managed_shared_memory*)segment;
  //fprintf(stderr, "close segment %p\n", segm);
  delete segm;
}

JNIEXPORT jobject JNICALL
Java_com_jetbrains_cef_remote_SharedMemory_wrapNativeMem(JNIEnv* env,
                                                              jclass clazz,
                                                              jlong pdata,
                                                              jint length) {
  return env->NewDirectByteBuffer((void*)pdata, length);
}

#ifdef __cplusplus
}
#endif
