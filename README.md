[![official JetBrains project](https://jb.gg/badges/official-plastic.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
# Welcome
This is a fork of [java-cef](https://github.com/chromiumembedded/java-cef). It's distributed as part of
[JetBrainsRuntime](https://github.com/JetBrains/JetBrainsRuntime).

# Quick Links
* [File a bug](https://youtrack.jetbrains.com/newIssue?project=JBR&description=**Problem+description**%0A%5BA+short+problem+description%5D%0A%0A**Way+to+reproduce**%0A1.+Open++%E2%80%A6%0A2.+Go+to+%E2%80%A6%0A3.+Press+or+click+%E2%80%A6%0A4.+Observer+%E2%80%A6%0A5.+Expected+%E2%80%A6%0A%0A**Screen+records+or+screenshorts**%0A%5BPut+here+some+video+or+pictures+if+applicable%5D%0A%0A**Version+and+environment**%0AJetBrains+Runtime+version%3A+%5Bexample+17.0.8%2B7-b1000.22+aarch64.+In+a+JetBrains+IDEs+it+can+be+copied+from+%60About+IntelliJ+IDEA%60%2C+%60About+Clion%60+etc.%5D+%0AOS%3A+%5Bput+here+the+OS+name+and+version%28e.g.+macOS+13.4.1+%2C+Ubuntu+22.10%2C+Windows+11%29%5D%0ACPU%3A+%5Bput+here+CPU+architecture+like+arm64%2C+M1%2C+M2%2C+x86%2C+etc.%5D%0AAdditional+info%3A+%5BAnything+you+think+might+be+important+in+your+environment.+For+example%2C+in+the+case+of+graphics+problems%2C+you+can+tell+us+the+number+of+monitors+you+are+using%2C+their+resolutions+and+scaling+factors%5D%0A%0A**Additional+context**%0A%5Bput+here%5D%0A%0A%0A%0A&c=add+Board+Bug2Fix+Vitaly+Provodin&c=add+Board+JBR+Planing+No+Fix+versions&c=Subsystem+jcef&c=Assignee+Vladimir.Kharitonov)
* [Issue tracker](https://youtrack.jetbrains.com/issues/JBR?q=Subsystem:%20jcef%20)
* [JCEF in IntelliJ Platform Plugin SDK](https://blog.jetbrains.com/platform/2019/10/introducing-jetbrains-platform-slack-for-plugin-developers/)
* [CEF forum](https://www.magpcss.org/ceforum/index.php)
* [CEF on github](https://github.com/chromiumembedded)

# Help
* Check the issue [tracker](https://youtrack.jetbrains.com/issues/JBR?q=Subsystem:%20jcef%20). Probably the problem is known.
* Vote the for the opened issues. We track votes and take it into consideration for planning.
* [JetBrains Platform Slack community](https://plugins.jetbrains.com/slack)
* Check [CEF forum](https://www.magpcss.org/ceforum/index.php). It is a great source of knowledge about cef project.
* Report a bug at JetBrains [youtrack](https://youtrack.jetbrains.com/newIssue?project=JBR&description=**Problem+description**%0A%5BA+short+problem+description%5D%0A%0A**Way+to+reproduce**%0A1.+Open++%E2%80%A6%0A2.+Go+to+%E2%80%A6%0A3.+Press+or+click+%E2%80%A6%0A4.+Observer+%E2%80%A6%0A5.+Expected+%E2%80%A6%0A%0A**Screen+records+or+screenshorts**%0A%5BPut+here+some+video+or+pictures+if+applicable%5D%0A%0A**Version+and+environment**%0AJetBrains+Runtime+version%3A+%5Bexample+17.0.8%2B7-b1000.22+aarch64.+In+a+JetBrains+IDEs+it+can+be+copied+from+%60About+IntelliJ+IDEA%60%2C+%60About+Clion%60+etc.%5D+%0AOS%3A+%5Bput+here+the+OS+name+and+version%28e.g.+macOS+13.4.1+%2C+Ubuntu+22.10%2C+Windows+11%29%5D%0ACPU%3A+%5Bput+here+CPU+architecture+like+arm64%2C+M1%2C+M2%2C+x86%2C+etc.%5D%0AAdditional+info%3A+%5BAnything+you+think+might+be+important+in+your+environment.+For+example%2C+in+the+case+of+graphics+problems%2C+you+can+tell+us+the+number+of+monitors+you+are+using%2C+their+resolutions+and+scaling+factors%5D%0A%0A**Additional+context**%0A%5Bput+here%5D%0A%0A%0A%0A&c=add+Board+Bug2Fix+Vitaly+Provodin&c=add+Board+JBR+Planing+No+Fix+versions&c=Subsystem+jcef&c=Assignee+Vladimir.Kharitonov).

# Building JCEF
## Building on Windows
1. Get sources.
   ```
   git clone https://github.com/JetBrains/jcef.git
   ```
2. Install [Apache Ant](https://ant.apache.org/manual/install.html). Make sure that `ANT_HOME` is set.
3. Set `JAVA_HOME`.
4. Set `VS160COMNTOOLS` env variable to point onto your Visual Studio 2019 Community installation. E.g.
   ```
   VS160COMNTOOLS='C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\Common7\Tools'.
   ```
5. Install cmake. Set `JCEF_CMAKE` env variable. E.g.
   ```
   `JCEF_CMAKE='C:\cmake-3.27.0-rc2-windows-x86_64'`.
   ```
6. Run at `<project_root>\jb\tools\windows` directory. Replace `x86_64` with `arm64` if needed.
   ```
   cmd /c build.bat all x86_64
   ```
7. Check `jcef_win_x86_64.tar.gz` or `jcef_win_arm64.tar.gz` file in the project root directory.

### Building JetBrainsRuntime with JCEF.
1. Assume that we are working on `x86_64` platform. For `x64` or `x86_64` has to replaced with `arm64` or `aarch64`.

2. Prepare the [build environment for JetBrainsRuntime(JBR)](https://github.com/JetBrains/JetBrainsRuntime/tree/jbr17#windows-1).
   Next command are run **under Cygwin bash** installed on this step.

   Note: You may skip NVDA controller client installation, but you'd need to patch `mkimages_x64.sh` script.
3. Get JBR sources 
   ```bash
   $ git clone https://github.com/JetBrains/JetBrainsRuntime.git
   $ cd JetBrainsRuntime
   ```
   
4. Switch to the required branch. E.g.
   ```bash
   $ git checkout jbr17
   ```
   
5. Bring jcef build artifact from the previous steps. Assuming that our platform is `x86_64`.
   ```bash
   $ mkdir jcef_win_x64
   $ tar -xvzf <jcef_project_root>/jcef_win_x86_64.tar.gz -C ./jcef_win_x64
   ```
   
6. (Optional). If NVDA Controller Client is not installed, patch [mkimages_x64.sh](https://github.com/JetBrains/JetBrainsRuntime/blob/jbr17/jb/project/tools/windows/scripts/mkimages_x64.sh).
   Remove the following line:
   ```
       --with-nvdacontrollerclient=$NVDA_PATH \
   ```

7. Set `BOOT_JDK` env var.

8. Run:
   ```bash
      $ bash jb/project/tools/windows/scripts/mkimages_x64.sh 1 jcef
   ```
   `1` - is the build number.
   
9. Check output dirs:
   ```bash
   $ ls jbr*
   jbr_jcef-17.0.8.1-windows-x64-b1:
   bin  conf  include  legal  lib  release
   
   jbrsdk_jcef-17.0.8.1-windows-x64-b1:
   bin  conf  include  jmods  legal  lib  release
   ```

## Building on Linux
1. Get tools to build jcef `apt-get install ant git gcc cmake`.
2. Get tools to build jcef rpc `apt-get install bison flex pkg-config`.
3. Get sources
   ```bash
   git clone https://github.com/JetBrains/jcef.git
   ```
4. Set `JAVA_HOME`.
5. Run `./jb/tools/linux/build.sh all <x86_64 or arm64>` at the project root.
6. Check `jcef_linux_<x86_64 or arm64>.tar.gz` file in the project root directory.

## Building on Mac
1. Get sources
   `git clone https://github.com/JetBrains/jcef.git`
2. Set `JAVA_HOME`.
3. Run `./jb/tools/mac/build.sh all <x86_64 or arm64>` at the project root.
4. Check `jcef_mac_<x86_64 or arm64>.tar.gz` file in the project root directory.

# Developing with CLion
To be done

# Developing with IntelliJ IDEA and running the tests
1. Open `<project_root>/jb/project/java-gradle/build.gradle` as project in IntelliJ IDEA.
2. Edit `<project_root>/jb/project/java-gradle/gradle.properties` to specify path to the tested JBR+JCEF build.
   E.g.
   ```
   jbr_win = <path to jbr>/jbr_jcef-17.0.8.1-windows-x64-b1
   jbr_mac = <path to jbr>/jbrsdk_jcef-17.0.8.1-osx-aarch64-b1/Contents/Home
   jbr_linux = <path to jbr>/jbr
   ```
3. Reload the gradle project.
4. Navigate a test in `<project_root>/java_tests/tests/junittests` and run.

# Out of process CEF
See [remote/README.md](remote/README.md).