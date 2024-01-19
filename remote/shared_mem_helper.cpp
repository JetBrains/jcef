#include <jni.h>

#include <boost/interprocess/managed_shared_memory.hpp>
#include <boost/interprocess/sync/named_mutex.hpp>

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

  env->ReleaseStringUTFChars(sid, strSid);
  return (jlong)segment;
}

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_SharedMemory_getPointer(JNIEnv* env,
                                                            jclass clazz,
                                                            jlong segment,
                                                            jlong handle) {
  if (!segment)
    return 0;
  managed_shared_memory * segm = (managed_shared_memory*)segment;
  return (jlong)segm->get_address_from_handle(handle);
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_SharedMemory_closeSharedSegment(JNIEnv* env,
                                                            jclass clazz,
                                                            jlong segment) {
  if (!segment)
    return;
  managed_shared_memory * segm = (managed_shared_memory*)segment;
  delete segm;
}

JNIEXPORT jobject JNICALL
Java_com_jetbrains_cef_remote_SharedMemory_wrapNativeMem(JNIEnv* env,
                                                              jclass clazz,
                                                              jlong pdata,
                                                              jint length) {
  if (!pdata)
    return 0;
  return env->NewDirectByteBuffer((void*)pdata, length);
}

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_SharedMemory_openSharedMutex(JNIEnv* env,
                                                             jclass clazz,
                                                             jstring uid) {
  if (!uid)
    return 0;
  named_mutex * mutex = nullptr;
  const char* strSid = env->GetStringUTFChars(uid, nullptr);
  if (strSid)
    mutex = new named_mutex(open_only, strSid);
  env->ReleaseStringUTFChars(uid, strSid);
  return (jlong)mutex;
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_SharedMemory_lockSharedMutex(JNIEnv* env,
                                                      jclass clazz,
                                                      jlong mutex) {
  if (!mutex)
    return;
  named_mutex * m = (named_mutex*)mutex;
  m->lock();
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_SharedMemory_unlockSharedMutex(JNIEnv* env,
                                                           jclass clazz,
                                                           jlong mutex) {
  if (!mutex)
    return;
  named_mutex * m = (named_mutex*)mutex;
  m->unlock();
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_SharedMemory_closeSharedMutex(JNIEnv* env,
                                                              jclass clazz,
                                                              jlong mutex) {
  if (!mutex)
    return;
  named_mutex * m = (named_mutex*)mutex;
  delete m;
}

#ifdef __cplusplus
}
#endif
