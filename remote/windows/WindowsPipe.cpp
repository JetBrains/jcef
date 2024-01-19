#include <jni.h>
#include <windows.h>
#include <winerror.h>
#include <winnt.h>
#include <vector>

#define THROW_IO(prefix, ...)                                                  \
  do {                                                                         \
    char _buf[1024];                                                           \
    snprintf(_buf, 1024, prefix ? prefix : "%s", __VA_ARGS__);                 \
    jclass exClass = (env)->FindClass("java/io/IOException");                  \
    if (exClass != NULL) {                                                     \
      (env)->ThrowNew(exClass, _buf);                                          \
    }                                                                          \
  } while (0);

#define FILL_ERROR(prefix, buf)                                                \
  do {                                                                         \
    char err[sizeof(buf)];                                                     \
    FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,  \
                  NULL, GetLastError(),                                        \
                  MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), err, sizeof(buf), \
                  NULL);                                                       \
    size_t len = strnlen(err, sizeof(err));                                    \
    if (err[len - 2] == '\r')                                                  \
      err[len - 2] = '\0';                                                     \
    snprintf(buf, sizeof(buf), prefix ? prefix : "%s (error code %ld)", err,   \
             GetLastError());                                                  \
  } while (0);


#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_CreateNamedPipe(
    JNIEnv *env, jclass clazz, jstring pipeName, jint dwOpenMode,
    jint dwPipeMode, jint nMaxInstances, jint nOutBufferSize,
    jint nInBufferSize, jint nDefaultTimeout
) {
    LPCWSTR name = (LPCWSTR)(env)->GetStringChars(pipeName, 0);
    jlong handle = (jlong)(CreateNamedPipeW(
        name,
        dwOpenMode,
        dwPipeMode,
        nMaxInstances,
        nOutBufferSize,
        nInBufferSize,
        nDefaultTimeout,
        NULL));
    if (handle == (jlong)INVALID_HANDLE_VALUE) {
      char buf[512];
      FILL_ERROR(NULL, buf);
      THROW_IO("Couldn't create named pipe for %ws (%s)", name, buf);
    }

    return (jlong)handle;
}

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_OpenFile(
    JNIEnv *env, jclass clazz, jstring pipeName) {
  LPCWSTR name = (LPCWSTR)(env)->GetStringChars(pipeName, 0);
  HANDLE handle =
      CreateFileW(name, GENERIC_READ | GENERIC_WRITE,
                  0,    // no sharing
                  NULL, // default security attributes
                  OPEN_EXISTING,
                  FILE_FLAG_OVERLAPPED, // need overlapped for true
                                         // asynchronous read/write access
                  NULL);                // no template file
  if (handle == INVALID_HANDLE_VALUE) {
    char buf[512];
    FILL_ERROR(NULL, buf);
    THROW_IO("Couldn't open file %ws (%s)", name, buf);
  }
  return (jlong)handle;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_ConnectNamedPipe(
    JNIEnv *env, jclass clazz, jlong handlePointer,
    jlong overlappedPointer
) {
  jboolean result = ConnectNamedPipe((HANDLE)handlePointer, (LPOVERLAPPED)overlappedPointer);
  return result ? -1 : (jint)GetLastError();
}

JNIEXPORT jboolean JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_DisconnectNamedPipe(
    JNIEnv *env, jclass clazz, jlong handlePointer
) {
  return DisconnectNamedPipe((HANDLE)handlePointer);
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_read(
    JNIEnv *env, jclass clazz, jlong waitable, jlong hFile,
    jbyteArray buffer, jint offset, jint length
) {
  HANDLE handle = (HANDLE)hFile;
  OVERLAPPED olap = {0};
  olap.hEvent = (HANDLE)waitable;

  std::vector<unsigned char> readBuf(length);
  DWORD bytes_read = 0;
  BOOL immediate = ReadFile(handle, readBuf.data(), length, &bytes_read, &olap);
  if (!immediate) {
    if (GetLastError() != ERROR_IO_PENDING) {
      char buf[256];
      FILL_ERROR("ReadFile() failed: %s (error code %ld)", buf);
      THROW_IO(NULL, buf);
      return bytes_read;
    }
  }

  if (!GetOverlappedResult(handle, &olap, &bytes_read, TRUE)) {
    char buf[256];
    FILL_ERROR("GetOverlappedResult() failed for read operation: %s (error code %ld)", buf);
    THROW_IO(NULL, buf);
    return bytes_read;
  }

  (env)->SetByteArrayRegion(buffer, offset, bytes_read, (const jbyte *)readBuf.data());
  return bytes_read;
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_write(
    JNIEnv *env, jclass clazz, jlong waitable, jlong hHandle,
    jbyteArray buffer, jint offset, jint length
) {
  HANDLE handle = (HANDLE)hHandle;
  OVERLAPPED olap = {0};
  olap.hEvent = (HANDLE)waitable;

  jbyte *bytes = (env)->GetByteArrayElements(buffer, 0);
  BOOL immediate =
      WriteFile(handle, bytes + (DWORD)offset, (DWORD)length, NULL, &olap);
  if (!immediate) {
    if (GetLastError() != ERROR_IO_PENDING) {
      char buf[256];
      FILL_ERROR("ReadFile() failed: %s (error code %ld)", buf);
      THROW_IO(NULL, buf);
    }
  }
  DWORD bytes_written = 0;
  if (!GetOverlappedResult(handle, &olap, &bytes_written, TRUE)) {
    char buf[256];
    FILL_ERROR("GetOverlappedResult() failed for write operation: %s (error code %ld)", buf);
    THROW_IO(NULL, buf);
  }
  if (bytes_written != (DWORD)length) {
    char buf[256];
    snprintf(buf, 256,
             "WriteFile() wrote less bytes than requested: expected %d bytes, "
             "but wrote %ld bytes",
             length, bytes_written);
    THROW_IO(NULL, buf);
  }
  (env)->ReleaseByteArrayElements(buffer, bytes, JNI_ABORT);
}

JNIEXPORT jboolean JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_CloseHandle(
    JNIEnv *env, jclass clazz, jlong handlePointer
) {
  return CloseHandle((HANDLE)handlePointer);
}

JNIEXPORT jboolean JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_GetOverlappedResult(
    JNIEnv *env, jclass clazz, jlong handlePointer,
    jlong overlappedPointer
) {
  DWORD len = 0;
  return GetOverlappedResult((HANDLE)handlePointer,
                             (LPOVERLAPPED)overlappedPointer, &len, TRUE);
}

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_CreateEvent(
    JNIEnv *env, jclass clazz, jboolean manualReset,
    jboolean initialState, jstring lpName
) {
  LPCWSTR name = lpName ? (LPCWSTR)(env)->GetStringChars(lpName, 0) : NULL;
  HANDLE handle = CreateEventW(NULL, manualReset, initialState, name);
  if (handle == INVALID_HANDLE_VALUE) {
    char buf[512];
    FILL_ERROR(NULL, buf);
    THROW_IO("Couldn't create event %ws (%s)", name, buf);
  }
  return (jlong)handle;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_GetLastError(
    JNIEnv *env, jclass clazz
) {
  return GetLastError();
}

JNIEXPORT jboolean JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_FlushFileBuffers(
    JNIEnv *env, jclass clazz, jlong handlePointer
) {
  return FlushFileBuffers((HANDLE)handlePointer);
}

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_NewOverlapped(
    JNIEnv *env, jclass clazz, jlong handlePointer
) {
  HANDLE handle = (HANDLE)handlePointer;
  LPOVERLAPPED op =
      (LPOVERLAPPED)HeapAlloc(GetProcessHeap(), HEAP_ZERO_MEMORY, sizeof(OVERLAPPED));
  op->hEvent = handle;
  return (jlong)op;
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_DeleteOverlapped(
    JNIEnv *env, jclass clazz, jlong overlappedPointer
) {
  HeapFree(GetProcessHeap(), 0, (void *)overlappedPointer);
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_ERROR_1IO_1PENDING(
    JNIEnv *env, jclass clazz) {
  return ERROR_IO_PENDING;
};

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_ERROR_1NO_1DATA(
    JNIEnv *env, jclass clazz) {
  return ERROR_NO_DATA;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_ERROR_1PIPE_1CONNECTED(
    JNIEnv *env, jclass clazz) {
  return ERROR_PIPE_CONNECTED;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_FILE_1ALL_1ACCESS(
    JNIEnv *env, jclass clazz) {
  return FILE_ALL_ACCESS;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_FILE_1FLAG_1FIRST_1PIPE_1INSTANCE(
    JNIEnv *env, jclass clazz) {
  return FILE_FLAG_FIRST_PIPE_INSTANCE;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_FILE_1FLAG_1OVERLAPPED(
    JNIEnv *env, jclass clazz) {
  return FILE_FLAG_OVERLAPPED;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_FILE_1GENERIC_1READ(
    JNIEnv *env, jclass clazz) {
  return FILE_GENERIC_READ;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_GENERIC_1READ(
    JNIEnv *env, jclass clazz) {
  return GENERIC_READ;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_GENERIC_1WRITE(
    JNIEnv *env, jclass clazz) {
  return GENERIC_WRITE;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_WindowsPipe_PIPE_1ACCESS_1DUPLEX(
    JNIEnv *env, jclass clazz) {
  return PIPE_ACCESS_DUPLEX;
}

#ifdef __cplusplus
}
#endif
