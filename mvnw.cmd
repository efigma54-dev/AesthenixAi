@REM Maven wrapper for Windows
@echo off
set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar

if defined JAVA_HOME (
  set JAVA_CMD="%JAVA_HOME%\bin\java.exe"
) else (
  set JAVA_CMD=java.exe
)

%JAVA_CMD% -classpath "%MAVEN_WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
