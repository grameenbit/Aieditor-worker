@echo off
set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
set DEFAULT_JVM_OPTS=-Dfile.encoding=UTF-8 "-Xmx64m" "-Xms64m"
if defined JAVA_HOME (set JAVACMD=%JAVA_HOME%\bin\java.exe) else (set JAVACMD=java.exe)
"%JAVACMD%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=gradlew" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
