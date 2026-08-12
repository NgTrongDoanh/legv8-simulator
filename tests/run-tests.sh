#!/bin/bash
set -euo pipefail

TEST_TMP=$(mktemp -d /tmp/legv8-tests.XXXXXX)
trap 'rm -rf "$TEST_TMP"' EXIT

find src -name '*.java' -print0 \
    | xargs -0 javac -encoding UTF-8 -Xlint:all,-serial -d "$TEST_TMP/classes" -cp 'lib/*'
javac -encoding UTF-8 -Xlint:all -d "$TEST_TMP/classes" \
    -cp "$TEST_TMP/classes:lib/*" tests/RegressionTests.java
java -cp "$TEST_TMP/classes:lib/*" RegressionTests
