@echo off
setlocal
set "DIR=%~dp0"
for /f "delims=" %%I in ('dir /b /s "%DIR%build\libs\nardol-*-all.jar" 2^>nul') do (
    set "JAR=%%I"
    goto :run
)
call "%DIR%gradlew.bat" shadowJar -q
for /f "delims=" %%I in ('dir /b /s "%DIR%build\libs\nardol-*-all.jar" 2^>nul') do (
    set "JAR=%%I"
    goto :run
)
:run
java -jar "%JAR%" %*
