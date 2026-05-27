@echo off
title Khoi Chay He Thong Dat Hang Nhap Khau - Nhom 18
echo ============================================================
echo   KHOI CHAY HE THONG DAT HANG NHAP KHAU - NHOM 18
echo ============================================================
echo.
cd /d "C:\Users\Legion\OneDrive\Máy tính\Import System"
echo Dang lam viec tai thu muc: %CD%
echo.
echo [1/2] Dang clean va bien dich ma nguon...
call mvn clean compile
if %ERRORLEVEL% neq 0 (
    echo.
    echo [LOI] Bien dich that bai! Vui long kiem tra lai.
    pause
    exit /b %ERRORLEVEL%
)
echo.
echo [2/2] Dang khoi chay ung dung JavaFX GUI...
call mvn javafx:run
if %ERRORLEVEL% neq 0 (
    echo.
    echo [LOI] Co loi xay ra khi chay ung dung!
    pause
    exit /b %ERRORLEVEL%
)
pause
