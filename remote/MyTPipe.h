#ifndef JCEF_MYTPIPE_H
#define JCEF_MYTPIPE_H

#include <thrift/transport/TTransport.h>
#include <thrift/transport/TVirtualTransport.h>
#include <thrift/windows/Sync.h>
#include <thrift/TNonCopyable.h>
#include <thrift/windows/Sync.h>

using namespace apache::thrift::transport;
using namespace apache::thrift;

class MyTPipeImpl;

class MyTPipe : public TVirtualTransport<MyTPipe> {
 public:
  // Constructs a new pipe object.
  MyTPipe(std::shared_ptr<TConfiguration> config = nullptr);
  explicit MyTPipe(const char* pipename, std::shared_ptr<TConfiguration> config = nullptr);
  explicit MyTPipe(const std::string& pipename, std::shared_ptr<TConfiguration> config = nullptr);
  explicit MyTPipe(TAutoHandle& Pipe, std::shared_ptr<TConfiguration> config = nullptr); // this ctor will clear out / move from Pipe

  // Destroys the pipe object, closing it if necessary.
  virtual ~MyTPipe();

  // Returns whether the pipe is open & valid.
  bool isOpen() const override;

  // Checks whether more data is available in the pipe.
  bool peek() override;

  // Creates and opens the named/anonymous pipe.
  void open() override;

  // Shuts down communications on the pipe.
  void close() override;

  // Reads from the pipe.
  virtual uint32_t read(uint8_t* buf, uint32_t len);

  // Writes to the pipe.
  virtual void write(const uint8_t* buf, uint32_t len);

  // Accessors
  std::string getPipename();
  void setPipename(const std::string& pipename);
  HANDLE getPipeHandle(); // doubles as the read handle for anon pipe
  void setPipeHandle(HANDLE pipehandle);
  HANDLE getWrtPipeHandle();
  void setWrtPipeHandle(HANDLE pipehandle);
  long getConnTimeout();
  void setConnTimeout(long seconds);

  // this function is intended to be used in generic / template situations,
  // so its name needs to be the same as TPipeServer's
  HANDLE getNativeWaitHandle();

 private:
  std::shared_ptr<MyTPipeImpl> impl_;

  std::string pipename_;

  long TimeoutSeconds_;
  bool isAnonymous_;
};


#endif  // JCEF_MYTPIPE_H
