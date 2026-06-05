@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo [FastStylus] Running Demo (via JitPack)...
cd examples\00-basic-usage
call mvn compile exec:java -Dexec.mainClass=faststylus.StylusDemo
cd ..\..
pause
