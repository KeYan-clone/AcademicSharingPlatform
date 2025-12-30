@echo off
:: 模型基准测试运行脚本 (Windows版)

:: 默认参数
set MODELS=deepseek-v3 qwen-max glm-4 baichuan2-13b-chat yi-34b-chat
set DATASET=ai_service\data\eval_questions.json
set OUTPUT_DIR=ai_service\benchmark\results
set LIMIT=10

:: 创建输出目录
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

:: 获取当前时间戳
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "TIMESTAMP=%dt:~0,8%_%dt:~8,6%"
set RESULTS_FILE=%OUTPUT_DIR%\results_%TIMESTAMP%.json
set REPORT_FILE=%OUTPUT_DIR%\report_%TIMESTAMP%.md

:: 显示参数
echo ===== 模型基准测试 =====
echo 模型: %MODELS%
echo 数据集: %DATASET%
echo 结果文件: %RESULTS_FILE%
echo 报告文件: %REPORT_FILE%
echo 问题数量限制: %LIMIT%
echo =======================

:: 检查是否安装了所需依赖
pip list | findstr "bert-score rouge-score matplotlib" > nul
if %errorlevel% neq 0 (
    echo 警告: 未安装所有依赖项，可能导致某些指标无法计算
    echo 请先运行: pip install -r ai_service\benchmark\requirements.txt
    set /p CONTINUE="是否继续? (y/n): "
    if /i "%CONTINUE%" neq "y" (
        echo 已取消
        exit /b 0
    )
)

:: 从配置文件直接读取环境变量
set CONFIG_FILE=%USERPROFILE%\.config\edusoft\.env
if exist "%CONFIG_FILE%" (
    echo 从配置文件加载API密钥: %CONFIG_FILE%
    for /f "tokens=*" %%a in (%CONFIG_FILE%) do (
        set %%a
    )
    echo API密钥加载完成
) else (
    echo 警告: 未找到配置文件 %CONFIG_FILE%
    echo 可能需要先运行 setup_env.bat 设置API密钥
    set /p CONTINUE="是否继续? (y/n): "
    if /i "%CONTINUE%" neq "y" (
        echo 已取消
        exit /b 0
    )
)

:: 运行基准测试
echo 开始运行模型评估...
python -m ai_service.benchmark.compare_models ^
    --models %MODELS% ^
    --dataset %DATASET% ^
    --out %RESULTS_FILE% ^
    --limit %LIMIT%

if %errorlevel% neq 0 (
    echo 模型评估失败!
    exit /b 1
)

:: 生成报告
echo 生成评估报告...
python -m ai_service.benchmark.report ^
    --input %RESULTS_FILE% ^
    --output %REPORT_FILE%

if %errorlevel% neq 0 (
    echo 报告生成失败!
    exit /b 1
)

echo 评估完成!
echo 结果保存在: %RESULTS_FILE%
echo 报告保存在: %REPORT_FILE%

pause 