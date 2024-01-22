#include "SharedBufferManager.h"

#include "../CefUtils.h"
#include "../log/Log.h"

using namespace boost::interprocess;

SharedBuffer::SharedBuffer(std::string uid, size_t len)
    : myUid(uid), myLen(len) {
  Log::trace("Allocate shared buffer '%s' | %d bytes", uid.c_str(), len);
  LNDCTT(5*1000);

  const unsigned int additionalBytes = 1024;
  if (shared_memory_object::remove(uid.c_str()))
    Log::debug("Removed shared mem '%s'", uid.c_str());
  // TODO: check allocation errors, catch and process exceptions
#ifdef WIN32
  mySharedSegment = new managed_windows_shared_memory(create_only, uid.c_str(),
                                              len + additionalBytes);
#else
  mySharedSegment = new managed_shared_memory(create_only, uid.c_str(),
                                              len + additionalBytes);
#endif

  mySharedMem = mySharedSegment->allocate(len);
  mySharedMemHandle = mySharedSegment->get_handle_from_address(mySharedMem);

  if (named_mutex::remove(myUid.c_str()))
    Log::debug("Removed shared mutex '%s'", uid.c_str());
  myMutex = new named_mutex(create_only, myUid.c_str());
}

void SharedBuffer::_releaseShared() {
  if (mySharedSegment != nullptr) {
    // TODO: remove unnecessary dealloc (since going to remove whole shared
    // segment)
    mySharedSegment->deallocate(mySharedMem);
    delete mySharedSegment;

    mySharedSegment = nullptr;
    mySharedMem = nullptr;
  }
  if (myMutex != nullptr) {
    delete myMutex;
    myMutex = nullptr;
  }
  shared_memory_object::remove(myUid.c_str());
  named_mutex::remove(myUid.c_str());
}

void SharedBuffer::lock() {
  if (myMutex != nullptr)
    myMutex->lock();
}

bool SharedBuffer::tryLock() {
  return myMutex != nullptr ? myMutex->try_lock() : false;
}

void SharedBuffer::unlock() {
  if (myMutex != nullptr)
    myMutex->unlock();
}

SharedBuffer::~SharedBuffer() {
  _releaseShared();
}

SharedBufferManager::SharedBufferManager(int cid, int bid) {
  myPrefix = string_format("CefRasterC%dB%d_", cid, bid);
}

SharedBuffer* SharedBufferManager::_getOrCreateBuffer(size_t size, int index) {
  SharedBuffer* buf = myPool[index];
  if (buf == nullptr || buf->size() < size) {
    if (buf != nullptr)
      delete buf;
    myPool[index] = buf =
        new SharedBuffer(myPrefix + string_format("%d_%d", size, index), (int)size);
  }
  return buf;
}

SharedBuffer& SharedBufferManager::getLockedBuffer(size_t size) {
  myLastUsed = (myLastUsed + 1) % POOL_SIZE;
  SharedBuffer* buf = _getOrCreateBuffer(size, myLastUsed);

  if (!buf->tryLock()) {
    // It seems that selected buffer is used now. Select another.
    myLastUsed = (myLastUsed + 1) % POOL_SIZE;
    buf = _getOrCreateBuffer(size, myLastUsed);
    buf->lock();
  }

  return *buf;
}

SharedBufferManager::~SharedBufferManager() {
  for (int c = 0; c < POOL_SIZE; ++c)
    if (myPool[c] != nullptr) {
      delete myPool[c];
      myPool[c] = nullptr;
    }
}
