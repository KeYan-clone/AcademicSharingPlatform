#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
简化的过滤和报告生成脚本

直接从已有的合并结果中过滤掉Baichuan模型，并重新生成报告
"""

import json
import os
import sys
import time
import shutil
from pathlib import Path
import subprocess
import logging

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger("run_filter")

# 输入文件路径
INPUT_FILE = "batch_results/merged_results_20250715_220031.json"
# 要排除的模型
EXCLUDE_MODEL = "baichuan2-13b-chat"
# 时间戳
TIMESTAMP = time.strftime("%Y%m%d_%H%M%S")
# 输出文件路径
OUTPUT_FILE = f"batch_results/filtered_results_{TIMESTAMP}.json"
# 报告文件路径
REPORT_FILE = f"batch_results/filtered_report_{TIMESTAMP}.md"

def main():
    """主函数"""
    # 检查输入文件是否存在
    if not os.path.exists(INPUT_FILE):
        logger.error(f"输入文件不存在: {INPUT_FILE}")
        return 1
    
    # 加载并过滤结果
    try:
        with open(INPUT_FILE, 'r', encoding='utf-8') as f:
            all_results = json.load(f)
        
        logger.info(f"已加载 {len(all_results)} 条结果记录")
        
        # 过滤掉指定模型的结果
        filtered_results = [r for r in all_results if r.get("model") != EXCLUDE_MODEL]
        
        logger.info(f"过滤后剩余 {len(filtered_results)} 条结果记录 (移除了 {len(all_results) - len(filtered_results)} 条)")
        
        # 保存过滤后的结果
        os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
        with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
            json.dump(filtered_results, f, ensure_ascii=False, indent=2)
        
        logger.info(f"已保存过滤后的结果到: {OUTPUT_FILE}")
    except Exception as e:
        logger.error(f"过滤结果失败: {str(e)}")
        return 1
    
    # 运行报告生成脚本
    try:
        cmd = [
            sys.executable, 
            "report.py", 
            "--input", OUTPUT_FILE, 
            "--output", REPORT_FILE
        ]
        
        logger.info(f"生成报告: {' '.join(cmd)}")
        subprocess.run(cmd, check=True)
        
        logger.info(f"报告已生成: {REPORT_FILE}")
        
        # 复制生成的图表到项目根目录
        for img_file in ["benchmark_latency.png", "benchmark_quality.png"]:
            if os.path.exists(img_file):
                target_path = "../../" + img_file
                shutil.copy(img_file, target_path)
                logger.info(f"已复制图表到项目根目录: {target_path}")
        
        logger.info("处理完成!")
        logger.info(f"过滤后的结果: {OUTPUT_FILE}")
        logger.info(f"新的评估报告: {REPORT_FILE}")
        return 0
    except Exception as e:
        logger.error(f"生成报告失败: {str(e)}")
        return 1

if __name__ == "__main__":
    sys.exit(main()) 