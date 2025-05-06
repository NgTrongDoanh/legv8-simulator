# Makefile for LEGv8 CPU Simulator

# --- Project Configuration ---
SRC_DIR := src
BIN_DIR := bin
RES_DIR := resources
LIB_DIR := lib
MAIN_CLASS := Application # Default main class, can be overridden
# MAIN_CLASS := Legv8Simulator # For command-line testing

# --- Java Configuration ---
JAVAC := javac
JAVA := java
JAVAC_FLAGS := -encoding UTF-8 -Xlint:all,-serial # Added -Xlint for more warnings, excluding serial
JAVA_FLAGS :=

# --- OS Specific Configuration ---
# Default to Unix-like settings
RM := rm -rf
MKDIR_P := mkdir -p
CP_R := cp -r
CP_SEP := :
FIND_CMD := find

ifeq ($(OS),Windows_NT)
    RM := rmdir /s /q
    MKDIR_P := mkdir
    CP_R := xcopy /E /I /Y /Q
    CP_SEP := ;
    # For dir /s /b, we need to handle backslashes if SRC_DIR contains them
    # This is a bit tricky with make's find. Sticking to simpler for now.
    # A common workaround for Windows 'find' is to use a temp file with 'dir'
else ifeq ($(shell uname -s | cut -c 1-5),MINGW) # Git Bash or MinGW
    CP_SEP := ; # MinGW java often expects ;
    # RM, MKDIR_P, CP_R usually work as Unix commands here
else ifeq ($(shell uname -s | cut -c 1-6),CYGWIN) # Cygwin
    CP_SEP := ; # Cygwin java often expects ;
    # RM, MKDIR_P, CP_R usually work as Unix commands here
endif

# --- Classpath Construction ---
# Compile Classpath
# Initialize with BIN_DIR
COMPILE_CP := $(BIN_DIR)
# Add libraries if LIB_DIR exists and contains JARs
ifneq ("$(wildcard $(LIB_DIR)/*.jar)","")
    COMPILE_CP := $(COMPILE_CP)$(CP_SEP)$(LIB_DIR)/*
endif

# Runtime Classpath
# Initialize with BIN_DIR
RUN_CP := $(BIN_DIR)
# Add libraries if LIB_DIR exists and contains JARs
ifneq ("$(wildcard $(LIB_DIR)/*.jar)","")
    RUN_CP := $(RUN_CP)$(CP_SEP)$(LIB_DIR)/*
endif

# --- Source Files ---
# Create a list of all .java files
# Using a temporary file for sources list, compatible with @sources.txt
SOURCES_LIST_FILE := $(BIN_DIR)/sources.list

# --- Targets ---
.PHONY: all build run clean help

all: build

# Build the project
build: $(BIN_DIR) copy_resources $(SOURCES_LIST_FILE)
	@echo "Compiling Java source files..."
	@echo "Classpath: $(COMPILE_CP)"
	$(JAVAC) $(JAVAC_FLAGS) -d "$(BIN_DIR)" -sourcepath "$(SRC_DIR)" -cp "$(COMPILE_CP)" @$(SOURCES_LIST_FILE)
	@echo "---------------------"
	@echo "BUILD SUCCESSFUL!"
	@echo "Output directory: $(BIN_DIR)"
	@echo "To run: make run"
	@echo "---------------------"

# Create the build directory
$(BIN_DIR):
	@echo "Creating build directory ($@)..."
	-$(RM) "$@" > /dev/null 2>&1 || del /Q /S "$@" > nul 2>&1 || true # Try Unix, then Windows del
	$(MKDIR_P) "$@"

# Copy resources
copy_resources: $(BIN_DIR)
	@echo "Copying resources from $(RES_DIR) to $(BIN_DIR)/$(RES_DIR)..."
	-$(RM) "$(BIN_DIR)/$(RES_DIR)" > /dev/null 2>&1 || del /Q /S "$(BIN_DIR)/$(RES_DIR)" > nul 2>&1 || true
	$(MKDIR_P) "$(BIN_DIR)/$(RES_DIR)"
ifeq ($(OS),Windows_NT)
	if exist "$(RES_DIR)" ( $(CP_R) "$(RES_DIR)\*" "$(BIN_DIR)\$(RES_DIR)\" > nul ) else ( echo "Warning: Resource directory '$(RES_DIR)' not found." )
else
	if [ -d "$(RES_DIR)" ]; then \
		$(CP_R) "$(RES_DIR)/." "$(BIN_DIR)/$(RES_DIR)/"; \
	else \
		echo "Warning: Resource directory '$(RES_DIR)' not found. Creating empty one in bin."; \
		$(MKDIR_P) "$(BIN_DIR)/$(RES_DIR)"; \
	fi
endif
	@echo "Resources copied."

# Generate the list of source files
$(SOURCES_LIST_FILE):
	@echo "Finding Java source files in $(SRC_DIR)..."
	$(MKDIR_P) $(BIN_DIR) # Ensure BIN_DIR exists for sources.list
ifeq ($(OS),Windows_NT)
	dir /s /b "$(SRC_DIR)\*.java" > "$(subst /,\,$(SOURCES_LIST_FILE))"
	@echo Found `find /v /c "" < "$(subst /,\,$(SOURCES_LIST_FILE))"` source files.
else
	$(FIND_CMD) "$(SRC_DIR)" -name "*.java" > "$(SOURCES_LIST_FILE)"
	@echo "Found `wc -l < $(SOURCES_LIST_FILE) | tr -d ' '` source files."
endif


# Run the application
run: build
	@echo "Running simulator ($(MAIN_CLASS))..."
	@echo "Using Classpath: $(RUN_CP)"
	@echo "--- Simulator Output Start ---"
	cd "$(BIN_DIR)" && $(JAVA) $(JAVA_FLAGS) -cp ".$(CP_SEP)..$(CP_SEP)../$(LIB_DIR)/*" $(MAIN_CLASS) $(ARGS)
	@echo "--- Simulator Output End ---"

# Clean up build artifacts
clean:
	@echo "Cleaning up build artifacts..."
ifeq ($(OS),Windows_NT)
	if exist "$(BIN_DIR)" ( $(RM) "$(BIN_DIR)" )
	if exist "$(subst /,\,$(SOURCES_LIST_FILE))" ( del "$(subst /,\,$(SOURCES_LIST_FILE))" )
else
	$(RM) "$(BIN_DIR)"
	$(RM) -f "$(SOURCES_LIST_FILE)" # remove sources.list if it's not in BIN_DIR
endif
	@echo "Cleanup complete."

# Display help
help:
	@echo "Available targets:"
	@echo "  all           - Build the project (default)."
	@echo "  build         - Compile Java source files and copy resources."
	@echo "  run           - Run the compiled application. Use ARGS=\"your_args\" to pass arguments."
	@echo "                  Example: make run ARGS=\"-debug -file test.asm\""
	@echo "  clean         - Remove build artifacts (the $(BIN_DIR) directory)."
	@echo "  help          - Show this help message."
	@echo ""
	@echo "Configuration Variables (can be overridden):"
	@echo "  MAIN_CLASS    - The main class to run (default: $(MAIN_CLASS))"
	@echo "                  Example: make run MAIN_CLASS=AnotherMainClass"
	@echo "  SRC_DIR       - Source directory (default: $(SRC_DIR))"
	@echo "  BIN_DIR       - Build output directory (default: $(BIN_DIR))"
	@echo "  LIB_DIR       - Libraries directory (default: $(LIB_DIR))"
	@echo "  RES_DIR       - Resources directory (default: $(RES_DIR))"
