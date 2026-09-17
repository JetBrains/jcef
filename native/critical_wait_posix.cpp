// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#include "critical_wait.h"

#include <sys/time.h>

// CriticalLock

CriticalLock::CriticalLock() {
  pthread_mutex_init(&lock_, nullptr);
}

CriticalLock::~CriticalLock() {
  pthread_mutex_destroy(&lock_);
}

void CriticalLock::Lock() {
  pthread_mutex_lock(&lock_);
}

void CriticalLock::Unlock() {
  pthread_mutex_unlock(&lock_);
}

// CriticalWait

CriticalWait::CriticalWait(CriticalLock* lock) : lock_(lock) {
  pthread_cond_init(&cond_, nullptr);
}

CriticalWait::~CriticalWait() {
  pthread_cond_destroy(&cond_);
}

void CriticalWait::Wait() {
  pthread_cond_wait(&cond_, &lock_->lock_);
}

bool CriticalWait::Wait(unsigned int maxWaitMs) {
  struct timeval tv;
  struct timespec ts;

  gettimeofday(&tv, nullptr);
  unsigned long long nsec =
      (unsigned long long)tv.tv_usec * 1000 + (unsigned long long)maxWaitMs * 1000000;
  ts.tv_sec = tv.tv_sec + (time_t)(nsec / 1000000000);
  ts.tv_nsec = (long)(nsec % 1000000000);

  int res = pthread_cond_timedwait(&cond_, &lock_->lock_, &ts);
  return res == 0;
}

void CriticalWait::WakeUp() {
  pthread_cond_signal(&cond_);
}
