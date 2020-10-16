module jogl.all {
  requires java.desktop;
  requires gluegen.rt;

  exports com.jogamp.opengl.awt;
  exports com.jogamp.nativewindow;
  exports com.jogamp.opengl;

  exports jogamp.opengl.gl4;
  exports jogamp.opengl.egl;
  exports jogamp.opengl.awt;
  exports jogamp.opengl.windows.wgl;
  exports jogamp.opengl.windows.wgl.awt;
  exports jogamp.nativewindow.windows;

  opens com.jogamp.opengl.egl to gluegen.rt;
  opens jogamp.opengl.gl4 to gluegen.rt;
  opens jogamp.opengl.windows.wgl to gluegen.rt;
}
