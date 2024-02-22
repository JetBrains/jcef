#include "SharedBufferManager.h"

#include "../Utils.h"
#include "../log/Log.h"

using namespace boost::interprocess;

namespace {
  size_t nearestMemorySize(size_t len) {
    constexpr int latticeSizeBits = 19; // i.e. 512 Kb
    return ((len >> latticeSizeBits) + 1) << latticeSizeBits;
  }
}

SharedBuffer::SharedBuffer(std::string uid, size_t len)
    : myUid(uid), myLen(len) {
  Log::trace("Allocate shared buffer '%s' | %.2f Mb", uid.c_str(), len/(1024*1024.f));
  const Clock::time_point startTime = Clock::now();
  const unsigned int additionalBytes = 256;
  shared_memory_object::remove(uid.c_str());

  const Clock::time_point t1 = Clock::now();
  // TODO: check allocation errors, catch and process exceptions
#ifdef WIN32
  mySharedSegment = new managed_windows_shared_memory(create_only, uid.c_str(),
                                              len + additionalBytes);
#else
  mySharedSegment = new managed_shared_memory(create_only, uid.c_str(),len + additionalBytes);
#endif
  const Clock::time_point t2 = Clock::now();
  mySharedMem = mySharedSegment->allocate(len);
  mySharedMemHandle = mySharedSegment->get_handle_from_address(mySharedMem);

  const Clock::time_point t3 = Clock::now();
  named_mutex::remove(myUid.c_str());

  const Clock::time_point t4 = Clock::now();
  myMutex = new named_mutex(create_only, myUid.c_str());

  if (Log::isTraceEnabled()) {
    const Clock::time_point entTime = Clock::now();
    const long spentMs = (long)std::chrono::duration_cast<std::chrono::microseconds>(entTime - startTime).count();
    if (spentMs > 5*1000) {
      Duration d1 = std::chrono::duration_cast<std::chrono::microseconds>(t1 - startTime);
      Duration d2 = std::chrono::duration_cast<std::chrono::microseconds>(t2 - t1);
      Duration d3 = std::chrono::duration_cast<std::chrono::microseconds>(t3 - t2);
      Duration d4 = std::chrono::duration_cast<std::chrono::microseconds>(t4 - t3);
      Duration d5 = std::chrono::duration_cast<std::chrono::microseconds>(entTime - t4);
      Log::trace("\t SharedBuffer '%s' (%d bytes), ctor spent mcs: remove mem %d; ctor %d; alloc %d; remove mutex %d; mutex ctor %d",
                 uid.c_str(), len, (int)d1.count(), (int)d2.count(), (int)d3.count(), (int)d4.count(), (int)d5.count());
    }
  }
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

SharedBufferManager::SharedBufferManager(int bid) {
  myPrefix = string_format("CefRasterB%d_", bid);
}

SharedBuffer* SharedBufferManager::_getOrCreateBuffer(size_t size, int index) {
  SharedBuffer* buf = myPool[index];
  if (buf == nullptr || buf->size() < size) {
    if (buf != nullptr)
      delete buf;
    myPool[index] = buf =
        new SharedBuffer(myPrefix + string_format("%d_%d", size, index), nearestMemorySize(size));
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
