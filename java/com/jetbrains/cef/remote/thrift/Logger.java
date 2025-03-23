package com.jetbrains.cef.remote.thrift;

import org.cef.misc.CefLog;

import java.text.MessageFormat;

public class Logger {
    private final String name;

    public Logger(String name) {
        this.name = name;
    }

    public void error(String message) {
        CefLog.Error(makeMessage(message));
    }

    public void error(String message, Throwable t) {
        CefLog.Error(makeMessage(message, t));
    }

    public void warn(String message) {
        CefLog.Warn(makeMessage(message));
    }

    public void warn(String message, Throwable t) {
        CefLog.Warn(makeMessage(message, t));
    }

    public void debug(String message) {
        CefLog.Debug(makeMessage(message));
    }

    public void debug(String message, Throwable t) {
        CefLog.Debug(makeMessage(message, t));
    }

    public void info(String message) {
        CefLog.Info(makeMessage(message));
    }

    public void info(String message, Throwable t) {
        CefLog.Info(makeMessage(message, t));
    }

    String makeMessage(String message) {
        return MessageFormat.format("[thrift:{0}] {1}", name, message);
    }

    String makeMessage(String message, Throwable e) {
        return MessageFormat.format("[thrift:{0}] {1} - {2}", name, message, e);
    }
}


