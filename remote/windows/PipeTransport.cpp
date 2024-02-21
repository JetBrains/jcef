#include "PipeTransport.h"

#include "thrift/transport/TTransportException.h"
#include "thrift/windows/Sync.h"

uint32_t pseudo_sync_read(HANDLE pipe, HANDLE event, uint8_t* buf, uint32_t len);
void pseudo_sync_write(HANDLE pipe, HANDLE event, const uint8_t* buf, uint32_t len);

class WindowsPipeImpl : apache::thrift::TNonCopyable {
 public:
  WindowsPipeImpl() {}
  virtual ~WindowsPipeImpl() {}
  virtual uint32_t read(uint8_t* buf, uint32_t len) = 0;
  virtual void write(const uint8_t* buf, uint32_t len) = 0;
  virtual HANDLE getPipeHandle() = 0; // doubles as the read handle for anon pipe
  virtual void setPipeHandle(HANDLE pipehandle) = 0;
  virtual HANDLE getWrtPipeHandle() { return INVALID_HANDLE_VALUE; }
  virtual void setWrtPipeHandle(HANDLE) {}
  virtual bool isBufferedDataAvailable() { return false; }
  virtual HANDLE getNativeWaitHandle() { return INVALID_HANDLE_VALUE; }
};

class WindowsNamedPipeImpl : public WindowsPipeImpl {
 public:
  explicit WindowsNamedPipeImpl(TAutoHandle &pipehandle) : Pipe_(pipehandle.release()) {}
  virtual ~WindowsNamedPipeImpl() {}
  virtual uint32_t read(uint8_t* buf, uint32_t len) {
    return pseudo_sync_read(Pipe_.h, read_event_.h, buf, len);
  }
  virtual void write(const uint8_t* buf, uint32_t len) {
    pseudo_sync_write(Pipe_.h, write_event_.h, buf, len);
  }

  virtual HANDLE getPipeHandle() { return Pipe_.h; }
  virtual void setPipeHandle(HANDLE pipehandle) { Pipe_.reset(pipehandle); }

 private:
  TManualResetEvent read_event_;
  TManualResetEvent write_event_;
  TAutoHandle Pipe_;
};

void pseudo_sync_write(HANDLE pipe, HANDLE event, const uint8_t* buf, uint32_t len) {
  OVERLAPPED tempOverlap;
  memset(&tempOverlap, 0, sizeof(tempOverlap));
  tempOverlap.hEvent = event;

  uint32_t written = 0;
  while (written < len) {
    BOOL result = ::WriteFile(pipe, buf + written, len - written, nullptr, &tempOverlap);

    if (result == FALSE && ::GetLastError() != ERROR_IO_PENDING) {
      DWORD err = ::GetLastError();
      throw TTransportException(TTransportException::UNKNOWN, "PipeTransport: WriteFile failed, err: " + TOutput::strerror_s(err));
    }

    DWORD bytes = 0;
    result = ::GetOverlappedResult(pipe, &tempOverlap, &bytes, TRUE);
    if (!result) {
      DWORD err = ::GetLastError();
      throw TTransportException(TTransportException::UNKNOWN, "PipeTransport: GetOverlappedResult failed, err: " + TOutput::strerror_s(err));
    }
    written += bytes;
  }
}

uint32_t pseudo_sync_read(HANDLE pipe, HANDLE event, uint8_t* buf, uint32_t len) {
  OVERLAPPED tempOverlap;
  memset(&tempOverlap, 0, sizeof(tempOverlap));
  tempOverlap.hEvent = event;

  BOOL result = ::ReadFile(pipe, buf, len, nullptr, &tempOverlap);

  int err = ::GetLastError();
  if (result == FALSE && err != ERROR_IO_PENDING) {
    throw TTransportException(TTransportException::UNKNOWN, "PipeTransport: ReadFile failed, err: " + TOutput::strerror_s(err));
  }

  DWORD bytes = 0;
  result = ::GetOverlappedResult(pipe, &tempOverlap, &bytes, TRUE);
  if (!result) {
    err = ::GetLastError();
    if (err == ERROR_BROKEN_PIPE)
      throw TTransportException(TTransportException::END_OF_FILE, "PipeTransport: GetOverlappedResult failed, err: broken pipe");
    throw TTransportException(TTransportException::UNKNOWN, "PipeTransport: GetOverlappedResult failed, err: " + TOutput::strerror_s(err));
  }
  return bytes;
}

//---- Constructors ----

PipeTransport::PipeTransport(const char* pipename, std::shared_ptr<TConfiguration> config) : TimeoutSeconds_(3),
      isAnonymous_(false), TVirtualTransport(config) {
  setPipename(pipename);
}

PipeTransport::PipeTransport(const std::string& pipename, std::shared_ptr<TConfiguration> config) : TimeoutSeconds_(3),
      isAnonymous_(false), TVirtualTransport(config) {
  setPipename(pipename);
}

PipeTransport::PipeTransport(TAutoHandle &Pipe, std::shared_ptr<TConfiguration> config)
    : impl_(new WindowsNamedPipeImpl(Pipe)), TimeoutSeconds_(3),
      isAnonymous_(false), TVirtualTransport(config) {
}

PipeTransport::PipeTransport(std::shared_ptr<TConfiguration> config) : TimeoutSeconds_(3), isAnonymous_(false),
      TVirtualTransport(config) {
}

PipeTransport::~PipeTransport() {
}

//---------------------------------------------------------
// Transport callbacks
//---------------------------------------------------------
bool PipeTransport::isOpen() const {
  return impl_.get() != nullptr;
}

bool PipeTransport::peek() {
  return isOpen();
}

void PipeTransport::open() {
  if (isOpen())
    return;

  TAutoHandle hPipe;
  do {
    DWORD flags = FILE_FLAG_OVERLAPPED; // async mode, so we can do reads at the same time as writes
    hPipe.reset(CreateFileA(pipename_.c_str(),
                            GENERIC_READ | GENERIC_WRITE,
                            0,             // no sharing
                            nullptr,          // default security attributes
                            OPEN_EXISTING, // opens existing pipe
                            flags,
                            nullptr)); // no template file

    if (hPipe.h != INVALID_HANDLE_VALUE)
      break; // success!

    if (::GetLastError() != ERROR_PIPE_BUSY) {
      int err = ::GetLastError();
      throw TTransportException(TTransportException::NOT_OPEN, "PipeTransport::open CreateFile failed, err: " + TOutput::strerror_s(err));
    }
  } while (::WaitNamedPipeA(pipename_.c_str(), TimeoutSeconds_ * 1000));

  if (hPipe.h == INVALID_HANDLE_VALUE) {
    int err = ::GetLastError();
    throw TTransportException(TTransportException::NOT_OPEN, "PipeTransport::open CreateFile failed, err: " + TOutput::strerror_s(err));
  }

  impl_.reset(new WindowsNamedPipeImpl(hPipe));
}

void PipeTransport::close() {
  impl_.reset();
}

uint32_t PipeTransport::read(uint8_t* buf, uint32_t len) {
  checkReadBytesAvailable(len);
  if (!isOpen())
    throw TTransportException(TTransportException::NOT_OPEN, "PipeTransport::read, called read on non-open pipe.");
  return impl_->read(buf, len);
}

void PipeTransport::write(const uint8_t* buf, uint32_t len) {
  if (!isOpen())
    throw TTransportException(TTransportException::NOT_OPEN, "PipeTransport::write, called write on non-open pipe.");
  impl_->write(buf, len);
}

//---------------------------------------------------------
// Accessors
//---------------------------------------------------------

std::string PipeTransport::getPipename() {
  return pipename_;
}

void PipeTransport::setPipename(const std::string& pipename) {
  if (pipename.find("\\\\") == std::string::npos)
    pipename_ = "\\\\.\\pipe\\" + pipename;
  else
    pipename_ = pipename;
}

HANDLE PipeTransport::getPipeHandle() {
  if (impl_)
    return impl_->getPipeHandle();
  return INVALID_HANDLE_VALUE;
}

void PipeTransport::setPipeHandle(HANDLE pipehandle) {
  if (isAnonymous_)
    impl_->setPipeHandle(pipehandle);
  else
  {
    TAutoHandle pipe(pipehandle);
    impl_.reset(new WindowsNamedPipeImpl(pipe));
  }
}

HANDLE PipeTransport::getWrtPipeHandle() {
  if (impl_)
    return impl_->getWrtPipeHandle();
  return INVALID_HANDLE_VALUE;
}

void PipeTransport::setWrtPipeHandle(HANDLE pipehandle) {
  if (impl_)
    impl_->setWrtPipeHandle(pipehandle);
}

HANDLE PipeTransport::getNativeWaitHandle() {
  if (impl_)
    return impl_->getNativeWaitHandle();
  return INVALID_HANDLE_VALUE;
}

long PipeTransport::getConnTimeout() {
  return TimeoutSeconds_;
}

void PipeTransport::setConnTimeout(long seconds) {
  TimeoutSeconds_ = seconds;
}
