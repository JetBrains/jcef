if not exist build.xml (
    echo error: not in jcef root dir
    exit /b 1
)
set MODULAR_SDK=./out/win64/modular-sdk

echo *** create archive...
where bash
if %ERRORLEVEL% neq 0 (
    echo *** using c:\cygwin64\bin
    set PATH=c:\cygwin64\bin;"%PATH%"
)
echo HERE
@rem temp exclude jogl
bash -c "cat $MODULAR_SDK/modules_src/jcef/module-info.java | grep -v jogl | grep -v gluegen > __tmp" || exit /b 1
bash -c "mv __tmp $MODULAR_SDK/modules_src/jcef/module-info.java" || exit /b 1

bash -c "[ -d jcef_win_x64 ] || mkdir jcef_win_x64" || exit /b 1
bash -c "cp -R jcef_build/native/Release/* jcef_win_x64/" || exit /b 1
bash -c "cp -R $MODULAR_SDK jcef_win_x64/" || exit /b 1

bash -c "tar -cvzf jcef_win_x64.tar.gz -C jcef_win_x64 $(ls jcef_win_x64)" || exit /b 1
bash -c "rm -rf jcef_win_x64" || exit /b 1