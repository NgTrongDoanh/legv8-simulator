@echo off
setlocal

REM --- Cau hinh bien ---
set BIN_DIR=bin
set LIB_DIR=lib
set MAIN_CLASS=Application

REM --- Kiem tra thu muc build ---
if not exist "%BIN_DIR%" (
    echo Loi: Khong tim thay thu muc build '%BIN_DIR%'.
    echo Vui long chay build.bat truoc.
    exit /b 1
)

REM --- Xay dung Classpath ---
set RUN_CP=%BIN_DIR%
if exist "%LIB_DIR%" (
    dir /B "%LIB_DIR%\*.jar" >nul 2>&1
    if not errorlevel 1 (
        echo Dang them thu vien tu %LIB_DIR% vao classpath chay...
        set RUN_CP=%RUN_CP%;%LIB_DIR%\*
    ) else (
        echo Thong tin: Thu muc thu vien '%LIB_DIR%' rong. Chay khong can thu vien ngoai.
    )
) else (
    echo Thong tin: Khong tim thay thu muc thu vien '%LIB_DIR%'. Chay khong can thu vien ngoai.
)
echo Su dung Classpath: %RUN_CP%

REM --- Chay ung dung ---
echo Dang chay trinh mo phong (%MAIN_CLASS%)...
echo --- Bat dau dau ra trinh mo phong ---
java -cp "%RUN_CP%" %MAIN_CLASS% %*
set JAVA_EXIT_CODE=%ERRORLEVEL%
echo --- Ket thuc dau ra trinh mo phong ---

if not %JAVA_EXIT_CODE%==0 (
    echo Trinh mo phong ket thuc voi ma loi: %JAVA_EXIT_CODE%
    exit /b %JAVA_EXIT_CODE%
) else (
    echo Trinh mo phong hoan thanh thanh cong.
    exit /b 0
)