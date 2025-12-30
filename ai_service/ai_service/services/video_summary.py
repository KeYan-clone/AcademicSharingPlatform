"""
视频摘要生成服务
支持通过视频内容分析生成摘要，包括内容概述、阶段划分、重要时间点等
"""
import os
import tempfile
import re
from typing import Dict, List, Optional, Any
import moviepy.editor as mp
import whisper
from utils.logger import api_logger as logger
from services.model_selector import ModelSelector
from services.prompts import PromptTemplates


class VideoSummaryService:
    """视频摘要生成服务"""
    
    def __init__(self):
        self.model_selector = ModelSelector()
        
    def generate_video_summary(self, video_path: str, video_title: str = "", course_name: str = "") -> Dict[str, Any]:
        """
        生成视频摘要
        
        Args:
            video_path: 视频文件路径
            video_title: 视频标题
            course_name: 课程名称
            
        Returns:
            包含视频摘要信息的字典
        """
        try:
            # 1. 提取视频元信息
            video_info = self._extract_video_info(video_path)
            
            # 2. 提取音频并转写为文本
            transcript = self._extract_audio_transcript(video_path)
            
            if not transcript:
                logger.warning(f"视频 {video_path} 无可识别的音频内容")
                return {
                    "summary": "该视频无音频内容或音频无法识别",
                    "stages": [],
                    "keyPoints": [],
                    "duration": video_info.get("duration", 0),
                    "transcript": ""
                }
            
            # 3. 生成视频摘要
            summary_result = self._generate_summary_from_transcript(
                transcript, video_title, course_name, video_info
            )
            
            # 4. 添加视频基本信息
            summary_result.update({
                "duration": video_info.get("duration", 0),
                "transcript": transcript.get("full_text", ""),
                "video_title": video_title,
                "course_name": course_name
            })
            
            logger.info(f"成功生成视频摘要: {video_title}")
            return summary_result
            
        except Exception as e:
            logger.error(f"生成视频摘要失败: {video_path}, 错误: {str(e)}")
            raise
    
    def _extract_video_info(self, video_path: str) -> Dict[str, Any]:
        """提取视频基本信息"""
        try:
            video = mp.VideoFileClip(video_path)
            info = {
                "duration": int(video.duration) if video.duration else 0,
                "fps": video.fps if hasattr(video, 'fps') else 0,
                "size": video.size if hasattr(video, 'size') else [0, 0]
            }
            video.close()
            return info
        except Exception as e:
            logger.error(f"提取视频信息失败: {video_path}, 错误: {str(e)}")
            return {"duration": 0, "fps": 0, "size": [0, 0]}
    
    def _extract_audio_transcript(self, video_path: str) -> Optional[Dict[str, Any]]:
        """提取音频并转写为文本"""
        audio_path = None
        try:
            # 1. 创建临时音频文件
            with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tmp_audio:
                audio_path = tmp_audio.name
            
            # 2. 提取音频
            video = mp.VideoFileClip(video_path)
            if video.audio is None:
                logger.warning(f"视频文件 {video_path} 不包含音频轨道")
                return None
            
            # 输出为16KHz、16-bit的WAV格式（Whisper推荐）
            video.audio.write_audiofile(
                audio_path,
                fps=16000,
                codec='pcm_s16le',
                verbose=False,
                logger=None
            )
            video.close()
            
            # 3. 使用Whisper进行语音识别
            model = whisper.load_model("base")
            result = model.transcribe(audio_path, language='zh')
            
            # 4. 处理转写结果
            segments = result.get('segments', [])
            full_text = result.get('text', '').strip()
            
            if not full_text:
                logger.warning(f"Whisper未识别到音频内容: {video_path}")
                return None
            
            # 5. 格式化分段信息
            formatted_segments = []
            for seg in segments:
                if seg.get('text', '').strip():
                    formatted_segments.append({
                        'text': seg.get('text', '').strip(),
                        'start': round(seg.get('start', 0), 1),
                        'end': round(seg.get('end', 0), 1)
                    })
            
            return {
                "full_text": full_text,
                "segments": formatted_segments
            }
            
        except Exception as e:
            logger.error(f"音频转写失败: {video_path}, 错误: {str(e)}")
            return None
        finally:
            # 清理临时文件
            if audio_path and os.path.exists(audio_path):
                try:
                    os.remove(audio_path)
                except:
                    pass
    
    def _generate_summary_from_transcript(
        self, 
        transcript: Dict[str, Any], 
        video_title: str, 
        course_name: str,
        video_info: Dict[str, Any]
    ) -> Dict[str, Any]:
        """基于转写文本生成视频摘要"""
        try:
            # 1. 构造提示词
            prompt = PromptTemplates.get_video_summary_prompt(
                transcript=transcript.get("full_text", ""),
                segments=transcript.get("segments", []),
                video_title=video_title,
                course_name=course_name,
                duration=video_info.get("duration", 0)
            )
            
            # 2. 调用大模型生成摘要
            response = self.model_selector.chat_completion(
                messages=[{"role": "user", "content": prompt}], 
                purpose="video_summary"
            )
            content = response.choices[0].message.content
            
            # 3. 解析JSON结果
            result = self._safe_json_loads(content)
            
            logger.info("成功生成视频摘要")
            return result
            
        except Exception as e:
            logger.error(f"生成视频摘要失败: {str(e)}")
            # 返回基础摘要
            return {
                "summary": "自动摘要生成失败，但视频内容已成功转写",
                "stages": [
                    {
                        "title": "完整内容",
                        "start_time": 0,
                        "end_time": video_info.get("duration", 0),
                        "description": transcript.get("full_text", "")[:200] + "..."
                    }
                ],
                "keyPoints": []
            }
    
    def _safe_json_loads(self, text: str) -> Dict[str, Any]:
        """安全解析JSON，包含清理和错误处理"""
        import json
        try:
            # 移除可能的markdown代码块标记
            text = re.sub(r'```json\s*', '', text)
            text = re.sub(r'```\s*$', '', text)
            text = text.strip()
            
            # 尝试解析JSON
            return json.loads(text)
        except json.JSONDecodeError as e:
            logger.error(f"JSON解析失败: {str(e)}")
            logger.error(f"原始文本: {text}")
            
            # 返回默认结构
            return {
                "summary": "摘要生成过程中出现格式错误",
                "stages": [],
                "keyPoints": []
            }
    
    def generate_summary_from_text(self, text_content: str, video_title: str = "", course_name: str = "") -> Dict[str, Any]:
        """
        基于已有文本生成视频摘要（用于测试或已有转写文本的情况）
        """
        try:
            # 模拟分段信息
            sentences = text_content.split('。')
            segments = []
            current_time = 0
            for i, sentence in enumerate(sentences):
                if sentence.strip():
                    segments.append({
                        'text': sentence.strip() + '。',
                        'start': current_time,
                        'end': current_time + 5  # 假设每句话5秒
                    })
                    current_time += 5
            
            transcript = {
                "full_text": text_content,
                "segments": segments
            }
            
            video_info = {"duration": current_time}
            
            return self._generate_summary_from_transcript(transcript, video_title, course_name, video_info)
            
        except Exception as e:
            logger.error(f"基于文本生成摘要失败: {str(e)}")
            raise
