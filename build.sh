#!/bin/bash
# Build script that ensures Java 21 is used

export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean package "$@"
