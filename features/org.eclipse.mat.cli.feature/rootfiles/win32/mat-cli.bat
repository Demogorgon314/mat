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

pushd "%_DIRNAME%" >nul 2>&1
if errorlevel 1 (
  >&2 echo Unable to change directory to "%_DIRNAME%"
  exit /b 1
)

if defined MAT_CLI_CONFIG_DIR (
  set "_CONFIG_DIR=%MAT_CLI_CONFIG_DIR%"
  set "_CONFIG_EXPLICIT=1"
) else (
  set "_CONFIG_EXPLICIT=0"
  set "_CONFIG_BASE="
  if defined LOCALAPPDATA set "_CONFIG_BASE=%LOCALAPPDATA%"
  if not defined _CONFIG_BASE if defined APPDATA set "_CONFIG_BASE=%APPDATA%"
  if not defined _CONFIG_BASE if defined TEMP set "_CONFIG_BASE=%TEMP%"
  if not defined _CONFIG_BASE set "_CONFIG_BASE=%_DIRNAME%runtime"
  set "_CONFIG_DIR=%_CONFIG_BASE%\mat-cli\configuration"
)

if defined MAT_CLI_DATA_DIR (
  set "_DATA_DIR=%MAT_CLI_DATA_DIR%"
  set "_DATA_EXPLICIT=1"
) else (
  set "_DATA_EXPLICIT=0"
  set "_DATA_BASE="
  if defined LOCALAPPDATA set "_DATA_BASE=%LOCALAPPDATA%"
  if not defined _DATA_BASE if defined APPDATA set "_DATA_BASE=%APPDATA%"
  if not defined _DATA_BASE if defined TEMP set "_DATA_BASE=%TEMP%"
  if not defined _DATA_BASE set "_DATA_BASE=%_DIRNAME%runtime"
  set "_DATA_DIR=%_DATA_BASE%\mat-cli\workspace"
)

if not exist "%_CONFIG_DIR%" mkdir "%_CONFIG_DIR%" >nul 2>&1
if not exist "%_CONFIG_DIR%" if "%_CONFIG_EXPLICIT%"=="0" (
  set "_CONFIG_DIR=%TEMP%\mat-cli\configuration"
  if not exist "%_CONFIG_DIR%" mkdir "%_CONFIG_DIR%" >nul 2>&1
)
if not exist "%_CONFIG_DIR%" (
  >&2 echo Unable to create writable runtime directory "%_CONFIG_DIR%"
  popd >nul 2>&1
  exit /b 1
)

if not exist "%_DATA_DIR%" mkdir "%_DATA_DIR%" >nul 2>&1
if not exist "%_DATA_DIR%" if "%_DATA_EXPLICIT%"=="0" (
  set "_DATA_DIR=%TEMP%\mat-cli\workspace"
  if not exist "%_DATA_DIR%" mkdir "%_DATA_DIR%" >nul 2>&1
)
if not exist "%_DATA_DIR%" (
  >&2 echo Unable to create writable runtime directory "%_DATA_DIR%"
  popd >nul 2>&1
  exit /b 1
)

if defined MAT_CLI_VMARGS (
  "%_JAVA%" %MAT_CLI_VMARGS% -jar "%_LAUNCHER%" -configuration "%_CONFIG_DIR%" -data "%_DATA_DIR%" -nosplash -product org.eclipse.mat.cli.product -application org.eclipse.mat.cli.app %*
) else (
  "%_JAVA%" -Xmx1024m -jar "%_LAUNCHER%" -configuration "%_CONFIG_DIR%" -data "%_DATA_DIR%" -nosplash -product org.eclipse.mat.cli.product -application org.eclipse.mat.cli.app %*
)

popd >nul 2>&1
endlocal
