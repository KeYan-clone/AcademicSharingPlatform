"""
基于FAISS的向量数据库服务，用于存储和检索文档向量
"""
import os
import json
import numpy as np
import faiss
from typing import List, Tuple, Dict

class FAISSDatabase:
    """FAISS向量数据库"""
    
    def __init__(self, dim: int = 1536):  # DeepSeek embedding维度为1536
        """初始化FAISS索引"""
        self.index = faiss.IndexFlatL2(dim)  # 使用L2距离（欧几里得距离）
        self.contents: List[str] = []  # 存储文本内容
        self.sources: List[str] = []   # 存储来源信息
        
    def add_embeddings(self, embeddings: np.ndarray, contents: List[str], sources: List[str]):
        """
        添加向量到数据库
        """
        if len(embeddings) != len(contents) or len(embeddings) != len(sources):
            raise ValueError("embeddings, contents and sources must have the same length")
            
        self.index.add(embeddings)
        self.contents.extend(contents)
        self.sources.extend(sources)
        
    def search(self, query_embedding: np.ndarray, top_k: int = 5) -> List[Dict[str, str]]:
        """
        检索最相似的文档
        返回: [{'content': str, 'source': str}, ...]
        """
        # 确保query_embedding是2D数组
        if len(query_embedding.shape) == 1:
            query_embedding = query_embedding.reshape(1, -1)
            
        # 搜索最近的k个向量
        distances, indices = self.index.search(query_embedding, top_k)
        
        results = []
        for rank, idx in enumerate(indices[0]):  # 取第一个查询的结果
            dist = distances[0][rank]
            if idx < 0:
                continue
            if idx < len(self.contents):
                results.append({
                    'content': self.contents[idx],
                    'source': self.sources[idx],
                    'distance': float(dist)
                })
        return results
    
    def save(self, directory: str):
        """
        保存数据库到文件
        """
        os.makedirs(directory, exist_ok=True)
        
        # 保存FAISS索引（Windows + 非 ASCII 路径可能失败，添加降级方案）
        index_path = os.path.join(directory, 'index.faiss')
        try:
            faiss.write_index(self.index, index_path)
        except Exception as e:
            import tempfile, shutil, logging
            logger = logging.getLogger('faiss_db')
            logger.warning(f"write_index failed at {index_path}: {e}. Using temp fallback.")
            tmp_dir = tempfile.mkdtemp()
            tmp_index = os.path.join(tmp_dir, 'index.faiss')
            faiss.write_index(self.index, tmp_index)
            os.makedirs(directory, exist_ok=True)
            shutil.move(tmp_index, index_path)
        
        # 保存文本内容和来源
        metadata = {
            'contents': self.contents,
            'sources': self.sources
        }
        with open(os.path.join(directory, 'metadata.json'), 'w', encoding='utf-8') as f:
            json.dump(metadata, f, ensure_ascii=False, indent=2)
            
    def load(self, directory: str):
        """
        从文件加载数据库
        """
        index_path = os.path.join(directory, 'index.faiss')
        try:
            # 加载FAISS索引
            self.index = faiss.read_index(index_path)
        except Exception as e:
            # Windows + 非 ASCII 路径读取失败时尝试使用临时英文路径
            import tempfile, shutil, logging, os as _os
            logger = logging.getLogger('faiss_db')
            logger.warning(f'read_index failed at {index_path}: {e}. Using temp fallback.')
            tmp_dir = tempfile.mkdtemp()
            tmp_index = _os.path.join(tmp_dir, 'index.faiss')
            shutil.copyfile(index_path, tmp_index)
            self.index = faiss.read_index(tmp_index)
        
        # 加载文本内容和来源
        with open(os.path.join(directory, 'metadata.json'), 'r', encoding='utf-8') as f:
            metadata = json.load(f)
            self.contents = metadata['contents']
            self.sources = metadata['sources'] 

    def remove_by_prefix(self, prefix: str) -> int:
        """
        删除来源以 prefix 开头的向量，并重建索引。
        返回被删除的向量数量。
        """
        if not prefix:
            return 0
        total = len(self.sources)
        if total == 0:
            return 0
        keep_indices = [i for i, src in enumerate(self.sources) if not str(src).startswith(prefix)]
        removed = total - len(keep_indices)
        if removed == 0:
            return 0

        # 重建索引
        d = self.index.d
        # 取出现有向量
        vectors = np.vstack([self.index.reconstruct(i) for i in range(total)])
        new_index = faiss.IndexFlatL2(d)
        if keep_indices:
            new_index.add(vectors[keep_indices])
        self.index = new_index
        self.contents = [self.contents[i] for i in keep_indices]
        self.sources = [self.sources[i] for i in keep_indices]
        return removed
