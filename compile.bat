@echo off
setlocal enabledelayedexpansion

echo =====================================================
echo FastStylus Native Compiler
echo =====================================================
echo.

REM Auto-detect JAVA_HOME
if not defined JAVA_HOME (
    for /f "tokens=*" %%a in ('where javac 2^>nul') do (
        set "JAVA_HOME=%%~dpa.."
    )
)

if not defined JAVA_HOME (
    echo ERROR: JAVA_HOME not found. Please install JDK 17+.
    pause
    exit /b 1
)

echo Using JAVA_HOME: %JAVA_HOME%

REM Auto-detect Visual Studio
set "VS_PATH="
for %%p in (
    "C:\Program Files\Microsoft Visual Studio\2022\Community"
    "C:\Program Files\Microsoft Visual Studio\2022\Enterprise"
    "C:\Program Files\Microsoft Visual Studio\2022\Professional"
    "C:\Program Files\Microsoft Visual Studio\2022\BuildTools"
    "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools"
) do (
    if exist "%%~p\VC\Auxiliary\Build\vcvars64.bat" (
        set "VS_PATH=%%~p"
        goto :found_vs
    )
)

:found_vs
if not defined VS_PATH (
    echo ERROR: Visual Studio 2022 not found.
    pause
    exit /b 1
)

echo Using Visual Studio: %VS_PATH%

REM Setup environment
call "%VS_PATH%\VC\Auxiliary\Build\vcvars64.bat"

REM Create build directory
if not exist build mkdir build

echo.
echo Compiling FastStylus Native DLL...
echo =====================================================

cl /LD /Fe:build\faststylus.dll ^
    native\FastStylus.cpp ^
    user32.lib ^
    /I"%JAVA_HOME%\include" ^
    /I"%JAVA_HOME%\include\win32" ^
    /EHsc /std:c++17 /O2 /W3 ^
    /link /DEF:native\FastStylus.def

if %ERRORLEVEL% neq 0 (
    echo.
    echo =====================================================
    echo COMPILATION FAILED
    echo =====================================================
    pause
    exit /b 1
)

echo.
echo =====================================================
echo BUILD SUCCESSFUL: build\faststylus.dll
echo =====================================================
echo.
pause
