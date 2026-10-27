#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper script for Unix/Linux
# ----------------------------------------------------------------------------

WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER="org.apache.maven.wrapper.MavenWrapperMain"

# Find Java
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Maven wrapper jar not found: $WRAPPER_JAR" >&2
    exit 1
fi

exec "$JAVACMD" -cp "$WRAPPER_JAR" "$WRAPPER_LAUNCHER" "$@"
