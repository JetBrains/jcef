module jcef {
    requires java.desktop;
    requires jogl.all;
    requires gluegen.rt;

    exports org.cef;
    exports org.cef.browser;
    exports org.cef.callback;
    exports org.cef.handler;
    exports org.cef.misc;
    exports org.cef.network;
}
