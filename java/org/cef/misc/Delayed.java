package org.cef.misc;

import com.jetbrains.cef.remote.CefServer;

import java.util.ArrayList;

// Helper class used for delayed actions.
public class Delayed {
    private static final String TRACE_FILTER = Utils.getString("JCEF_DELAYED_TRACE_FILTER");
    private final ArrayList<Runnable> myDelayedActions = new ArrayList<>();
    private final String myName;
    private final boolean myDoTrace; // just for debugging convenience
    private volatile boolean myIsFinished = false;
    private volatile boolean myIsDisposed = false;
    private Runnable myBeforeDelayed = null;

    public Delayed() {
        this(null);
    }

    public Delayed(String name) {
        myName = name == null || name.isEmpty() ? "Delayed[" + this + "]" : "Delayed[" + name + "]";
        myDoTrace = TRACE_FILTER != null && (TRACE_FILTER.isEmpty() || "all".equalsIgnoreCase(TRACE_FILTER) || myName.contains(TRACE_FILTER));
    }

    public boolean isFinished() { return myIsFinished; }
    public boolean isDisposed() { return myIsDisposed; }

    public void finishOnConnection(CefServer server) {
        finishOnConnection(server, false);
    }
    public void finishOnConnection(CefServer server, boolean isFirst) {
        finishOnConnection(server, isFirst, null);
    }
    public void finishOnConnection(CefServer server, Runnable before) {
        finishOnConnection(server, false, before);
    }
    public void finishOnConnection(CefServer server, boolean isFirst, Runnable before) {
        myBeforeDelayed = before;
        server.onConnected(()->{
            finishNow();
        }, myName + ".executeAllOnConnection", isFirst);
    }

    public void finishNow() {
        if (myDoTrace)
            CefLog.Debug("%s: finish now", myName);
        if (myBeforeDelayed != null)
            myBeforeDelayed.run();
        synchronized (myDelayedActions) {
            myDelayedActions.forEach(r -> r.run());
            myDelayedActions.clear();
            myIsFinished = true;
        }
    }

    public boolean runOrDelay(Runnable action) {
        return runOrDelay(action, null);
    }

    public boolean runOrDelay(Runnable action, String name) {
        return runOrDelay(action, name, false);
    }

    // Returns true when action was executed immediately (for example, when server already was connected).
    public boolean runOrDelay(Runnable action, String name, boolean first) {
        if (action == null)
            return false;

        synchronized (myDelayedActions) {
            if (myIsDisposed)
                return false;
            if (myIsFinished) {
                if (myDoTrace)
                    CefLog.Debug("%s: run '%s'%s", myName, name == null || name.isEmpty() ? action : name, first ? " (first)" : "");
                action.run();
                return true;
            }
            if (myDoTrace)
                CefLog.Debug("%s: delay '%s'%s", myName, name == null || name.isEmpty() ? action : name, first ? " (first)" : "");
            if (first)
                myDelayedActions.add(0, action);
            else
                myDelayedActions.add(action);
        }
        return false;
    }

    public void dispose() {
        if (myDoTrace)
            CefLog.Debug("%s: dispose", myName);
        synchronized (myDelayedActions) {
            myIsDisposed = true;
            myDelayedActions.clear();
        }
    }
}