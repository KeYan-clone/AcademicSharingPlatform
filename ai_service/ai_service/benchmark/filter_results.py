#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
结果过滤脚本

从已有的评估结果中过滤掉指定模型的数据，并重新生成报告
运行方式：
    python filter_results.py --input <合并结果文件> --exclude baichuan2-13b-chat
"""

import json
import time
import argparse
import logging
import os
import sys
import subprocess
from pathlib import Path
from typing import Dict, List, Any, Tuple

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger("filter_results")


def filter_results(input_file: str, exclude_models: List[str], output_file: str = None) -> Tuple[bool, str]:
    """过滤结果文件，移除指定模型的数据
    
    Args:
        input_file: 输入结果文件路径
        exclude_models: 要排除的模型名称列表
        output_file: 输出文件路径，如果为None则自动生成
        
    Returns:
        (是否成功, 输出文件路径)
    """
    try:
        # 加载原始结果
        with open(input_file, 'r', encoding='utf-8') as f:
            all_results = json.load(f)
        
        original_count = len(all_results)
        logger.info(f"已加载 {original_count} 条结果记录")
        
        # 过滤掉指定模型的结果
        filtered_results = [r for r in all_results if r.get("model") not in exclude_models]
        filtered_count = len(filtered_results)
        
        logger.info(f"过滤后剩余 {filtered_count} 条结果记录 (移除了 {original_count - filtered_count} 条)")
        
        # 如果没有指定输出文件，则自动生成
        if output_file is None:
            timestamp = time.strftime("%Y%m%d_%H%M%S")
            output_file = f"filtered_results_{timestamp}.json"
            
            # 如果输入文件在特定目录，则输出文件也放在同一目录
            input_path = Path(input_file)
            if input_path.parent.name:
                output_file = str(input_path.parent / output_file)
        
        # 保存过滤后的结果
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(filtered_results, f, ensure_ascii=False, indent=2)
        
        logger.info(f"已保存过滤后的结果到: {output_file}")
        return True, output_file
    
    except Exception as e:
        logger.error(f"过滤结果失败: {str(e)}")
        return False, None


def generate_report(results_file: str, output_report: str = None) -> Tuple[bool, str]:
    """生成评估报告
    
    Args:
        results_file: 结果文件路径
        output_report: 输出报告文件路径，如果为None则自动生成
        
    Returns:
        (是否成功, 输出报告文件路径)
    """
    # 如果没有指定输出报告文件，则自动生成
    if output_report is None:
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        output_report = f"filtered_report_{timestamp}.md"
        
        # 如果结果文件在特定目录，则输出报告也放在同一目录
        input_path = Path(results_file)
        if input_path.parent.name:
            output_report = str(input_path.parent / output_report)
    
    # 使用相对路径调用报告生成脚本
    cmd = [
        sys.executable, 
        "report.py", 
        "--input", results_file, 
        "--output", output_report
    ]
    
    logger.info(f"生成报告: {' '.join(cmd)}")
    
    try:
        subprocess.run(cmd, check=True)
        logger.info(f"报告已生成: {output_report}")
        
        # 拷贝生成的图表到报告所在目录
        report_dir = Path(output_report).parent
        for img_file in ["benchmark_latency.png", "benchmark_quality.png"]:
            if os.path.exists(img_file):
                target_path = report_dir / img_file
                import shutil
                shutil.copy(img_file, target_path)
                logger.info(f"已拷贝图表: {img_file} -> {target_path}")
        
        return True, output_report
    except subprocess.CalledProcessError as e:
        logger.error(f"生成报告失败: {e}")
        return False, None


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="过滤评估结果并重新生成报告")
    parser.add_argument("--input", required=True,
                        help="输入结果文件路径")
    parser.add_argument("--output", default=None,
                        help="输出结果文件路径")
    parser.add_argument("--report", default=None,
                        help="输出报告文件路径")
    parser.add_argument("--exclude", nargs="+", default=["baichuan2-13b-chat"],
                        help="要排除的模型列表")
    args = parser.parse_args()
    
    # 过滤结果
    success, filtered_file = filter_results(args.input, args.exclude, args.output)
    if not success:
        logger.error("过滤结果失败，退出程序")
        return 1
    
    # 生成报告
    success, report_file = generate_report(filtered_file, args.report)
    if not success:
        logger.error("生成报告失败")
        return 1
    
    logger.info("处理完成!")
    logger.info(f"过滤后的结果: {filtered_file}")
    logger.info(f"新的评估报告: {report_file}")
    return 0


if __name__ == "__main__":
    sys.exit(main()) 