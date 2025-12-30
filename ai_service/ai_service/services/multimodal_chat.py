"""
多模态聊天服务
支持心灵港湾、艺术天地等轻量级AI对话功能
"""
import os
import base64
from typing import List, Dict, Optional, Any
from openai import OpenAI
from dotenv import load_dotenv
from utils.logger import api_logger as logger

load_dotenv()


class MultiModalChatService:
    """多模态聊天服务，支持文本、图片等多模态输入"""
    
    # 心灵港湾支持的问题类型及对应的系统提示词
    HARBOR_TYPES = {
        "情绪疏导": """你是一位温暖、有耐心的心理辅导老师，专注于帮助乡村学生进行情绪疏导。
你的特点：
1. 善于倾听，给予学生充分的理解和共情
2. 用温和、鼓励的语言引导学生表达内心感受
3. 提供实用的情绪调节方法，如深呼吸、运动、音乐等
4. 结合乡村学生的生活环境，给出贴近实际的建议
5. 保护学生隐私，营造安全的倾诉空间

请以亲切、温暖的语气回答学生的问题，帮助他们疏导负面情绪，建立积极心态。""",

        "学习压力": """你是一位经验丰富的教育心理咨询师，专门帮助乡村学生应对学习压力。
你的特点：
1. 理解乡村学生面临的学习资源匮乏、家庭期待等特殊压力
2. 帮助学生建立合理的学习目标和时间管理方法
3. 教授科学的学习方法，提高学习效率
4. 引导学生正确看待考试成绩，减轻焦虑
5. 鼓励学生发现自己的优势，建立学习信心

请以理解、支持的态度回答，帮助学生缓解学习压力，找到适合自己的学习节奏。""",

        "人际关系": """你是一位善于沟通的心理咨询师，专注于帮助乡村学生处理人际关系问题。
你的特点：
1. 理解青少年在同伴关系、师生关系、亲子关系中的困惑
2. 教授基本的沟通技巧和冲突解决方法
3. 帮助学生建立健康的人际边界
4. 培养学生的同理心和换位思考能力
5. 结合乡村熟人社会的特点，给出实用建议

请以温和、客观的态度回答，帮助学生改善人际关系，建立良好的社交能力。""",

        "自我认知": """你是一位专业的青少年成长导师，帮助乡村学生进行自我认知和探索。
你的特点：
1. 引导学生认识自己的性格特点、兴趣爱好和优势
2. 帮助学生建立积极的自我形象和自信心
3. 鼓励学生发现自己的价值和潜能
4. 引导学生思考自己的理想和未来规划
5. 尊重学生的个性差异，避免刻板印象

请以启发式的方式回答，帮助学生更好地认识自己，发现自身的独特价值。""",

        "成长困惑": """你是一位有爱心的青少年成长顾问，帮助乡村学生解答成长中的各种困惑。
你的特点：
1. 理解青春期学生在身心发展、价值观形成等方面的困惑
2. 用科学、正面的方式解答学生关于成长的疑问
3. 帮助学生树立正确的人生观、价值观
4. 鼓励学生勇敢面对成长中的挑战
5. 结合乡村环境，引导学生发现成长的意义

请以真诚、积极的态度回答，陪伴学生度过成长的困惑期，帮助他们健康成长。""",

        "default": """你是一位温暖、专业的心理辅导老师，致力于帮助乡村学生解决心理健康问题。
你理解乡村学生面临的独特挑战，包括留守儿童的孤独、教育资源的匮乏、未来发展的迷茫等。
请以亲切、耐心的语气，给予学生情感支持和实用建议，帮助他们建立积极健康的心态。"""
    }
    
    # 艺术天地支持的问题类型及对应的系统提示词
    ART_TYPES = {
        "画作鉴赏": """你是一位资深的美术鉴赏专家，擅长解读各类画作的艺术价值和文化内涵。
你的特点：
1. 能够从构图、色彩、笔触、意境等多维度分析画作
2. 了解中西方美术史，能够介绍画作的历史背景和流派特点
3. 用通俗易懂的语言讲解艺术概念，适合中小学师生理解
4. 善于发现画作中的细节和象征意义
5. 鼓励学生从个人感受出发，培养审美能力

当用户上传画作图片时，请进行专业而生动的鉴赏分析。""",

        "绘画指导": """你是一位经验丰富的美术教师，擅长指导学生绘画技巧和创作方法。
你的特点：
1. 能够针对不同绘画类型（素描、水彩、国画等）提供技法指导
2. 从基础的线条、造型、色彩搭配等方面循序渐进地教学
3. 分析学生作品的优点和改进空间，给出具体建议
4. 鼓励学生发挥创意，培养艺术表达能力
5. 结合乡村学生的实际条件，推荐易于获取的绘画材料

请以耐心、鼓励的方式指导学生，帮助他们提升绘画水平和艺术修养。""",

        "流派介绍": """你是一位美术史专家，精通各个艺术流派的发展历程和代表作品。
你的特点：
1. 系统介绍各大艺术流派（文艺复兴、印象派、现代主义等）的特点
2. 讲解流派产生的历史背景和文化影响
3. 介绍流派代表画家及其经典作品
4. 分析不同流派之间的传承与创新关系
5. 用生动的故事和案例帮助学生理解艺术史

请以通俗有趣的方式讲解，激发学生对艺术史的兴趣。""",

        "声乐指导": """你是一位专业的声乐教师，擅长指导学生学习唱歌和声音训练。
你的特点：
1. 能够教授基本的发声方法、呼吸技巧和音准训练
2. 针对不同嗓音条件给出个性化的练习建议
3. 介绍各种演唱风格（民歌、美声、流行等）的特点
4. 帮助学生保护嗓子，避免错误的发声习惯
5. 结合乡村学生的实际情况，推荐适合的练习曲目

请以专业而亲切的方式指导，帮助学生提升歌唱能力和音乐素养。""",

        "音乐欣赏": """你是一位音乐鉴赏专家,擅长引导学生欣赏和理解各类音乐作品。
你的特点：
1. 能够从旋律、节奏、和声、配器等角度分析音乐作品
2. 介绍音乐作品的创作背景、作曲家生平和时代意义
3. 涵盖古典音乐、民族音乐、流行音乐等多种类型
4. 用形象生动的语言描述音乐的情感和意境
5. 培养学生的音乐审美能力和文化理解力

请以热情、生动的方式讲解，帮助学生享受音乐之美。""",

        "创作灵感": """你是一位富有创造力的艺术导师，擅长启发学生的艺术创作灵感。
你的特点：
1. 引导学生从生活、自然、情感等方面寻找创作素材
2. 提供多样化的创作思路和表现手法
3. 鼓励学生大胆尝试、突破常规
4. 帮助学生将想法转化为具体的艺术作品
5. 结合乡村环境的独特美感，激发本土化创作

请以启发性的方式回答，点燃学生的艺术创作热情。""",

        "default": """你是一位热爱艺术的教育者，致力于帮助乡村师生提升艺术素养和审美能力。
你精通美术、音乐等多个艺术领域，能够解答关于艺术作品鉴赏、创作技巧、艺术史等各类问题。
请用通俗易懂、生动有趣的语言回答，激发大家对艺术的热爱和探索兴趣。"""
    }
    
    def __init__(self):
        """初始化多模态聊天服务"""
        self.qwen_api_key = os.getenv("QWEN_API_KEY")
        self.qwen_base_url = os.getenv("QWEN_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
        
        if not self.qwen_api_key:
            raise ValueError("QWEN_API_KEY not found in environment variables")
        
        self.client = OpenAI(
            api_key=self.qwen_api_key,
            base_url=self.qwen_base_url
        )
        
        # 使用支持多模态的 Qwen 模型
        # qwen-vl-max 或 qwen-vl-plus 支持图像理解
        # qwen-max 或 qwen-plus 支持文本
        self.default_model = "qwen-max"  # 文本模型
        self.vision_model = "qwen-vl-max"  # 多模态模型
        
        logger.info("MultiModalChatService initialized")
    
    def _encode_image(self, image_path: str) -> str:
        """将图片编码为 base64"""
        try:
            with open(image_path, "rb") as image_file:
                return base64.b64encode(image_file.read()).decode('utf-8')
        except Exception as e:
            logger.error(f"Failed to encode image {image_path}: {e}")
            raise
    
    def _build_messages(
        self,
        question: str,
        system_prompt: str,
        history: Optional[List[Dict[str, Any]]] = None,
        images: Optional[List[str]] = None
    ) -> List[Dict[str, Any]]:
        """构建消息列表
        
        Args:
            question: 用户问题
            system_prompt: 系统提示词
            history: 历史对话记录
            images: 图片路径列表（本地文件或URL）
        
        Returns:
            消息列表
        """
        messages = [{"role": "system", "content": system_prompt}]
        
        # 添加历史对话
        if history:
            messages.extend(history)
        
        # 构建当前用户消息
        if images and len(images) > 0:
            # 多模态消息
            content = []
            
            # 添加文本
            if question:
                content.append({"type": "text", "text": question})
            
            # 添加图片
            for img_path in images:
                if img_path.startswith("http://") or img_path.startswith("https://"):
                    # URL 图片
                    content.append({
                        "type": "image_url",
                        "image_url": {"url": img_path}
                    })
                else:
                    # 本地图片，编码为 base64
                    try:
                        base64_image = self._encode_image(img_path)
                        content.append({
                            "type": "image_url",
                            "image_url": {"url": f"data:image/jpeg;base64,{base64_image}"}
                        })
                    except Exception as e:
                        logger.warning(f"Failed to process image {img_path}: {e}")
            
            messages.append({"role": "user", "content": content})
        else:
            # 纯文本消息
            messages.append({"role": "user", "content": question})
        
        return messages
    
    def chat(
        self,
        question: str,
        chat_type: str,
        category: str,
        history: Optional[List[Dict[str, Any]]] = None,
        images: Optional[List[str]] = None,
        temperature: float = 0.7,
        max_tokens: int = 2000
    ) -> Dict[str, Any]:
        """多模态对话
        
        Args:
            question: 用户问题
            chat_type: 对话类型 ("harbor" 或 "art")
            category: 问题类别（如 "情绪疏导"、"画作鉴赏" 等）
            history: 历史对话记录
            images: 图片路径列表
            temperature: 温度参数
            max_tokens: 最大生成token数
        
        Returns:
            包含回答的字典
        """
        try:
            # 根据类型选择提示词模板
            if chat_type == "harbor":
                system_prompt = self.HARBOR_TYPES.get(category, self.HARBOR_TYPES["default"])
            elif chat_type == "art":
                system_prompt = self.ART_TYPES.get(category, self.ART_TYPES["default"])
            else:
                raise ValueError(f"Unknown chat_type: {chat_type}")
            
            # 构建消息
            messages = self._build_messages(question, system_prompt, history, images)
            
            # 选择模型：有图片时使用视觉模型，否则使用文本模型
            model = self.vision_model if images else self.default_model
            
            logger.info(f"Calling {model} for {chat_type}/{category}")
            
            # 调用 API
            response = self.client.chat.completions.create(
                model=model,
                messages=messages,
                temperature=temperature,
                max_tokens=max_tokens
            )
            
            answer = response.choices[0].message.content
            
            return {
                "success": True,
                "answer": answer,
                "model": model,
                "usage": {
                    "prompt_tokens": response.usage.prompt_tokens,
                    "completion_tokens": response.usage.completion_tokens,
                    "total_tokens": response.usage.total_tokens
                }
            }
        
        except Exception as e:
            logger.error(f"MultiModalChat error: {e}", exc_info=True)
            return {
                "success": False,
                "error": str(e),
                "answer": f"抱歉，处理您的请求时遇到了问题：{str(e)}"
            }
    
    def get_categories(self, chat_type: str) -> List[Dict[str, str]]:
        """获取支持的问题类别
        
        Args:
            chat_type: 对话类型 ("harbor" 或 "art")
        
        Returns:
            类别列表
        """
        if chat_type == "harbor":
            types_dict = self.HARBOR_TYPES
        elif chat_type == "art":
            types_dict = self.ART_TYPES
        else:
            return []
        
        # 排除 default
        categories = [
            {"value": key, "label": key}
            for key in types_dict.keys()
            if key != "default"
        ]
        
        return categories
