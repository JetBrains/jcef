package com.jetbrains.cef.remote;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

public class MultiHandler<T> {
    private final ArrayList<T> myHandlers = new ArrayList<T>();

    public MultiHandler<T> addHandler(T handler) {
        synchronized (myHandlers) {
            myHandlers.add(handler);
        }
        return this;
    }

    public boolean removeHandler(T handler) {
        synchronized (myHandlers) {
            return myHandlers.remove(handler);
        }
    }

    public void removeAllHandlers() {
        synchronized (myHandlers) {
            myHandlers.clear();
        }
    }

    public void handle(Consumer<T> func) {
        synchronized (myHandlers) {
            for (T h: myHandlers)
                func.accept(h);
        }
    }

    // Invokes all handlers.
    // Returns true if at least one of handlers returns true.
    public boolean handleBool(Function<T, Boolean> func) {
        synchronized (myHandlers) {
            boolean result = false;
            for (T h: myHandlers)
                result |= func.apply(h);
            return result;
        }
    }
}
