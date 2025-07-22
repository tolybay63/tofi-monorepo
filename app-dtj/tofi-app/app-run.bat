@echo off

rem in JC_JVM java parameters -Dxxx=yyy

set P1=%~dp0_jc/app-run-classpath.txt
if not exist %P1% (
    call jc prepare
)
set CP=@%P1%

set JVM=
set JVM=%JVM% -cp %CP%
set JVM=%JVM% -Djandcode.app.appdir=%~dp0
set JVM=%JVM% -Djandcode.app.cmdname=%~n0
set JVM=%JVM% -Dfile.encoding=UTF-8
set MAIN=tofi.app.main.Main

call jc @ java %JVM% %JC_JVM% %MAIN% %*
