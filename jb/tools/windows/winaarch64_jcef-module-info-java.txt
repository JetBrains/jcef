module jcef {
    requires java.desktop;
    //requires jogl.all;

    exports org.cef;
    exports org.cef.browser;
    exports org.cef.callback;
    exports org.cef.handler;
    exports org.cef.misc;
    exports org.cef.network;

    exports com.jetbrains.cef;

    opens org.cef; // [tav] todo: provide necessary API instead
}
