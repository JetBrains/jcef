[![official JetBrains project](https://jb.gg/badges/official-plastic.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
# Welcome
This is a fork of [jcef](https://github.com/chromiumembedded/java-cef). It's distributed as part of
[JetBrainsRuntime](https://github.com/JetBrains/JetBrainsRuntime).

# Quick Links
* [File a bug](https://youtrack.jetbrains.com/newIssue?project=JBR&description=**Problem+description**%0A%5BA+short+problem+description%5D%0A%0A**Way+to+reproduce**%0A1.+Open++%E2%80%A6%0A2.+Go+to+%E2%80%A6%0A3.+Press+or+click+%E2%80%A6%0A4.+Observer+%E2%80%A6%0A5.+Expected+%E2%80%A6%0A%0A**Screen+records+or+screenshorts**%0A%5BPut+here+some+video+or+pictures+if+applicable%5D%0A%0A**Version+and+environment**%0AJetBrains+Runtime+version%3A+%5Bexample+17.0.8%2B7-b1000.22+aarch64.+In+a+JetBrains+IDEs+it+can+be+copied+from+%60About+IntelliJ+IDEA%60%2C+%60About+Clion%60+etc.%5D+%0AOS%3A+%5Bput+here+the+OS+name+and+version%28e.g.+macOS+13.4.1+%2C+Ubuntu+22.10%2C+Windows+11%29%5D%0ACPU%3A+%5Bput+here+CPU+architecture+like+arm64%2C+M1%2C+M2%2C+x86%2C+etc.%5D%0AAdditional+info%3A+%5BAnything+you+think+might+be+important+in+your+environment.+For+example%2C+in+the+case+of+graphics+problems%2C+you+can+tell+us+the+number+of+monitors+you+are+using%2C+their+resolutions+and+scaling+factors%5D%0A%0A**Additional+context**%0A%5Bput+here%5D%0A%0A%0A%0A&c=add+Board+Bug2Fix+Vitaly+Provodin&c=add+Board+JBR+Planing+No+Fix+versions&c=Subsystem+jcef&c=Assignee+Vladimir.Kharitonov)
* [Issue tracker](https://youtrack.jetbrains.com/issues/JBR?q=Subsystem:%20jcef%20)
* [JCEF in IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/jcef.html)
* [CEF forum](https://www.magpcss.org/ceforum/index.php)
* [CEF on github](https://github.com/chromiumembedded)

# Help
* Check the issue [tracker](https://youtrack.jetbrains.com/issues/JBR?q=Subsystem:%20jcef%20). Probably the problem is known.
* Vote the for the opened issues. We track votes and take it into consideration for planning.
* [JetBrains Platform Slack community](https://plugins.jetbrains.com/slack)
* Check [CEF forum](https://www.magpcss.org/ceforum/index.php). It is a great source of knowledge about cef project.
* Report a bug at JetBrains [youtrack](https://youtrack.jetbrains.com/newIssue?project=JBR&description=**Problem+description**%0A%5BA+short+problem+description%5D%0A%0A**Way+to+reproduce**%0A1.+Open++%E2%80%A6%0A2.+Go+to+%E2%80%A6%0A3.+Press+or+click+%E2%80%A6%0A4.+Observer+%E2%80%A6%0A5.+Expected+%E2%80%A6%0A%0A**Screen+records+or+screenshorts**%0A%5BPut+here+some+video+or+pictures+if+applicable%5D%0A%0A**Version+and+environment**%0AJetBrains+Runtime+version%3A+%5Bexample+17.0.8%2B7-b1000.22+aarch64.+In+a+JetBrains+IDEs+it+can+be+copied+from+%60About+IntelliJ+IDEA%60%2C+%60About+Clion%60+etc.%5D+%0AOS%3A+%5Bput+here+the+OS+name+and+version%28e.g.+macOS+13.4.1+%2C+Ubuntu+22.10%2C+Windows+11%29%5D%0ACPU%3A+%5Bput+here+CPU+architecture+like+arm64%2C+M1%2C+M2%2C+x86%2C+etc.%5D%0AAdditional+info%3A+%5BAnything+you+think+might+be+important+in+your+environment.+For+example%2C+in+the+case+of+graphics+problems%2C+you+can+tell+us+the+number+of+monitors+you+are+using%2C+their+resolutions+and+scaling+factors%5D%0A%0A**Additional+context**%0A%5Bput+here%5D%0A%0A%0A%0A&c=add+Board+Bug2Fix+Vitaly+Provodin&c=add+Board+JBR+Planing+No+Fix+versions&c=Subsystem+jcef&c=Assignee+Vladimir.Kharitonov).

# Building JCEF
1. Get sources
   `git clone https://github.com/JetBrains/jcef.git`
2. Set `JAVA_HOME`.
3. Run `./jb/tools/mac/build.sh all <platform>` or `./jb/tools/mac/build.bat all <platform>` for Windows at the project root. `<platform>` might be `amd64` or `x86_64`.
4. Check `jcef_*.tar.gz` file.

# Developing with CLion
To be done

# Developing with IntelliJ IDEA and running the tests
To be done

# Out of process CEF
See [remote/README.md](remote/README.md).