#!/bin/bash

# --- configure variables ---
BIN_DIR="bin"
LIB_DIR="lib"
MAIN_CLASS="Application"

if [ ! -d "$BIN_DIR" ]; then
    echo "Error: Build directory '$BIN_DIR' not found."
    echo "Please run ./build.sh first."
    exit 1
fi

# --- classpath separator ---
CLASSPATH_SEP=":" # for Unix-like systems (Linux, macOS)
if [[ "$OSTYPE" == "cygwin" ]] || [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]]; then
  CLASSPATH_SEP=";" # for windows
fi

RUN_CP="$BIN_DIR"

# --- for libraries (if exists) ---
if [ -d "$LIB_DIR" ] && ls "$LIB_DIR"/*.jar 1> /dev/null 2>&1; then
    echo "Adding libraries from $LIB_DIR to runtime classpath."
    RUN_CP="$RUN_CP$CLASSPATH_SEP$LIB_DIR/*"
else
    echo "Info: Library directory '$LIB_DIR' not found or empty. Running without external libraries."
fi

# --- Run app ---
echo "Running simulator ($MAIN_CLASS)..."
echo "Using Classpath: $RUN_CP"
echo "--- Simulator Output Start ---"
java -cp "$RUN_CP" "$MAIN_CLASS" "$@"

# --- Capture exit code ---
JAVA_EXIT_CODE=$?

echo "--- Simulator Output End ---"

# --- Check exit code ---
if [ $JAVA_EXIT_CODE -ne 0 ]; then
    echo "Simulator exited with error code: $JAVA_EXIT_CODE"
    exit $JAVA_EXIT_CODE
else
    echo "Simulator finished successfully."
    exit 0
fi