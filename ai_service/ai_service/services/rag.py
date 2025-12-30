"""
RAG (Retrieval-Augmented Generation) 服务
用于基于本地知识库生成教学内容、习题等
"""
import os
import json
from typing import List, Dict, Optional, Any
from openai import OpenAI
from .model_selector import ModelSelector
from dotenv import load_dotenv
from .embedding import EmbeddingService
from .faiss_db import FAISSDatabase
from .prompts import PromptTemplates
from utils.logger import rag_logger as logger
from pydantic import BaseModel
import numpy as np
import re
import shutil
import math


def _env_flag(name: str, default: str = "true") -> bool:
    """Convert environment variable to boolean with a safe default."""
    value = os.getenv(name, default)
    if value is None:
        return False
    return str(value).strip().lower() in {"1", "true", "yes", "on"}

load_dotenv()
class TimePlanItem(BaseModel):
    content: str
    minutes: int
    step: str

class RAGService:
    """RAG 服务类（接受可变 StorageService 以支持知识库本地化）"""

    def __init__(self, storage_service: 'StorageService'):
        """初始化服务

        Args:
            storage_service: 提供存储路径、向量数据库目录等信息
        """
        # 延迟导入，避免循环引用
        from .storage import StorageService  # type: ignore

        if not isinstance(storage_service, StorageService):
            raise TypeError("storage_service must be instance of StorageService")

        self.storage_service = storage_service
        self.embedding_service = EmbeddingService()

        # 统一模型访问入口
        self.model_selector = ModelSelector()

        # 检索增强配置
        self.initial_recall_multiplier = max(1.0, float(os.getenv("RAG_INITIAL_RECALL_MULTIPLIER", "3")))
        self.initial_recall_min = max(4, int(os.getenv("RAG_INITIAL_RECALL_MIN", "8")))
        self.reranker_enabled = _env_flag("RAG_USE_RERANKER", "true")
        self.reranker_method = os.getenv("RAG_RERANKER_METHOD", "llm").strip().lower()
        self.reranker_max_chars = max(120, int(os.getenv("RAG_RERANK_MAX_CHARS", "480")))
        self.hierarchical_enabled = _env_flag("RAG_USE_HIERARCHICAL", "true")
        self.hierarchy_source_k = max(1, int(os.getenv("RAG_HIERARCHY_SOURCE_K", "4")))
        self.hierarchy_chunks_per_source = max(1, int(os.getenv("RAG_HIERARCHY_CHUNKS_PER_SOURCE", "2")))
        self.hierarchy_bonus = float(os.getenv("RAG_HIERARCHY_SOURCE_BONUS", "0.1"))

        # ---------------- 多知识库支持 ----------------
        self.vector_dbs: dict[str, FAISSDatabase] = {}

        vector_db_paths = self.storage_service.get_vector_db_paths()
        logger.info(f"Initializing RAGService with vector DB paths: {vector_db_paths}")

        for path in vector_db_paths:
            db = FAISSDatabase(dim=self.embedding_service.dimensions)
            index_file = os.path.join(path, "index.faiss")
            if os.path.exists(index_file):
                try:
                    db.load(path)
                    # 检查索引维度
                    if hasattr(db, "index") and hasattr(db.index, "d"):
                        index_dim = db.index.d
                        if index_dim != self.embedding_service.dimensions:
                            logger.warning(
                                f"FAISS索引维度({index_dim})与当前embedding维度({self.embedding_service.dimensions})不一致，"
                                f"将自动重建索引并清空原有向量库。"
                            )
                            # 删除旧索引文件夹或文件
                            if os.path.isdir(path):
                                shutil.rmtree(path)
                                os.makedirs(path, exist_ok=True)
                            # 重新初始化空索引
                            db = FAISSDatabase(dim=self.embedding_service.dimensions)
                            # 清空内容缓存，防止内容与索引不一致
                            if hasattr(db, "contents"):
                                db.contents.clear()
                            if hasattr(db, "sources"):
                                db.sources.clear()
                    logger.info(f"Loaded vector DB from {path}")
                except Exception as e:
                    logger.warning(f"Load failed for {path}: {e}. Using empty DB.")
            else:
                os.makedirs(path, exist_ok=True)
                logger.info(f"Vector DB not found in {path}, initialized empty.")

            self.vector_dbs[path] = db

        # 兼容旧接口：保留 self.vector_db 指向第一个库
        first_path = self.storage_service.get_vector_db_path()
        self.vector_db = self.vector_dbs[first_path]

    # ------------------------------------------------------------------
    # Internal helper – centralised LLM call with automatic fallback
    # ------------------------------------------------------------------
    def _chat_completion(self, messages: list[dict[str, str]], purpose: str | None = None):
        """Wrapper around ModelSelector.chat_completion returning content str."""
        response = self.model_selector.chat_completion(messages=messages, purpose=purpose)
        return response.choices[0].message.content

    @staticmethod
    def _distance_to_score(distance: Optional[float]) -> float:
        """Convert FAISS distance to a similarity score for comparison."""
        if distance is None:
            return 0.0
        try:
            distance = max(distance, 0.0)
            return 1.0 / (1.0 + distance)
        except Exception:
            return 0.0

    @staticmethod
    def _truncate_text(text: str, max_chars: int) -> str:
        """Keep prompts concise while preserving signal."""
        if not text:
            return ""
        text = text.strip()
        if len(text) <= max_chars:
            return text
        return text[:max_chars].rstrip() + "..."

    @staticmethod
    def _normalize_source(source: str) -> str:
        return (source or "unknown").strip()

    def _deduplicate_docs(self, docs: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        seen = set()
        unique: List[Dict[str, Any]] = []
        for doc in docs:
            key = (doc.get('content'), doc.get('source'))
            if key in seen:
                continue
            seen.add(key)
            unique.append(doc)
        return unique

    def _group_by_source(self, docs: List[Dict[str, Any]]) -> Dict[str, List[Dict[str, Any]]]:
        buckets: Dict[str, List[Dict[str, Any]]] = {}
        for doc in docs:
            source = self._normalize_source(doc.get('source', ''))
            buckets.setdefault(source, []).append(doc)
        for source_docs in buckets.values():
            source_docs.sort(key=lambda d: d.get('distance', 1e9))
        return buckets

    def _select_hierarchical_candidates(self, docs: List[Dict[str, Any]], top_k: int) -> List[Dict[str, Any]]:
        if not docs:
            return []
        grouped = self._group_by_source(docs)
        if len(grouped) == 1:
            return grouped[next(iter(grouped))]

        source_scores = []
        for source, source_docs in grouped.items():
            best_distance = source_docs[0].get('distance', 1e9)
            coverage_bonus = self.hierarchy_bonus * math.log(len(source_docs) + 1)
            source_scores.append((best_distance - coverage_bonus, source))
        source_scores.sort(key=lambda x: x[0])

        max_sources = max(self.hierarchy_source_k, min(len(source_scores), top_k))
        per_source_limit = max(1, self.hierarchy_chunks_per_source)
        candidates: List[Dict[str, Any]] = []

        for _, source in source_scores[:max_sources]:
            candidates.extend(grouped[source][:per_source_limit])

        if len(candidates) < top_k:
            remaining: List[Dict[str, Any]] = []
            for _, source in source_scores[max_sources:]:
                remaining.extend(grouped[source])
            candidates.extend(remaining[: max(0, top_k - len(candidates))])

        return candidates if candidates else docs

    def _rerank_with_llm(self, query: str, docs: List[Dict[str, Any]], top_k: int) -> List[Dict[str, Any]]:
        if not docs or self.reranker_method != "llm":
            return docs

        formatted_docs = []
        for idx, doc in enumerate(docs, start=1):
            snippet = self._truncate_text(doc.get('content', ''), self.reranker_max_chars)
            formatted_docs.append(
                f"Document {idx}:\nSource: {doc.get('source', 'unknown')}\nDistance: {doc.get('distance', 'N/A')}\nContent: {snippet}"
            )

        documents_block = "\n\n".join(formatted_docs)
        min_return = max(1, top_k)
        prompt = (
            "你是一名高质量的检索重排序器。请基于用户问题评估每个文档与问题的相关性，"
            "仅考虑文档提供的信息，不要臆造。\n"
            f"用户问题: {query}\n\n"
            "待评估文档列表:\n"
            f"{documents_block}\n\n"
            "请返回 JSON，格式如下：\n"
            "{\n  \"ranking\": [\n    {\"doc_id\": 1, \"score\": 0.95, \"reason\": \"简要说明\"}\n  ]\n}\n"
            "其中 doc_id 为上文文档编号（整数），score 为 0~1 的相关性分数。\n"
            f"请至少返回 {min_return} 条目，并按 score 从高到低排序。"
        )

        try:
            response = self.model_selector.chat_completion(
                messages=[{"role": "user", "content": prompt}],
                purpose="rag_rerank"
            )
            content = response.choices[0].message.content
            ranking_data = self.safe_json_loads(content)
        except Exception as exc:
            logger.warning(f"LLM reranker failed, fallback to distance order: {exc}")
            return docs

        if isinstance(ranking_data, dict):
            ranking_list = ranking_data.get("ranking") or ranking_data.get("results") or []
        elif isinstance(ranking_data, list):
            ranking_list = ranking_data
        else:
            ranking_list = []

        order: List[Dict[str, Any]] = []
        used = set()
        for item in ranking_list:
            doc_id = item.get("doc_id") if isinstance(item, dict) else item
            if doc_id is None:
                continue
            if isinstance(doc_id, str):
                match = re.search(r"\d+", doc_id)
                doc_idx = int(match.group()) if match else None
            else:
                doc_idx = int(doc_id)
            if not doc_idx:
                continue
            if not 1 <= doc_idx <= len(docs):
                continue
            if doc_idx in used:
                continue
            used.add(doc_idx)
            order.append(docs[doc_idx - 1])
            if len(order) >= top_k:
                break

        if len(order) < top_k:
            for idx, doc in enumerate(docs, start=1):
                if idx in used:
                    continue
                order.append(doc)
                if len(order) >= top_k:
                    break

        return order if order else docs

    def add_to_knowledge_base(self, chunks: List[Dict[str, str]]):
        """将文档添加到所有激活知识库"""
        try:
            embeddings, contents, sources = self.embedding_service.get_chunks_embeddings(chunks)

            for path, db in self.vector_dbs.items():
                db.add_embeddings(embeddings, contents, sources)
                db.save(path)
                logger.info(f"Added {len(chunks)} chunks to KB at {path}")
        except Exception as e:
            logger.error(f"Error adding chunks to knowledge base: {str(e)}")
            raise

    def search_knowledge_base(self, query: str, top_k: int = 5) -> List[Dict[str, str]]:
        """在所有知识库中搜索，合并结果"""
        try:
            # 若知识库为空，直接返回空列表
            if not hasattr(self.vector_db, "contents") or not self.vector_db.contents:
                logger.warning("知识库为空，search_knowledge_base直接返回空列表")
                return []
            query_embedding = self.embedding_service.get_embedding(query)
            if isinstance(query_embedding, list):
                query_embedding = np.array(query_embedding, dtype=np.float32)

            initial_k = max(top_k, self.initial_recall_min)
            initial_k = max(initial_k, int(math.ceil(top_k * self.initial_recall_multiplier)))

            aggregated: List[Dict[str, Any]] = []
            for path, db in self.vector_dbs.items():
                results = db.search(query_embedding, initial_k)
                for item in results:
                    item['score'] = self._distance_to_score(item.get('distance'))
                    item['source'] = item.get('source') or path
                    aggregated.append(item)

            if not aggregated:
                logger.info(f"No matches found for query: {query}")
                return []

            deduped = self._deduplicate_docs(aggregated)

            candidates = sorted(deduped, key=lambda x: x.get('distance', 1e9))
            if self.hierarchical_enabled and len(deduped) > top_k:
                hierarchical_k = max(top_k, self.hierarchy_source_k * self.hierarchy_chunks_per_source)
                hier_candidates = self._select_hierarchical_candidates(deduped, hierarchical_k)
                candidates = sorted(self._deduplicate_docs(hier_candidates), key=lambda x: x.get('distance', 1e9))

            rerank_pool_size = min(len(candidates), max(top_k, self.initial_recall_min))
            if self.reranker_enabled and candidates:
                rerank_input = candidates[:rerank_pool_size]
                ranked = self._rerank_with_llm(query, rerank_input, top_k)
            else:
                ranked = sorted(candidates, key=lambda x: x.get('distance', 1e9))

            final_results = self._deduplicate_docs(ranked)[:top_k]

            logger.info(
                "Search across %d KBs (initial_k=%d, candidates=%d, final=%d) for query: %s",
                len(self.vector_dbs),
                initial_k,
                len(candidates),
                len(final_results),
                query
            )
            return final_results
        except Exception as e:
            logger.error(f"Error searching knowledge bases: {str(e)}")
            raise

    # ------------------------------------------------------------------
    # Paper QA: retrieve top chunks and answer with citations
    # ------------------------------------------------------------------
    def answer_question(self, question: str, top_k: int = 5, kb_id: Optional[str] = None) -> Dict[str, Any]:
        """
        面向论文/知识库的通用问答，返回答案与引用片段。
        可选 kb_id 用于按知识库过滤检索结果。
        """
        hits = self.search_knowledge_base(question, top_k * 3)
        if kb_id:
            prefix = f"{kb_id}:"
            hits = [h for h in hits if str(h.get("source", "")).startswith(prefix)]
        hits = hits[:top_k]
        if not hits:
            return {
                "answer": "未找到相关信息，可能知识库为空或未包含相关内容。",
                "references": []
            }

        context_blocks = []
        references: List[Dict[str, Any]] = []
        for idx, item in enumerate(hits, 1):
            content = item.get("content", "")
            source_raw = item.get("source", "")
            source = self._normalize_source(source_raw)
            # 去掉 kb 前缀，保留文件名与页码/段落信息
            # 示例：kbId:filename.pdf 第1页 -> filename.pdf 第1页
            if ":" in source:
                display_source = source.split(":")[-1]
            else:
                display_source = source
            display_source = os.path.basename(display_source)
            score = item.get("score") or self._distance_to_score(item.get("distance"))
            context_blocks.append(f"[{idx}] 来源: {display_source}\n{self._truncate_text(content, 500)}")
            references.append({
                "index": idx,
                "source": display_source
            })

        system_prompt = (
            "你是论文解读助手，只能基于提供的片段回答。\n"
            "要求：\n"
            "1) 如果有参考片段，必须给出要点式答案（2-5 条），不要回复“未找到相关信息”。\n"
            "2) 仅在没有任何参考片段时才说“未找到相关信息”。\n"
            "3) 引用时用 [数字] 标注，对应片段编号，且仅使用提供的编号，不要虚构其他数字。\n"
            "4) 不要编造未提供的内容、公式或数据。"
        )
        user_prompt = (
            f"问题：{question}\n\n"
            f"参考片段：\n{chr(10).join(context_blocks)}\n\n"
            "请基于参考片段用中文要点式回答，每条末尾标注 [数字]。若部分问题未覆盖，也请给出可总结的关键信息。"
        )

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt}
        ]
        answer = self._chat_completion(messages, purpose="paper_qa")
        # 如果模型返回未找到相关信息，但实际有检索结果，则兜底给出来源提示
        if hits and ("未找到相关信息" in answer or "未找到相关内容" in answer):
            summary_lines = [f"{ref['source']}" for ref in references[:5]]
            fallback = "未能直接生成答案，可能相关的文档：\n- " + "\n- ".join(summary_lines)
            return {"answer": fallback, "references": references}
        return {"answer": answer, "references": references}

    def delete_kb(self, kb_id: str) -> int:
        """
        按知识库 ID 删除对应前缀的向量，返回删除的向量数量。
        """
        if not kb_id:
            return 0
        prefix = f"{kb_id}:"
        removed_total = 0
        for path, db in self.vector_dbs.items():
            removed = db.remove_by_prefix(prefix)
            if removed > 0:
                db.save(path)
                if len(db.sources) == 0:
                    # 向量清空时删除索引和元数据文件，避免残留
                    try:
                        index_file = os.path.join(path, "index.faiss")
                        meta_file = os.path.join(path, "metadata.json")
                        if os.path.exists(index_file):
                            os.remove(index_file)
                        if os.path.exists(meta_file):
                            os.remove(meta_file)
                    except Exception as e:
                        logger.warning(f"Failed to clean empty vector db at {path}: {e}")
            # 同步删除该 kb 的文档目录
            kb_doc_dir = os.path.join(os.path.dirname(os.path.dirname(path)), "storage", "documents", kb_id)
            if os.path.exists(kb_doc_dir):
                try:
                    shutil.rmtree(kb_doc_dir)
                    logger.info(f"Deleted documents dir for kb {kb_id}: {kb_doc_dir}")
                except Exception as e:
                    logger.warning(f"Failed to delete documents dir {kb_doc_dir}: {e}")
            removed_total += removed
        return removed_total

    def safe_json_loads(self, text):
        """
        尝试从文本中提取并解析 JSON（更健壮）。
        """
        def _strip_code_fences(s: str) -> str:
            s = s.strip()
            # 去掉 Markdown 代码块围栏 ``` 或 ```json
            if s.startswith("```"):
                # 删除第一行围栏
                s = re.sub(r'^```[a-zA-Z0-9_-]*\n', '', s)
                # 删除结尾围栏
                s = re.sub(r'\n```\s*$', '', s)
            return s

        def _remove_bom_and_controls(s: str) -> str:
            # 去掉BOM
            if s.startswith('\ufeff'):
                s = s.lstrip('\ufeff')
            # 移除除 \t\n\r 外的 C0 控制字符，避免 json 解析报 Invalid control character
            return re.sub(r'[\x00-\x08\x0B-\x0C\x0E-\x1F]', ' ', s)

        def _balance_braces_extract(s: str) -> str | None:
            # 从首个 { 开始，尝试平衡大括号，提取一个 JSON 对象字符串
            start = s.find('{')
            if start == -1:
                return None
            depth = 0
            in_str = False
            esc = False
            for i, ch in enumerate(s[start:], start=start):
                if in_str:
                    if esc:
                        esc = False
                    elif ch == '\\':
                        esc = True
                    elif ch == '"':
                        in_str = False
                else:
                    if ch == '"':
                        in_str = True
                    elif ch == '{':
                        depth += 1
                    elif ch == '}':
                        depth -= 1
                        if depth == 0:
                            return s[start:i+1]
            return None

        def _fix_trailing_commas(s: str) -> str:
            # 移除对象或数组中末尾的多余逗号
            return re.sub(r',\s*([}\]])', r'\1', s)

        raw = _strip_code_fences(text)
        raw = _remove_bom_and_controls(raw)
        try:
            return json.loads(raw)
        except Exception as e1:
            # 继续尝试提取最外层 JSON 并做轻度修复
            candidate = _balance_braces_extract(raw)
            if candidate is None:
                # 退而求其次：正则贪婪匹配
                m = re.search(r'(\{[\s\S]*\})', raw)
                candidate = m.group(1) if m else None
            if candidate:
                candidate = _remove_bom_and_controls(candidate)
                candidate = _fix_trailing_commas(candidate)
                try:
                    return json.loads(candidate)
                except Exception as e2:
                    pass
            # 最后兜底：请求模型将文本修正为合法 JSON（严格返回 JSON，无围栏）
            try:
                repair_prompt = (
                    "将下面的模型输出修正为合法 JSON。只返回修正后的 JSON，本句不要出现在结果中，不要使用Markdown围栏：\n\n" + raw[:6000]
                )
                repaired = self._chat_completion([
                    {"role": "user", "content": repair_prompt}
                ], purpose="json_fix")
                repaired = _strip_code_fences(_remove_bom_and_controls(repaired or ""))
                return json.loads(repaired)
            except Exception as e3:
                raise ValueError(f"JSON解析失败，原始片段：{raw[:200]}... e1={e1} e2={e2 if 'e2' in locals() else ''} e3={e3}")

    def generate_teaching_content(self, course_outline: str, course_name: str, expected_hours: int, constraints: Optional[str] = None) -> Dict:
        """根据课程大纲生成教学内容 (支持 constraints)"""
        try:
            # 1. 调用大模型从大纲中提取关键知识点
            prompt = PromptTemplates.get_knowledge_points_extraction_prompt(
                course_name=course_name,
                course_outline=course_outline
            )

            content = self._chat_completion([
                {"role": "user", "content": prompt}
            ], purpose="knowledge_points_extraction")
            knowledge_points = [k.strip() for k in content.strip().split('\n') if k.strip()]
            logger.info(f"Extracted {len(knowledge_points)} knowledge points")

            # 2. 为每个知识点检索相关内容
            knowledge_base = {}
            for point in knowledge_points:
                if not point:
                    logger.warning(f"跳过空知识点")
                    continue
                try:
                    relevant_docs = self.search_knowledge_base(point)
                except Exception as e:
                    logger.error(f"知识点 '{point}' 检索失败: {e}")
                    relevant_docs = []
                knowledge_base[point] = relevant_docs

            # 3. 生成教学内容
            logger.info("start generate teaching content\n")
            prompt = PromptTemplates.get_teaching_content_generation_prompt(
                course_name=course_name,
                course_outline=course_outline,
                expected_hours=expected_hours,
                knowledge_base=knowledge_base,
                constraints=constraints
            )
            logger.info("test1\n");
            content = self._chat_completion([
                {"role": "user", "content": prompt}
            ], purpose="teaching_content_generation")
            logger.info("test2\n")
            logger.info("大模型原始返回内容: %s", content)
            try:
                logger.info(f"准备进入 safe_json_loads 解析内容")
                result = self.safe_json_loads(content)
                logger.info(f"safe_json_loads 解析成功，结果类型: {type(result)}")
                # 检查并适配新结构
                lessons = result.get('lessons', [])
                for lesson in lessons:
                    # timePlan 字段应为详细的时间分配列表
                    time_plan = lesson.get('timePlan', [])
                    logger.info(f"课时: {lesson.get('title', '')}，时间分配: {time_plan}")
                    # 其它字段如 knowledgePoints、practiceContent、teachingGuidance 也做日志
                    logger.info(f"知识点: {lesson.get('knowledgePoints', [])}")
                    logger.info(f"实训练习: {lesson.get('practiceContent', '')}")
                    logger.info(f"教学指导: {lesson.get('teachingGuidance', '')}")
            except Exception as e:
                logger.error(f"JSON解析失败，原始内容：{content[:200]}... 错误信息: {e}")
                raise
            logger.info("Successfully generated teaching content")
            return result
        except Exception as e:
            logger.error(f"Error generating teaching content: {str(e)}")
            raise

    def generate_teaching_content_detail(self, title: str, knowledgePoints: List[str], practiceContent: str, teachingGuidance: str, timePlan: List[TimePlanItem], constraints: str | None = None) -> Dict:
        """根据已有教案生成详细教案（内容更丰富，但仅基于传入参数）"""
        try:
            # 构造详细教案生成的提示词
            prompt = PromptTemplates.get_teaching_content_detail_prompt(
                title=title,
                knowledgePoints=knowledgePoints,
                practiceContent=practiceContent,
                teachingGuidance=teachingGuidance,
                timePlan=timePlan,
                constraints=constraints
            )
            content = self._chat_completion([
                {"role": "user", "content": prompt}
            ], purpose="teaching_content_detail")
            logger.info("详细教案 LLM 返回内容: %s", content)
            result = self.safe_json_loads(content)
            logger.info("Successfully generated detailed teaching content")
            return result
        except Exception as e:
            logger.error(f"Error generating teaching content detail: {str(e)}")
            raise

    def regenerate_teaching_content_detail(self, title: str, knowledgePoints: List[str], practiceContent: str, teachingGuidance: str, timePlan: List[TimePlanItem], constraints: str | None = None) -> Dict:
        """根据已有教案生成一版全新的教案（内容充实度保持一致，无需更丰富）"""
        try:
            prompt = PromptTemplates.get_regenerate_teaching_content_prompt(
                title=title,
                knowledgePoints=knowledgePoints,
                practiceContent=practiceContent,
                teachingGuidance=teachingGuidance,
                timePlan=timePlan,
                constraints=constraints
            )
            content = self._chat_completion([
                {"role": "user", "content": prompt}
            ], purpose="regenerate_teaching_content")
            logger.info("全新教案 LLM 返回内容: %s", content)
            result = self.safe_json_loads(content)
            logger.info("Successfully regenerated teaching content detail")
            return result
        except Exception as e:
            logger.error(f"Error regenerating teaching content detail: {str(e)}")
            raise

    def generate_exercises(self, course_name: str, lesson_content: str, difficulty: str = "medium", choose_count: int = 5, fill_blank_count: int = 5, question_count: int = 2, custom_types: dict = None) -> Dict:
        """生成练习题"""
        try:
            prompt = PromptTemplates.get_exercise_generation_prompt(
                course_name=course_name,
                lesson_content=lesson_content,
                difficulty=difficulty,
                choose_count=choose_count,
                fill_blank_count=fill_blank_count,
                question_count=question_count,
                custom_types=custom_types
            )
            content = self._chat_completion([
                {"role": "user", "content": prompt}
            ], purpose="exercise_generation")
            logger.info(f"LLM raw output:\n{content}")
            result = self.safe_json_loads(content)
            logger.info(f"Successfully generated exercises for {course_name}")
            return result
        except Exception as e:
            logger.error(f"Error generating exercises: {str(e)}")
            raise

    def evaluate_answer(self, question: str, student_answer: str, reference_answer: str) -> Dict:
        """评估学生答案"""
        try:
            prompt = PromptTemplates.get_answer_evaluation_prompt(
                question=question,
                student_answer=student_answer,
                reference_answer=reference_answer
            )

            content = self._chat_completion([
                {"role": "user", "content": prompt}
            ], purpose="answer_evaluation")

            result = self.safe_json_loads(content)
            logger.info("Successfully evaluated student answer")
            return result

        except Exception as e:
            logger.error(f"Error evaluating answer: {str(e)}")
            raise

    def evaluate_subjective_answer(self, question: str, student_answer: str, reference_answer: str, max_score: float) -> Dict:
        """评估主观题答案"""
        try:
            # 1. 获取相关知识点的内容
            relevant_docs = self.search_knowledge_base(question)

            # 2. 生成评估提示
            prompt = PromptTemplates.get_subjective_answer_evaluation_prompt(
                question=question,
                student_answer=student_answer,
                reference_answer=reference_answer,
                max_score=max_score
            )

            # 3. 调用大模型进行评估
            content = self._chat_completion([
                {"role": "user", "content": prompt}
            ], purpose="subjective_answer_evaluation")

            # 4. 解析结果
            result = self.safe_json_loads(content)

            # 5. 添加知识点位置信息
            if relevant_docs:
                result['knowledge_context'] = relevant_docs

            logger.info("Successfully evaluated subjective answer")
            return result

        except Exception as e:
            logger.error(f"Error evaluating subjective answer: {str(e)}")
            raise

    def analyze_exercise(self, exercise_questions: List[Dict]) -> Dict:
        """分析练习整体情况"""
        try:
            # 1. 提取所有题目的知识点
            all_knowledge_points = []
            for question in exercise_questions:
                relevant_docs = self.search_knowledge_base(question.content)
                all_knowledge_points.extend(relevant_docs)
            # 2. 对象转字典，便于序列化
            exercise_questions_dict = [vars(q) for q in exercise_questions]
            # 3. 生成分析提示
            prompt = PromptTemplates.get_exercise_analysis_prompt(exercise_questions_dict)

            # 4. 调用大模型进行分析
            response = self.model_selector.chat_completion(messages=[{"role": "user", "content": prompt}], purpose="exercise_analysis")

            # 5. 解析结果
            content = response.choices[0].message.content
            result = self.safe_json_loads(content)

            # 6. 添加知识点上下文
            result['knowledge_context'] = all_knowledge_points

            logger.info("Successfully analyzed exercise")
            return result

        except Exception as e:
            logger.error(f"Error analyzing exercise: {str(e)}")
            raise

    def answer_student_question(self, question: str, course_name: Optional[str] = None, chat_history: Optional[List[Dict[str, str]]] = None, top_k: int = 5) -> Dict:
        """在线学习助手 - 回答学生问题

        参数:
            question: 学生提出的问题
            course_name: 课程名称，可选，用于日志或过滤资料
            chat_history: 对话上下文，可选，列表元素格式同 OpenAI messages
            top_k: 检索的文档数量
        返回:
            {{"answer": str, "references": List[Dict], "knowledgePoints": List[str]}}
        """
        try:
            # 1. 检索相关知识库内容
            # 如果课程名称不为空，则在问题前加上课程信息
            if course_name:
                question = f"对于{course_name}" + question
            
            relevant_docs = self.search_knowledge_base(question, top_k=top_k)

            # 1.1 对检索内容做清洗，移除潜在的控制字符并限制长度，避免影响大模型输出的 JSON 合法性
            def _sanitize(s: str, max_len: int) -> str:
                s = re.sub(r'[\x00-\x08\x0B-\x0C\x0E-\x1F]', ' ', s or '')
                s = s.replace('\u0000', ' ').strip()
                if len(s) > max_len:
                    s = s[:max_len] + '...'
                return s

            sanitized_docs = []
            for d in relevant_docs:
                sanitized_docs.append({
                    'content': _sanitize(d.get('content', ''), 800),
                    'source': _sanitize(d.get('source', ''), 200)
                })

            # 1.2 清洗问题与历史对话
            safe_question = _sanitize(question, 500)
            safe_history = None
            if chat_history:
                safe_history = []
                for m in chat_history:
                    safe_history.append({
                        'role': m.get('role', 'user'),
                        'content': _sanitize(m.get('content', ''), 500)
                    })

            # 2. 构造提示词
            prompt = PromptTemplates.get_online_assistant_prompt(
                question=safe_question,
                relevant_docs=sanitized_docs,
                course_name=course_name or "",
                chat_history=safe_history
            )

            # 3. 调用大模型生成回答
            response = self.model_selector.chat_completion(messages=[{"role": "user", "content": prompt}], purpose="online_assistant")
            content = response.choices[0].message.content
            logger.info("LLM 返回内容: %s", content)

            # 4. 解析JSON
            result = self.safe_json_loads(content)

            # 5. 如果需要，补充引用材料原始信息
            if "references" in result and isinstance(result["references"], list):
                # 可能模型返回的引用仅含摘要，确保包含source
                enriched_refs = []
                for ref in result["references"]:
                    source = ref.get("source")
                    # 查找原 doc content
                    match_doc = next((d for d in relevant_docs if d["source"] == source), None)
                    if match_doc:
                        ref.setdefault("content", match_doc["content"])
                    enriched_refs.append(ref)
                result["references"] = enriched_refs

            logger.info("Successfully answered student question")
            return result
        except Exception as e:
            logger.error(f"Error answering student question: {str(e)}")
            raise

    def generate_student_exercise(self, requirements: str, knowledge_preferences: str, wrong_questions: Optional[List[Dict[str, Any]]] = None) -> Dict:
        """根据学生需求、知识点偏好以及历史错题生成练习题"""
        try:
            prompt = PromptTemplates.get_student_exercise_generation_prompt(
                requirements=requirements,
                knowledge_preferences=knowledge_preferences,
                wrong_questions=wrong_questions or []
            )

            response = self.model_selector.chat_completion(messages=[{"role": "user", "content": prompt}], purpose="student_exercise_generation")

            content = response.choices[0].message.content
            logger.info(f"LLM raw output for student exercise:\n{content}")

            result = self.safe_json_loads(content)
            logger.info("Successfully generated student exercise")
            return result
        except Exception as e:
            logger.error(f"Error generating student exercise: {str(e)}")
            raise

    def generate_course_optimization(
        self,
        course_name: str,
        section_name: str,
        average_score: float,
        error_rate: float,
        student_count: int
    ) -> Dict:
        """生成课程优化建议"""
        try:
            # 1. 检索相关知识库内容
            query = f"{course_name} {section_name}"
            relevant_docs = self.search_knowledge_base(query)

            # 2. 生成优化建议
            prompt = PromptTemplates.get_course_optimization_prompt(
                course_name=course_name,
                section_name=section_name,
                average_score=average_score,
                error_rate=error_rate,
                student_count=student_count,
                relevant_docs=relevant_docs
            )

            response = self.model_selector.chat_completion(messages=[{"role": "user", "content": prompt}], purpose="course_optimization")

            content = response.choices[0].message.content
            result = self.safe_json_loads(content)
            logger.info("Successfully generated course optimization suggestions")
            return result

        except Exception as e:
            logger.error(f"Error generating course optimization suggestions: {str(e)}")
            raise

    def revise_teaching_content(self, original_plan: Dict, feedback: str) -> Dict:
        """根据教师反馈修改教学大纲"""
        try:
            prompt = PromptTemplates.get_teaching_content_feedback_prompt(original_plan, feedback)
            response = self.model_selector.chat_completion(messages=[{"role": "user", "content": prompt}], purpose="teaching_content_feedback")
            content = response.choices[0].message.content
            logger.info("教学大纲反馈 LLM 返回内容: %s", content)
            result = self.safe_json_loads(content)
            logger.info("Successfully revised teaching content according to feedback")
            return result
        except Exception as e:
            logger.error(f"Error revising teaching content: {str(e)}")
            raise

    def generate_step_detail(self, lesson_title: str, step_name: str, current_content: str, knowledge_points: List[str]) -> Dict:
        """生成课时中某个环节的详细内容"""
        try:
            prompt = PromptTemplates.get_step_detail_prompt(
                lesson_title=lesson_title,
                step_name=step_name,
                current_content=current_content,
                knowledge_points=knowledge_points
            )
            response = self.model_selector.chat_completion(messages=[{"role": "user", "content": prompt}], purpose="step_detail")
            content = response.choices[0].message.content
            logger.info("课时环节细节 LLM 返回内容: %s", content)
            result = self.safe_json_loads(content)
            logger.info("Successfully generated step detail")
            return result
        except Exception as e:
            logger.error(f"Error generating step detail: {str(e)}")
            raise

    # ---------------- 新增：根据已选题目生成相似练习 -----------------
    def generate_exercise_from_selected(self, selected_questions: List[Dict[str, Any]], requirements: str = "", knowledge_preferences: str = "") -> Dict:
        """基于学生选定题目生成相关练习题"""
        try:
            prompt = PromptTemplates.get_selected_questions_generation_prompt(
                selected_questions=selected_questions,
                requirements=requirements,
                knowledge_preferences=knowledge_preferences
            )

            response = self.model_selector.chat_completion(messages=[{"role": "user", "content": prompt}], purpose="selected_questions_exercise")
            content = response.choices[0].message.content
            logger.info(f"LLM raw output for selected-question exercise:\n{content}")
            result = self.safe_json_loads(content)
            logger.info("Successfully generated selected-based student exercise")
            return result
        except Exception as e:
            logger.error(f"Error generating selected-based exercise: {str(e)}")
            raise

    # -------------------- 动态更新存储路径 --------------------
    def reload_vector_db(self):
        """重新加载所有向量数据库（在路径列表变更后调用）"""
        logger.info("Reloading vector databases...")
        self.vector_dbs.clear()
        
        vector_db_paths = self.storage_service.get_vector_db_paths()
        logger.info(f"Reloading with vector DB paths: {vector_db_paths}")
        
        for path in vector_db_paths:
            db = FAISSDatabase(dim=self.embedding_service.dimensions)
            index_file = os.path.join(path, "index.faiss")
            if os.path.exists(index_file):
                try:
                    db.load(path)
                    logger.info(f"Loaded existing vector DB from {path}")
                except Exception as e:
                    logger.warning(f"Load failed for {path}: {e}. Using empty DB.")
            else:
                os.makedirs(path, exist_ok=True)
                logger.info(f"Vector DB not found in {path}, initialized empty.")

            self.vector_dbs[path] = db

        # 更新兼容旧接口的引用
        if self.vector_dbs:
            first_path = list(self.vector_dbs.keys())[0]
            self.vector_db = self.vector_dbs[first_path]
        else:
            logger.warning("No vector databases available after reload")
        
        logger.info(f"Vector DB reload complete. Active databases: {list(self.vector_dbs.keys())}")
