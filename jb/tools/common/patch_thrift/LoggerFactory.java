package org.apache.thrift;

public class LoggerFactory {
    public static Logger getLogger(String name) { return new Logger(); }

    public static Logger getLogger(Class<?> clazz) { return new Logger(); }

}
