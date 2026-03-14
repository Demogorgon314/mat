@echo off
REM
REM Run the Eclipse Memory Analyzer CLI application.
REM

setlocal
set _DIRNAME=.\
if "%OS%" == "Windows_NT" set _DIRNAME=%~dp0%
set _JAVA=java
set _LAUNCHER=

if defined JAVA_HOME set _JAVA=%JAVA_HOME%\bin\java.exe

for %%I in ("%_DIRNAME%plugins\org.eclipse.equinox.launcher_*.jar") do (
  if exist "%%~fI" (
    set _LAUNCHER=%%~fI
    goto launcher_found
  )
)

>&2 echo Unable to locate org.eclipse.equinox.launcher_*.jar under "%_DIRNAME%plugins"
exit /b 1

:launcher_found

if defined MAT_CLI_VMARGS (
  "%_JAVA%" %MAT_CLI_VMARGS% -jar "%_LAUNCHER%" -configuration "%_DIRNAME%configuration" -nosplash -product org.eclipse.mat.cli.product -application org.eclipse.mat.cli.app %*
) else (
  "%_JAVA%" -Xmx1024m -jar "%_LAUNCHER%" -configuration "%_DIRNAME%configuration" -nosplash -product org.eclipse.mat.cli.product -application org.eclipse.mat.cli.app %*
)

endlocal
