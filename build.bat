@echo off
setlocal enabledelayedexpansion

REM --- Cau hinh bien ---
set SRC_DIR=src
set BIN_DIR=bin
set RES_DIR=resources
set LIB_DIR=lib
set SOURCES_LIST=sources.txt

REM --- Kiem tra trinh bien dich Java ---
where javac >nul 2>&1
if errorlevel 1 (
    echo Loi: Khong tim thay lenh 'javac'. Vui long cai dat JDK va them vao PATH. Huy bo.
    exit /b 1
)
echo Tim thay trinh bien dich Java (javac).

REM --- Xoa thu muc build cu ---
echo Dang xoa thu muc build cu (%BIN_DIR%)...
if exist "%BIN_DIR%" (
    rmdir /s /q "%BIN_DIR%" >nul 2>&1
    if errorlevel 1 (
        echo Canh bao: Khong the xoa hoan toan thu muc %BIN_DIR%. Co the co file dang duoc su dung. Tiep tuc...
    )
)

REM --- Tao thu muc build moi ---
echo Dang tao thu muc build (%BIN_DIR%)...
mkdir "%BIN_DIR%"
if errorlevel 1 (
    echo Loi: Khong the tao thu muc %BIN_DIR%. Huy bo.
    exit /b 1
)

REM --- Sao chep tai nguyen ---
if exist "%RES_DIR%" (
    echo Dang sao chep tai nguyen tu %RES_DIR% den %BIN_DIR%\%RES_DIR%...
    mkdir "%BIN_DIR%\%RES_DIR%" >nul 2>&1
    xcopy /E /I /Y /Q "%RES_DIR%\*" "%BIN_DIR%\%RES_DIR%\" >nul
    if errorlevel 1 (
        echo Loi: Khong the sao chep tai nguyen. Huy bo.
        rmdir /s /q "%BIN_DIR%"
        exit /b 1
    )
    echo Tai nguyen da duoc sao chep thanh cong.
) else (
    echo Canh bao: Khong tim thay thu muc tai nguyen '%RES_DIR%'. Bo qua sao chep tai nguyen.
    mkdir "%BIN_DIR%\%RES_DIR%" >nul 2>&1
)

REM --- Tim file ma nguon Java ---
echo Dang tim cac file ma nguon Java trong %SRC_DIR%...
dir /S /B /A-D "%SRC_DIR%\*.java" > "%SOURCES_LIST%" 2>nul

REM --- Kiem tra file danh sach nguon ---
echo DEBUG: Kiem tra su ton tai cua "%SOURCES_LIST%"
if not exist "%SOURCES_LIST%" (
     echo Loi: Khong tao duoc file danh sach nguon "%SOURCES_LIST%". Co the khong co file .java hoac loi quyen ghi. Huy bo.
     exit /b 1
) else (
     echo DEBUG: File "%SOURCES_LIST%" ton tai.
)

REM *** Cach kiem tra kich thuoc file an toan hon ***
set FileSizeCheckPassed=false
for %%F in ("%SOURCES_LIST%") do (
    echo DEBUG: Dang kiem tra kich thuoc file "%%F"
    if %%~zF GTR 0 (
        echo DEBUG: Kich thuoc file %%~zF la > 0. OK.
        set FileSizeCheckPassed=true
    ) else (
        echo DEBUG: Kich thuoc file %%~zF la 0 hoac khong doc duoc.
        set FileSizeCheckPassed=false
    )
    REM Chi chay vong lap mot lan vi chi co mot file trong for nay
    goto :after_size_check
)
REM Neu vong for khong chay (file khong ton tai mac du check o tren?)
echo DEBUG: Vong for kiem tra kich thuoc khong chay duoc. FileSizeCheckPassed van la %FileSizeCheckPassed%
set FileSizeCheckPassed=false

:after_size_check
echo DEBUG: Ket qua kiem tra kich thuoc: FileSizeCheckPassed = %FileSizeCheckPassed%
if "%FileSizeCheckPassed%" == "false" (
    echo Loi: File danh sach nguon "%SOURCES_LIST%" bi rong hoac khong doc duoc kich thuoc. Huy bo.
    if exist "%SOURCES_LIST%" del "%SOURCES_LIST%"
    exit /b 1
)

REM Dem file (khong qua quan trong nhung de debug)
set count=0
for /f %%A in ('findstr /R /N "^" "%SOURCES_LIST%" ^| find /C ":"') do set count=%%A
echo Tim thay %count% file ma nguon.

REM --- Xay dung Classpath ---
set COMPILE_CP=%BIN_DIR%
if exist "%LIB_DIR%" (
    dir /B "%LIB_DIR%\*.jar" >nul 2>&1
    if not errorlevel 1 (
        echo Dang them thu vien tu %LIB_DIR% vao classpath bien dich...
        set COMPILE_CP=%COMPILE_CP%;%LIB_DIR%\*
    ) else (
        echo Canh bao: Thu muc thu vien '%LIB_DIR%' ton tai nhung khong chua file .jar nao.
    )
) else (
    echo Thong tin: Khong tim thay thu muc thu vien '%LIB_DIR%'. Bien dich khong can thu vien ngoai.
)
echo Classpath bien dich: %COMPILE_CP%

REM --- Bien dich file ma nguon Java ---
echo Dang bien dich cac file ma nguon Java...
javac -encoding UTF-8 -Xlint:all,-serial -d "%BIN_DIR%" -sourcepath "%SRC_DIR%" -cp "%COMPILE_CP%" "@%SOURCES_LIST%"

if errorlevel 1 (
    echo ---------------------
    echo BUILD THAT BAI: Phat hien loi bien dich.
    echo ---------------------
    if exist "%SOURCES_LIST%" del "%SOURCES_LIST%"
    exit /b 1
)

echo Bien dich thanh cong.
if exist "%SOURCES_LIST%" del "%SOURCES_LIST%"

REM --- Thong bao ket qua ---
echo.
echo ---------------------
echo BUILD THANH CONG!
echo Thu muc dau ra: %BIN_DIR%
echo De chay chuong trinh, su dung: app.bat
echo Vi du: app.bat
echo Hoac tu thu muc goc:
echo java -cp "%BIN_DIR%;%LIB_DIR%\*" Application
echo ---------------------
exit /b 0