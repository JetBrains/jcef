package org.cef.misc;

import org.cef.CefSettings;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

// NOTE: java.util.Logger with FileHandler(CefSettings.log_file) doesn't work properly along with CEF-logging
// so use this separate primitive logger
// TODO: support log4j or similar
public class CefLog {
    public static CefLog INSTANCE;
    private static final SimpleDateFormat ourTimeFormat = new SimpleDateFormat("mm:ss:SSS");

    private PrintStream myPrintStream;
    private CefSettings.LogSeverity mySeverity;


    public static void initFileLogger(CefSettings settings) {
        if (settings.log_file == null) {
            return;
        }
        if (settings.log_severity == CefSettings.LogSeverity.LOGSEVERITY_DISABLE) {
            return;
        }
        if (settings.log_severity == CefSettings.LogSeverity.LOGSEVERITY_DEFAULT) {
            System.out.println("Don't initialize file logger (because severity==default)");
            return;
        }

        System.out.println("Initialize file logger, severity=" + settings.log_severity + ", path='" + settings.log_file + "'");
        try {
            INSTANCE = new CefLog(settings.log_file, settings.log_severity);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private CefLog(String log_file, CefSettings.LogSeverity log_severity) throws FileNotFoundException {
        myPrintStream = new PrintStream(new FileOutputStream(log_file, true), true);
        mySeverity = log_severity;
    }

    private static String shortSeverity(CefSettings.LogSeverity log_severity) {
        switch (log_severity) {
            case LOGSEVERITY_DEFAULT:
                return "D";
            case LOGSEVERITY_VERBOSE:
                return "V";
            case LOGSEVERITY_INFO:
                return "I";
            case LOGSEVERITY_WARNING:
                return "W";
            case LOGSEVERITY_ERROR:
                return "E";
            case LOGSEVERITY_FATAL:
                return "F";
            case LOGSEVERITY_DISABLE:
                return "d";
        }
        return "";
    }

    public void debug(String msg) { log(CefSettings.LogSeverity.LOGSEVERITY_VERBOSE, msg); }
    public void info(String msg) { log(CefSettings.LogSeverity.LOGSEVERITY_INFO, msg); }
    public void warn(String msg) { log(CefSettings.LogSeverity.LOGSEVERITY_WARNING, msg); }
    public void error(String msg) { log(CefSettings.LogSeverity.LOGSEVERITY_ERROR, msg); }

    public void debug(String msg, Object... args) { log(CefSettings.LogSeverity.LOGSEVERITY_VERBOSE, msg, args); }
    public void info(String msg, Object... args) { log(CefSettings.LogSeverity.LOGSEVERITY_INFO, msg, args); }
    public void warn(String msg, Object... args) { log(CefSettings.LogSeverity.LOGSEVERITY_WARNING, msg, args); }
    public void error(String msg, Object... args) { log(CefSettings.LogSeverity.LOGSEVERITY_ERROR, msg, args); }

    public void log(CefSettings.LogSeverity log_severity, String msg) {
        log(log_severity, msg, null);
    }
    public void log(CefSettings.LogSeverity log_severity, String msg, Object... args) {
        if (mySeverity.compareTo(log_severity) < 0)
            return;
        myPrintStream.printf("JCEF_%s(%s): %s\n", shortSeverity(log_severity), ourTimeFormat.format(new Date()),
                args == null || args.length == 0 ? msg : String.format(msg, args));
    }

    static public void Debug(String msg, Object... args) { Log(CefSettings.LogSeverity.LOGSEVERITY_VERBOSE, msg, args); }
    static public void Info(String msg, Object... args) { Log(CefSettings.LogSeverity.LOGSEVERITY_INFO, msg, args); }
    static public void Warn(String msg, Object... args) { Log(CefSettings.LogSeverity.LOGSEVERITY_WARNING, msg, args); }
    static public void Error(String msg, Object... args) { Log(CefSettings.LogSeverity.LOGSEVERITY_ERROR, msg, args); }

    static public void Log(CefSettings.LogSeverity log_severity, String msg, Object... args) {
        if (msg == null)
            return;

        if (INSTANCE != null)
            INSTANCE.log(log_severity, msg, args);
        else {
            if (!msg.endsWith("\n"))
                msg += "\n";
            System.err.printf("JCEF_%s(%s): %s\n", shortSeverity(log_severity), ourTimeFormat.format(new Date()),
                    args == null || args.length == 0 ? msg : String.format(msg, args));
        }
    }
}
