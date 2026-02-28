package org.cef.misc;

import com.jetbrains.cef.remote.CefServer;

import java.util.ArrayList;

// Helper class used for delayed actions.
public class Delayed {
    private final ArrayList<Runnable> myDelayedActions = new ArrayList<>();
    private final String myName;
    private volatile boolean myIsFinished = false;
    private volatile boolean myIsDisposed = false;
    private Runnable myBeforeDelayed = null;

    public Delayed() {
        this(null);
    }

    public Delayed(String name) {
        myName = name == null || name.isEmpty() ? "Delayed[" + this + "]" : "Delayed[" + name + "]";
    }

    public boolean isFinished() { return myIsFinished; }

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
        if (myBeforeDelayed != null)
            myBeforeDelayed.run();
        synchronized (myDelayedActions) {
            myDelayedActions.forEach(r -> r.run());
            myDelayedActions.clear();
            myIsFinished = true;
        }
    }

    // TODO: rename to runOrDelay
    public boolean runOrSchedule(Runnable action) {
        return runOrSchedule(action, null);
    }

    public boolean runOrSchedule(Runnable action, String name) {
        return runOrSchedule(action, name, false);
    }

    // Returns true when action was executed immediately (for example, when server already was connected).
    public boolean runOrSchedule(Runnable action, String name, boolean first) {
        if (action == null)
            return false;

        synchronized (myDelayedActions) {
            if (myIsDisposed)
                return false;
            if (myIsFinished) {
                action.run();
                return true;
            }
            CefLog.Debug("%s: schedule '%s'%s", myName, name == null || name.isEmpty() ? action : name, first ? " (first)" : "");
            if (first)
                myDelayedActions.add(0, action);
            else
                myDelayedActions.add(action);
        }
        return false;
    }

    public void dispose() {
        synchronized (myDelayedActions) {
            myIsDisposed = true;
            myDelayedActions.clear();
        }
    }
}