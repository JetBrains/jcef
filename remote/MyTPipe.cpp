
#include "MyTPipe.h"


#include <thrift/transport/TTransportException.h>
#include <thrift/windows/OverlappedSubmissionThread.h>
#include <thrift/windows/Sync.h>

uint32_t pipe_read(HANDLE pipe, uint8_t* buf, uint32_t len);
void pipe_write(HANDLE pipe, const uint8_t* buf, uint32_t len);

uint32_t pseudo_sync_read(HANDLE pipe, HANDLE event, uint8_t* buf, uint32_t len);
void pseudo_sync_write(HANDLE pipe, HANDLE event, const uint8_t* buf, uint32_t len);

class MyTPipeImpl : apache::thrift::TNonCopyable {
 public:
  MyTPipeImpl() {}
  virtual ~MyTPipeImpl() {}
  virtual uint32_t read(uint8_t* buf, uint32_t len) = 0;
  virtual void write(const uint8_t* buf, uint32_t len) = 0;
  virtual HANDLE getPipeHandle() = 0; // doubles as the read handle for anon pipe
  virtual void setPipeHandle(HANDLE pipehandle) = 0;
  virtual HANDLE getWrtPipeHandle() { return INVALID_HANDLE_VALUE; }
  virtual void setWrtPipeHandle(HANDLE) {}
  virtual bool isBufferedDataAvailable() { return false; }
  virtual HANDLE getNativeWaitHandle() { return INVALID_HANDLE_VALUE; }
};

class TNamedPipeImpl : public MyTPipeImpl {
 public:
  explicit TNamedPipeImpl(TAutoHandle &pipehandle) : Pipe_(pipehandle.release()) {}
  virtual ~TNamedPipeImpl() {}
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


// If you want a select-like loop to work, use this subclass.  Be warned...
// the read implementation has several context switches, so this is slower
// than using the regular named pipe implementation
class MyTWaitableNamedPipeImpl : public MyTPipeImpl {
 public:
  explicit MyTWaitableNamedPipeImpl(TAutoHandle &pipehandle)
      : begin_unread_idx_(0), end_unread_idx_(0) {
    readOverlap_.action = TOverlappedWorkItem::READ;
    readOverlap_.h = pipehandle.h;
    cancelOverlap_.action = TOverlappedWorkItem::CANCELIO;
    cancelOverlap_.h = pipehandle.h;
    buffer_.resize(1024 /*arbitrary buffer size*/, '\0');
    beginAsyncRead(&buffer_[0], static_cast<uint32_t>(buffer_.size()));
    Pipe_.reset(pipehandle.release());
  }
  virtual ~MyTWaitableNamedPipeImpl() {
    // see if there is an outstanding read request
    if (begin_unread_idx_ == end_unread_idx_) {
      // if so, cancel it, and wait for the dead completion
      thread_->addWorkItem(&cancelOverlap_);
      readOverlap_.overlappedResults(false /*ignore errors*/);
    }
  }
  virtual uint32_t read(uint8_t* buf, uint32_t len);
  virtual void write(const uint8_t* buf, uint32_t len) {
    pseudo_sync_write(Pipe_.h, write_event_.h, buf, len);
  }
  
  virtual HANDLE getPipeHandle() { return Pipe_.h; }
  virtual void setPipeHandle(HANDLE pipehandle) { Pipe_.reset(pipehandle); }
  virtual bool isBufferedDataAvailable() { return begin_unread_idx_ < end_unread_idx_; }
  virtual HANDLE getNativeWaitHandle() { return ready_event_.h; }

 private:
  void beginAsyncRead(uint8_t* buf, uint32_t len);
  uint32_t endAsyncRead();

  TAutoOverlapThread thread_;
  TAutoHandle Pipe_;
  TOverlappedWorkItem readOverlap_;
  TOverlappedWorkItem cancelOverlap_;
  TManualResetEvent ready_event_;
  TManualResetEvent write_event_;
  std::vector<uint8_t> buffer_;
  uint32_t begin_unread_idx_;
  uint32_t end_unread_idx_;
};

void MyTWaitableNamedPipeImpl::beginAsyncRead(uint8_t* buf, uint32_t len) {
  begin_unread_idx_ = end_unread_idx_ = 0;
  readOverlap_.reset(buf, len, ready_event_.h);
  thread_->addWorkItem(&readOverlap_);
  if (readOverlap_.success == FALSE && readOverlap_.last_error != ERROR_IO_PENDING) {
    GlobalOutput.perror("TPipe ::ReadFile errored GLE=", readOverlap_.last_error);
    throw TTransportException(TTransportException::UNKNOWN, "TPipe: ReadFile failed");
  }
}

uint32_t MyTWaitableNamedPipeImpl::endAsyncRead() {
  return readOverlap_.overlappedResults();
}

uint32_t MyTWaitableNamedPipeImpl::read(uint8_t* buf, uint32_t len) {
  if (begin_unread_idx_ == end_unread_idx_) {
    end_unread_idx_ = endAsyncRead();
  }

  uint32_t __idxsize = end_unread_idx_ - begin_unread_idx_;
  uint32_t bytes_to_copy = (len < __idxsize) ? len : __idxsize;
  memcpy(buf, &buffer_[begin_unread_idx_], bytes_to_copy);
  begin_unread_idx_ += bytes_to_copy;
  if (begin_unread_idx_ != end_unread_idx_) {
    assert(len == bytes_to_copy);
    // we were able to fulfill the read with just the bytes in our
    // buffer, and we still have buffer left
    return bytes_to_copy;
  }
  uint32_t bytes_copied = bytes_to_copy;

  // all of the requested data has been read.  Kick off an async read for the next round.
  beginAsyncRead(&buffer_[0], static_cast<uint32_t>(buffer_.size()));

  return bytes_copied;
}

void pseudo_sync_write(HANDLE pipe, HANDLE event, const uint8_t* buf, uint32_t len) {
  OVERLAPPED tempOverlap;
  memset(&tempOverlap, 0, sizeof(tempOverlap));
  tempOverlap.hEvent = event;

  uint32_t written = 0;
  while (written < len) {
    BOOL result = ::WriteFile(pipe, buf + written, len - written, nullptr, &tempOverlap);

    if (result == FALSE && ::GetLastError() != ERROR_IO_PENDING) {
      GlobalOutput.perror("MyTPipe ::WriteFile errored GLE=", ::GetLastError());
      throw TTransportException(TTransportException::UNKNOWN, "MyTPipe: write failed");
    }

    DWORD bytes = 0;
    result = ::GetOverlappedResult(pipe, &tempOverlap, &bytes, TRUE);
    if (!result) {
      GlobalOutput.perror("MyTPipe ::GetOverlappedResult errored GLE=", ::GetLastError());
      throw TTransportException(TTransportException::UNKNOWN, "MyTPipe: GetOverlappedResult failed");
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
    GlobalOutput.perror("MyTPipe ::ReadFile errored GLE=", err);
    throw TTransportException(TTransportException::UNKNOWN, "MyTPipe: read failed");
  }

  DWORD bytes = 0;
  result = ::GetOverlappedResult(pipe, &tempOverlap, &bytes, TRUE);
  if (!result) {
    err = ::GetLastError();
    if (err == ERROR_BROKEN_PIPE)
      throw TTransportException(TTransportException::END_OF_FILE, "MyTPipe: GetOverlappedResult failed, ERROR_BROKEN_PIPE");
    GlobalOutput.perror("MyTPipe ::GetOverlappedResult errored GLE=", err);
    throw TTransportException(TTransportException::UNKNOWN, "MyTPipe: GetOverlappedResult failed", err);
  }
  return bytes;
}

//---- Constructors ----

MyTPipe::MyTPipe(const char* pipename, std::shared_ptr<TConfiguration> config) : TimeoutSeconds_(3),
      isAnonymous_(false), TVirtualTransport(config) {
  setPipename(pipename);
}

MyTPipe::MyTPipe(const std::string& pipename, std::shared_ptr<TConfiguration> config) : TimeoutSeconds_(3),
      isAnonymous_(false), TVirtualTransport(config) {
  setPipename(pipename);
}

MyTPipe::MyTPipe(TAutoHandle &Pipe, std::shared_ptr<TConfiguration> config)
    : impl_(new TNamedPipeImpl(Pipe)), TimeoutSeconds_(3),
      isAnonymous_(false), TVirtualTransport(config) {
}

MyTPipe::MyTPipe(std::shared_ptr<TConfiguration> config) : TimeoutSeconds_(3), isAnonymous_(false),
      TVirtualTransport(config) {
}

MyTPipe::~MyTPipe() {
}

//---------------------------------------------------------
// Transport callbacks
//---------------------------------------------------------
bool MyTPipe::isOpen() const {
  return impl_.get() != nullptr;
}

bool MyTPipe::peek() {
  return isOpen();
}

void MyTPipe::open() {
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
      GlobalOutput.perror("MyTPipe::open ::CreateFile errored GLE=", ::GetLastError());
      throw TTransportException(TTransportException::NOT_OPEN, "Unable to open pipe");
    }
  } while (::WaitNamedPipeA(pipename_.c_str(), TimeoutSeconds_ * 1000));

  if (hPipe.h == INVALID_HANDLE_VALUE) {
    GlobalOutput.perror("MyTPipe::open ::CreateFile errored GLE=", ::GetLastError());
    throw TTransportException(TTransportException::NOT_OPEN, "Unable to open pipe");
  }

  impl_.reset(new TNamedPipeImpl(hPipe));
}

void MyTPipe::close() {
  impl_.reset();
}

uint32_t MyTPipe::read(uint8_t* buf, uint32_t len) {
  checkReadBytesAvailable(len);
  if (!isOpen())
    throw TTransportException(TTransportException::NOT_OPEN, "Called read on non-open pipe");
  return impl_->read(buf, len);
}

uint32_t pipe_read(HANDLE pipe, uint8_t* buf, uint32_t len) {
  DWORD cbRead;
  int fSuccess = ReadFile(pipe,    // pipe handle
                          buf,     // buffer to receive reply
                          len,     // size of buffer
                          &cbRead, // number of bytes read
                          nullptr);   // not overlapped

  if (!fSuccess && GetLastError() != ERROR_MORE_DATA)
    return 0; // No more data, possibly because client disconnected.

  return cbRead;
}

void MyTPipe::write(const uint8_t* buf, uint32_t len) {
  if (!isOpen())
    throw TTransportException(TTransportException::NOT_OPEN, "Called write on non-open pipe");
  impl_->write(buf, len);
}

void pipe_write(HANDLE pipe, const uint8_t* buf, uint32_t len) {
  DWORD cbWritten;
  int fSuccess = WriteFile(pipe,       // pipe handle
                           buf,        // message
                           len,        // message length
                           &cbWritten, // bytes written
                           nullptr);      // not overlapped

  if (!fSuccess)
    throw TTransportException(TTransportException::NOT_OPEN, "Write to pipe failed");
}

//---------------------------------------------------------
// Accessors
//---------------------------------------------------------

std::string MyTPipe::getPipename() {
  return pipename_;
}

void MyTPipe::setPipename(const std::string& pipename) {
  if (pipename.find("\\\\") == std::string::npos)
    pipename_ = "\\\\.\\pipe\\" + pipename;
  else
    pipename_ = pipename;
}

HANDLE MyTPipe::getPipeHandle() {
  if (impl_)
    return impl_->getPipeHandle();
  return INVALID_HANDLE_VALUE;
}

void MyTPipe::setPipeHandle(HANDLE pipehandle) {
  if (isAnonymous_)
    impl_->setPipeHandle(pipehandle);
  else
  {
    TAutoHandle pipe(pipehandle);
    impl_.reset(new TNamedPipeImpl(pipe));
  }
}

HANDLE MyTPipe::getWrtPipeHandle() {
  if (impl_)
    return impl_->getWrtPipeHandle();
  return INVALID_HANDLE_VALUE;
}

void MyTPipe::setWrtPipeHandle(HANDLE pipehandle) {
  if (impl_)
    impl_->setWrtPipeHandle(pipehandle);
}

HANDLE MyTPipe::getNativeWaitHandle() {
  if (impl_)
    return impl_->getNativeWaitHandle();
  return INVALID_HANDLE_VALUE;
}

long MyTPipe::getConnTimeout() {
  return TimeoutSeconds_;
}

void MyTPipe::setConnTimeout(long seconds) {
  TimeoutSeconds_ = seconds;
}
