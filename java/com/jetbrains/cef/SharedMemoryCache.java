package com.jetbrains.cef;

import org.cef.misc.Utils;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class SharedMemoryCache {
    private static final int CLEAN_CACHE_TIME_MS = Utils.getInteger("jcef.remote.clean_shmem_cache_time_ms", 10*1000); // 10 sec
    private static final int CACHE_SIZE = Utils.getInteger("jcef.remote.shmem_cache_size", 2); // 2 is quite enough for the current frame and backbuffer

    private final Map<String, SharedMemory.WithRaster> myCache = new ConcurrentHashMap<>();

    public SharedMemory.WithRaster get(String sharedMemName, long sharedMemHandle) {
        SharedMemory.WithRaster mem = myCache.get(sharedMemName);
        if (mem == null) {
            cleanCacheIfNecessary();
            mem = new SharedMemory.WithRaster(sharedMemName, sharedMemHandle);
            myCache.put(sharedMemName, mem);
        }
        mem.lasUsedMs = System.currentTimeMillis();
        return mem;
    }

    private void cleanCacheIfNecessary() {
        final long timeMs = System.currentTimeMillis();
        if (myCache.size() < CACHE_SIZE)
            return;

        ArrayList<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, SharedMemory.WithRaster> item: myCache.entrySet())
            if (timeMs - item.getValue().lasUsedMs > CLEAN_CACHE_TIME_MS)
                toRemove.add(item.getKey());

        for (String name: toRemove)
            myCache.remove(name);
    }
}
