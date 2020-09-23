# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

echo "*** patch gluegen-rt.jar..."
cd "$JCEF_ROOT_DIR"/third_party/jogamp/jar || exit 1

"$JAVA_HOME"/bin/jdeps --generate-module-info . gluegen-rt.jar
"$JAVA_HOME"/bin/javac --patch-module gluegen.rt=gluegen-rt.jar gluegen.rt/module-info.java

mv gluegen.rt/module-info.class .

"$JAVA_HOME"/bin/jar uf gluegen-rt.jar module-info.class

rm module-info.class

echo "*** patch jogl-all.jar..."
mkdir jogl.all
echo "module jogl.all {
  requires java.desktop;
  requires gluegen.rt;

  // to jcef
  exports com.jogamp.opengl.awt;
  exports com.jogamp.nativewindow;
  exports com.jogamp.opengl;

  exports jogamp.nativewindow.x11 to gluegen.rt;
  exports jogamp.opengl.x11.glx to gluegen.rt;
  exports jogamp.opengl.gl4 to gluegen.rt;
  exports jogamp.opengl.egl to gluegen.rt;
  exports jogamp.nativewindow.x11.awt to gluegen.rt;
  exports jogamp.opengl.awt to gluegen.rt;

  opens jogamp.opengl.x11.glx to gluegen.rt;
  opens com.jogamp.opengl.egl to gluegen.rt;
  opens jogamp.opengl.gl4 to gluegen.rt;
}" > jogl.all/module-info.java

"$JAVA_HOME"/bin/javac --module-path gluegen-rt.jar --patch-module jogl.all=jogl-all.jar jogl.all/module-info.java

mv jogl.all/module-info.class .

"$JAVA_HOME"/bin/jar uf jogl-all.jar module-info.class

rm module-info.class

cd "$JCEF_ROOT_DIR" || exit 1
