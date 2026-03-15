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

set "_PRODUCT_VERSION=unknown"
if exist "%_DIRNAME%.eclipseproduct" (
  for /f "usebackq tokens=1,* delims==" %%A in (`type "%_DIRNAME%.eclipseproduct"`) do (
    if /I "%%A"=="version" set "_PRODUCT_VERSION=%%B"
  )
)

set "_INSTALL_TOKEN=%_PRODUCT_VERSION%"
for %%I in ("%_DIRNAME%plugins\org.eclipse.mat.cli_*.jar") do (
  if exist "%%~fI" (
    for /f "tokens=2 delims=_" %%J in ("%%~nI") do set "_INSTALL_TOKEN=%%J"
    goto install_token_found
  )
)

:install_token_found

set "_OS_ID=win32"
set "_ARCH_ID=%PROCESSOR_ARCHITECTURE%"
if /I "%_ARCH_ID%"=="AMD64" set "_ARCH_ID=x86_64"
if /I "%_ARCH_ID%"=="ARM64" set "_ARCH_ID=aarch64"
if /I "%_ARCH_ID%"=="X86" set "_ARCH_ID=x86"
set "_RUNTIME_ID=v3-%_PRODUCT_VERSION%-%_INSTALL_TOKEN%-%_OS_ID%-%_ARCH_ID%"

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
  set "_CONFIG_DIR=%_CONFIG_BASE%\mat-cli\%_RUNTIME_ID%\configuration"
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
  set "_DATA_DIR=%_DATA_BASE%\mat-cli\%_RUNTIME_ID%\workspace"
)

if not exist "%_CONFIG_DIR%" mkdir "%_CONFIG_DIR%" >nul 2>&1
if not exist "%_CONFIG_DIR%" if "%_CONFIG_EXPLICIT%"=="0" (
  set "_CONFIG_DIR=%TEMP%\mat-cli\%_RUNTIME_ID%\configuration"
  if not exist "%_CONFIG_DIR%" mkdir "%_CONFIG_DIR%" >nul 2>&1
)
if not exist "%_CONFIG_DIR%" (
  >&2 echo Unable to create writable runtime directory "%_CONFIG_DIR%"
  exit /b 1
)

if not exist "%_DATA_DIR%" mkdir "%_DATA_DIR%" >nul 2>&1
if not exist "%_DATA_DIR%" if "%_DATA_EXPLICIT%"=="0" (
  set "_DATA_DIR=%TEMP%\mat-cli\%_RUNTIME_ID%\workspace"
  if not exist "%_DATA_DIR%" mkdir "%_DATA_DIR%" >nul 2>&1
)
if not exist "%_DATA_DIR%" (
  >&2 echo Unable to create writable runtime directory "%_DATA_DIR%"
  exit /b 1
)

if defined MAT_CLI_VMARGS (
  "%_JAVA%" %MAT_CLI_VMARGS% -jar "%_LAUNCHER%" -configuration "%_CONFIG_DIR%" -data "%_DATA_DIR%" -nosplash -product org.eclipse.mat.cli.product -application org.eclipse.mat.cli.app %*
) else (
  "%_JAVA%" -Xmx1024m -jar "%_LAUNCHER%" -configuration "%_CONFIG_DIR%" -data "%_DATA_DIR%" -nosplash -product org.eclipse.mat.cli.product -application org.eclipse.mat.cli.app %*
)

endlocal
