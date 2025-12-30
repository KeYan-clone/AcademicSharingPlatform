import matplotlib.pyplot as plt
import numpy as np
import matplotlib as mpl

# -------- 中文字体配置（与 report.py 中的方式一致） --------
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'WenQuanYi Micro Hei', 'Arial Unicode MS', 'sans-serif']
plt.rcParams['axes.unicode_minus'] = False
try:
    mpl.font_manager._rebuild()
except Exception:
    # 若 _rebuild 在部分环境不可用，忽略即可
    pass

# 手动填入评估数据（来自 filtered_report_20250716_000638.md）
models = ["glm-4", "deepseek-v3", "qwen-max", "yi-34b-chat"]
latencies = [1.87, 6.34, 5.97, 2.72]  # 平均延迟 (秒)
bertscore = [0.599, 0.560, 0.548, 0.579]
source_recall = [0.016, 0.018, 0.024, 0.026]
rouge_l = [0.269, 0.182, 0.234, 0.248]

# -------- 延迟对比图 --------
plt.figure(figsize=(8, 5), dpi=120)
bars = plt.barh(models, latencies, color="skyblue")
plt.xlabel("平均响应时间 (秒)")
plt.title("模型响应延迟对比")
plt.grid(axis="x", linestyle="--", alpha=0.6)

# 给条形图添加标签
for bar in bars:
    width = bar.get_width()
    plt.text(width + 0.1, bar.get_y() + bar.get_height() / 2,
             f"{width:.2f}s", va='center', fontsize=9)

plt.tight_layout()
plt.savefig("benchmark_latency_custom.png", dpi=120)
plt.close()

# -------- 质量指标对比图 --------
fig, ax = plt.subplots(figsize=(10, 6), dpi=120)
x = np.arange(len(models))
width = 0.25

b1 = ax.bar(x - width, bertscore, width, label="BERTScore-F1")
b2 = ax.bar(x, source_recall, width, label="Source Recall")
b3 = ax.bar(x + width, rouge_l, width, label="Rouge-L")

ax.set_xlabel("模型")
ax.set_ylabel("得分")
ax.set_title("模型质量指标对比")
ax.set_xticks(x)
ax.set_xticklabels(models)
ax.legend()
ax.grid(axis="y", linestyle="--", alpha=0.6)

# 为每个柱子添加数值标签
def add_labels(bars):
    for bar in bars:
        height = bar.get_height()
        ax.text(bar.get_x() + bar.get_width() / 2, height + 0.005,
                f"{height:.3f}", ha='center', va='bottom', fontsize=8)

add_labels(b1)
add_labels(b2)
add_labels(b3)

plt.tight_layout()
plt.savefig("benchmark_quality_custom.png", dpi=120)
plt.close()

print("已生成图表: benchmark_latency_custom.png, benchmark_quality_custom.png") 