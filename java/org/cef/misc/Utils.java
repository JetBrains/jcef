package org.cef.misc;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class Utils {
    public static boolean getBoolean(String varName) {
        if (Boolean.getBoolean(varName))
            return true;

        String sval = System.getenv(varName);
        if (sval == null || sval.isEmpty())
            return false;

        return sval.trim().toLowerCase().equals("true");
    }

    public static boolean getBoolean(String varName, boolean defVal) {
        String val = System.getProperty(varName);
        if (val == null) {
            val = System.getenv(varName);
            if (val == null || val.isEmpty())
                return defVal;
        }

        return val.trim().equalsIgnoreCase("true");
    }

    public static int getInteger(String varName, int defaultVal) {
        int valFromSystem = Integer.getInteger(varName, defaultVal);
        if (valFromSystem != defaultVal)
            return valFromSystem;

        String sval = System.getenv(varName);
        if (sval == null || sval.isEmpty())
            return defaultVal;

        try {
            return Integer.parseInt(sval);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return defaultVal;
    }

    public static String getString(String varName) {
        return getString(varName, null);
    }

    public static String getString(String varName, String defaultVal) {
        String valFromSystem = System.getProperty(varName, null);
        if (valFromSystem != null && !valFromSystem.isEmpty())
            return valFromSystem;

        String sval = System.getenv(varName);
        return sval == null || sval.isEmpty() ? defaultVal : sval;
    }

    // CEF does not accept ".." in path
    public static String normalizePath(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
