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

    public static void init(CefSettings settings) {
        if (settings.log_file != null
            && settings.log_severity != CefSettings.LogSeverity.LOGSEVERITY_DISABLE
            && settings.log_severity != CefSettings.LogSeverity.LOGSEVERITY_DEFAULT
        ) {
            try {
                System.out.println("Initialize file logger, severity=" + settings.log_severity + ", path='" + settings.log_file + "'");
                PrintStream ps = new PrintStream(new FileOutputStream(settings.log_file, true), true);
                INSTANCE = new CefLog(ps, settings.log_severity);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (INSTANCE == null)
            INSTANCE = new CefLog(System.err, settings.log_severity);
    }

    private CefLog(PrintStream ps, CefSettings.LogSeverity log_severity) {
        myPrintStream = ps;
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
        if (msg == null)
            return;
        if (mySeverity.compareTo(log_severity) <= 0) {
            myPrintStream.printf("JCEF_%s(%s): %s\n", shortSeverity(log_severity), ourTimeFormat.format(new Date()),
                    args == null || args.length == 0 ? msg : String.format(msg, args));
        }
    }

    static public void Debug(String msg, Object... args) { Log(CefSettings.LogSeverity.LOGSEVERITY_VERBOSE, msg, args); }
    static public void Info(String msg, Object... args) { Log(CefSettings.LogSeverity.LOGSEVERITY_INFO, msg, args); }
    static public void Warn(String msg, Object... args) { Log(CefSettings.LogSeverity.LOGSEVERITY_WARNING, msg, args); }
    static public void Error(String msg, Object... args) { Log(CefSettings.LogSeverity.LOGSEVERITY_ERROR, msg, args); }

    static public void Log(CefSettings.LogSeverity log_severity, String msg, Object... args) {
        if (msg == null || INSTANCE == null)
            return;
        INSTANCE.log(log_severity, msg, args);
    }
}
