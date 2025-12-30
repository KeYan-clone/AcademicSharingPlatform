#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
模型评估结果分析脚本

分析不同模型的测试结果，计算指标，生成报告
运行方式：
    python -m ai_service.benchmark.report --input ai_service/benchmark/results.json [--output report.md]
"""

import json
import argparse
import logging
from pathlib import Path
from typing import Dict, List, Any
import sys
import os
import numpy as np
import pandas as pd
from collections import defaultdict
import matplotlib.pyplot as plt
from tabulate import tabulate
import matplotlib as mpl

# 确保ai_service包可导入
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger("benchmark_report")

try:
    import bert_score
    from rouge_score import rouge_scorer
    HAS_METRICS = True
except ImportError:
    logger.warning("未安装bert_score或rouge_score，将无法计算语义相似度指标")
    logger.warning("请运行: pip install bert-score rouge-score")
    HAS_METRICS = False

# 配置matplotlib支持中文显示
def setup_chinese_font():
    """配置matplotlib支持中文字体"""
    try:
        # 检测操作系统
        plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'WenQuanYi Micro Hei', 'Arial Unicode MS', 'sans-serif']
        plt.rcParams['axes.unicode_minus'] = False
        
        # 验证字体是否可用
        mpl.font_manager._rebuild()
        
        # 测试中文显示
        plt.figure(figsize=(1, 1))
        plt.text(0.1, 0.1, '测试')
        plt.close()
        logger.info("中文字体配置成功")
    except Exception as e:
        logger.warning(f"中文字体配置失败，图表中的中文可能无法正确显示: {str(e)}")

# 初始化中文字体配置
setup_chinese_font()


class ModelEvaluator:
    """模型评估工具类"""
    
    def __init__(self, results_file: str):
        """初始化评估器
        
        Args:
            results_file: 结果JSON文件路径
        """
        self.results = self._load_results(results_file)
        self.models = self._extract_models()
        self.questions = self._extract_questions()
        
        # 初始化评分器
        if HAS_METRICS:
            self.rouge_scorer = rouge_scorer.RougeScorer(['rougeL'], use_stemmer=True)
    
    def _load_results(self, results_file: str) -> List[Dict]:
        """加载结果文件
        
        Args:
            results_file: 结果JSON文件路径
            
        Returns:
            结果列表
        """
        try:
            with open(results_file, 'r', encoding='utf-8') as f:
                results = json.load(f)
            logger.info(f"已加载 {len(results)} 条结果记录")
            return results
        except Exception as e:
            logger.error(f"加载结果文件失败: {str(e)}")
            return []
    
    def _extract_models(self) -> List[str]:
        """提取所有模型名称
        
        Returns:
            模型名称列表
        """
        models = sorted(list(set([r["model"] for r in self.results if "model" in r])))
        logger.info(f"检测到 {len(models)} 个模型: {', '.join(models)}")
        return models
    
    def _extract_questions(self) -> List[str]:
        """提取所有问题ID
        
        Returns:
            问题ID列表
        """
        questions = sorted(list(set([r["id"] for r in self.results if "id" in r])))
        logger.info(f"检测到 {len(questions)} 个问题")
        return questions
    
    def calculate_metrics(self) -> Dict[str, Dict[str, float]]:
        """计算各模型的评估指标
        
        Returns:
            模型评估指标字典
        """
        metrics = {}
        
        for model in self.models:
            model_results = [r for r in self.results if r.get("model") == model]
            
            # 基本指标
            avg_latency = np.mean([r.get("total_time", 0) for r in model_results if "total_time" in r])
            avg_tokens_out = np.mean([r.get("tokens_out", 0) for r in model_results if "tokens_out" in r])
            
            # 计算高级指标
            metrics[model] = {
                "avg_latency": avg_latency,
                "avg_tokens": avg_tokens_out,
                "sample_count": len(model_results)
            }
            
            # 如果安装了相关库，计算语义相似度
            if HAS_METRICS:
                bert_scores = []
                rouge_scores = []
                source_recall_scores = []
                
                for result in model_results:
                    if "answer" in result and "ground_truth" in result and result["ground_truth"]:
                        pred = result["answer"]
                        ref = result["ground_truth"]
                        
                        # BERTScore
                        try:
                            P, R, F1 = bert_score.score([pred], [ref], lang="zh", verbose=False)
                            bert_scores.append(F1.item())
                        except Exception as e:
                            logger.warning(f"计算BERTScore失败: {str(e)}")
                        
                        # Rouge-L
                        try:
                            rouge = self.rouge_scorer.score(ref, pred)
                            rouge_scores.append(rouge["rougeL"].fmeasure)
                        except Exception as e:
                            logger.warning(f"计算Rouge-L失败: {str(e)}")
                        
                        # Source Recall (检查回答中包含多少检索内容)
                        if "context" in result and result["context"]:
                            try:
                                source_texts = [ctx["content"] for ctx in result["context"]]
                                source_chunks = []
                                for source in source_texts:
                                    # 将源文本分成小块，以更好地匹配
                                    chunks = [source[i:i+30] for i in range(0, len(source), 20) if len(source[i:i+30]) >= 5]
                                    source_chunks.extend(chunks)
                                
                                source_matches = 0
                                for chunk in source_chunks:
                                    if chunk in pred:
                                        source_matches += 1
                                
                                if source_chunks:
                                    recall = source_matches / len(source_chunks)
                                    source_recall_scores.append(recall)
                            except Exception as e:
                                logger.warning(f"计算Source Recall失败: {str(e)}")
                
                # 更新指标
                if bert_scores:
                    metrics[model]["bertscore_f1"] = np.mean(bert_scores)
                if rouge_scores:
                    metrics[model]["rouge_l"] = np.mean(rouge_scores)
                if source_recall_scores:
                    metrics[model]["source_recall"] = np.mean(source_recall_scores)
        
        return metrics
    
    def generate_report(self, output_file: str = None) -> str:
        """生成评估报告
        
        Args:
            output_file: 输出文件路径
            
        Returns:
            报告文本
        """
        metrics = self.calculate_metrics()
        
        # 创建报告
        report = []
        report.append("# 模型评估报告\n")
        report.append(f"## 概览\n")
        report.append(f"- 评估模型数: {len(self.models)}")
        report.append(f"- 测试问题数: {len(self.questions)}")
        report.append("\n## 评估指标\n")
        
        # 创建指标表格
        headers = ["模型", "BERTScore-F1 ↑", "Source Recall ↑", "Rouge-L ↑", "平均延迟(s) ↓", "输出Token数 ↓"]
        rows = []
        
        for model in self.models:
            model_metrics = metrics[model]
            row = [
                model,
                f"{model_metrics.get('bertscore_f1', 0):.3f}",
                f"{model_metrics.get('source_recall', 0):.3f}",
                f"{model_metrics.get('rouge_l', 0):.3f}",
                f"{model_metrics.get('avg_latency', 0):.2f}",
                f"{int(model_metrics.get('avg_tokens', 0))}"
            ]
            rows.append(row)
        
        report.append(tabulate(rows, headers, tablefmt="pipe"))
        report.append("\n\n## 详细分析\n")
        
        # 生成可视化图表
        self._generate_charts(metrics)
        
        if os.path.exists("benchmark_latency.png"):
            report.append("\n### 响应延迟对比\n")
            report.append("![响应延迟对比](./benchmark_latency.png)\n")
        
        if os.path.exists("benchmark_quality.png"):
            report.append("\n### 回答质量对比\n")
            report.append("![回答质量对比](./benchmark_quality.png)\n")
        
        # 样本分析
        report.append("\n### 典型问题回答样本\n")
        
        # 选择几个典型问题作为样本
        if len(self.questions) > 3:
            sample_questions = self.questions[:3]
        else:
            sample_questions = self.questions
            
        for q_id in sample_questions:
            q_results = [r for r in self.results if r.get("id") == q_id]
            if not q_results:
                continue
                
            query = q_results[0].get("query", "未知问题")
            ground_truth = q_results[0].get("ground_truth", "无参考答案")
            
            report.append(f"\n#### 问题: {query}\n")
            report.append(f"**参考答案**: {ground_truth}\n")
            
            for model in self.models:
                model_results = [r for r in q_results if r.get("model") == model]
                if not model_results:
                    continue
                    
                answer = model_results[0].get("answer", "无回答")
                report.append(f"\n**{model}回答**:\n{answer}\n")
        
        report_text = "\n".join(report)
        
        # 保存报告
        if output_file:
            try:
                with open(output_file, 'w', encoding='utf-8') as f:
                    f.write(report_text)
                logger.info(f"报告已保存至: {output_file}")
            except Exception as e:
                logger.error(f"保存报告失败: {str(e)}")
        
        return report_text
    
    def _generate_charts(self, metrics: Dict[str, Dict[str, float]]):
        """生成评估图表
        
        Args:
            metrics: 模型指标数据
        """
        try:
            # 设置中文字体
            plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'WenQuanYi Micro Hei', 'Arial Unicode MS', 'sans-serif']
            plt.rcParams['axes.unicode_minus'] = False
            
            # 延迟对比图
            models = list(metrics.keys())
            latencies = [metrics[m].get("avg_latency", 0) for m in models]
            
            plt.figure(figsize=(10, 6), dpi=100)
            bars = plt.barh(models, latencies, color="skyblue")
            plt.xlabel("平均响应时间 (秒)", fontsize=12)
            plt.title("模型响应延迟对比", fontsize=14)
            plt.grid(axis="x", linestyle="--", alpha=0.7)
            
            # 添加数值标签
            for bar in bars:
                width = bar.get_width()
                plt.text(width + 0.1, bar.get_y() + bar.get_height()/2, f"{width:.2f}s", 
                         va='center', fontsize=10)
            
            plt.tight_layout()
            plt.savefig("benchmark_latency.png", dpi=100)
            plt.close()
            
            # 质量指标对比图
            if all(["bertscore_f1" in metrics[m] for m in models]):
                fig, ax = plt.subplots(figsize=(12, 8), dpi=100)
                
                x = np.arange(len(models))
                width = 0.2
                
                bertscore = [metrics[m].get("bertscore_f1", 0) for m in models]
                recall = [metrics[m].get("source_recall", 0) for m in models]
                rouge = [metrics[m].get("rouge_l", 0) for m in models]
                
                ax.bar(x - width, bertscore, width, label="BERTScore-F1")
                ax.bar(x, recall, width, label="Source Recall")
                ax.bar(x + width, rouge, width, label="Rouge-L")
                
                ax.set_xticks(x)
                ax.set_xticklabels(models)
                ax.set_ylabel("得分", fontsize=12)
                ax.set_title("模型质量指标对比", fontsize=14)
                ax.legend()
                ax.grid(axis="y", linestyle="--", alpha=0.7)
                
                # 添加数值标签
                for i, v in enumerate(bertscore):
                    ax.text(i - width, v + 0.01, f"{v:.3f}", ha='center', va='bottom', fontsize=8)
                for i, v in enumerate(recall):
                    ax.text(i, v + 0.01, f"{v:.3f}", ha='center', va='bottom', fontsize=8)
                for i, v in enumerate(rouge):
                    ax.text(i + width, v + 0.01, f"{v:.3f}", ha='center', va='bottom', fontsize=8)
                
                plt.tight_layout()
                plt.savefig("benchmark_quality.png", dpi=100)
                plt.close()
            
        except Exception as e:
            logger.error(f"生成图表失败: {str(e)}")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="分析模型评估结果并生成报告")
    parser.add_argument("--input", required=True, help="评估结果JSON文件路径")
    parser.add_argument("--output", default="ai_service/benchmark/model_report.md", help="输出报告文件路径")
    
    args = parser.parse_args()
    
    # 验证文件是否存在
    if not os.path.exists(args.input):
        logger.error(f"结果文件不存在: {args.input}")
        return
    
    # 分析结果并生成报告
    evaluator = ModelEvaluator(args.input)
    evaluator.generate_report(args.output)
    logger.info("报告生成完成")


if __name__ == "__main__":
    main()
