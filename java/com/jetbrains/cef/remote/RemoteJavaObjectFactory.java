package com.jetbrains.cef.remote;

import org.cef.misc.CefLog;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public class RemoteJavaObjectFactory<T> {
    private final Map<Integer, T> INSTANCES = new ConcurrentHashMap<>();
    private AtomicInteger COUNTER = new AtomicInteger(0);

    public T create(Function<Integer, T> creator) {
        final int newId = COUNTER.getAndIncrement();
        T result = creator.apply(newId);
        INSTANCES.put(newId, result);
        return result;
    }

    public T find(int id) {
        return INSTANCES.get(id);
    }

    public T find(Predicate<T> predicate) {
        for (T inst: INSTANCES.values())
            if (predicate.test(inst))
                return inst;
        return null;
    }

    public T get(int id) {
        T result = INSTANCES.get(id);
        if (result == null)
            CefLog.Error("Can't find instance in '%s' by id %d", INSTANCES, id);
        return result;
    }

    public void dispose(int id) { INSTANCES.remove(id); }

    protected String getGenericName() {
        return ((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getTypeName();
    }
}
