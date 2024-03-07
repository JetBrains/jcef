package org.apache.thrift;

public class Logger {
    public boolean isTraceEnabled() { return false; }

    public void trace(String var1) {}

    public void trace(String var1, Object var2) {}

    public void trace(String var1, Object var2, Object var3) {}

    public void trace(String var1, Object... var2) {}

    public void trace(String var1, Throwable var2) {}

    public boolean isDebugEnabled() { return false; }

    public void debug(String var1) {}

    public void debug(String var1, Object var2) {}

    public void debug(String var1, Object var2, Object var3) {}

    public void debug(String var1, Object... var2) {}

    public void debug(String var1, Throwable var2) {}


    public boolean isInfoEnabled() { return false; }

    public void info(String var1) {}

    public void info(String var1, Object var2) {}

    public void info(String var1, Object var2, Object var3) {}

    public void info(String var1, Object... var2) {}

    public void info(String var1, Throwable var2) {}

    public boolean isWarnEnabled() { return false; }

    public void warn(String var1) {}

    public void warn(String var1, Object var2) {}

    public void warn(String var1, Object... var2) {}

    public void warn(String var1, Object var2, Object var3) {}

    public void warn(String var1, Throwable var2) {}

    public boolean isErrorEnabled() { return false; }

    public void error(String var1) {}

    public void error(String var1, Object var2) {}

    public void error(String var1, Object var2, Object var3) {}

    public void error(String var1, Object... var2) {}

    public void error(String var1, Throwable var2) {}
}
