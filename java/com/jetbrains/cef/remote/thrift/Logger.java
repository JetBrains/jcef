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

    public void error(String format, Object... args) {
        log(Level.ERROR, format, args);
    }

    public void warn(String message) {
        CefLog.Warn(makeMessage(message));
    }

    public void warn(String message, Throwable t) {
        CefLog.Warn(makeMessage(message, t));
    }

    public void warn(String format, Object... args) {
        log(Level.WARN, format, args);
    }

    public void debug(String message) {
        CefLog.Debug(makeMessage(message));
    }

    public void debug(String message, Throwable t) {
        CefLog.Debug(makeMessage(message, t));
    }

    public void debug(String format, Object... args) {
        log(Level.DEBUG, format, args);
    }

    public void info(String message) {
        CefLog.Info(makeMessage(message));
    }

    public void info(String message, Throwable t) {
        CefLog.Info(makeMessage(message, t));
    }

    public void info(String format, Object... args) {
        log(Level.INFO, format, args);
    }

    public void trace(String message) {
        CefLog.Debug(makeMessage(message));
    }

    public void trace(String message, Throwable t) {
        CefLog.Debug(makeMessage(message, t));
    }

    public void trace(String format, Object... args) {
        log(Level.DEBUG, format, args);
    }

    private enum Level {
        ERROR,
        WARN,
        DEBUG,
        INFO
    }

    // Mimics the slf4j "{}" placeholder style used by Thrift, including the convention that a
    // trailing Throwable argument that is not consumed by a placeholder is logged separately.
    private void log(Level level, String format, Object... args) {
        Throwable t = extractThrowable(format, args);
        String message = formatMessage(format, args);
        switch (level) {
            case ERROR:
                CefLog.Error(t != null ? makeMessage(message, t) : makeMessage(message));
                break;
            case WARN:
                CefLog.Warn(t != null ? makeMessage(message, t) : makeMessage(message));
                break;
            case DEBUG:
                CefLog.Debug(t != null ? makeMessage(message, t) : makeMessage(message));
                break;
            case INFO:
                CefLog.Info(t != null ? makeMessage(message, t) : makeMessage(message));
                break;
        }
    }

    private static Throwable extractThrowable(String format, Object... args) {
        if (args == null || args.length == 0) {
            return null;
        }
        if (args.length > countPlaceholders(format) && args[args.length - 1] instanceof Throwable) {
            return (Throwable) args[args.length - 1];
        }
        return null;
    }

    private static String formatMessage(String format, Object... args) {
        if (args == null || args.length == 0) {
            return format;
        }
        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        while (i < format.length()) {
            if (argIndex < args.length
                    && format.charAt(i) == '{'
                    && i + 1 < format.length()
                    && format.charAt(i + 1) == '}') {
                sb.append(args[argIndex]);
                argIndex++;
                i += 2;
            } else {
                sb.append(format.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    private static int countPlaceholders(String format) {
        int count = 0;
        int idx = format.indexOf("{}");
        while (idx != -1) {
            count++;
            idx = format.indexOf("{}", idx + 2);
        }
        return count;
    }

    String makeMessage(String message) {
        return MessageFormat.format("[thrift:{0}] {1}", name, message);
    }

    String makeMessage(String message, Throwable e) {
        return MessageFormat.format("[thrift:{0}] {1} - {2}", name, message, e);
    }
}
