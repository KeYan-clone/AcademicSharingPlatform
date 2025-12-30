# 模型比较基准测试工具

本目录包含用于评估和比较不同LLM模型在RAG问答场景下表现的工具。

## 功能概述

- 支持评估多种模型（DeepSeek-V3、Qwen-7B-Chat、glm-4、Baichuan2-13B-Chat、yi-34b-chat等）
- 使用统一的RAG检索方案（基于FAISS向量检索）
- 自动计算多种指标（BERTScore-F1、Source Recall、Rouge-L、延迟、Token数等）
- 生成可视化报告（Markdown格式，包含图表）

## 安装依赖

```bash
# 安装基本依赖
pip install -r benchmark/requirements.txt

# 安装主项目依赖（若未安装）
pip install -e .
```

## 环境变量配置

在使用前，请确保设置了相应模型的API密钥环境变量：

- DeepSeek-V3: `DEEPSEEK_API_KEY`
- Qwen-7B-Chat: `DASHSCOPE_API_KEY`(阿里千问)
- glm-4: `ZHIPUAI_API_KEY`（清华智谱）
- Baichuan2-13B-Chat: `BAICHUAN_API_KEY`（
- yi-34b-chat: `YI_API_KEY`

您可以在 `~/.config/edusoft/` 目录下创建环境变量文件，或直接在命令行中设置。

## 使用方法

### 1. 运行模型评估

```bash
python -m ai_service.benchmark.compare_models \
       --models deepseek-v3 qwen-max glm-4 \
       --dataset ai_service/data/eval_questions.json \
       --out ai_service/benchmark/results.json \
       --top_k 4 \
       --limit 20  # 可选，限制测试的问题数量
```

参数说明：
- `--models`：要评估的模型列表，以空格分隔
- `--dataset`：测试问题集的JSON文件路径
- `--out`：结果输出路径
- `--top_k`：检索文档数量
- `--limit`：限制测试的问题数量（可选）

### 2. 生成评估报告

```bash
python -m ai_service.benchmark.report \
       --input ai_service/benchmark/results.json \
       --output ai_service/benchmark/model_report.md
```

参数说明：
- `--input`：评估结果JSON文件路径
- `--output`：输出报告文件路径（可选，默认为`ai_service/benchmark/model_report.md`）

## 评估指标说明

1. **BERTScore-F1**：衡量生成回答与标准答案的语义相似度，基于BERT模型。分数越高越好。

2. **Source Recall**：衡量回答中引用检索片段内容的比例，反映模型对检索内容的利用程度。分数越高越好。

3. **Rouge-L**：衡量生成回答与标准答案的文本重叠度，基于最长公共子序列。分数越高越好。

4. **平均延迟**：从提问到回答的平均耗时（秒）。越低越好。

5. **输出Token数**：模型生成的平均Token数，用于估算成本。

## 数据格式

### 输入数据格式

问题数据集JSON格式：
```json
[
  {
    "id": "q1",
    "query": "问题文本",
    "answer": "标准答案"
  },
  ...
]
```

### 输出结果格式

```json
[
  {
    "id": "q1",
    "model": "deepseek-v3",
    "query": "问题文本",
    "answer": "模型回答",
    "retrieve_time": 0.5,
    "generate_time": 2.3,
    "total_time": 2.8,
    "tokens_out": 120,
    "tokens_in": 450,
    "ground_truth": "标准答案",
    "context": [{"content": "检索片段1", "source": "来源1"}, ...]
  },
  ...
]
```

## 注意事项

1. 评估过程中会调用相应模型的API，请确保有足够的API额度。

2. 计算BERTScore和Rouge-L需要安装额外的依赖包，如果安装失败，系统会跳过这些指标的计算。

3. 生成图表需要matplotlib库，请确保正确安装。

4. 测试数据集应包含标准答案字段，以便计算质量指标。

5. 建议在小批量数据上先测试脚本功能，再进行大规模评估。 