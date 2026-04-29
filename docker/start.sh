#!/bin/bash

exec java $JVM_OPTS \
-Djava.net.preferIPv4Stack=true \
-jar ${PROJECT_DIR}/${TARGET_JAR}