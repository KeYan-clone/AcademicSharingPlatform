"""
AI服务初始化脚本
"""
import os
import sys
import subprocess
from pathlib import Path

def check_python_version():
    """检查Python版本"""
    if sys.version_info < (3, 8):
        print("Error: Python 3.8 or higher is required")
        sys.exit(1)

def setup_environment():
    """设置环境变量"""
    env_path = Path(".env")
    if not env_path.exists():
        with open(env_path, "w", encoding='utf-8') as f:
            f.write("""# DeepSeek API配置
DEEPSEEK_API_KEY=your_api_key_here

# 存储配置
STORAGE_PATH=storage
VECTOR_DB_PATH=storage/vector_db

# 服务配置
HOST=0.0.0.0
PORT=8000
DEBUG=True
""")
        print("Created .env file, please update with your API key")

def setup_storage():
    """设置存储目录"""
    directories = [
        "storage",
        "storage/documents",
        "storage/temp",
        "storage/vector_db",
        "storage/logs"
    ]
    
    for directory in directories:
        os.makedirs(directory, exist_ok=True)
    print("Successfully set up storage directories")

def main():
    """主函数"""
    print("Initializing AI service...")
    
    # 检查Python版本
    check_python_version()
    print("Python version check passed")
    
    # 设置环境变量
    setup_environment()
    
    # 设置存储目录
    setup_storage()
    
    print("""
初始化完成！请按以下步骤操作：

1. 编辑 .env 文件，设置你的 DeepSeek API 密钥

2. 启动服务：
   uvicorn main:app --reload --host 0.0.0.0 --port 8000

3. 访问API文档：
   http://localhost:8000/docs

注意事项：
- 确保有足够的磁盘空间用于存储文档和向量数据库
- 定期备份 storage/vector_db 目录
- 查看 logs 目录下的日志文件以监控服务运行状态
""")

if __name__ == "__main__":
    main() 