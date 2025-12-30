#!/bin/bash
# 模型基准测试运行脚本

# 默认参数
MODELS="deepseek-v3 qwen-max"
DATASET="ai_service/data/eval_questions.json"
OUTPUT_DIR="ai_service/benchmark/results"
LIMIT=10

# 创建输出目录
mkdir -p "$OUTPUT_DIR"

# 获取当前时间戳
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_FILE="$OUTPUT_DIR/results_$TIMESTAMP.json"
REPORT_FILE="$OUTPUT_DIR/report_$TIMESTAMP.md"

# 显示参数
echo "===== 模型基准测试 ====="
echo "模型: $MODELS"
echo "数据集: $DATASET"
echo "结果文件: $RESULTS_FILE"
echo "报告文件: $REPORT_FILE"
echo "问题数量限制: $LIMIT"
echo "======================="

# 检查是否安装了所需依赖
pip list | grep -E 'bert-score|rouge-score|matplotlib' > /dev/null
if [ $? -ne 0 ]; then
    echo "警告: 未安装所有依赖项，可能导致某些指标无法计算"
    echo "请先运行: pip install -r ai_service/benchmark/requirements.txt"
    read -p "是否继续? (y/n): " CONTINUE
    if [[ $CONTINUE != "y" && $CONTINUE != "Y" ]]; then
        echo "已取消"
        exit 0
    fi
fi

# 加载环境变量（如果存在）
ENV_FILE="$HOME/.config/edusoft/.env"
if [ -f "$ENV_FILE" ]; then
    source "$ENV_FILE"
    echo "已加载环境变量文件: $ENV_FILE"
else
    echo "警告: 未找到环境变量文件 $ENV_FILE"
    echo "可能需要先运行 setup_env.sh 设置API密钥"
    read -p "是否继续? (y/n): " CONTINUE
    if [[ $CONTINUE != "y" && $CONTINUE != "Y" ]]; then
        echo "已取消"
        exit 0
    fi
fi

# 运行基准测试
echo "开始运行模型评估..."
python -m ai_service.benchmark.compare_models \
    --models $MODELS \
    --dataset $DATASET \
    --out $RESULTS_FILE \
    --limit $LIMIT

if [ $? -ne 0 ]; then
    echo "模型评估失败!"
    exit 1
fi

# 生成报告
echo "生成评估报告..."
python -m ai_service.benchmark.report \
    --input $RESULTS_FILE \
    --output $REPORT_FILE

if [ $? -ne 0 ]; then
    echo "报告生成失败!"
    exit 1
fi

echo "评估完成!"
echo "结果保存在: $RESULTS_FILE"
echo "报告保存在: $REPORT_FILE" 