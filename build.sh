#!/bin/bash

# --- configure variables ---
SRC_DIR="src"
BIN_DIR="bin"
RES_DIR="resources"
LIB_DIR="lib"

# --- check java compiler ---
command -v javac >/dev/null 2>&1 || { echo >&2 "Error: 'javac' command not found. Please install JDK and ensure it's in your PATH. Aborting."; exit 1; }
echo "Java compiler (javac) found."

# --- delete old build directory ---
echo "Removing old build directory ($BIN_DIR)..."
rm -rf "$BIN_DIR"

# --- create new build directory ---
echo "Creating build directory ($BIN_DIR)..."
mkdir -p "$BIN_DIR"
if [ $? -ne 0 ]; then
    echo "Error: Failed to create directory $BIN_DIR. Aborting."
    exit 1
fi

# --- copy resources ---
echo "Copying all resources from $RES_DIR to $BIN_DIR/$RES_DIR..."
if [ -d "$RES_DIR" ]; then
    mkdir -p "$BIN_DIR/$RES_DIR"
    cp -r "$RES_DIR/." "$BIN_DIR/$RES_DIR/"
    if [ $? -ne 0 ]; then
        echo "Error: Failed to copy resources. Aborting."
        rm -rf "$BIN_DIR"
        exit 1
    fi
    echo "Resources copied successfully."
else
    echo "Warning: Resource directory '$RES_DIR' not found. Skipping resource copy."
    mkdir -p "$BIN_DIR/$RES_DIR"
fi


# --- check source directory ---
echo "Finding Java source files in $SRC_DIR..."
find "$SRC_DIR" -name "*.java" > sources.txt
if [ ! -s sources.txt ]; then
    echo "Error: No Java source files found in $SRC_DIR. Aborting."
    rm -f sources.txt
    exit 1
fi
echo "Found $(wc -l < sources.txt) source files."

# --- classpath separator ---
CLASSPATH_SEP=":" # for Unix-like systems (Linux, macOS)
if [[ "$OSTYPE" == "cygwin" ]] || [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]]; then
  CLASSPATH_SEP=";" # for windows
fi

# --- compile classpath ---
COMPILE_CP="$BIN_DIR"

# --- for libraries (if exists) ---
if [ -d "$LIB_DIR" ]; then
    if ls "$LIB_DIR"/*.jar 1> /dev/null 2>&1; then
        echo "Adding libraries from $LIB_DIR to compile classpath..."
        COMPILE_CP="$COMPILE_CP$CLASSPATH_SEP$LIB_DIR/*"
    else
        echo "Warning: Library directory '$LIB_DIR' exists but contains no .jar files."
    fi
else
    echo "Info: Library directory '$LIB_DIR' not found. Compiling without external libraries."
fi

# --- compile Java source files ---
echo "Compiling Java source files..."
echo "Classpath: $COMPILE_CP"
javac -encoding UTF-8 -d "$BIN_DIR" -sourcepath "$SRC_DIR" -cp "$COMPILE_CP" @sources.txt

# --- check compilation result ---
if [ $? -ne 0 ]; then
    echo "---------------------"
    echo "BUILD FAILED: Compilation errors detected."
    echo "---------------------"
    rm sources.txt
    exit 1
fi
echo "Compilation successful."
rm sources.txt 

# --- Log output ---
echo ""
echo "---------------------"
echo "BUILD SUCCESSFUL!"
echo "Output directory: $BIN_DIR"
echo "To run (example, adjust main class):"
echo "cd $BIN_DIR"
echo "java -cp \".${CLASSPATH_SEP}../$LIB_DIR/*\" legv8.gui.Application" 
echo "Or run from the project root:"
echo "java -cp \"$BIN_DIR${CLASSPATH_SEP}$LIB_DIR/*\" legv8.gui.Application"
echo "---------------------"

exit 0