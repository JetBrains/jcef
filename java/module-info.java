module jcef {
    requires java.desktop;
    requires jogl.all;
    requires org.apache.thrift;
    requires org.slf4j;

    exports org.cef;
    exports org.cef.browser;
    exports org.cef.callback;
    exports org.cef.handler;
    exports org.cef.input;
    exports org.cef.misc;
    exports org.cef.network;
    exports org.cef.security;

    exports com.jetbrains.cef;

    opens org.cef; // [tav] todo: provide necessary API instead
}
