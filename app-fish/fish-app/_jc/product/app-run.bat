@echo off

rem in JC_JVM java parameters -Dxxx=yyy

set CP=%~dp0lib;%~dp0lib\*

set JVM=
set JVM=%JVM% -cp %CP%
set JVM=%JVM% -Djandcode.app.appdir=%~dp0
set JVM=%JVM% -Djandcode.app.cmdname=%~n0
set JVM=%JVM% -Dfile.encoding=UTF-8
set MAIN=fish.app.main.Main

java %JVM% %JC_JVM% %MAIN% %*
