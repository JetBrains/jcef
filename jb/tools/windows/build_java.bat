if not exist build.xml (
    echo error: not in jcef root dir
    exit /b 1
)
if "%env.ANT_HOME%" == "" (
    echo error: env.ANT_HOME is not set
    exit /b 1
)

set PATH=%env.ANT_HOME%\bin;"%PATH%"

if "%ALT_JAVA_HOME%" == "" (
    if not exist jbrsdk (
        echo error: "jbrsdk" dir does not exist and ALT_JAVA_HOME is not set
        exit /b 1
    )
    set JAVA_HOME=jbrsdk    
) else (
    set JAVA_HOME=%ALT_JAVA_HOME%
)

echo *** compile java...
cd tools
compile.bat win64 Release
cd ..