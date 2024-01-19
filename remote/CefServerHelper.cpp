#include <jni.h>

#include <boost/interprocess/managed_shared_memory.hpp>
#include <boost/interprocess/sync/named_mutex.hpp>

using namespace boost::interprocess;


#include <jni.h>
#include <windows.h>
#include <winerror.h>
#include <winnt.h>

#define UNUSED 
#define SECURITY_DESCRIPTOR_SIZE 64 * 1024
#define HANDLE_OR_ERROR(h)                                                     \
  (h == (jlong)INVALID_HANDLE_VALUE) ? -((jlong)GetLastError()) : (jlong)h
#define DEBUG 0
#define THROW_IO(prefix, ...)                                                  \
  do {                                                                         \
    char _buf[1024];                                                           \
    snprintf(_buf, 1024, prefix ? prefix : "%s", __VA_ARGS__);                 \
    jclass exClass = (env)->FindClass("java/io/IOException");            \
    if (exClass != NULL) {                                                     \
      (env)->ThrowNew(exClass, _buf);                                    \
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

#define LOGON_DACL 2 // must match the value in Win32SecurityLevel.java

static int createSecurityWithDacl(PSECURITY_ATTRIBUTES pSA, DWORD accessMask,
                                  BOOL logon) {
  int result = 0;
  PSECURITY_DESCRIPTOR pSD = HeapAlloc(GetProcessHeap(), HEAP_ZERO_MEMORY,
                                       SECURITY_DESCRIPTOR_MIN_LENGTH);
  if (!InitializeSecurityDescriptor(pSD, SECURITY_DESCRIPTOR_REVISION)) {
    wchar_t buf[256];
    FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                   NULL, GetLastError(),
                   MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), buf,
                   (sizeof(buf) / sizeof(wchar_t)), NULL);
    if (DEBUG)
      wprintf(L"Failed to initialize security descriptor %d %s\n",
              GetLastError(), buf);
    result = 1;
    goto psdfail;
  }
  GetLastError();

  PSID psid = NULL;
  HANDLE phToken;
  OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &phToken);
  long unsigned int tokenInformationLength;
  PTOKEN_GROUPS groups = NULL;
  PTOKEN_OWNER owner = NULL;
  if (logon) {
    GetTokenInformation(phToken, TokenGroups, NULL, 0, &tokenInformationLength);
    groups =
        (PTOKEN_GROUPS)HeapAlloc(GetProcessHeap(), HEAP_ZERO_MEMORY,
                                 tokenInformationLength * sizeof(TOKEN_GROUPS));
    GetTokenInformation(phToken, TokenOwner, groups, 0,
                        &tokenInformationLength);
    GetLastError();
    for (DWORD i = 0; i < groups->GroupCount; ++i) {
      SID_AND_ATTRIBUTES a = groups->Groups[i];
      if ((a.Attributes & SE_GROUP_LOGON_ID) == SE_GROUP_LOGON_ID) {
        psid = a.Sid;
        break;
      }
    }
  } else {
    GetTokenInformation(phToken, TokenOwner, NULL, 0, &tokenInformationLength);
    owner =
        (PTOKEN_OWNER)HeapAlloc(GetProcessHeap(), HEAP_ZERO_MEMORY,
                                tokenInformationLength * sizeof(TOKEN_OWNER));
    GetTokenInformation(phToken, TokenOwner, owner, tokenInformationLength,
                        &tokenInformationLength);
    psid = owner->Owner;
  }
  if (psid == NULL)
    goto psidfail;

  DWORD cbAcl = sizeof(ACL) + sizeof(ACCESS_ALLOWED_ACE) + GetLengthSid(psid);
  cbAcl = (cbAcl + (sizeof(DWORD) - 1)) & 0xfffffffc;

  PACL pAcl = (PACL)HeapAlloc(GetProcessHeap(), 0, cbAcl);
  if (!InitializeAcl(pAcl, cbAcl, ACL_REVISION)) {
    wchar_t buf[256];
    FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                   NULL, GetLastError(),
                   MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), buf,
                   (sizeof(buf) / sizeof(wchar_t)), NULL);
    if (DEBUG)
      wprintf(L"Failed to initialize acl %d %s\n", GetLastError(), buf);
    result = 1;
    goto exit;
  }
  if (!AddAccessAllowedAce(pAcl, ACL_REVISION, accessMask, psid)) {
    wchar_t buf[256];
    FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                   NULL, GetLastError(),
                   MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), buf,
                   (sizeof(buf) / sizeof(wchar_t)), NULL);
    if (DEBUG)
      wprintf(L"Failed to add access allowed ace %d %s\n", GetLastError(), buf);
    result = 1;
    goto exit;
  }
  if (!SetSecurityDescriptorDacl(pSD, TRUE, pAcl, FALSE)) {
    wchar_t buf[256];
    FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                   NULL, GetLastError(),
                   MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), buf,
                   (sizeof(buf) / sizeof(wchar_t)), NULL);
    if (DEBUG)
      wprintf(L"Failed to set security despcriptor %d %s\n", GetLastError(),
              buf);
    result = 1;
    pSA->nLength = 0;
    pSA->lpSecurityDescriptor = NULL;
    pSA->bInheritHandle = FALSE;
  } else {
    pSA->nLength = SECURITY_DESCRIPTOR_SIZE;
    pSA->lpSecurityDescriptor = pSD;
    pSA->bInheritHandle = FALSE;
  }

exit:
  HeapFree(GetProcessHeap(), 0, pAcl);
psidfail:
  if (groups)
    HeapFree(GetProcessHeap(), 0, groups);
  if (owner)
    HeapFree(GetProcessHeap(), 0, owner);
  CloseHandle(phToken);
psdfail:
  HeapFree(GetProcessHeap(), 0, pSD);

  return result;
}

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


JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_CreateNamedPipeNative(
    JNIEnv *env, jclass clazz, jstring lpName, jint dwOpenMode,
    jint dwPipeMode, jint nMaxInstances, jint nOutBufferSize,
    jint nIntBufferSize, jint nDefaultTimeout, jint lpSecurityAttributes,
    jint security) {
  PSECURITY_ATTRIBUTES pSA = 
      (PSECURITY_ATTRIBUTES)(security
                                 ? HeapAlloc(GetProcessHeap(), HEAP_ZERO_MEMORY,
                                             sizeof(SECURITY_ATTRIBUTES))
                                 : NULL);
  int err = security ? createSecurityWithDacl(pSA, lpSecurityAttributes,
                                              security == LOGON_DACL)
                     : 0;
  if (!err || !security) {
    LPCWSTR name = (LPCWSTR)(env)->GetStringChars(lpName, 0);

    jlong handle = (jlong)(CreateNamedPipeW(
        name, dwOpenMode, dwPipeMode, nMaxInstances, nOutBufferSize,
        nIntBufferSize, nDefaultTimeout, pSA));
    if (handle == (jlong)INVALID_HANDLE_VALUE) {
      char buf[512];
      FILL_ERROR(NULL, buf);
      THROW_IO("Couldn't create named pipe for %s (%s)",
               (env)->GetStringUTFChars(lpName, 0), buf);
    }
    if (pSA)
      HeapFree(GetProcessHeap(), 0, pSA);
    return handle;
  } else {
    char buf[512];
    FILL_ERROR("Couldn't create security acl -- %s (error code %ld)", buf);
    jclass exClass = (env)->FindClass("java/io/IOException");
    if (exClass != NULL) {
      if (pSA)
        HeapFree(GetProcessHeap(), 0, pSA);
      return (env)->ThrowNew(exClass, buf);
    }
    if (pSA)
      HeapFree(GetProcessHeap(), 0, pSA);
    return -1;
  }
}

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_CreateFileNative(
    JNIEnv *env, jclass clazz, jstring lpName) {
  LPCWSTR name = (LPCWSTR)(env)->GetStringChars(lpName, 0);

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
    THROW_IO("Couldn't open file %s (%s)",
             (env)->GetStringUTFChars(lpName, 0), buf);
  }
  return (jlong)handle;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_ConnectNamedPipeNative(
    UNUSED JNIEnv *env, jclass clazz, jlong handlePointer,
    jlong overlappedPointer) {
  jboolean result =
      ConnectNamedPipe((HANDLE)handlePointer, (LPOVERLAPPED)overlappedPointer);
  return result ? -1 : (jint)GetLastError();
}

JNIEXPORT jboolean JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_DisconnectNamedPipe(
    UNUSED JNIEnv *env, jclass clazz, jlong handlePointer) {
  return DisconnectNamedPipe((HANDLE)handlePointer);
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_readNative(
    JNIEnv *env, jclass clazz, jlong waitable, jlong hFile,
    jbyteArray buffer, jint offset, jint length, jboolean strict) {
  HANDLE handle = (HANDLE)hFile;
  OVERLAPPED olap = {0};
  olap.hEvent = (HANDLE)waitable;

  DWORD bytes_read = 0;
  LPVOID read_buffer = HeapAlloc(GetProcessHeap(), HEAP_ZERO_MEMORY, length);
  BOOL immediate = ReadFile(handle, read_buffer, length, &bytes_read, &olap);
  if (!immediate) {
    if (GetLastError() != ERROR_IO_PENDING) {
      char buf[256];
      FILL_ERROR("ReadFile() failed: %s (error code %ld)", buf);
      THROW_IO(NULL, buf);
    }
  }

  fprintf(stderr, "start read pipe.\n");
  if (!GetOverlappedResult(handle, &olap, &bytes_read, TRUE)) {
    fprintf(stderr, "err 1.\n");
    char buf[256];
    FILL_ERROR(
        "GetOverlappedResult() failed for read operation: %s (error code %ld)",
        buf);
    THROW_IO(NULL, buf);
  }
  fprintf(stderr, "\tfinish read pipe.\n");
  if (strict && (bytes_read != (DWORD)length)) {
    char buf[256];
    snprintf(buf, 256,
             "ReadFile() read less bytes than requested: expected %d bytes, "
             "but read %ld bytes",
             length, bytes_read);
    THROW_IO(NULL, buf);
  }
  (env)->SetByteArrayRegion(buffer, offset, bytes_read, (const jbyte *)read_buffer);
  HeapFree(GetProcessHeap(), 0, read_buffer);
  return bytes_read;
}

/*
 * Class:     org_scalasbt_ipcsocket_JNIWin32NamedPipeLibraryProvider
 * Method:    writeNative
 * Signature: (JJ[BII)V
 */
JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_writeNative(
    JNIEnv *env, jclass clazz, jlong waitable, jlong hHandle,
    jbyteArray buffer, jint offset, jint length) {
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
    FILL_ERROR(
        "GetOverlappedResult() failed for write operation: %s (error code %ld)",
        buf);
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
Java_com_jetbrains_cef_remote_Win32Pipe_CloseHandleNative(
    UNUSED JNIEnv *env, jclass clazz, jlong handlePointer) {
  return CloseHandle((HANDLE)handlePointer);
}

JNIEXPORT jboolean JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_GetOverlappedResultNative(
    UNUSED JNIEnv *env, jclass clazz, jlong handlePointer,
    jlong overlappedPointer) {
  DWORD len = 0;
  return GetOverlappedResult((HANDLE)handlePointer,
                             (LPOVERLAPPED)overlappedPointer, &len, TRUE);
}

JNIEXPORT jboolean JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_CancelIoEx(
    UNUSED JNIEnv *env, jclass clazz, jlong handlePointer) {
  return CancelIoEx((HANDLE)handlePointer, NULL);
}

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_CreateEventNative(
    JNIEnv *env, jclass clazz, jboolean manualReset,
    jboolean initialState, jstring lpName) {
  LPCWSTR name =
      lpName ? (LPCWSTR)(env)->GetStringChars(lpName, 0) : NULL;
  HANDLE handle = CreateEventW(NULL, manualReset, initialState, name);
  if (handle == INVALID_HANDLE_VALUE) {
    char buf[512];
    FILL_ERROR(NULL, buf);
    THROW_IO("Couldn't create event %s (%s)",
             (env)->GetStringUTFChars(lpName, 0), buf);
  }
  return (jlong)handle;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_GetLastError(
    UNUSED JNIEnv *env, jclass clazz) {
  return GetLastError();
}

JNIEXPORT jboolean JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_FlushFileBuffersNative(
    UNUSED JNIEnv *env, jclass clazz, jlong handlePointer) {
  return FlushFileBuffers((HANDLE)handlePointer);
}

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_NewOverlappedNative(
    UNUSED JNIEnv *env, jclass clazz, jlong handlePointer) {
  HANDLE handle = (HANDLE)handlePointer;
  UNUSED LPOVERLAPPED op =
      (LPOVERLAPPED)HeapAlloc(GetProcessHeap(), HEAP_ZERO_MEMORY, sizeof(OVERLAPPED));
  op->hEvent = handle;
  return (jlong)op;
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_DeleteOverlappedNative(
    UNUSED JNIEnv *env, jclass clazz, jlong overlappedPointer) {
  HeapFree(GetProcessHeap(), 0, (void *)overlappedPointer);
}

// Constants follow:
JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_ERROR_1IO_1PENDING(
    UNUSED JNIEnv *env, jclass clazz) {
  return ERROR_IO_PENDING;
};

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_ERROR_1NO_1DATA(
    UNUSED JNIEnv *env, jclass clazz) {
  return ERROR_NO_DATA;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_ERROR_1PIPE_1CONNECTED(
    UNUSED JNIEnv *env, jclass clazz) {
  return ERROR_PIPE_CONNECTED;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_FILE_1ALL_1ACCESS(
    UNUSED JNIEnv *env, jclass clazz) {
  return FILE_ALL_ACCESS;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_FILE_1FLAG_1FIRST_1PIPE_1INSTANCE(
    UNUSED JNIEnv *env, jclass clazz) {
  return FILE_FLAG_FIRST_PIPE_INSTANCE;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_FILE_1FLAG_1OVERLAPPED(
    UNUSED JNIEnv *env, jclass clazz) {
  return FILE_FLAG_OVERLAPPED;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_FILE_1GENERIC_1READ(
    UNUSED JNIEnv *env, jclass clazz) {
  return FILE_GENERIC_READ;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_GENERIC_1READ(
    UNUSED JNIEnv *env, jclass clazz) {
  return GENERIC_READ;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_GENERIC_1WRITE(
    UNUSED JNIEnv *env, jclass clazz) {
  return GENERIC_WRITE;
}

JNIEXPORT jint JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_PIPE_1ACCESS_1DUPLEX(
    UNUSED JNIEnv *env, jclass clazz) {
  return PIPE_ACCESS_DUPLEX;
}

JNIEXPORT jstring JNICALL
Java_com_jetbrains_cef_remote_Win32Pipe_getErrorMessage(
    JNIEnv *env, jclass clazz, jint errorCode) {
  wchar_t buf[256];
  FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                 NULL, errorCode, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                 buf, (sizeof(buf) / sizeof(wchar_t)), NULL);
  size_t len = wcsnlen(buf, 256);
  if (len > 0 && buf[len - 1] == '\n') {
    buf[len - 1] = '0';
    len--;
  }
  return (env)->NewString((const jchar *)buf, len - 1);
}

#ifdef __cplusplus
}
#endif
