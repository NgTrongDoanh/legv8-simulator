# Makefile for LEGv8 CPU Simulator

# --- Project Configuration ---
SRC_DIR := src
BIN_DIR := bin
RES_DIR := resources
LIB_DIR := lib
MAIN_CLASS := Application # Default main class, can be overridden

# --- Java Configuration ---
JAVAC := javac
JAVA := java
JAVAC_FLAGS := -encoding UTF-8 -Xlint:all,-serial
JAVA_FLAGS :=

# --- OS Specific Configuration ---
RM := rm -rf
MKDIR_P := mkdir -p
CP_R := cp -r
CP_SEP := :
FIND_CMD := find

ifeq ($(OS),Windows_NT)
    RM := rmdir /S /Q
    MKDIR_P := mkdir
    CP_R := xcopy /E /I /Y /Q
    CP_SEP := ;
endif

# --- Classpath Construction ---
COMPILE_CP := $(BIN_DIR)
ifneq ("$(wildcard $(LIB_DIR)/*.jar)","")
    COMPILE_CP := $(COMPILE_CP)$(CP_SEP)$(LIB_DIR)/*
endif

RUN_CP := $(BIN_DIR)
ifneq ("$(wildcard $(LIB_DIR)/*.jar)","")
    RUN_CP := $(RUN_CP)$(CP_SEP)$(LIB_DIR)/*
endif

SOURCES_LIST_FILE := $(BIN_DIR)/sources.list

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
ifeq ($(OS),Windows_NT)
	-$(RM) "$(BIN_DIR)" >nul 2>&1 || true
else
	-$(RM) "$(BIN_DIR)" > /dev/null 2>&1 || true
endif
	$(MKDIR_P) "$(BIN_DIR)"

# Copy resources
copy_resources: $(BIN_DIR)
	@echo "Copying resources from $(RES_DIR) to $(BIN_DIR)/$(RES_DIR)..."
ifeq ($(OS),Windows_NT)
	-if exist "$(BIN_DIR)\$(RES_DIR)" $(RM) "$(BIN_DIR)\$(RES_DIR)" >nul 2>&1
	$(MKDIR_P) "$(BIN_DIR)\$(RES_DIR)"
	if exist "$(RES_DIR)" ( $(CP_R) "$(RES_DIR)\*" "$(BIN_DIR)\$(RES_DIR)\" >nul ) else ( echo Warning: Resource directory '$(RES_DIR)' not found. )
else
	-$(RM) "$(BIN_DIR)/$(RES_DIR)" > /dev/null 2>&1
	$(MKDIR_P) "$(BIN_DIR)/$(RES_DIR)"
	if [ -d "$(RES_DIR)" ]; then \
		$(CP_R) "$(RES_DIR)/." "$(BIN_DIR)/$(RES_DIR)/"; \
	else \
		echo "Warning: Resource directory '$(RES_DIR)' not found. Creating empty one in bin."; \
		$(MKDIR_P) "$(BIN_DIR)/$(RES_DIR)"; \
	fi
endif
	@echo "Resources copied."

# Generate list of source files
$(SOURCES_LIST_FILE):
	@echo "Finding Java source files in $(SRC_DIR)..."
	-$(MKDIR_P) "$(BIN_DIR)"
ifeq ($(OS),Windows_NT)
	cmd /C "for /R $(SRC_DIR) %%%%f in (*.java) do @echo %%%%f" > "$(subst /,\,$(SOURCES_LIST_FILE))"
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
ifeq ($(OS),Windows_NT)
	cd "$(BIN_DIR)" && $(JAVA) $(JAVA_FLAGS) -cp ".;..;..\$(LIB_DIR)\*" $(MAIN_CLASS) $(ARGS)
else
	cd "$(BIN_DIR)" && $(JAVA) $(JAVA_FLAGS) -cp ".:..:../$(LIB_DIR)/*" $(MAIN_CLASS) $(ARGS)
endif
	@echo "--- Simulator Output End ---"

# Clean up build artifacts
clean:
	@echo "Cleaning up build artifacts..."
ifeq ($(OS),Windows_NT)
	if exist "$(BIN_DIR)" ( $(RM) "$(BIN_DIR)" )
	if exist "$(subst /,\,$(SOURCES_LIST_FILE))" ( del "$(subst /,\,$(SOURCES_LIST_FILE))" )
else
	$(RM) "$(BIN_DIR)"
	$(RM) -f "$(SOURCES_LIST_FILE)"
endif
	@echo "Cleanup complete."

# Help message
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
	@echo "  SRC_DIR       - Source directory (default: $(SRC_DIR))"
	@echo "  BIN_DIR       - Build output directory (default: $(BIN_DIR))"
	@echo "  LIB_DIR       - Libraries directory (default: $(LIB_DIR))"
	@echo "  RES_DIR       - Resources directory (default: $(RES_DIR))"
