package org.cef.misc;

public class Utils {
    public static boolean getBoolean(String varName) {
        if (Boolean.getBoolean(varName))
            return true;

        String sval = System.getenv(varName);
        if (sval == null || sval.isEmpty())
            return false;

        return sval.trim().toLowerCase().equals("true");
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
}
