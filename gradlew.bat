@ECHO OFF
SETLOCAL
WHERE gradle >NUL 2>NUL
IF %ERRORLEVEL% NEQ 0 (
  ECHO Error: gradle no esta instalado en el sistema.
  EXIT /B 1
)
CALL gradle %*
