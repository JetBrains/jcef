#include "Utils.h"

#include "include/cef_path_util.h"

#include <iostream>
#include <typeinfo>
#include <string>

#if defined(OS_WIN)

#include <tlhelp32.h>
#include <windows.h>

namespace utils {

int GetPid() {
  return (int)GetCurrentProcessId();
}

int GetParentPid() {
  DWORD pid = GetCurrentProcessId();
  int ppid = 0;
  HANDLE hProcess;
  PROCESSENTRY32 pe32;

  hProcess = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
  if (hProcess == INVALID_HANDLE_VALUE)
    return ppid;

  pe32.dwSize = sizeof(PROCESSENTRY32);
  if (!Process32First(hProcess, &pe32)) {
    CloseHandle(hProcess);
    return ppid;
  }

  do {
    if (pe32.th32ProcessID == pid) {
      ppid = (int)pe32.th32ParentProcessID;
      break;
    }
  } while (Process32Next(hProcess, &pe32));

  CloseHandle(hProcess);
  return ppid;
}

std::string GetTempFile(const std::string& identifer, bool useParentId) {
  std::stringstream tmpName;
  CefString tmpPath;
  if (!CefGetPath(PK_DIR_TEMP, tmpPath)) {
    TCHAR lpPathBuffer[MAX_PATH];
    GetTempPath(MAX_PATH, lpPathBuffer);
    tmpPath.FromWString(lpPathBuffer);
  }
  tmpName << tmpPath.ToString().c_str() << "\\";
  tmpName << "jcef-p" << (useParentId ? GetParentPid() : GetPid());
  tmpName << (identifer.empty() ? "" : "_") << identifer.c_str() << ".tmp";
  return tmpName.str();
}

// MSVC doesn't need demangling; __FUNCSIG__ often more reliable
std::string demangle(const char* name) {
  return std::string(name);
}

} // namespace utils
#else

#include <cxxabi.h>
#include <memory>

namespace utils {
int GetPid() {
  return getpid();
}

int GetParentPid() {
  return getppid();
}

std::string GetTempFile(const std::string& identifer, bool useParentId) {
  std::stringstream tmpName;
  CefString tmpPath;
  if (!CefGetPath(PK_DIR_TEMP, tmpPath))
    tmpPath = "/tmp/";
  tmpName << tmpPath.ToString().c_str();
  tmpName << "jcef-p" << (useParentId ? GetParentPid() : GetPid());
  tmpName << (identifer.empty() ? "" : "_") << identifer.c_str() << ".tmp";
  return tmpName.str();
}

std::string demangle(const char* name) {
  int status = -1;
  std::unique_ptr<char, void(*)(void*)> res{
      abi::__cxa_demangle(name, nullptr, nullptr, &status),
      std::free
  };
  return (status == 0) ? std::string(res.get()) : std::string(name);
}

} // namespace utils

#endif // OS_WIN

namespace utils {
  bool isFileExist(const char* pathname) {
    if (FILE *file = fopen(pathname, "r")) {
      fclose(file);
      return true;
    }
    return false;
  }
}

bool getBoolEnv(const std::string & envName, bool defaultVal/* = false*/) {
  const char* sval = getenv(envName.c_str());
  if (sval == nullptr)
    return defaultVal;

  return strcmp(sval, "1") == 0 || strcmp(sval, "true") == 0;
}

long getLongEnv(const std::string & envName, long defVal) {
  const char* sval = getenv(envName.c_str());
  if (sval == nullptr)
    return defVal;

  return atol(sval);
}
