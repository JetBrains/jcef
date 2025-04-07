#ifdef WIN32
#include <windows.h>
#include "windows/PipeTransportServer.h"
#include <thread>
#include <TlHelp32.h>
#endif //WIN32

#include <thrift/server/TThreadedServer.h>
#include <thrift/transport/TServerSocket.h>
#include <thrift/transport/TTransportUtils.h>

#include "CefUtils.h"
#include "ServerApplication.h"
#include "ServerHandler.h"
#include "log/Log.h"

#include <boost/date_time/posix_time/posix_time.hpp>
#include <filesystem>

using namespace apache::thrift;
using namespace apache::thrift::transport;
using namespace apache::thrift::server;

using namespace thrift_codegen;

#ifdef OS_MAC
extern void initMacApplication();
#else
#include "include/cef_app.h"
#include "handlers/app/HelperApp.h"
#endif

#ifndef NDEBUG
#if defined(OS_WIN)
void waitForDebug() {
  // Likely the call to
  // https://learn.microsoft.com/en-us/windows/win32/api/debugapi/nf-debugapi-isdebuggerpresent
  // can help
  printf("Waiting for debugger is not supported on windows");
}
#elif defined(OS_LINUX)
void waitForDebug() {
  printf("Waiting for debugger is not supported on linux");
// Probably something like this could work:
// bool isDebuggerAttached() {
//   std::ifstream statusFile("/proc/self/status");
//   std::string line;
//   while (std::getline(statusFile, line)) {
//     if (line.compare(0, 11, "TracerPid:") == 0) {
//       // Extract the tracer PID value
//       int tracerPid = std::stoi(line.substr(11));
//       return tracerPid != 0;
//     }
//   }
//   return false;
// }
}
#elif defined(OS_MAC)
#include <sys/types.h>
#include <sys/sysctl.h>

bool isDebuggerAttached() {
  int mib[4];
  mib[0] = CTL_KERN;
  mib[1] = KERN_PROC;
  mib[2] = KERN_PROC_PID;
  mib[3] = getpid();

  kinfo_proc info;
  info.kp_proc.p_flag = 0;
  size_t size = sizeof(info);

  if (sysctl(mib, 4, &info, &size, nullptr, 0) == -1) {
    perror("sysctl failure");
    return false;
  }

  return (info.kp_proc.p_flag & P_TRACED) != 0;
}

void waitForDebug() {
  printf("Waiting for debugger(PID=%d)...", getpid());

  while (!isDebuggerAttached()) {
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
  }
  printf("Attached");
}

#endif
#endif

#if defined(OS_WIN)

#include <signal.h>
#include <dbghelp.h>

void printStack(std::stringstream & os) {
// TODO: make compilable with Visual Studio 2019 OR increase compiler version
//  unsigned int   i;
//  void         * stack[ 100 ];
//  unsigned short frames;
//  SYMBOL_INFO  * symbol;
//  HANDLE         process;
//
//  process = GetCurrentProcess();
//
//  SymInitialize( process, NULL, TRUE );
//
//  frames               = CaptureStackBackTrace( 0, 100, stack, NULL );
//  symbol               = ( SYMBOL_INFO * )calloc( sizeof( SYMBOL_INFO ) + 256 * sizeof( char ), 1 );
//  symbol->MaxNameLen   = 255;
//  symbol->SizeOfStruct = sizeof( SYMBOL_INFO );
//
//  for(i = 0; i < frames; i++) {
//    SymFromAddr( process, ( DWORD64 )( stack[ i ] ), 0, symbol );
//    os << frames - i - 1 << ": " << symbol->Name << " - " << string_format("0x%0llX", symbol->Address) << std::endl;
//  }
//
//  free( symbol );
}

void signalHandler(int signum) {
  Log::error("Received SIGNAL %d.", signum);
  std::stringstream os;
  printStack(os);
  Log::error("Stacktrace:\n%s", os.str().c_str());
  std::exit(signum);
}

DWORD GetParentProcessPid() {
  HANDLE hSnapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
  if (hSnapshot == INVALID_HANDLE_VALUE)
    return 0;

  PROCESSENTRY32 processEntry = {};
  processEntry.dwSize = sizeof(PROCESSENTRY32);

  if (Process32First(hSnapshot, &processEntry)) {
    DWORD CurrentProcessId = GetCurrentProcessId();
    do {
      if (processEntry.th32ProcessID == CurrentProcessId)
        break;
    } while (Process32Next(hSnapshot, &processEntry));
  }

  CloseHandle(hSnapshot);
  return processEntry.th32ParentProcessID;
}

#else // OS_WIN

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

#endif // OS_WIN

int main(int argc, char* argv[]) {
  const boost::posix_time::ptime t0 =  boost::posix_time::microsec_clock::local_time();
  setThreadName("main");
#if defined(OS_LINUX)
  CefRefPtr<CefApp> cefApp = nullptr;
  CefRefPtr<CefCommandLine> command_line = CefCommandLine::CreateCommandLine();
  command_line->InitFromArgv(argc, argv);
  const std::string& process_type = command_line->GetSwitchValue("type");
  if (process_type == "renderer" || process_type == "zygote")
    cefApp = new HelperApp();
  // On Linux the zygote process is used to spawn other process types. Since
  // we don't know what type of process it will be give it the renderer
  // client.

  CefMainArgs main_args(argc, argv);
  int exit_code = CefExecuteProcess(main_args, cefApp, nullptr);
  if (exit_code >= 0) {
    return exit_code;
  }
#elif WIN32
  // Execute subprocess (if necessary)
  CefRefPtr<CefApp> cefApp = nullptr;
  CefRefPtr<CefCommandLine> command_line = CefCommandLine::CreateCommandLine();
  command_line->InitFromString(::GetCommandLineW());
  const std::string& process_type = command_line->GetSwitchValue("type");
  const bool isMainBrowserProcess = process_type.empty();
  if (!isMainBrowserProcess) {
    // Initialize watchdog thread.
    DWORD parentProcessPid = GetParentProcessPid();
    HANDLE hParentProcess = OpenProcess(SYNCHRONIZE, FALSE, parentProcessPid);

    std::thread([hParentProcess]() {
      WaitForSingleObject(hParentProcess, INFINITE);
      ExitProcess(0);
    }).detach();
  }

  if (process_type == "renderer")
    cefApp = new HelperApp();

  CefMainArgs main_args(GetModuleHandle(0));
  const int result = CefExecuteProcess(main_args, cefApp, nullptr);
  if (result >= 0) {
    // If CefExecuteProcess called for the browser process (identified by no "type" command-line value)
    // it will return immediately with a value of -1.
    return result;
  }
#elif OS_MAC
  initMacApplication();
#endif

  if (getBoolEnv("CEF_SERVER_CATCH_SIGNALS")) {
    std::cout << "Install SIGNAL handlers." << std::endl;
    signal(SIGSEGV, signalHandler);
  }

  const boost::posix_time::ptime t1 =  boost::posix_time::microsec_clock::local_time();
  fprintf(stdout, "Starting cer server. Pre-initialize spent %d ms.\n", (int)(t1 - t0).total_milliseconds());
  ServerApplication& app = ServerApplication::instance();
  if (!app.init(argc, argv)) {
    // Can't load CEF framework library.
    return 100;
  }
  const CommandLineArgs& cmdArgs = app.getCmdArgs();

#ifndef NDEBUG
  if (cmdArgs.waitDebugger()) {
    waitForDebug();
  }
#endif

  boost::posix_time::ptime t2 = boost::posix_time::microsec_clock::local_time();
  Log::trace("Start CEF initialization. ServerApplication initialization spent %d ms.", (t2 - t1).total_milliseconds());
  const bool success = CefUtils::initializeCef();
  if (!success) {
    Log::error("Cef initialization failed");
    return 101;
  }

  const boost::posix_time::ptime t3 =  boost::posix_time::microsec_clock::local_time();
  Log::trace("Create server transport. CEF initialization spent %d ms.", (t3 - t2).total_milliseconds());
  std::shared_ptr<TServerTransport> serverTransport;
  if (cmdArgs.useTcp()) {
    Log::info("TCP transport will be used, port=%d", cmdArgs.getPort());
    serverTransport = std::make_shared<TServerSocket>("127.0.0.1", cmdArgs.getPort());
  } else {
    std::string pipePath = cmdArgs.getPipe();
    if (pipePath.empty()) {
      Log::error("Pipe path is empty, exit.");
      return 102;
    }
#ifdef WIN32
    if (pipePath.rfind("\\\\.\\pipe\\", 0) != 0)
      pipePath = "\\\\.\\pipe\\" + pipePath;
    Log::info("Windows-pipe transport will be used, path=%s", pipePath.c_str());
    serverTransport = std::make_shared<PipeTransportServer>(pipePath);
#else
    Log::info("Pipe transport will be used, path=%s", pipePath.c_str());
    std::remove(pipePath.c_str());
    serverTransport = std::make_shared<TServerSocket>(pipePath.c_str());
#endif //WIN32
  }
  std::shared_ptr<apache::thrift::TProcessorFactory> processorFactory = app.getProcessorFactory();
  std::shared_ptr<TThreadedServer> server = std::make_shared<TThreadedServer>(
      processorFactory,
      serverTransport,
      std::make_shared<TBufferedTransportFactory>(),
      std::make_shared<TBinaryProtocolFactory>());

  const boost::posix_time::ptime t4 =  boost::posix_time::microsec_clock::local_time();
  Log::trace("Start listening thread. Transport initialization spent %d ms.", (t4 - t3).total_milliseconds());
  std::thread servThread([=]() {
    setThreadName("ServerListener");
    try {
      server->serve();
    } catch (TException& e) {
      Log::error("Exception in listening thread");
      Log::error(e.what());
    } catch (...) {
      Log::error("Unknown exception in listening thread");
    }
    Log::debug("Done, server stopped.");
  });

  const boost::posix_time::ptime t6 =  boost::posix_time::microsec_clock::local_time();
  Log::trace("Run CEF loop. Total initialization time %d ms.", (t6 - t0).total_milliseconds());

  CefUtils::runCefLoop();
  Log::debug("Finished message loop.");
  server->stop();
  servThread.join();
  app.stopWatcher();
#ifndef WIN32
  if (!cmdArgs.useTcp())
    std::remove(cmdArgs.getPipe().c_str());
#endif  // WIN32
  if (cmdArgs.deleteRootCacheDir() && !app.isDefaultRoot()) {
    Log::debug("Remove root cache dir '%s'", app.getRootPath().c_str());
    try {
      std::filesystem::remove_all(app.getRootPath());
    } catch (const std::filesystem::filesystem_error& ex) {
      Log::error("Failed to remove root cache dir '%s'. Error: %s. Error code: %s", app.getRootPath().c_str(), ex.what(), ex.code().message().c_str());
    }
  }
  Log::debug("Buy [%s]!", cmdArgs.getTransportDesc().c_str());
  return 0;
}
