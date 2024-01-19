#ifndef JCEF_PIPETRANSPORTSERVER_H
#define JCEF_PIPETRANSPORTSERVER_H

#include <memory>
#include "thrift/transport/TServerTransport.h"

#define TPIPE_SERVER_MAX_CONNS_DEFAULT PIPE_UNLIMITED_INSTANCES

using namespace apache::thrift::transport;
using namespace apache::thrift;

class WindowsPipeServerImpl;

// This is slightly modified version of apache::thrift::transport::TPipe
class PipeTransportServer : public TServerTransport {
 public:
  PipeTransportServer(const std::string& pipename, uint32_t bufsize);
  PipeTransportServer(const std::string& pipename, uint32_t bufsize, uint32_t maxconnections);
  PipeTransportServer(const std::string& pipename,
              uint32_t bufsize,
              uint32_t maxconnections,
              const std::string& securityDescriptor);
  PipeTransportServer(const std::string& pipename);

  virtual ~PipeTransportServer();

  bool isOpen() const override;

  // Standard transport callbacks
  void interrupt() override;
  void close() override;
  void listen() override;

  // Accessors
  std::string getPipename();
  void setPipename(const std::string& pipename);
  int getBufferSize();
  void setBufferSize(int bufsize);
  HANDLE getPipeHandle(); // Named Pipe R/W -or- Anonymous pipe Read handle
  HANDLE getWrtPipeHandle();
  HANDLE getClientRdPipeHandle();
  HANDLE getClientWrtPipeHandle();
  bool getAnonymous();
  void setAnonymous(bool anon);
  void setMaxConnections(uint32_t maxconnections);
  void setSecurityDescriptor(const std::string& securityDescriptor);

  // this function is intended to be used in generic / template situations,
  // so its name needs to be the same as TPipe's
  HANDLE getNativeWaitHandle();

 protected:
  virtual std::shared_ptr<TTransport> acceptImpl();

 private:
  std::shared_ptr<WindowsPipeServerImpl> impl_;

  std::string pipename_;
  std::string securityDescriptor_;
  uint32_t bufsize_;
  uint32_t maxconns_;
  bool isAnonymous_;
};


#endif  // JCEF_PIPETRANSPORTSERVER_H
