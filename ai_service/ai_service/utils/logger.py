"""
日志工具模块
"""
import os
import logging
from logging.handlers import RotatingFileHandler
from datetime import datetime

def setup_logger(name: str, log_dir: str = "logs") -> logging.Logger:
    """
    设置日志记录器
    """
    # 创建日志目录
    os.makedirs(log_dir, exist_ok=True)
    
    # 创建logger
    logger = logging.getLogger(name)
    logger.setLevel(logging.INFO)
    
    # 日志格式
    formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # 控制台处理器
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)
    
    # 文件处理器（按日期轮转）
    today = datetime.now().strftime('%Y-%m-%d')
    file_handler = RotatingFileHandler(
        filename=f"{log_dir}/{name}_{today}.log",
        maxBytes=10*1024*1024,  # 10MB
        backupCount=5,
        encoding='utf-8'
    )
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)
    
    return logger

# 创建各个模块的logger
rag_logger = setup_logger('rag')
embedding_logger = setup_logger('embedding')
doc_parser_logger = setup_logger('doc_parser')
api_logger = setup_logger('api') 