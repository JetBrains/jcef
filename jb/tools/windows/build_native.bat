if not exist build.xml (
    echo error: not in jcef root dir
    exit /b 1
)
if "%env.VS140COMNTOOLS%" == "" (
    echo error: env.VS140COMNTOOLS is not set
    exit /b 1
)
if "%env.CMAKE_37_PATH%" == "" (
    echo error: env.CMAKE_37_PATH is not set
    exit /b 1
)
if "%env.PYTHON_27_PATH%" == "" (
    echo error: env.PYTHON_27_PATH is not set
    exit /b 1
)
if "%ALT_JAVA_HOME%" == "" (
    if not exist jbrsdk (
        echo error: "jbrsdk" dir does not exist and ALT_JAVA_HOME is not set
        exit /b 1
    )
    set JAVA_HOME=jbrsdk    
) else (
    set JAVA_HOME=%ALT_JAVA_HOME%
)

echo *** set VS14 env...
call "%env.VS140COMNTOOLS%\..\..\VC\vcvarsall.bat" amd64 || exit /b 1

mkdir jcef_build
cd jcef_build

echo *** run cmake...
set RC=
set PATH=%env.CMAKE_37_PATH%\bin;%env.PYTHON_27_PATH%;"%PATH%"
cmake -G "Visual Studio 14 Win64" ..

echo *** run MSBuild.exe...
"c:\Program Files (x86)\MSBuild\14.0\Bin\MSBuild.exe" /t:Rebuild /p:Configuration=Release .\jcef.sln

cd ..