#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
模型比较测试脚本

用于对比不同LLM模型在本地知识库问答场景下的表现
运行方式：
    python -m ai_service.benchmark.compare_models --models deepseek-v3 qwen-max glm-4 baichuan2-13b-chat yi-34b-chat --dataset data/eval_questions.json --out results.json
"""

import json
import time
import argparse
import logging
from pathlib import Path
from typing import Dict, List, Any, Optional
import sys
import os

# 确保ai_service包可导入
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger("benchmark")

# 使用LLM客户端代理类
class LLMClient:
    """统一模型调用封装，适配不同模型的API"""
    
    def __init__(self, model_name: str):
        """初始化指定模型的客户端
        
        Args:
            model_name: 模型名称，支持 'deepseek-v3', 'qwen-max', 'glm-4', 'baichuan2-13b-chat', 'yi-34b-chat'
        """
        self.model_name = model_name
        self.last_usage = {"completion_tokens": 0, "prompt_tokens": 0}
        
        # 根据模型名称配置API参数
        if "deepseek" in model_name:
            from openai import OpenAI
            self.api_type = "openai"
            api_key = os.getenv("DEEPSEEK_API_KEY", "")
            if not api_key:
                logger.error("未找到DEEPSEEK_API_KEY环境变量")
                raise ValueError("请设置DEEPSEEK_API_KEY环境变量")
            
            self.client = OpenAI(
                api_key=api_key,
                base_url="https://api.deepseek.com/v1"
            )
            self.model = "deepseek-chat"
            
        elif "qwen" in model_name:
            from openai import OpenAI
            self.api_type = "openai"
            api_key = os.getenv("DASHSCOPE_API_KEY", "")
            if not api_key:
                logger.error("未找到DASHSCOPE_API_KEY环境变量")
                raise ValueError("请设置DASHSCOPE_API_KEY环境变量")
                
            self.client = OpenAI(
                api_key=api_key,
                base_url="https://dashscope.aliyuncs.com/compatible-mode/v1"
            )
            self.model = "qwen-max"
            
        elif "chatglm" in model_name:
            from openai import OpenAI
            self.api_type = "openai"
            api_key = os.getenv("ZHIPUAI_API_KEY", "")
            if not api_key:
                logger.error("未找到ZHIPUAI_API_KEY环境变量")
                raise ValueError("请设置ZHIPUAI_API_KEY环境变量")
                
            self.client = OpenAI(
                api_key=api_key,
                base_url="https://open.bigmodel.cn/api/paas/v4"
            )
            self.model = "glm-4"
            
        elif "baichuan" in model_name:
            from openai import OpenAI
            self.api_type = "openai"
            api_key = os.getenv("BAICHUAN_API_KEY", "")
            if not api_key:
                logger.error("未找到BAICHUAN_API_KEY环境变量")
                raise ValueError("请设置BAICHUAN_API_KEY环境变量")
                
            self.client = OpenAI(
                api_key=api_key,
                base_url="https://api.baichuan-ai.com/v1"
            )
            self.model = "Baichuan2-13B-Chat"
            
        elif "yi" in model_name:
            from openai import OpenAI
            self.api_type = "openai"
            api_key = os.getenv("YI_API_KEY", "")
            if not api_key:
                logger.error("未找到YI_API_KEY环境变量")
                raise ValueError("请设置YI_API_KEY环境变量")
                
            self.client = OpenAI(
                api_key=api_key,
                base_url="https://api.lingyiwanwu.com/v1"
            )
            self.model = "yi-34b-chat"
            
        else:
            raise ValueError(f"不支持的模型: {model_name}")
            
        logger.info(f"已初始化{model_name}模型客户端")
    
    def generate(self, prompt: str) -> str:
        """生成回答
        
        Args:
            prompt: 提示词
            
        Returns:
            模型回答文本
        """
        try:
            if self.api_type == "openai":
                response = self.client.chat.completions.create(
                    model=self.model,
                    messages=[{"role": "user", "content": prompt}]
                )
                
                self.last_usage = {
                    "prompt_tokens": response.usage.prompt_tokens if hasattr(response, "usage") else 0, 
                    "completion_tokens": response.usage.completion_tokens if hasattr(response, "usage") else 0
                }
                
                return response.choices[0].message.content
            else:
                raise ValueError(f"不支持的API类型: {self.api_type}")
        except Exception as e:
            logger.error(f"模型{self.model_name}调用失败: {str(e)}")
            return f"模型调用错误: {str(e)}"


class RAGService:
    """简化版RAG服务，用于基准测试"""
    
    def __init__(self, llm: LLMClient):
        """初始化RAG服务
        
        Args:
            llm: LLM客户端
        """
        from ai_service.services.embedding import EmbeddingService
        from ai_service.services.faiss_db import FAISSDatabase
        
        self.llm = llm
        self.embedding_service = EmbeddingService()
        
        # 使用默认向量库
        vector_db_path = os.path.join('ai_service', 'data', 'vector_db')
        self.vector_db = FAISSDatabase(dim=self.embedding_service.dimensions)
        
        if os.path.exists(os.path.join(vector_db_path, "index.faiss")):
            try:
                self.vector_db.load(vector_db_path)
                logger.info(f"已加载向量库: {vector_db_path}")
            except Exception as e:
                logger.warning(f"向量库加载失败: {str(e)}")
                
    def retrieve(self, query: str, top_k: int = 4) -> List[Dict[str, str]]:
        """检索相关文档
        
        Args:
            query: 查询文本
            top_k: 返回结果数量
            
        Returns:
            相关文档列表
        """
        try:
            query_embedding = self.embedding_service.get_embedding(query)
            
            # 修复：确保查询向量是numpy数组
            if isinstance(query_embedding, list):
                import numpy as np
                query_embedding = np.array(query_embedding, dtype=np.float32)
            
            results = self.vector_db.search(query_embedding, top_k)
            return results
        except Exception as e:
            logger.error(f"检索失败: {str(e)}")
            return []
    
    def generate_answer(self, query: str, context: List[Dict[str, str]]) -> str:
        """生成回答
        
        Args:
            query: 用户问题
            context: 检索到的相关文档
            
        Returns:
            生成的回答
        """
        # 构建提示词
        prompt = self._build_rag_prompt(query, context)
        
        # 调用LLM生成回答
        answer = self.llm.generate(prompt)
        
        return answer
    
    def _build_rag_prompt(self, query: str, context: List[Dict[str, str]]) -> str:
        """构建RAG提示词
        
        Args:
            query: 用户问题
            context: 检索到的相关文档
            
        Returns:
            完整的提示词
        """
        context_str = "\n\n".join([f"内容 {i+1}:\n{doc['content']}" for i, doc in enumerate(context)])
        
        prompt = f"""作为一个教学助手，请基于以下资料回答用户的问题。请保持专业、简洁和准确。如果无法从资料中找到答案，请直接说明不知道，不要编造信息。

===== 参考资料 =====
{context_str}
====================

用户问题: {query}

回答:"""
        
        return prompt


def run_eval(model_name: str, questions: List[Dict], top_k: int = 4) -> List[Dict]:
    """评估一个模型在所有问题上的表现
    
    Args:
        model_name: 模型名称
        questions: 问题列表
        top_k: 检索文档数量
    
    Returns:
        评估结果列表
    """
    results = []
    
    try:
        # 初始化模型和RAG服务
        llm = LLMClient(model_name)
        rag = RAGService(llm=llm)
        
        for i, q in enumerate(questions):
            logger.info(f"[{i+1}/{len(questions)}] 评估问题 {q['id']}: {q['query'][:30]}...")
            
            try:
                # 检索相关文档
                start_retrieve = time.time()
                ctx = rag.retrieve(q["query"], top_k=top_k)
                retrieve_time = time.time() - start_retrieve
                
                # 生成回答
                start_generate = time.time()
                ans = rag.generate_answer(q["query"], ctx)
                generate_time = time.time() - start_generate
                
                # 总耗时
                total_time = retrieve_time + generate_time
                
                # 记录结果
                results.append({
                    "id": q["id"],
                    "model": model_name,
                    "query": q["query"],
                    "answer": ans,
                    "retrieve_time": retrieve_time,
                    "generate_time": generate_time, 
                    "total_time": total_time,
                    "tokens_out": llm.last_usage.get("completion_tokens", 0),
                    "tokens_in": llm.last_usage.get("prompt_tokens", 0),
                    "ground_truth": q.get("answer", ""),
                    "context": [{"content": c["content"], "source": c["source"]} for c in ctx]
                })
                
                logger.info(f"完成问题 {q['id']} (检索: {retrieve_time:.2f}s, 生成: {generate_time:.2f}s)")
                
            except Exception as e:
                logger.error(f"问题 {q['id']} 评估失败: {str(e)}")
                results.append({
                    "id": q["id"],
                    "model": model_name,
                    "query": q["query"],
                    "error": str(e),
                    "ground_truth": q.get("answer", "")
                })
    
    except Exception as e:
        logger.error(f"模型 {model_name} 评估失败: {str(e)}")
    
    return results


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="比较不同模型在RAG问答中的表现")
    parser.add_argument("--models", nargs="+", required=True, 
                        help="要测试的模型列表，如 'deepseek-v3 qwen-max'")
    parser.add_argument("--dataset", default="ai_service/data/eval_questions.json",
                        help="测试数据集路径")
    parser.add_argument("--out", default="ai_service/benchmark/results.json",
                        help="结果输出路径")
    parser.add_argument("--top_k", type=int, default=4,
                        help="检索文档数量")
    parser.add_argument("--limit", type=int, default=None,
                        help="限制测试问题数量")
    
    args = parser.parse_args()
    
    # 加载问题
    try:
        questions = json.loads(Path(args.dataset).read_text(encoding="utf-8"))
        if args.limit:
            questions = questions[:args.limit]
        logger.info(f"已加载 {len(questions)} 个测试问题")
    except Exception as e:
        logger.error(f"加载测试问题失败: {str(e)}")
        return
    
    # 运行评估
    all_results = []
    for model in args.models:
        logger.info(f"开始评估模型: {model}")
        model_results = run_eval(model, questions, top_k=args.top_k)
        all_results.extend(model_results)
        logger.info(f"模型 {model} 评估完成，共 {len(model_results)} 条结果")
    
    # 保存结果
    try:
        output_path = Path(args.out)
        output_path.parent.mkdir(exist_ok=True, parents=True)
        output_path.write_text(json.dumps(all_results, ensure_ascii=False, indent=2), encoding="utf-8")
        logger.info(f"结果已保存至: {output_path}")
    except Exception as e:
        logger.error(f"保存结果失败: {str(e)}")


if __name__ == "__main__":
    main()
