#include "CrashHandler.h"
#include "log/Log.h"

#ifdef WIN32
#include <windows.h>
#include <dbghelp.h>

#include <fstream>
#include <iomanip>
#include <iostream>

void CrashLog(const std::string& message, std::ofstream * logFile = nullptr) {
  std::cerr << message << std::endl;
  if (!Log::isStdStreamLogger())
    Log::error(message.c_str());

  if (logFile != nullptr && logFile->is_open()) {
    *logFile << message << std::endl;
    logFile->flush();
  }
}

void PrintStackTrace(std::stringstream & os, EXCEPTION_POINTERS* pExceptionPtrs = nullptr) {
  HANDLE hProcess = GetCurrentProcess();
  HANDLE hThread = GetCurrentThread();

  CONTEXT context;
  if (pExceptionPtrs) {
    context = *pExceptionPtrs->ContextRecord;
  } else {
    RtlCaptureContext(&context);
  }

  SymInitialize(hProcess, NULL, TRUE);

  DWORD imageType = IMAGE_FILE_MACHINE_I386;
#ifdef _M_X64
  imageType = IMAGE_FILE_MACHINE_AMD64;
#elif _M_ARM64
  imageType = IMAGE_FILE_MACHINE_ARM64;
#endif

  STACKFRAME64 stackFrame = {};
  stackFrame.AddrPC.Offset = context.Rip; // x64
  stackFrame.AddrPC.Mode = AddrModeFlat;
  stackFrame.AddrFrame.Offset = context.Rbp;
  stackFrame.AddrFrame.Mode = AddrModeFlat;
  stackFrame.AddrStack.Offset = context.Rsp;
  stackFrame.AddrStack.Mode = AddrModeFlat;

  os << "Stack trace:\n";

  for (int frame = 0; frame < 64; frame++) {
    BOOL result = StackWalk64(
        imageType,
        hProcess,
        hThread,
        &stackFrame,
        &context,
        NULL,
        SymFunctionTableAccess64,
        SymGetModuleBase64,
        NULL
    );

    if (!result) break;

    DWORD64 address = stackFrame.AddrPC.Offset;
    if (address == 0) break;

    // Get symbol info
    char buffer[sizeof(SYMBOL_INFO) + MAX_SYM_NAME * sizeof(TCHAR)];
    PSYMBOL_INFO symbol = (PSYMBOL_INFO)buffer;
    symbol->SizeOfStruct = sizeof(SYMBOL_INFO);
    symbol->MaxNameLen = MAX_SYM_NAME;

    DWORD64 displacement = 0;
    if (SymFromAddr(hProcess, address, &displacement, symbol)) {
      os << "  [" << std::setw(2) << frame << "] "
         << symbol->Name << " + 0x" << std::hex << displacement
         << " (0x" << address << ")" << std::dec << std::endl;
    } else {
      os << "  [" << std::setw(2) << frame << "] 0x"
         << std::hex << address << std::dec << std::endl;
    }

    // Get source file and line info
    IMAGEHLP_LINE64 line = {};
    line.SizeOfStruct = sizeof(IMAGEHLP_LINE64);
    DWORD lineDisplacement = 0;
    if (SymGetLineFromAddr64(hProcess, address, &lineDisplacement, &line)) {
      os << "      File: " << line.FileName
         << ", Line: " << line.LineNumber << std::endl;
    }
  }

  SymCleanup(hProcess);
}

std::string GetExceptionDesc(DWORD exceptionCode) {
  switch (exceptionCode) {
    case EXCEPTION_ACCESS_VIOLATION:
      return "Access violation";
    case EXCEPTION_DATATYPE_MISALIGNMENT:
      return "Datatype misalignment";
    case EXCEPTION_BREAKPOINT:
      return "Breakpoint";
    case EXCEPTION_SINGLE_STEP:
      return "Single step";
    case EXCEPTION_ARRAY_BOUNDS_EXCEEDED:
      return "Array bounds exceeded";
    case EXCEPTION_FLT_DENORMAL_OPERAND:
      return "Floating point denormal operand";
    case EXCEPTION_FLT_DIVIDE_BY_ZERO:
      return "Floating point divide by zero";
    case EXCEPTION_FLT_INEXACT_RESULT:
      return "Floating point inexact result";
    case EXCEPTION_FLT_INVALID_OPERATION:
      return "Floating point invalid operation";
    case EXCEPTION_FLT_OVERFLOW:
      return "Floating point overflow";
    case EXCEPTION_FLT_STACK_CHECK:
      return "Floating point stack check";
    case EXCEPTION_FLT_UNDERFLOW:
      return "Floating point underflow";
    case EXCEPTION_INT_DIVIDE_BY_ZERO:
      return "Integer divide by zero";
    case EXCEPTION_INT_OVERFLOW:
      return "Integer overflow";
    case EXCEPTION_PRIV_INSTRUCTION:
      return "Privileged instruction";
    case EXCEPTION_IN_PAGE_ERROR:
        return "In page error";
    case EXCEPTION_ILLEGAL_INSTRUCTION:
      return "Illegal instruction";
    case EXCEPTION_NONCONTINUABLE_EXCEPTION:
      return "Noncontinuable exception";
    case EXCEPTION_STACK_OVERFLOW:
      return "Stack overflow";
    case EXCEPTION_INVALID_DISPOSITION:
      return "Invalid disposition";
    case EXCEPTION_GUARD_PAGE:
      return "Guard page violation";
    case EXCEPTION_INVALID_HANDLE:
      return "Invalid handle";
    default:
      return "Exception Code: " + std::to_string(exceptionCode);
  }
}

LONG WINAPI ExceptionHandler(EXCEPTION_POINTERS* pExceptionPtrs) {
  const DWORD exceptionCode = pExceptionPtrs->ExceptionRecord->ExceptionCode;
  if (exceptionCode == EXCEPTION_BREAKPOINT)
    return EXCEPTION_CONTINUE_EXECUTION;

  std::ofstream logFile;
  std::time_t now = std::chrono::system_clock::to_time_t(std::chrono::system_clock::now());
  char buf[64] = {0};
  std::strftime(buf, sizeof(buf), "%Y-%m-%d_%H-%M-%S", std::localtime(&now));
  logFile.open("crash_stacktrace_" + std::string(buf) + ".txt", std::ios::app);
  CrashLog("*** APPLICATION CRASHED ***\n", &logFile);
  CrashLog("Exception: " + GetExceptionDesc(exceptionCode), &logFile);

  std::stringstream os;
  PrintStackTrace(os, pExceptionPtrs);
  CrashLog(os.str(), &logFile);

  return EXCEPTION_EXECUTE_HANDLER; // Terminate the application
}

void setupCrashHandler() {
  Log::info("Enable debug privileges and SetUnhandledExceptionFilter.");
  // optional, for better symbol resolution
  HANDLE hToken;
  if (OpenProcessToken(GetCurrentProcess(), TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY, &hToken)) {
    TOKEN_PRIVILEGES tp;
    LookupPrivilegeValue(NULL, SE_DEBUG_NAME, &tp.Privileges[0].Luid);
    tp.PrivilegeCount = 1;
    tp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;
    AdjustTokenPrivileges(hToken, FALSE, &tp, 0, NULL, NULL);
    CloseHandle(hToken);
  }

  // Setup crash handler
  LPTOP_LEVEL_EXCEPTION_FILTER prevHandler = SetUnhandledExceptionFilter(ExceptionHandler);
  if (prevHandler != nullptr)
    Log::info("Previous unhandled exception filter was %p.", prevHandler);
}

#else // WIN32

#include <execinfo.h>
#include <signal.h>
#include <stdlib.h>
#include <unistd.h>

void signalHandler(int signum) {
  Log::error("Received SIGNAL %d.", signum);
  void *array[64];
  size_t size = backtrace(array, 64); // get void*'s for all entries on the stack

  // print out all the frames to stderr
  backtrace_symbols_fd(array, size, STDERR_FILENO);
  Log::error("Stacktrace (size %d) was printed to stderr.", size);
  std::exit(signum);
}

void setupCrashHandler() {
  Log::info("Install SIGNAL handlers.");
  signal(SIGSEGV, signalHandler);
}

#endif // WIN32