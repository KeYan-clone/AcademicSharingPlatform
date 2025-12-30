#!/bin/bash
# 模型API密钥设置脚本

# 检查配置目录是否存在，不存在则创建
CONFIG_DIR="$HOME/.config/edusoft"
if [ ! -d "$CONFIG_DIR" ]; then
    mkdir -p "$CONFIG_DIR"
    echo "创建配置目录: $CONFIG_DIR"
fi

# 环境变量配置文件路径
ENV_FILE="$CONFIG_DIR/.env"

# 提示用户输入各个模型的API密钥
echo "请输入各个模型的API密钥（如果没有可以留空）"

# DeepSeek API Key
read -p "DeepSeek API Key: " DEEPSEEK_KEY
if [ ! -z "$DEEPSEEK_KEY" ]; then
    grep -v "DEEPSEEK_API_KEY" "$ENV_FILE" > "$ENV_FILE.tmp" 2>/dev/null || touch "$ENV_FILE.tmp"
    echo "DEEPSEEK_API_KEY=$DEEPSEEK_KEY" >> "$ENV_FILE.tmp"
    mv "$ENV_FILE.tmp" "$ENV_FILE"
    echo "已设置 DEEPSEEK_API_KEY"
fi

# Qwen API Key (DashScope)
read -p "DashScope API Key (Qwen): " DASHSCOPE_KEY
if [ ! -z "$DASHSCOPE_KEY" ]; then
    grep -v "DASHSCOPE_API_KEY" "$ENV_FILE" > "$ENV_FILE.tmp" 2>/dev/null || touch "$ENV_FILE.tmp"
    echo "DASHSCOPE_API_KEY=$DASHSCOPE_KEY" >> "$ENV_FILE.tmp"
    mv "$ENV_FILE.tmp" "$ENV_FILE"
    echo "已设置 DASHSCOPE_API_KEY"
fi

# ChatGLM API Key (ZhipuAI)
read -p "ZhipuAI API Key (ChatGLM): " ZHIPUAI_KEY
if [ ! -z "$ZHIPUAI_KEY" ]; then
    grep -v "ZHIPUAI_API_KEY" "$ENV_FILE" > "$ENV_FILE.tmp" 2>/dev/null || touch "$ENV_FILE.tmp"
    echo "ZHIPUAI_API_KEY=$ZHIPUAI_KEY" >> "$ENV_FILE.tmp"
    mv "$ENV_FILE.tmp" "$ENV_FILE"
    echo "已设置 ZHIPUAI_API_KEY"
fi

# Baichuan API Key
read -p "Baichuan API Key: " BAICHUAN_KEY
if [ ! -z "$BAICHUAN_KEY" ]; then
    grep -v "BAICHUAN_API_KEY" "$ENV_FILE" > "$ENV_FILE.tmp" 2>/dev/null || touch "$ENV_FILE.tmp"
    echo "BAICHUAN_API_KEY=$BAICHUAN_KEY" >> "$ENV_FILE.tmp"
    mv "$ENV_FILE.tmp" "$ENV_FILE"
    echo "已设置 BAICHUAN_API_KEY"
fi

# Yi API Key
read -p "Yi API Key: " YI_KEY
if [ ! -z "$YI_KEY" ]; then
    grep -v "YI_API_KEY" "$ENV_FILE" > "$ENV_FILE.tmp" 2>/dev/null || touch "$ENV_FILE.tmp"
    echo "YI_API_KEY=$YI_KEY" >> "$ENV_FILE.tmp"
    mv "$ENV_FILE.tmp" "$ENV_FILE"
    echo "已设置 YI_API_KEY"
fi

echo ""
echo "环境变量已配置完成，保存在 $ENV_FILE"
echo "可以通过以下命令加载环境变量："
echo "source $ENV_FILE"
echo ""

# 询问是否立即加载环境变量
read -p "是否立即加载环境变量? (y/n): " LOAD_ENV
if [[ $LOAD_ENV == "y" || $LOAD_ENV == "Y" ]]; then
    source "$ENV_FILE"
    echo "环境变量已加载"
fi

# 显示配置指南
echo ""
echo "=== 模型基准测试环境配置指南 ==="
echo "1. 请确保已安装所需依赖:"
echo "   pip install -r benchmark/requirements.txt"
echo ""
echo "2. 运行模型评估:"
echo "   python -m ai_service.benchmark.compare_models --models deepseek-v3 qwen-max --dataset ai_service/data/eval_questions.json --out results.json --limit 10"
echo ""
echo "3. 生成评估报告:"
echo "   python -m ai_service.benchmark.report --input results.json --output model_report.md"
echo "=========================================" 