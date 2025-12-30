@echo off
:: 结果过滤脚本 (Windows版)
:: 从已有的评估结果中过滤掉指定模型的数据，并重新生成报告

:: 从配置文件加载环境变量
set CONFIG_FILE=%USERPROFILE%\.config\edusoft\.env
if exist "%CONFIG_FILE%" (
    echo 从配置文件加载API密钥: %CONFIG_FILE%
    for /f "tokens=*" %%a in (%CONFIG_FILE%) do (
        set %%a
    )
    echo API密钥加载完成
)

:: 检查依赖
pip list | findstr "bert-score rouge-score matplotlib" > nul
if %errorlevel% neq 0 (
    echo 警告: 未安装所有依赖项，可能导致某些指标无法计算
    echo 正在安装依赖...
    pip install -r ai_service\benchmark\requirements.txt
)

:: 使用固定的合并结果文件
set MERGED_RESULT=batch_results\merged_results_20250715_220031.json

if not exist "%MERGED_RESULT%" (
    echo 未找到合并结果文件: %MERGED_RESULT%
    echo 请确认文件路径是否正确
    pause
    exit /b 1
)

echo 使用合并结果文件: %MERGED_RESULT%

:: 设置输出文件名
set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set OUTPUT_FILE=batch_results\filtered_results_%TIMESTAMP%.json
set REPORT_FILE=batch_results\filtered_report_%TIMESTAMP%.md

:: 运行过滤脚本
echo 正在过滤结果并重新生成报告...
python filter_results.py --input %MERGED_RESULT% --output %OUTPUT_FILE% --report %REPORT_FILE% --exclude baichuan2-13b-chat

if %errorlevel% neq 0 (
    echo 过滤结果失败!
    pause
    exit /b 1
)

:: 复制图表到项目根目录
echo 正在复制图表到项目根目录...
copy benchmark_latency.png ..\..\ > nul
copy benchmark_quality.png ..\..\ > nul

echo 处理完成!
echo 过滤后的结果: %OUTPUT_FILE%
echo 新的评估报告: %REPORT_FILE%
echo 图表已复制到项目根目录

pause 