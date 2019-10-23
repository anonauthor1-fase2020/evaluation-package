#!/usr/bin/env bash

# If your regular Java version is below 11, uncomment the line with "export" and
# set JAVA_HOME to the path of a Java executable with version 11 or higher. That
# is, because at least Java 11 is required to run the static analyzer.

#export JAVA_HOME=/usr/lib/jvm/java-11-oracle

if [ ! -z "$JAVA_HOME" ];
then
    export PATH="$JAVA_HOME/bin:$PATH"
fi
