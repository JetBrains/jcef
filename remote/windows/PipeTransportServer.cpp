#include "PipeTransportServer.h"
#include "PipeTransport.h"

#include <cstring>
#include "thrift/thrift-config.h"

#include "thrift/TNonCopyable.h"

#include <AccCtrl.h>
#include <AclAPI.h>
#include <sddl.h>
#include "thrift/windows/OverlappedSubmissionThread.h"
#include "thrift/windows/Sync.h"

using namespace apache::thrift::transport;
using namespace apache::thrift;

using std::shared_ptr;

namespace {

// Windows - set security to allow non-elevated apps
// to access pipes created by elevated apps.
// Full access to everyone
const std::string DEFAULT_PIPE_SECURITY{"D:(A;;FA;;;WD)"};

}

class WindowsPipeServerImpl : apache::thrift::TNonCopyable {
 public:
  WindowsPipeServerImpl() {}
  virtual ~WindowsPipeServerImpl() {}
  virtual void interrupt() = 0;
  virtual std::shared_ptr<TTransport> acceptImpl() = 0;

  virtual HANDLE getPipeHandle() = 0;
  virtual HANDLE getWrtPipeHandle() = 0;
  virtual HANDLE getClientRdPipeHandle() = 0;
  virtual HANDLE getClientWrtPipeHandle() = 0;
  virtual HANDLE getNativeWaitHandle() { return nullptr; }
};

class WindowsNamedPipeServer : public WindowsPipeServerImpl {
 public:
  WindowsNamedPipeServer(const std::string& pipename,
                   uint32_t bufsize,
                   uint32_t maxconnections,
                   const std::string& securityDescriptor)
      : stopping_(false),
        pipename_(pipename),
        bufsize_(bufsize),
        maxconns_(maxconnections),
        securityDescriptor_(securityDescriptor) {
    connectOverlap_.action = TOverlappedWorkItem::CONNECT;
    cancelOverlap_.action = TOverlappedWorkItem::CANCELIO;
    TAutoCrit lock(pipe_protect_);
    initiateNamedConnect(lock);
  }
  virtual ~WindowsNamedPipeServer() {}

  virtual void interrupt() {
    TAutoCrit lock(pipe_protect_);
    cached_client_.reset();
    if (Pipe_.h != INVALID_HANDLE_VALUE) {
      stopping_ = true;
      cancelOverlap_.h = Pipe_.h;
      // This should wake up GetOverlappedResult
      thread_->addWorkItem(&cancelOverlap_);
    }
  }

  virtual std::shared_ptr<TTransport> acceptImpl();

  virtual HANDLE getPipeHandle() { return Pipe_.h; }
  virtual HANDLE getWrtPipeHandle() { return INVALID_HANDLE_VALUE; }
  virtual HANDLE getClientRdPipeHandle() { return INVALID_HANDLE_VALUE; }
  virtual HANDLE getClientWrtPipeHandle() { return INVALID_HANDLE_VALUE; }
  virtual HANDLE getNativeWaitHandle() { return listen_event_.h; }

 private:
  bool createNamedPipe(const TAutoCrit &lockProof);
  void initiateNamedConnect(const TAutoCrit &lockProof);

  TAutoOverlapThread thread_;
  TOverlappedWorkItem connectOverlap_;
  TOverlappedWorkItem cancelOverlap_;

  bool stopping_;
  std::string pipename_;
  std::string securityDescriptor_;
  uint32_t bufsize_;
  uint32_t maxconns_;
  TManualResetEvent listen_event_;

  TCriticalSection pipe_protect_;
  // only read or write these variables underneath a locked pipe_protect_
  std::shared_ptr<PipeTransport> cached_client_;
  TAutoHandle Pipe_;
};

HANDLE PipeTransportServer::getNativeWaitHandle() {
  if (impl_)
    return impl_->getNativeWaitHandle();
  return nullptr;
}

//---- Constructors ----
PipeTransportServer::PipeTransportServer(const std::string& pipename, uint32_t bufsize)
    : bufsize_(bufsize), isAnonymous_(false) {
  setMaxConnections(TPIPE_SERVER_MAX_CONNS_DEFAULT);
  setPipename(pipename);
  setSecurityDescriptor(DEFAULT_PIPE_SECURITY);
}

PipeTransportServer::PipeTransportServer(const std::string& pipename, uint32_t bufsize, uint32_t maxconnections)
    : bufsize_(bufsize), isAnonymous_(false) {
  setMaxConnections(maxconnections);
  setPipename(pipename);
  setSecurityDescriptor(DEFAULT_PIPE_SECURITY);
}

PipeTransportServer::PipeTransportServer(const std::string& pipename,
                         uint32_t bufsize,
                         uint32_t maxconnections,
                         const std::string& securityDescriptor)
    : bufsize_(bufsize), isAnonymous_(false) {
  setMaxConnections(maxconnections);
  setPipename(pipename);
  setSecurityDescriptor(securityDescriptor);
}

PipeTransportServer::PipeTransportServer(const std::string& pipename) : bufsize_(1024), isAnonymous_(false) {
  setMaxConnections(TPIPE_SERVER_MAX_CONNS_DEFAULT);
  setPipename(pipename);
  setSecurityDescriptor(DEFAULT_PIPE_SECURITY);
}


//---- Destructor ----
PipeTransportServer::~PipeTransportServer() {}

bool PipeTransportServer::isOpen() const {
  return (impl_->getPipeHandle() != INVALID_HANDLE_VALUE);
}

//---------------------------------------------------------
// Transport callbacks
//---------------------------------------------------------
void PipeTransportServer::listen() {
  if (isAnonymous_)
    return;
  impl_.reset(new WindowsNamedPipeServer(pipename_, bufsize_, maxconns_, securityDescriptor_));
}

shared_ptr<TTransport> PipeTransportServer::acceptImpl() {
  return impl_->acceptImpl();
}

void WindowsNamedPipeServer::initiateNamedConnect(const TAutoCrit &lockProof) {
  if (stopping_)
    return;
  if (!createNamedPipe(lockProof)) {
    throw TTransportException(TTransportException::NOT_OPEN, "PipeTransportServer CreateNamedPipe failed");
  }

  // The prior connection has been handled, so close the gate
  ResetEvent(listen_event_.h);
  connectOverlap_.reset(nullptr, 0, listen_event_.h);
  connectOverlap_.h = Pipe_.h;
  thread_->addWorkItem(&connectOverlap_);

  // Wait for the client to connect; if it succeeds, the
  // function returns a nonzero value. If the function returns
  // zero, GetLastError should return ERROR_PIPE_CONNECTED.
  if (connectOverlap_.success) {
    cached_client_.reset(new PipeTransport(Pipe_));
    // make sure people know that a connection is ready
    SetEvent(listen_event_.h);
    return;
  }

  DWORD dwErr = connectOverlap_.last_error;
  switch (dwErr) {
    case ERROR_PIPE_CONNECTED:
      cached_client_.reset(new PipeTransport(Pipe_));
      // make sure people know that a connection is ready
      SetEvent(listen_event_.h);
      return;
    case ERROR_IO_PENDING:
      return; // acceptImpl will do the appropriate WaitForMultipleObjects
    default:
      throw TTransportException(TTransportException::NOT_OPEN, "PipeTransportServer ConnectNamedPipe failed, err: " + TOutput::strerror_s(dwErr));
  }
}

shared_ptr<TTransport> WindowsNamedPipeServer::acceptImpl() {
  {
    TAutoCrit lock(pipe_protect_);
    if (cached_client_.get() != nullptr) {
      shared_ptr<PipeTransport> client;
      // zero out cached_client, since we are about to return it.
      client.swap(cached_client_);

      // kick off the next connection before returning
      initiateNamedConnect(lock);
      return client; // success!
    }
  }

  if (Pipe_.h == INVALID_HANDLE_VALUE) {
    throw TTransportException(TTransportException::NOT_OPEN, "WindowsNamedPipeServer: someone called accept on a closed pipe server.");
  }

  DWORD dwDummy = 0;

  // For the most part, Pipe_ should be protected with pipe_protect_.  We can't
  // reasonably do that here though without breaking interruptability.  However,
  // this should be safe, though I'm not happy about it.  We only need to ensure
  // that no one writes / modifies Pipe_.h while we are reading it.  Well, the
  // only two things that should be modifying Pipe_ are acceptImpl, the
  // functions it calls, and the destructor.  Those things shouldn't be run
  // concurrently anyway.  So this call is 'really' just a read that may happen
  // concurrently with interrupt, and that should be fine.
  if (GetOverlappedResult(Pipe_.h, &connectOverlap_.overlap, &dwDummy, TRUE)) {
    TAutoCrit lock(pipe_protect_);
    shared_ptr<PipeTransport> client;
    try {
      client.reset(new PipeTransport(Pipe_));
    } catch (TTransportException& ttx) {
      if (ttx.getType() == TTransportException::INTERRUPTED) {
        throw;
      }

      // kick off the next connection before throwing
      initiateNamedConnect(lock);
      throw TTransportException(TTransportException::CLIENT_DISCONNECT, ttx.what());
    }
    // kick off the next connection before returning
    initiateNamedConnect(lock);
    return client; // success!
  }
  // if we got here, then we are in an error / shutdown case
  DWORD gle = GetLastError(); // save error before doing cleanup
  if(gle == ERROR_OPERATION_ABORTED) {
    TAutoCrit lock(pipe_protect_);    	// Needed to insure concurrent thread to be out of interrupt.
    throw TTransportException(TTransportException::INTERRUPTED, "PipeTransportServer: server interrupted, err: " + TOutput::strerror_s(gle));
  }
  throw TTransportException(TTransportException::NOT_OPEN, "PipeTransportServer: client connection failed, err: " + TOutput::strerror_s(gle));
}

void PipeTransportServer::interrupt() {
  if (impl_)
    impl_->interrupt();
}

void PipeTransportServer::close() {
  impl_.reset();
}

bool WindowsNamedPipeServer::createNamedPipe(const TAutoCrit& /*lockProof*/) {

  PSECURITY_DESCRIPTOR psd = nullptr;
  ULONG size = 0;

  if (!ConvertStringSecurityDescriptorToSecurityDescriptorA(securityDescriptor_.c_str(),
                                                            SDDL_REVISION_1, &psd, &size)) {
    DWORD lastError = GetLastError();
    throw TTransportException(TTransportException::NOT_OPEN, "PipeTransportServer::ConvertStringSecurityDescriptorToSecurityDescriptorA() failed, err:" + TOutput::strerror_s(lastError));
  }

  SECURITY_ATTRIBUTES sa;
  sa.nLength = sizeof(SECURITY_ATTRIBUTES);
  sa.lpSecurityDescriptor = psd;
  sa.bInheritHandle = FALSE;

  // Create an instance of the named pipe
  TAutoHandle hPipe(CreateNamedPipeA(pipename_.c_str(),        // pipe name
                                     PIPE_ACCESS_DUPLEX |      // read/write access
                                         FILE_FLAG_OVERLAPPED, // async mode
                                     PIPE_TYPE_BYTE |          // byte type pipe
                                         PIPE_READMODE_BYTE,   // byte read mode
                                     maxconns_,                // max. instances
                                     bufsize_,                 // output buffer size
                                     bufsize_,                 // input buffer size
                                     0,                        // client time-out
                                     &sa));                    // security attributes

  auto lastError = GetLastError();
  if (psd)
    LocalFree(psd);

  if (hPipe.h == INVALID_HANDLE_VALUE) {
    Pipe_.reset();
    throw TTransportException(TTransportException::NOT_OPEN, "TCreateNamedPipe() failed, err: " + TOutput::strerror_s(lastError));
  }

  Pipe_.reset(hPipe.release());
  return true;
}

//---------------------------------------------------------
// Accessors
//---------------------------------------------------------
std::string PipeTransportServer::getPipename() {
  return pipename_;
}

void PipeTransportServer::setPipename(const std::string& pipename) {
  if (pipename.find("\\\\") == std::string::npos)
    pipename_ = "\\\\.\\pipe\\" + pipename;
  else
    pipename_ = pipename;
}

int PipeTransportServer::getBufferSize() {
  return bufsize_;
}
void PipeTransportServer::setBufferSize(int bufsize) {
  bufsize_ = bufsize;
}

HANDLE PipeTransportServer::getPipeHandle() {
  return impl_ ? impl_->getPipeHandle() : INVALID_HANDLE_VALUE;
}
HANDLE PipeTransportServer::getWrtPipeHandle() {
  return impl_ ? impl_->getWrtPipeHandle() : INVALID_HANDLE_VALUE;
}
HANDLE PipeTransportServer::getClientRdPipeHandle() {
  return impl_ ? impl_->getClientRdPipeHandle() : INVALID_HANDLE_VALUE;
}
HANDLE PipeTransportServer::getClientWrtPipeHandle() {
  return impl_ ? impl_->getClientWrtPipeHandle() : INVALID_HANDLE_VALUE;
}

bool PipeTransportServer::getAnonymous() {
  return isAnonymous_;
}
void PipeTransportServer::setAnonymous(bool anon) {
  isAnonymous_ = anon;
}

void PipeTransportServer::setSecurityDescriptor(const std::string& securityDescriptor) {
  securityDescriptor_ = securityDescriptor;
}

void PipeTransportServer::setMaxConnections(uint32_t maxconnections) {
  if (maxconnections == 0)
    maxconns_ = 1;
  else if (maxconnections > PIPE_UNLIMITED_INSTANCES)
    maxconns_ = PIPE_UNLIMITED_INSTANCES;
  else
    maxconns_ = maxconnections;
}