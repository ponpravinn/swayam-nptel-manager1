@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.2.0
@REM
@REM Optional ENV vars:
@REM   JAVA_HOME - location of a JDK home dir
@REM   MAVEN_BATCH_ECHO - set to "on" to enable the echo.
@REM   MAVEN_BATCH_PAUSE - set to "on" to pause the script before exiting.
@REM   MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM     e.g. to debug Maven itself, use
@REM       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@IF "%MAVEN_BATCH_ECHO%" == "on"  echo %MAVEN_BATCH_ECHO%

@REM Set local scope for the variables with windows NT shell
@IF "%OS%"=="Windows_NT" @SETLOCAL
@IF "%OS%"=="Windows_NT" @SET MAVEN_BATCH_ECHO=%MAVEN_BATCH_ECHO%

@REM ==== START VALIDATION ====
IF NOT "%JAVA_HOME%" == "" GOTO OkJHome
FOR %%i IN (java.exe) DO SET "JAVA_EXE=%%~$PATH:i"
IF NOT "%JAVA_EXE%" == "" GOTO OkJHome

echo.
echo Error: JAVA_HOME not found in your environment.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
echo.
GOTO error

:OkJHome
IF EXIST "%JAVA_HOME%\bin\java.exe" GOTO init

echo.
echo Error: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
GOTO error

:init
SET "MAVEN_PROJECTBASEDIR=%~dp0"

IF NOT "%MAVEN_OPTS%" == "" GOTO skipMavenOptsOverride
SET "MAVEN_OPTS=-Xmx512m -XX:MaxMetaspaceSize=256m"
:skipMavenOptsOverride

SET "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
SET "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

IF EXIST %WRAPPER_JAR% GOTO execCmd

echo Downloading Maven wrapper...
"%JAVA_HOME%\bin\java.exe" -classpath "" ^
  -Dmaven.wrapper.jar=%WRAPPER_JAR% ^
  org.apache.maven.wrapper.WrapperDownloader %WRAPPER_JAR% 2>NUL

IF ERRORLEVEL 1 (
  echo Downloading with PowerShell...
  powershell -Command "&{[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $wrapperUrl='https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar'; (New-Object System.Net.WebClient).DownloadFile($wrapperUrl,'%WRAPPER_JAR%')}"
)

:execCmd
SET "MAVEN_JAVA_EXE=%JAVA_HOME%\bin\java.exe"

SET WRAPPER_OPTS=
IF NOT "%MAVEN_OPTS%" == "" SET "WRAPPER_OPTS=%WRAPPER_OPTS% %MAVEN_OPTS%"

"%MAVEN_JAVA_EXE%" %WRAPPER_OPTS% -classpath "%WRAPPER_JAR%" %WRAPPER_LAUNCHER% %*
IF ERRORLEVEL 1 GOTO error
GOTO end

:error
SET ERROR_CODE=1

:end
@IF "%MAVEN_BATCH_ECHO%" == "on" ECHO ...finished with error code %ERROR_CODE%.
@IF NOT "%MAVEN_BATCH_PAUSE%" == "" PAUSE
EXIT /B %ERROR_CODE%
