module jogl.all {
  requires java.desktop;
  requires gluegen.rt;

  // to jcef
  exports com.jogamp.opengl.awt;
  exports com.jogamp.nativewindow;
  exports com.jogamp.opengl;

  exports jogamp.nativewindow.x11 to gluegen.rt;
  exports jogamp.nativewindow.x11.awt to gluegen.rt;
  exports jogamp.opengl.x11.glx to gluegen.rt;
  exports jogamp.opengl.gl4 to gluegen.rt;
  exports jogamp.opengl.egl to gluegen.rt;
  exports jogamp.opengl.awt to gluegen.rt;

  opens jogamp.opengl.x11.glx to gluegen.rt;
  opens com.jogamp.opengl.egl to gluegen.rt;
  opens jogamp.opengl.gl4 to gluegen.rt;
}