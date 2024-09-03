package org.apache.thrift;

public class LoggerFactory {
    public static Logger getLogger(String name) {
        return new Logger(name);
    }

    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz.getName());
    }
}
