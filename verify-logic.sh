#!/usr/bin/env sh
set -eu

OUTPUT_DIR="build/logic-self-test"
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

if command -v javac >/dev/null 2>&1; then
  javac -d "$OUTPUT_DIR" \
    core/src/main/java/com/dungeonrpg/model/*.java \
    core/src/test/java/com/dungeonrpg/model/LogicSelfTest.java
else
  java -m jdk.compiler/com.sun.tools.javac.Main -d "$OUTPUT_DIR" \
    core/src/main/java/com/dungeonrpg/model/*.java \
    core/src/test/java/com/dungeonrpg/model/LogicSelfTest.java
fi

java -cp "$OUTPUT_DIR" com.projectenigma.model.LogicSelfTest
