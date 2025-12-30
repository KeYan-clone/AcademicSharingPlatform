"""
提示词管理模块
"""
from typing import List, Dict, Optional, Any
import json

class PromptTemplates:
    """提示词模板管理"""

    @staticmethod
    def get_knowledge_points_extraction_prompt(course_name: str, course_outline: str) -> str:
        """提取知识点的提示词"""
        return f"""
        作为一名经验丰富的教育专家，请分析以下课程大纲，提取关键知识点：

        课程名称：{course_name}
        课程大纲：
        {course_outline}

        请列出所有关键知识点，要求：
        1. 每个知识点独占一行
        2. 知识点应该具体且可操作
        3. 知识点的粒度要适中，既不要太大也不要太小
        4. 知识点之间应该有逻辑关联
        5. 确保覆盖大纲中的所有重要内容
        """

    @staticmethod
    def get_teaching_content_generation_prompt(
        course_name: str,
        course_outline: str,
        expected_hours: int,
        knowledge_base: Dict[str, List[Dict[str, str]]],
        constraints: Optional[str] = None
    ) -> str:
        """生成教学内容的提示词，可选 constraints 补充教师要求"""
        constraints_part = f"\n教师额外要求：\n{constraints}\n" if constraints else ""
        return f"""
        作为一名资深教育专家，请根据以下信息生成详细的教学方案：

        课程名称：{course_name}
        预期课时：{expected_hours}
        课程大纲：
        {course_outline}
{constraints_part}
        参考资料：
        {PromptTemplates._format_knowledge_base(knowledge_base)}

        请生成一个完整的教学方案，重点如下：

        1. 课时规划（简要说明）：
           - 将内容合理分配到{expected_hours}个课时（大部分情况只有一个课时）
           - 每个课时的时长为45分钟，老师希望你帮忙详细规划这45分钟的每个环节
           - 课时安排要循序渐进，难度逐步提升

        2. 每个课时必须包含（主要内容）：
           - 标题：简洁清晰地概括本课时内容
           - 详细的45分钟时间分配（如5min导入、10min知识讲解、15min案例分析、10min练习、5min总结），每个环节都要有具体内容说明
           - 知识点讲解：详细的教学内容，包括概念解释、原理分析、案例说明等
           - 实训练习：针对本课时内容设计的实践任务，帮助学生巩固所学知识
           - 教学指导建议：包括教学方法、重难点提示、可能的学生疑问及解答等

        3. 整体教学建议（简要说明）：
           - 总体时间分配策略
           - 教学方法和教具使用建议
           - 学生参与度提升建议
           - 课程难点突破建议

        请以JSON格式返回，结构如下：
        {{
            "lessons": [
                {{
                    "title": "课时标题",
                    "timePlan": [
                        {{"step": "导入", "minutes": 5, "content": "导入内容"}},
                        {{"step": "知识讲解", "minutes": 10, "content": "讲解内容"}},
                        {{"step": "案例分析", "minutes": 15, "content": "案例内容"}},
                        {{"step": "练习", "minutes": 10, "content": "练习内容"}},
                        {{"step": "总结", "minutes": 5, "content": "总结内容"}}
                    ],
                    "knowledgePoints": ["知识点1", "知识点2"],
                    "practiceContent": "实训练习内容",
                    "teachingGuidance": "教学指导建议"
                }}
            ],
            "totalHours": "总课时数",
            "teachingAdvice": "整体教学建议"
        }}

        注意事项：
        1. 每个课时的时间分配要具体、合理，内容要详细，便于老师直接使用
        2. 充分利用参考资料中的内容，确保生成的内容与资料保持一致
        3. 实训练习要贴近实际，难度适中
        4. 教学指导要具体实用，便于教师参考
        5. 确保总课时数与预期课时相符
        6. teachingAdvice 字段必须是字符串格式

        请只返回标准 JSON 格式，一次返回，不要 Markdown、不要注释、不要多余文字内容。
        """

    @staticmethod
    def get_exercise_generation_prompt(
        course_name: str,
        lesson_content: str,
        difficulty: str = "medium",
        choose_count: int = 5,
        fill_blank_count: int = 5,
        question_count: int = 2,
        custom_types: dict = None
    ) -> str:
        """生成练习题的提示词"""
        # 动态题型描述
        if custom_types:
            type_lines = [
                f"1. 选择题 {choose_count}道",
                f"2. 填空题 {fill_blank_count}道",
                f"3. 计算或者简答题 {question_count}道"
            ]
            for k, v in custom_types.items():
                type_lines.append(f"{k}：{v}道")
            type_desc = "\n".join(type_lines)
        else:
            type_desc = f"1. 选择题 {choose_count}道\n2. 填空题 {fill_blank_count}道\n3. 计算或者简答题 {question_count}道"
        return f"""
        作为一名专业的教育测评专家，请根据以下课程内容生成练习题：

        课程名称：{course_name}
        课程内容：
        {lesson_content}
        难度要求：{difficulty}

        请生成多种类型的练习题，包括：
        {type_desc}

        每道题目必须包含：
        1. 题目描述
        2. 参考答案
        3. 解题思路
        4. 相关知识点

        请以JSON格式返回，结构如下：
        {{
            "exercises": [
                {{
                    "type": "题目类型",
                    "question": "题目描述",
                    "options": "选项数组", //Array(String),选择题使用，除了单选、多选外数组返回空即可，生成的选项不要带A. B. C. D. 等前缀，直接返回选项内容即可。
                    "answer": "参考答案",  //选择题返回的答案依旧是 A|B|C|D 这种格式，而非选项本身
                    "explanation": "解题思路",
                    "knowledge_points": ["相关知识点1", "相关知识点2"]
                }}
            ]
        }}

        特殊要求：题目类型应当返回:尽管生成题目是按照上面要求的各种类型生成，但最终为了方便数据库存储，返回的类型应当属于'singlechoice', 'program', 'fillblank','judge' 四个的其中一个
        所有选择题,无论单选还是多选,type统一返回singlechoice。
        填空题为"fillblank", "judge"为判断题
        除此以外所有题目类型都显示"program"
        """

    @staticmethod
    def get_answer_evaluation_prompt(
        question: str,
        student_answer: str,
        reference_answer: str
    ) -> str:
        """评估学生答案的提示词"""
        return f"""
        作为一名经验丰富的教育评估专家，请评估学生的答案，分析部分用中文：

        题目：
        {question}

        标准答案：
        {reference_answer}

        学生答案：
        {student_answer}

        请提供以下评估内容：
        1. 得分（0-100分）
        2. 答案正确性分析
        3. 思路完整性分析
        4. 表达准确性分析
        5. 具体改进建议

        请以JSON格式返回，结构如下：
        {{
            "score": "得分",
            "correctness": "正确性分析",
            "completeness": "完整性分析",
            "accuracy": "准确性分析",
            "suggestions": ["改进建议1", "改进建议2"]
        }}
        """

    @staticmethod
    def get_subjective_answer_evaluation_prompt(question: str, student_answer: str, reference_answer: str, max_score: float) -> str:
        return f"""你将以严谨的教学评估专家身份，对学生的主观题答案进行打分。请严格遵守以下要求：

        ================= 题目信息 =================
        题目：{question}

        【参考答案】（不可泄露给学生，用于对照评分）：
        {reference_answer}

        【学生答案】（仅依据此内容评分，请勿使用参考答案填充学生答案缺失的部分，学生答案为空或无关不得分）：
        {student_answer}

        满分：{max_score}分
        
         ================= 评分说明 =================
        1. 先根据参考答案列出 3~5 个关键要点（KeyPoints）。
        2. 检查学生答案中覆盖的要点数量与正确性。
        3. 计算得分：
           • 覆盖 ≥80% 要点且基本正确 → 90%~100% * 满分
           • 覆盖 50%~79% 要点 → 60%~89% * 满分
           • 覆盖 20%~49% 要点 → 30%~59% * 满分
           • 覆盖 <20% 要点或答案明显偏离 → ≤29% * 满分
        4. 如学生答案为空或者过于简陋，直接判 0 分，并指出“答案过于简略，无法评估”。

        请仅返回下面 JSON，禁止额外文本：
        {{
            "score": float,  // 得分，不超过满分
            "analysis": {{
                "correct_points": [string],  // 答对的要点列表
                "incorrect_points": [string],  // 答错或缺失的要点列表
                "knowledge_points": [string]  // 相关知识点列表，包括章节位置
            }},
            "feedback": string,  // 具体的修正建议
            "improvement_suggestions": [string]  // 改进建议列表
        }}

        评分标准：
        1. 根据答案的完整性、准确性和表达的清晰度进行评分
        2. 正确点和错误点要具体明确
        3. 反馈要有建设性，并指出具体的改进方向，用中文
        4. 知识点要标明具体的章节位置

        请确保返回的是合法的JSON格式。"""

    @staticmethod
    def get_exercise_analysis_prompt(exercise_questions: list) -> str:
        questions_str = json.dumps(exercise_questions, ensure_ascii=False, indent=2)
        return f"""你是一个专业的教育分析专家。请分析以下练习题的完成情况,分析部分请用中文

        练习题信息：
        {questions_str}

        请按照以下JSON格式返回分析结果：
        {{
            "overall_analysis": {{
                "average_score": float,  // 平均得分率
                "difficulty_analysis": string,  // 难度分析
                "common_issues": [string]  // 普遍存在的问题
            }},
            "knowledge_points_analysis": [
                {{
                    "point": string,  // 知识点
                    "mastery_level": string,  // 掌握程度：excellent/good/fair/poor
                    "error_rate": float,  // 错误率
                    "chapter_location": string,  // 所在章节
                    "improvement_suggestions": [string]  // 改进建议
                }}
            ],
            "teaching_suggestions": {{
                "key_focus_areas": [string],  // 需要重点关注的领域
                "recommended_methods": [string],  // 建议的教学方法
                "resource_suggestions": [string]  // 建议的补充资源
            }}
        }}

        分析要求：
        1. 全面评估学生的知识掌握情况
        2. 找出普遍存在的问题和薄弱环节
        3. 提供具体可行的教学建议
        4. 建议要具体且有针对性

        请确保返回的是合法的JSON格式。"""

    @staticmethod
    def _format_knowledge_base(knowledge_base: Dict[str, List[Dict[str, str]]]) -> str:
        """格式化知识库内容"""
        formatted = []
        for point, docs in knowledge_base.items():
            formatted.append(f"知识点：{point}")
            for doc in docs:
                formatted.append(f"- 来源：{doc['source']}")
                formatted.append(f"  内容：{doc['content']}\n")
        return "\n".join(formatted)

    @staticmethod
    def get_online_assistant_prompt(
        question: str,
        relevant_docs: List[Dict[str, str]],
        course_name: str = "",
        chat_history: Optional[List[Dict[str, str]]] = None
    ) -> str:
        """生成在线学习助手回答问题的提示词

        参数说明：
        - question: 学生提出的问题
        - relevant_docs: 与问题最相关的知识库片段，列表元素形如{"content": "...", "source": "..."}
        - course_name: 当前课程名称，可选
        - chat_history: 历史对话，用于上下文记忆，可选，列表元素形如{"role": "user/assistant/system", "content": "..."}
        """
        # 格式化参考资料
        formatted_docs = []
        for doc in relevant_docs:
            formatted_docs.append(f"来源：{doc['source']}\n内容：{doc['content']}")
        docs_str = "\n\n".join(formatted_docs) if formatted_docs else "无"

        # 格式化历史对话 - 区分历史摘要和最近对话
        history_lines = []
        if chat_history:
            for idx, msg in enumerate(chat_history, 1):
                role = msg.get("role", "user")
                content = msg.get("content", "")
                
                if role == "system":
                    # 这是历史摘要
                    history_lines.append(f"[历史对话摘要]: {content}")
                elif role == "user":
                    history_lines.append(f"学生{idx}: {content}")
                elif role == "assistant":
                    history_lines.append(f"助教{idx}: {content}")
        history_str = "\n".join(history_lines) if history_lines else "无"

        return f"""
        你是一名在线学习助教，需要基于课程资料为学生提供权威、详细且易于理解的回答。

        课程名称：{course_name if course_name else '未指定'}

        对话历史（包含历史摘要和最近对话）：
        {history_str}

        学生问题：
        {question}

        可参考的课程资料片段：
        {docs_str}

        请遵循以下要求生成回答：
        1. 首先给出对学生问题的详细解答，条理清晰、逻辑严谨。
        2. 如果有历史对话摘要，请结合历史讨论内容为学生提供连贯的回答。
        3. 回答应引用上述资料片段中的关键信息，但不得照搬，可适当改写确保通顺。
        4. 提取与问题相关的关键知识点列表。
        5. 指出回答引用的资料来源（source 字段）及其内容摘要。
        6. 如果课程资料与学生问题完全没有关系，可以不参考课程资料，并在回答后说明问题可能与课程库无关

        请仅以 JSON 格式返回，不要用markdown包裹！结构如下：
        {{
            "answer": string,          // 详细回答
            "references": [            // 引用的资料片段
                {{
                    "source": string, // 资料来源，例如 "教材P12" 或 "文件名 第3页"
                    "content": string // 被引用内容摘要
                }}
            ],
            "knowledgePoints": [string] // 相关知识点列表
        }}

        注意：
        1. 返回必须是合法 JSON，不要包含 Markdown、注释或多余文字。
        2. references、knowledgePoints 至少返回一个元素，如无可用内容则返回空数组。
        """

    @staticmethod
    def get_student_exercise_generation_prompt(
        requirements: str,
        knowledge_preferences: str,
        wrong_questions: list
    ) -> str:
        import json
        wrong_questions_json = json.dumps(wrong_questions, ensure_ascii=False, indent=2) if wrong_questions else "无"
        return f"""
        作为一名专业的学习评测助手，请根据以下信息为学生生成一套练习题：

        学生练习需求：
        {requirements}

        知识点偏好：
        {knowledge_preferences}

        历史错题（请优先保留并包含在返回结果中）：
        {wrong_questions_json}

        任务要求：
        1. 在满足学生练习需求的基础上，补充生成新的题目，使总题目数量、题型、难度等符合 "学生练习需求" 的描述。
        2. 题型命名仅可使用 'singlechoice'（代表所有选择，无论单选多选）, 'fillblank'（代表填空题）, 'judge'（代表判断题）, 'program'（代表剩余的所有题目类型） 四种；
        3. 每道题目请包含以下字段：type、question、options、answer、explanation、knowledge_points。
        4. 请严格返回合法 JSON，结构如下所示；不要包含 Markdown、注释或多余文本。

        返回 JSON 结构示例：
        {{
            "exercises": [
                {{
                    "type": "singlechoice",
                    "question": "题目描述",
                    "options": ["选项1", "选项2", "选项3", "选项4"],
                    "answer": "A",
                    "explanation": "解题思路",
                    "knowledge_points": ["相关知识点1", "相关知识点2"]
                }}
            ]
        }}
        """

    @staticmethod
    def get_selected_questions_generation_prompt(selected_questions: list, requirements: str = "", knowledge_preferences: str = "") -> str:
        """根据学生明确选定的题目，生成风格/知识点相近的新题目"""
        import json
        selected_json = json.dumps(selected_questions, ensure_ascii=False, indent=2)
        req_part = f"\n补充练习需求：{requirements}\n" if requirements else ""
        pref_part = f"\n知识点偏好：{knowledge_preferences}\n" if knowledge_preferences else ""
        return f"""
        你是一名专业的教学测评专家，学生选择了以下题目作为练习参考：

        已选题目列表（JSON）：
        {selected_json}

        {req_part}{pref_part}

        任务：
        1. 在满足学生练习需求的基础上，补充生成新的题目，使总题目数量、题型、难度等符合 "学生练习需求" 的描述。
        2. 所生成的新题目的知识点需与已选题目保持紧密相关，切勿引入无关领域的内容。
        3. 题型命名仅可使用 'singlechoice'（代表所有选择，无论单选多选）, 'fillblank'（代表填空题）, 'judge'（代表判断题）, 'program'（代表剩余的所有题目类型） 四种；

        返回 JSON 结构示例：
        {{
            "exercises": [
                {{
                    "type": "singlechoice",
                    "question": "题目描述",
                    "options": ["选项1", "选项2", "选项3", "选项4"],
                    "answer": "A",
                    "explanation": "解题思路",
                    "knowledge_points": ["相关知识点1", "相关知识点2"]
                }}
            ]
        }}

        请严格输出合法 JSON，不要包含 Markdown 或多余说明。"""

    @staticmethod
    def get_teaching_content_detail_prompt(
        title: str,
        knowledgePoints: list,
        practiceContent: str,
        teachingGuidance: str,
        timePlan: list,
        constraints: str | None = None
    ) -> str:
        """
        根据已有教案生成更详细的教案（内容更丰富，但仅基于传入参数）
        """
        constraints_part = f"\n教师额外要求：\n{constraints}\n" if constraints else ""
        return f"""
        你是一名资深教育专家，请基于以下教案信息，生成内容更详细、丰富的教案。请注意，生成内容只能基于下述参数，不得引入任何外部知识或假设。

        教案标题：{title}

        知识点列表：
        {json.dumps(knowledgePoints, ensure_ascii=False, indent=2)}

        实训练习内容：
        {practiceContent}

        教学指导建议：
        {teachingGuidance}

        时间分配方案（timePlan）：
        {json.dumps([dict(item) for item in timePlan], ensure_ascii=False, indent=2)}

        要求：
        1. 对每个时间分配环节（timePlan）进行更细致的内容扩展，明确每个环节的教学目标、具体活动、师生活动细节、互动方式等。
        2. 对每个知识点，补充更详细的讲解，包括定义、原理、应用场景、常见误区、举例说明等。
        3. 对实训练习，细化为多个具体任务或步骤，并给出每步的目标与评价标准。
        4. 教学指导部分要补充更多实用建议，如教学重难点、易错点、师生互动建议、分层教学策略等。
        5. 生成的详细教案结构与原教案一致，但每个字段内容更丰富、具体，便于教师直接使用。
        6. 只允许使用传入参数中的内容进行扩展和细化，不得引入任何未给出的知识点或内容。
        7. practiceContent, teachingGuidance, title 字段必须是字符串格式，knowledgePoints 字段必须是字符串数组
        8. 如果教师提出额外要求，请充分考虑并体现：{constraints_part}
        9. 返回内容必须为标准JSON格式，结构如下，注意严格按照格式：

        {{
            "title": "课时标题",
            "timePlan": [
                {{
                    "step": "环节名称",
                    "minutes": 时长,
                    "content": "字符串形式！，本环节详细内容，包含教学目标、活动安排、师生活动、互动方式等"
                }}
            ],
            "knowledgePoints": ["知识点1"， "知识点2"],
            "practiceContent": "实训练习内容",
            "teachingGuidance": "教学指导建议"
        }}

        注意事项：
        - 所有内容必须基于输入参数进行扩展和细化，不得添加任何外部知识。
        - 返回内容必须为合法JSON，不要包含Markdown、注释或多余文字。
        """

    @staticmethod
    def get_regenerate_teaching_content_prompt(
        title: str,
        knowledgePoints: list,
        practiceContent: str,
        teachingGuidance: str,
        timePlan: list,
        constraints: str | None = None
    ) -> str:
        """
        根据已有教案生成一版全新的教案（内容充实度保持一致，无需更丰富）
        """
        constraints_part = f"\n教师额外要求：\n{constraints}\n" if constraints else ""
        return f"""
        你是一名资深教育专家，请基于以下教案信息，生成一版全新的教案。要求内容结构、充实度与原教案一致，但表达方式、内容细节等需有明显不同，避免与原教案重复。不得引入任何外部知识或假设，仅可基于下述参数。

        教案标题：{title}

        知识点列表：
        {json.dumps(knowledgePoints, ensure_ascii=False, indent=2)}

        实训练习内容：
        {practiceContent}

        教学指导建议：
        {teachingGuidance}

        时间分配方案（timePlan）：
        {json.dumps([dict(item) for item in timePlan], ensure_ascii=False, indent=2)}

        要求：
        1. 重新组织每个时间分配环节（timePlan）的内容，表达方式需与原教案不同，但内容充实度保持一致。
        2. 对每个知识点，重新表述讲解内容，避免与原教案重复，内容详实但不需更丰富。
        3. 实训练习部分重新拆解为具体任务或步骤，表达方式需有变化。
        4. 教学指导部分用不同表述方式给出建议，内容充实度与原教案一致。
        5. 生成的教案结构与原教案一致，便于教师直接使用。
        6. 只允许使用传入参数中的内容进行改写和重组，不得引入任何未给出的知识点或内容。
        7. 如果教师提出额外要求，请充分考虑并体现：{constraints_part}
        8. 返回内容必须为标准JSON格式，结构如下：

        {{
            "title": "课时标题",
            "timePlan": [
                {{
                    "step": "环节名称",
                    "minutes": 时长,
                    "content": "本环节详细内容"
                }}
            ],
            "knowledgePoints": ["知识点1", "知识点2"],
            "practiceContent": "实训练习内容",
            "teachingGuidance": "教学指导建议"
        }}

        注意事项：
        - 所有内容必须基于输入参数进行改写和重组，不得添加任何外部知识。
        - 返回内容必须为合法JSON，不要包含Markdown、注释或多余文字。
        """

    @staticmethod
    def get_course_optimization_prompt(
        course_name: str,
        section_name: str,
        average_score: float,
        error_rate: float,
        student_count: int,
        relevant_docs: List[Dict[str, str]]
    ) -> str:
        """生成课程优化建议的提示词"""
        # 格式化参考资料
        formatted_docs = []
        for doc in relevant_docs:
            formatted_docs.append(f"来源：{doc['source']}\n内容：{doc['content']}")
        docs_str = "\n\n".join(formatted_docs) if formatted_docs else "无"

        return f"""
        作为一名专业的教育分析专家，请根据以下信息为课程章节提供优化建议：

        课程名称：{course_name}
        章节名称：{section_name}
        平均得分：{average_score}
        错误率：{error_rate}%
        学生人数：{student_count}

        相关课程资料：
        {docs_str}

        请提供以下分析和建议：
        1. 根据平均分和错误率，分析学生对该章节的掌握情况
        2. 找出可能的教学难点和学生易错点
        3. 提供具体的教学优化建议
        4. 推荐合适的教学资源和练习方式

        请以JSON格式返回，结构如下：
        {{
            "suggestions": [string],  // 具体的优化建议列表
            "relatedKnowledgePoints": [string],  // 相关知识点列表
            "recommendedActions": [string]  // 推荐的具体行动列表
        }}

        注意：
        1. 建议要具体可行，便于教师直接采用
        2. 分析要基于实际数据，重点关注异常指标
        3. 推荐的行动要有针对性，并说明预期效果
        4. 返回必须是合法JSON，不要包含Markdown、注释或多余文字
        """

    @staticmethod
    def get_teaching_content_feedback_prompt(original_plan: dict, feedback: str) -> str:
        """根据教师反馈修改教学大纲的提示词"""
        import json
        return f"""
        你是一名资深的课程设计专家，下面是已经生成的教学大纲(JSON)：\n\n{json.dumps(original_plan, ensure_ascii=False, indent=2)}\n\n教师对该大纲提出了如下修改意见，请基于这些反馈对教学大纲进行相应的调整：\n\n{feedback}\n\n请注意：\n1. 保留原有结构(lessons 数组及 teachingAdvice 等字段)，仅修改必要的部分以满足反馈。\n2. 对 lessons 中受影响的课时进行调整，例如修改标题、时间分配、知识点或补充/删除环节。\n3. 若教师未指明修改某部分，则保持原样。\n4. 必须返回完整的教学大纲，格式与原始 JSON 一致。\n5. 仅返回合法 JSON，不要包含 Markdown 或额外说明。"""

    @staticmethod
    def get_step_detail_prompt(
        lesson_title: str,
        step_name: str,
        current_content: str,
        knowledge_points: list
    ) -> str:
        """生成课时中某一教学环节的更详细内容"""
        import json
        return f"""
        你是一位教学设计专家，正在完善课时 \"{lesson_title}\" 的教学环节。\n\n当前环节：{step_name}\n\n该环节已有内容：{current_content if current_content else '（无）'}\n\n该课时涉及的知识点：{json.dumps(knowledge_points, ensure_ascii=False, indent=2)}\n\n请基于以上信息，为 {step_name} 环节生成更详细的教学设计，包括：\n1. 教学目标\n2. 活动流程(可分多步)\n3. 师生活动要点\n4. 评估方式\n5. 所需教具/资源\n\n返回格式要求(JSON)：\n{{\n  \"step\": \"{step_name}\",\n  \"objectives\": [string],\n  \"activities\": [string],\n  \"teacherGuidance\": string,\n  \"studentTasks\": [string],\n  \"assessment\": string,\n  \"resources\": [string]\n}}\n\n仅返回合法 JSON，不要包含 Markdown、注释或多余文字。"""

    @staticmethod
    def get_video_summary_prompt(
        transcript: str,
        segments: List[Dict[str, Any]],
        video_title: str = "",
        course_name: str = "",
        duration: int = 0
    ) -> str:
        """生成视频摘要的提示词"""
        # 格式化分段信息
        segments_text = ""
        if segments:
            segments_text = "\n时间轴信息："
            for seg in segments[:20]:  # 限制显示前20个分段，避免提示词过长
                start_time = seg.get('start', 0)
                end_time = seg.get('end', 0)
                text = seg.get('text', '')
                segments_text += f"\n{start_time:.1f}s-{end_time:.1f}s: {text}"
            if len(segments) > 20:
                segments_text += f"\n... (还有{len(segments)-20}个分段)"

        return f"""
        作为一名专业的教育内容分析师，请对以下视频内容进行深度分析并生成详细摘要：

        视频信息：
        - 标题：{video_title or "未知标题"}
        - 课程：{course_name or "未知课程"}
        - 时长：{duration}秒 ({duration//60}分{duration%60}秒)

        视频转写内容：
        {transcript}
        {segments_text}

        请分析视频内容并生成详细摘要，要求包括：

        1. 内容概述：对整个视频内容的简洁总结
        2. 阶段划分：将视频内容分为几个逻辑阶段，每个阶段包括：
           - 阶段标题
           - 起始时间和结束时间
           - 该阶段的核心内容描述
        3. 重要知识点：提取视频中的关键知识点和要点
        4. 重要时间点：标注一些关键的时间节点（如重要概念讲解、示例演示等）

        请以JSON格式返回，结构如下：
        {{
            "summary": "视频整体内容概述",
            "stages": [
                {{
                    "title": "阶段标题",
                    "start_time": 开始时间(秒),
                    "end_time": 结束时间(秒),
                    "description": "该阶段核心内容描述"
                }}
            ],
            "keyPoints": [
                {{
                    "point": "知识点内容",
                    "timestamp": 相关时间点(秒),
                    "importance": "high|medium|low"
                }}
            ],
            "keyTimestamps": [
                {{
                    "time": 时间点(秒),
                    "event": "重要事件或内容描述",
                    "type": "concept|example|summary|question"
                }}
            ]
        }}

        注意事项：
        1. 阶段划分要合理，每个阶段应该有明确的主题
        2. 时间点要准确，基于提供的时间轴信息
        3. 知识点要具体且有教学价值
        4. 重要程度要根据内容的核心性和复杂性判断
        5. 确保返回的JSON格式正确，不要包含Markdown标记
        6. 如果视频内容较短，阶段可以相应减少

        请只返回标准JSON格式，不要包含任何额外的文字说明。
        """
