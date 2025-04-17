#!/bin/bash

# run.sh - Chạy chương trình mô phỏng LEGv8

# Thư mục chứa file class đã biên dịch và tài nguyên
BIN_DIR="bin"
# Tên lớp chính đầy đủ (bao gồm package)
MAIN_CLASS="simulator.core.Legv8Simulator"

# --- Kiểm tra thư mục bin ---
if [ ! -d "$BIN_DIR" ]; then
    echo "Error: Build directory '$BIN_DIR' not found."
    echo "Please run ./build.sh first."
    exit 1
fi

# --- Chạy chương trình ---
echo "Running simulator ($MAIN_CLASS)..."
echo "Classpath: $BIN_DIR"
echo "--- Simulator Output Start ---"

# Chạy Java, đặt classpath là thư mục bin
# Sử dụng "$@" để truyền bất kỳ tham số dòng lệnh nào vào chương trình Java (nếu cần sau này)
java -cp "$BIN_DIR" "$MAIN_CLASS" "$@"

# Lấy mã thoát của chương trình Java
JAVA_EXIT_CODE=$?

echo "--- Simulator Output End ---"

if [ $JAVA_EXIT_CODE -ne 0 ]; then
    echo "Simulator exited with error code: $JAVA_EXIT_CODE"
    exit $JAVA_EXIT_CODE
else
    echo "Simulator finished successfully."
    exit 0
fi