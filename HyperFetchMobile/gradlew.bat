@rem Gradle Wrapper 启动脚本 (Windows)
@rem 单一事实来源：本项目使用 Gradle 8.2（对应 AGP 8.2.0 + JDK 17）
@rem 执行要求：必须安装 JDK 17 及以上版本，并配置好 JAVA_HOME 或 Path

@if "%DEBUG%" == "" @echo off
@rem 允许本地 gradlew 优先级高于系统 gradle
@rem 设置默认字符编码，防止中文路径异常
chcp 65001 > nul

@rem 设置变量延迟展开
@setlocal enabledelayedexpansion

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%
@rem 移除末尾反斜杠
if "%APP_HOME:~-1%"=="\" set APP_HOME=%APP_HOME:~0,-1%

set WRAPPER_JAR="%APP_HOME%\gradle\wrapper\gradle-wrapper.jar"
set WRAPPER_PROPERTIES="%APP_HOME%\gradle\wrapper\gradle-wrapper.properties"
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem === JDK 检查：必须 JDK 17+ ===
java -version >nul 2>&1
if errorlevel 1 (
  echo [ERROR] 未检测到 Java，请安装 JDK 17 及以上版本并设置 JAVA_HOME。
  exit /b 1
)
@rem 提取主版本号
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVAV=%%~v
for /f "delims=." %%i in ("%JAVAV%") do set MAJOR=%%i
set MAJOR=%MAJOR:"=%
if %MAJOR% LSS 17 (
  echo [ERROR] 当前 Java 主版本为 %MAJOR%，本项目要求 JDK 17 及以上。请升级 JDK 后重试。
  exit /b 1
)
echo [OK] JDK 检查通过（版本 %JAVAV%）

@rem === Wrapper 检查与自动修复 ===
if not exist %WRAPPER_JAR% (
  echo [WARN] 未找到 gradle-wrapper.jar，尝试自动生成 Wrapper...
  where gradle >nul 2>&1
  if %ERRORLEVEL%==0 (
    echo [INFO] 检测到系统 Gradle，执行：gradle wrapper --gradle-version 8.2 --distribution-type bin
    pushd "%APP_HOME%"
    call gradle wrapper --gradle-version 8.2 --distribution-type bin
    popd
    if exist %WRAPPER_JAR% (
      echo [OK] Wrapper 生成成功。
      goto run
    )
  )
  echo [ERROR] 无法自动生成 Wrapper。请选择以下任一方式手动处理：
  echo   1) 安装系统 Gradle 8.2，然后执行：
  echo      gradle wrapper --gradle-version 8.2 --distribution-type bin
  echo   2) 使用 Android Studio 打开项目，IDE 会自动生成 Wrapper。
  echo   3) 手动从 GitHub Gradle 官方仓库下载 gradle-wrapper.jar 放入 gradle\wrapper\ 目录。
  exit /b 2
)

:run
@rem === 执行 Wrapper ===
java %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% ^
  "-Dorg.gradle.appname=%APP_BASE_NAME%" ^
  -classpath %WRAPPER_JAR% org.gradle.wrapper.GradleWrapperMain %*

:end
@rem 保存退出码供调用方使用
set EXITCODE=%ERRORLEVEL%
@endlocal & set EXITCODE=%EXITCODE%
exit /b %EXITCODE%
