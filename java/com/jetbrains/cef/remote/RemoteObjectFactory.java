package com.jetbrains.cef.remote;

import org.cef.misc.CefLog;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

public class RemoteObjectFactory<T> {
    private final ArrayList<T> INSTANCES = new ArrayList<T>();

    public T create(Function<Integer, T> creator) {
        synchronized (INSTANCES) {
            int freeIndex = 0;
            while (freeIndex < INSTANCES.size() && INSTANCES.get(freeIndex) != null)
                ++freeIndex;

            T result = creator.apply(freeIndex);
            if (freeIndex == INSTANCES.size())
                INSTANCES.add(result);
            else
                INSTANCES.set(freeIndex, result);

            return result;
        }
    }

    public T find(int id) {
        synchronized (INSTANCES) {
            return id >= INSTANCES.size() ? null : INSTANCES.get(id);
        }
    }

    public T find(Predicate<T> predicate) {
        synchronized (INSTANCES) {
            for (T inst: INSTANCES)
                if (predicate.test(inst))
                    return inst;
            return null;
        }
    }

    public T get(int id) {
        synchronized (INSTANCES) {
            T result = id >= INSTANCES.size() ? null : INSTANCES.get(id);
            if (result == null)
                CefLog.Error("Can't find instance of '%s' by id %d", getGenericName(), id);
            return result;
        }
    }

    public void dispose(int id) {
        synchronized (INSTANCES) {
            INSTANCES.set(id, null);
        }
    }

    protected String getGenericName() {
        return ((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getTypeName();
    }
}
