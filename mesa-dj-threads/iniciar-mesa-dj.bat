@echo off
cd /d "%~dp0src"

echo Compilando o projeto...
javac *.java

if errorlevel 1 (
    echo.
    echo Houve um erro ao compilar. Veja as mensagens acima.
    pause
    exit /b 1
)

echo Iniciando o servidor da Mesa DJ...
start "Mesa DJ - Servidor (nao feche esta janela)" cmd /k java ServidorWeb

timeout /t 2 /nobreak >nul
start http://localhost:8080

echo.
echo Pronto! O navegador deve abrir sozinho em alguns segundos.
echo Para encerrar a Mesa DJ, feche a janela "Mesa DJ - Servidor".
pause >nul
