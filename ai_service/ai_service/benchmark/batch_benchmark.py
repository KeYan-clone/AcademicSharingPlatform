#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
批量模型评估脚本

将大量问题分批评估，以避免一次性处理过多问题导致的超时或内存问题
并将多批次结果合并为最终综合报告

运行方式：
    python -m ai_service.benchmark.batch_benchmark
    
默认将eval_questions.json中的问题分为10批（每批20个），依次评估，并汇总结果
"""

import json
import time
import argparse
import logging
import os
import sys
import subprocess
from pathlib import Path
from typing import Dict, List, Any
from collections import defaultdict
import numpy as np
import shutil
import math

# 确保ai_service包可导入
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger("batch_benchmark")


def split_questions(questions_file: str, batch_size: int) -> List[List[Dict]]:
    """将问题数据集分割成多个批次
    
    Args:
        questions_file: 问题数据集文件路径
        batch_size: 每批问题数量
        
    Returns:
        分割后的批次列表
    """
    try:
        # 加载所有问题
        with open(questions_file, 'r', encoding='utf-8') as f:
            all_questions = json.load(f)
        
        logger.info(f"共加载 {len(all_questions)} 个问题")
        
        # 计算批次数量
        num_batches = math.ceil(len(all_questions) / batch_size)
        
        # 分割成批次
        batches = []
        for i in range(num_batches):
            start_idx = i * batch_size
            end_idx = min((i + 1) * batch_size, len(all_questions))
            batches.append(all_questions[start_idx:end_idx])
        
        logger.info(f"已将问题分为 {len(batches)} 批，每批约 {batch_size} 个问题")
        return batches
    
    except Exception as e:
        logger.error(f"分割问题失败: {str(e)}")
        return []


def save_batch_questions(batch: List[Dict], output_file: str):
    """保存一批问题到文件
    
    Args:
        batch: 一批问题
        output_file: 输出文件路径
    """
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(batch, f, ensure_ascii=False, indent=2)
        logger.info(f"批次问题已保存至: {output_file}")
    except Exception as e:
        logger.error(f"保存批次问题失败: {str(e)}")


def run_benchmark(models: List[str], questions_file: str, output_file: str):
    """运行单批次评估
    
    Args:
        models: 模型列表
        questions_file: 问题文件路径
        output_file: 结果输出路径
    """
    cmd = [
        sys.executable, 
        "-m", "ai_service.benchmark.compare_models", 
        "--models", *models,
        "--dataset", questions_file, 
        "--out", output_file
    ]
    
    logger.info(f"运行命令: {' '.join(cmd)}")
    
    try:
        subprocess.run(cmd, check=True)
        logger.info(f"批次评估完成，结果保存至: {output_file}")
        return True
    except subprocess.CalledProcessError as e:
        logger.error(f"批次评估失败: {e}")
        return False


def merge_results(result_files: List[str], output_file: str):
    """合并多个结果文件
    
    Args:
        result_files: 结果文件路径列表
        output_file: 合并后的输出文件路径
    """
    all_results = []
    
    try:
        for file in result_files:
            if os.path.exists(file):
                with open(file, 'r', encoding='utf-8') as f:
                    batch_results = json.load(f)
                all_results.extend(batch_results)
                logger.info(f"已加载结果文件: {file}, {len(batch_results)} 条结果")
            else:
                logger.warning(f"结果文件不存在: {file}")
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(all_results, f, ensure_ascii=False, indent=2)
        
        logger.info(f"已合并 {len(all_results)} 条结果到: {output_file}")
        return len(all_results) > 0
    
    except Exception as e:
        logger.error(f"合并结果失败: {str(e)}")
        return False


def generate_final_report(merged_results: str, output_report: str):
    """生成最终综合报告
    
    Args:
        merged_results: 合并后的结果文件路径
        output_report: 输出报告文件路径
    """
    cmd = [
        sys.executable, 
        "-m", "ai_service.benchmark.report", 
        "--input", merged_results, 
        "--output", output_report
    ]
    
    logger.info(f"生成综合报告: {' '.join(cmd)}")
    
    try:
        subprocess.run(cmd, check=True)
        logger.info(f"综合报告已生成: {output_report}")
        return True
    except subprocess.CalledProcessError as e:
        logger.error(f"生成综合报告失败: {e}")
        return False


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="批量运行模型评估")
    parser.add_argument("--models", nargs="+", 
                        default=["deepseek-v3", "qwen-max", "glm-4", "yi-34b-chat"],
                        help="要评估的模型列表")
    parser.add_argument("--questions", default="ai_service/data/eval_questions.json",
                        help="问题数据集路径")
    parser.add_argument("--batch-size", type=int, default=20,
                        help="每批次问题数量")
    parser.add_argument("--output-dir", default="ai_service/benchmark/batch_results",
                        help="批次结果输出目录")
    parser.add_argument("--skip-existing", action="store_true",
                        help="跳过已有的批次结果")
    args = parser.parse_args()
    
    # 创建输出目录
    os.makedirs(args.output_dir, exist_ok=True)
    os.makedirs(os.path.join(args.output_dir, "batches"), exist_ok=True)
    
    # 获取时间戳
    timestamp = time.strftime("%Y%m%d_%H%M%S")
    
    # 分割问题
    batches = split_questions(args.questions, args.batch_size)
    if not batches:
        logger.error("分割问题失败，退出程序")
        return
    
    # 存储各批次的结果文件路径
    batch_result_files = []
    
    # 运行每个批次
    for i, batch in enumerate(batches):
        batch_id = f"batch_{i+1:02d}"
        
        # 保存批次问题
        batch_questions_file = os.path.join(args.output_dir, "batches", f"{batch_id}_questions.json")
        save_batch_questions(batch, batch_questions_file)
        
        # 设置批次结果文件
        batch_results_file = os.path.join(args.output_dir, "batches", f"{batch_id}_results.json")
        batch_result_files.append(batch_results_file)
        
        # 检查是否需要跳过
        if args.skip_existing and os.path.exists(batch_results_file):
            logger.info(f"批次 {batch_id} 结果已存在，跳过评估")
            continue
        
        # 运行批次评估
        logger.info(f"开始评估批次 {batch_id} ({len(batch)} 个问题)")
        success = run_benchmark(args.models, batch_questions_file, batch_results_file)
        
        if not success:
            logger.warning(f"批次 {batch_id} 评估失败")
    
    # 合并结果
    merged_results_file = os.path.join(args.output_dir, f"merged_results_{timestamp}.json")
    success = merge_results(batch_result_files, merged_results_file)
    
    if not success:
        logger.error("合并结果失败，无法生成综合报告")
        return
    
    # 生成综合报告
    final_report_file = os.path.join(args.output_dir, f"final_report_{timestamp}.md")
    generate_final_report(merged_results_file, final_report_file)
    
    # 拷贝图表到输出目录
    for img_file in ["benchmark_latency.png", "benchmark_quality.png"]:
        if os.path.exists(img_file):
            shutil.copy(img_file, os.path.join(args.output_dir, f"{img_file.split('.')[0]}_{timestamp}.png"))
    
    logger.info(f"批量评估完成！")
    logger.info(f"合并结果: {merged_results_file}")
    logger.info(f"综合报告: {final_report_file}")


if __name__ == "__main__":
    main()
