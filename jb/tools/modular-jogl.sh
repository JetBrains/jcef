# Copyright 2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

#
# patch gluegen-rt.jar
#
cd third_party/jogamp/jar
$JAVA_HOME/bin/jdeps --generate-module-info . gluegen-rt.jar
$JAVA_HOME/bin/javac --patch-module gluegen.rt=gluegen-rt.jar gluegen.rt/module-info.java
mv gluegen.rt/module-info.class .
$JAVA_HOME/bin/jar uf gluegen-rt.jar module-info.class
rm module-info.class

#
# patch jogl-all.jar
#
mkdir jogl.all
echo "module jogl.all {
  requires java.desktop;
  requires gluegen.rt;
}" > jogl.all/module-info.java
$JAVA_HOME/bin/javac --module-path gluegen-rt.jar --patch-module jogl.all=jogl-all.jar jogl.all/module-info.java
mv jogl.all/module-info.class .
$JAVA_HOME/bin/jar uf jogl-all.jar module-info.class
rm module-info.class
