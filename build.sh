#!/bin/bash

# build.sh - Biên dịch dự án mô phỏng LEGv8

# Thư mục mã nguồn
SRC_DIR="src"
# Thư mục chứa file class đã biên dịch
BIN_DIR="bin"
# Thư mục chứa tài nguyên (file config)
RES_DIR="resources"
# File cấu hình lệnh
CONFIG_FILE="instructions_config.csv"

# --- Xóa thư mục bin cũ (tùy chọn, để đảm bảo build sạch) ---
echo "Removing old build directory ($BIN_DIR)..."
rm -rf "$BIN_DIR"

# --- Tạo thư mục bin mới ---
echo "Creating build directory ($BIN_DIR)..."
mkdir -p "$BIN_DIR"
if [ $? -ne 0 ]; then
    echo "Error: Failed to create directory $BIN_DIR. Aborting."
    exit 1
fi

# --- Tìm tất cả các file .java ---
echo "Finding Java source files in $SRC_DIR..."
# Sử dụng find để xử lý tốt hơn các tên file có khoảng trắng (mặc dù không nên có)
find "$SRC_DIR" -name "*.java" > sources.txt
if [ ! -s sources.txt ]; then
    echo "Error: No Java source files found in $SRC_DIR. Aborting."
    rm sources.txt
    exit 1
fi
echo "Found $(wc -l < sources.txt) source files."

# --- Biên dịch ---
echo "Compiling Java source files..."
javac -d "$BIN_DIR" -sourcepath "$SRC_DIR" @sources.txt

# Kiểm tra lỗi biên dịch
if [ $? -ne 0 ]; then
    echo "---------------------"
    echo "BUILD FAILED: Compilation errors detected."
    echo "---------------------"
    rm sources.txt
    exit 1
fi
echo "Compilation successful."
rm sources.txt

# --- Copy file cấu hình ---
CONFIG_SOURCE="$RES_DIR/$CONFIG_FILE"
CONFIG_DEST="$BIN_DIR/$CONFIG_FILE" # Copy vào cùng thư mục class

echo "Copying configuration file ($CONFIG_SOURCE to $CONFIG_DEST)..."
if [ -f "$CONFIG_SOURCE" ]; then
    cp "$CONFIG_SOURCE" "$CONFIG_DEST"
    if [ $? -ne 0 ]; then
        echo "Warning: Failed to copy configuration file."
        # Có thể không phải lỗi nghiêm trọng nếu config được load từ classpath khác
    else
        echo "Configuration file copied."
    fi
else
    echo "Warning: Configuration file $CONFIG_SOURCE not found. Simulator might fail if it relies on this file being copied to $BIN_DIR."
fi

echo "---------------------"
echo "BUILD SUCCESSFUL!"
echo "Output directory: $BIN_DIR"
echo "---------------------"

exit 0