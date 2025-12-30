@echo off
:: 批量模型评估脚本 (Windows版)

:: 从配置文件加载环境变量
set CONFIG_FILE=%USERPROFILE%\.config\edusoft\.env
if exist "%CONFIG_FILE%" (
    echo 从配置文件加载API密钥: %CONFIG_FILE%
    for /f "tokens=*" %%a in (%CONFIG_FILE%) do (
        set %%a
    )
    echo API密钥加载完成
) else (
    echo 警告: 未找到配置文件 %CONFIG_FILE%
    echo 请先运行 setup_env.bat 设置API密钥
    pause
    exit /b 1
)

:: 检查依赖
pip list | findstr "bert-score rouge-score matplotlib" > nul
if %errorlevel% neq 0 (
    echo 警告: 未安装所有依赖项，可能导致某些指标无法计算
    echo 正在安装依赖...
    pip install -r ai_service\benchmark\requirements.txt
    if %errorlevel% neq 0 (
        echo 安装依赖失败，请手动运行:
        echo pip install -r ai_service\benchmark\requirements.txt
        pause
        exit /b 1
    )
)

:: 创建结果目录
if not exist "ai_service\benchmark\batch_results" mkdir "ai_service\benchmark\batch_results"

:: 确认是否继续
echo ===================================================
echo          批量模型评估脚本
echo ===================================================
echo 本脚本将:
echo  - 将200个问题分成10批次(每批20个)进行评估
echo  - 评估所有配置的模型（包括 deepseek-v3, qwen-max, glm-4 等）
echo  - 将10批结果合并生成最终报告
echo ===================================================
echo 注意: 完整评估可能需要较长时间（取决于API响应速度）
echo 您可以随时按 Ctrl+C 终止，下次运行时使用 --skip-existing 参数继续
echo ===================================================
set /p CONFIRM="是否继续? (y/n): "

if /i "%CONFIRM%" neq "y" (
    echo 已取消
    exit /b 0
)

:: 运行批量评估脚本
echo 开始批量评估...
python -m ai_service.benchmark.batch_benchmark --models deepseek-v3 qwen-max glm-4 yi-34b-chat

if %errorlevel% neq 0 (
    echo 批量评估失败!
    pause
    exit /b 1
)

echo 批量评估完成！详细结果保存在 ai_service\benchmark\batch_results 目录下
echo 您可以查看最新的 final_report_*.md 文件获取完整评估报告
pause 