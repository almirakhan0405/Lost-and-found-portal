@echo off
cd /d "%~dp0"
if not exist target\smart-lost-found-portal-1.0.0.jar (
  echo Building project ^(first run, downloads dependencies^)...
  call mvn -q clean package -DskipTests || exit /b 1
)
java -jar target\smart-lost-found-portal-1.0.0.jar
