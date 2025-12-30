@echo off
:: 模型API密钥设置脚本 (Windows版)

:: 检查配置目录是否存在，不存在则创建
set CONFIG_DIR=%USERPROFILE%\.config\edusoft
if not exist "%CONFIG_DIR%" (
    mkdir "%CONFIG_DIR%"
    echo 创建配置目录: %CONFIG_DIR%
)

:: 环境变量配置文件路径
set ENV_FILE=%CONFIG_DIR%\.env

:: 提示用户输入各个模型的API密钥
echo 请输入各个模型的API密钥（如果没有可以留空）

:: DeepSeek API Key
set /p DEEPSEEK_KEY="DeepSeek API Key: "
if not "%DEEPSEEK_KEY%"=="" (
    echo DEEPSEEK_API_KEY=%DEEPSEEK_KEY%> "%ENV_FILE%.tmp"
    findstr /v "DEEPSEEK_API_KEY" "%ENV_FILE%" >> "%ENV_FILE%.tmp" 2>nul
    move /y "%ENV_FILE%.tmp" "%ENV_FILE%" >nul
    echo 已设置 DEEPSEEK_API_KEY
    setx DEEPSEEK_API_KEY "%DEEPSEEK_KEY%"
)


:: Qwen API Key (DashScope)
set /p DASHSCOPE_KEY="DashScope API Key (Qwen): "
if not "%DASHSCOPE_KEY%"=="" (
    echo DASHSCOPE_API_KEY=%DASHSCOPE_KEY%> "%ENV_FILE%.tmp"
    findstr /v "DASHSCOPE_API_KEY" "%ENV_FILE%" >> "%ENV_FILE%.tmp" 2>nul
    move /y "%ENV_FILE%.tmp" "%ENV_FILE%" >nul
    echo 已设置 DASHSCOPE_API_KEY
    setx DASHSCOPE_API_KEY "%DASHSCOPE_KEY%"
)

:: ChatGLM API Key (ZhipuAI)
set /p ZHIPUAI_KEY="ZhipuAI API Key (ChatGLM): "
if not "%ZHIPUAI_KEY%"=="" (
    echo ZHIPUAI_API_KEY=%ZHIPUAI_KEY%> "%ENV_FILE%.tmp"
    findstr /v "ZHIPUAI_API_KEY" "%ENV_FILE%" >> "%ENV_FILE%.tmp" 2>nul
    move /y "%ENV_FILE%.tmp" "%ENV_FILE%" >nul
    echo 已设置 ZHIPUAI_API_KEY
    setx ZHIPUAI_API_KEY "%ZHIPUAI_KEY%"
)

:: Baichuan API Key
set /p BAICHUAN_KEY="Baichuan API Key: "
if not "%BAICHUAN_KEY%"=="" (
    echo BAICHUAN_API_KEY=%BAICHUAN_KEY%> "%ENV_FILE%.tmp"
    findstr /v "BAICHUAN_API_KEY" "%ENV_FILE%" >> "%ENV_FILE%.tmp" 2>nul
    move /y "%ENV_FILE%.tmp" "%ENV_FILE%" >nul
    echo 已设置 BAICHUAN_API_KEY
    setx BAICHUAN_API_KEY "%BAICHUAN_KEY%"
)

:: Yi API Key
set /p YI_KEY="Yi API Key: "
if not "%YI_KEY%"=="" (
    echo YI_API_KEY=%YI_KEY%> "%ENV_FILE%.tmp"
    findstr /v "YI_API_KEY" "%ENV_FILE%" >> "%ENV_FILE%.tmp" 2>nul
    move /y "%ENV_FILE%.tmp" "%ENV_FILE%" >nul
    echo 已设置 YI_API_KEY
    setx YI_API_KEY "%YI_KEY%"
)

echo.
echo 环境变量已配置完成，保存在 %ENV_FILE%
echo 已设置当前用户的环境变量，可能需要重启命令提示符才能生效
echo.

:: 显示配置指南
echo.
echo === 模型基准测试环境配置指南 ===
echo 1. 请确保已安装所需依赖:
echo    pip install -r benchmark\requirements.txt
echo.
echo 2. 运行模型评估:
echo    python -m ai_service.benchmark.compare_models --models deepseek-v3 qwen-max --dataset ai_service/data/eval_questions.json --out results.json --limit 10
echo.
echo 3. 生成评估报告:
echo    python -m ai_service.benchmark.report --input results.json --output model_report.md
echo =========================================

pause 