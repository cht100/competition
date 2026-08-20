# -*- coding: utf-8 -*-
"""
AI分析服务模块
- LLM研判/谣言检测：调用阿里百炼 Qwen-plus 云端API
- 文本向量化：本地部署 BAAI/bge-large-zh-v1.5 模型（GPU推理）
"""

import json
import dashscope
from dashscope import Generation
from http import HTTPStatus
import numpy as np
import torch
from typing import Optional, List, Dict, Any

from config import (
    DASHSCOPE_API_KEY, MODEL_NAME, DISASTER_TYPES, INFO_TYPES,
    EMBEDDING_MODEL_NAME, EMBEDDING_MODEL_PATH, EMBEDDING_DEVICE,
    EMBEDDING_MAX_LENGTH, EMBEDDING_BATCH_SIZE, EMBEDDING_NORMALIZE,
    LORA_ADAPTER_PATH, LORA_ENABLED,
)

# =====================================================================
# 本地 Embedding 模型加载（服务启动时一次性加载到 GPU 显存）
# =====================================================================
import os
import logging
from sentence_transformers import SentenceTransformer

log = logging.getLogger(__name__)

def _load_embedding_model() -> SentenceTransformer:
    """加载本地 bge-large-zh-v1.5 模型 + LoRA 应急领域适配器（热插拔）"""
    model_path = EMBEDDING_MODEL_PATH if os.path.isdir(EMBEDDING_MODEL_PATH) else EMBEDDING_MODEL_NAME
    log.info(f"[Embedding] 正在加载基座模型: {model_path}，device={EMBEDDING_DEVICE}")
    model = SentenceTransformer(model_path, device=EMBEDDING_DEVICE)
    
    # 检测并加载 LoRA 适配器
    if LORA_ENABLED and os.path.isdir(LORA_ADAPTER_PATH):
        try:
            from peft import PeftModel
            base_transformer = model[0].auto_model
            peft_model = PeftModel.from_pretrained(base_transformer, LORA_ADAPTER_PATH)
            peft_model = peft_model.merge_and_unload()  # 合并权重，零额外推理开销
            model[0].auto_model = peft_model
            log.info(f"[Embedding] LoRA 适配器已加载并合并: {LORA_ADAPTER_PATH}")
        except Exception as e:
            log.warning(f"[Embedding] LoRA 适配器加载失败，使用基座模型: {e}")
    else:
        log.info("[Embedding] 未检测到 LoRA 适配器或已禁用，使用基座模型")
    
    log.info(f"[Embedding] 模型加载完成，维度={model.get_sentence_embedding_dimension()}，device={model.device}")
    return model

_embedding_model: SentenceTransformer = _load_embedding_model()


class AIAnalyzer:
    """AI分析器：LLM云端研判 + 本地Embedding推理"""
    
    def __init__(self):
        """初始化分析器，设置API密钥"""
        dashscope.api_key = DASHSCOPE_API_KEY
    
    def _call_model(self, prompt: str) -> Optional[str]:
        """
        调用阿里百炼大模型的内部方法
        
        Args:
            prompt: 输入的提示词
            
        Returns:
            模型返回的文本结果，失败返回None
        """
        try:
            response = Generation.call(
                model=MODEL_NAME,
                prompt=prompt,
                result_format='message'
            )
            
            if response.status_code == HTTPStatus.OK:
                return response.output.choices[0].message.content
            else:
                print(f"模型调用失败: {response.code} - {response.message}")
                return None
        except Exception as e:
            print(f"模型调用异常: {str(e)}")
            return None
    
    def analyze_message(self, text: str) -> Dict[str, Any]:
        """
        分析消息内容，返回灾种、信息类型、严重程度等信息
        
        Args:
            text: 待分析的文本内容
            
        Returns:
            包含分析结果的字典，包括：
            - disaster_type: 灾种类型
            - info_type: 信息类型
            - severity: 严重程度(1-3)
            - confidence: 置信度(0-100)
            - suggestion: 建议处置
            - keywords: 关键词列表
            - is_rumor: 是否为谣言
            - rumor_reason: 谣言判定原因
        """
        prompt = f"""请分析以下灾害相关信息，并以JSON格式返回结果。

待分析文本：
{text}

请从以下灾种类型中选择最匹配的一个：{', '.join(DISASTER_TYPES)}
请从以下信息类型中选择最匹配的一个：{', '.join(INFO_TYPES)}

置信度评估标准：
- 90-100分：信息完整、来源明确、内容详实、无模糊表述
- 70-89分：信息较完整、有明确地点和时间、内容基本可信
- 50-69分：信息部分完整、有一定可信度但存在模糊表述
- 30-49分：信息不完整、缺乏关键细节、可信度较低
- 0-29分：信息严重缺失、表述模糊、无法判断真实性

请返回以下JSON格式（不要包含markdown代码块标记）：
{{
    "disaster_type": "灾种类型",
    "info_type": "信息类型",
    "severity": 严重程度数字(0-3，0轻微、1一般、2严重、3特别严重),
    "confidence": 置信度(0-100的整数，严格按照上述标准评估),
    "suggestion": "建议处置措施",
    "keywords": ["关键词1", "关键词2"],
    "is_rumor": 是否为谣言(true/false),
    "rumor_reason": "如果是谣言，说明判定原因，否则为null"
}}

请直接返回JSON，不要有其他内容："""

        result = self._call_model(prompt)
        
        default_result = {
            "disaster_type": "其他",
            "info_type": "其他",
            "severity": 1,
            "confidence": 0,
            "suggestion": "无法分析，请人工核实",
            "keywords": [],
            "is_rumor": False,
            "rumor_reason": None
        }
        
        if result:
            try:
                clean_result = self._clean_json_response(result)
                parsed = json.loads(clean_result)
                for key in default_result:
                    if key not in parsed:
                        parsed[key] = default_result[key]
                return parsed
            except json.JSONDecodeError:
                print(f"JSON解析失败: {result}")
                return default_result
        
        return default_result
    
    @staticmethod
    def _clean_json_response(text: str) -> str:
        """清理模型返回的JSON文本，去除markdown代码块标记"""
        clean = text.strip()
        if clean.startswith('```'):
            lines = clean.split('\n')
            # Remove first line (```json or ```)
            lines = lines[1:]
            # Remove last line if it's ```
            if lines and lines[-1].strip() == '```':
                lines = lines[:-1]
            clean = '\n'.join(lines)
        return clean.strip()

    def detect_rumor(self, text: str) -> Dict[str, Any]:
        """
        检测文本是否为谣言
        
        Args:
            text: 待检测的文本内容
            
        Returns:
            包含谣言检测结果的字典：
            - is_rumor: 是否为谣言
            - confidence: 置信度
            - reason: 判定原因
            - suggestion: 处理建议
        """
        prompt = f"""请判断以下信息是否为谣言，并以JSON格式返回结果。

待检测文本：
{text}

请从以下角度分析：
1. 信息来源是否可靠
2. 内容是否夸大或失实
3. 是否存在常见谣言特征
4. 逻辑是否合理

请返回以下JSON格式（不要包含markdown代码块标记）：
{{
    "is_rumor": 是否为谣言(true/false),
    "confidence": 置信度(0-100的整数),
    "reason": "判定原因说明",
    "suggestion": "处理建议"
}}

请直接返回JSON，不要有其他内容："""

        result = self._call_model(prompt)
        
        default_result = {
            "is_rumor": False,
            "confidence": 0,
            "reason": "无法判断",
            "suggestion": "请人工核实"
        }
        
        if result:
            try:
                clean_result = self._clean_json_response(result)
                parsed = json.loads(clean_result)
                for key in default_result:
                    if key not in parsed:
                        parsed[key] = default_result[key]
                return parsed
            except json.JSONDecodeError:
                print(f"JSON解析失败: {result}")
                return default_result
        
        return default_result
    
    def get_embedding(self, text: str) -> Optional[List[float]]:
        """
        本地 bge-large-zh-v1.5 模型推理，获取 1024 维文本向量
        
        Args:
            text: 待向量化的文本（自动截断到 EMBEDDING_MAX_LENGTH tokens）
            
        Returns:
            L2 归一化后的 1024 维浮点数列表，失败返回 None
        """
        try:
            # bge-large 推荐对 query 添加 instruction prefix
            instruction = "为这个句子生成表示以用于检索相关段落："
            full_text = instruction + text

            embedding = _embedding_model.encode(
                full_text,
                normalize_embeddings=EMBEDDING_NORMALIZE,
                show_progress_bar=False,
            )
            return embedding.tolist()
        except Exception as e:
            print(f"本地Embedding推理异常: {str(e)}")
            return None
            return None
    
    def calculate_similarity(self, vec1: List[float], vec2: List[float]) -> float:
        """
        计算两个向量的余弦相似度
        
        Args:
            vec1: 第一个向量
            vec2: 第二个向量
            
        Returns:
            余弦相似度值（-1到1之间）
        """
        try:
            arr1 = np.array(vec1)
            arr2 = np.array(vec2)
            
            # 计算余弦相似度
            dot_product = np.dot(arr1, arr2)
            norm1 = np.linalg.norm(arr1)
            norm2 = np.linalg.norm(arr2)
            
            if norm1 == 0 or norm2 == 0:
                return 0.0
            
            similarity = dot_product / (norm1 * norm2)
            return float(similarity)
        except Exception as e:
            print(f"相似度计算异常: {str(e)}")
            return 0.0

    def get_batch_embeddings(self, texts: List[str]) -> Optional[List[List[float]]]:
        """
        批量文本向量化（GPU 并行推理，吞吐量远高于逐条调用）
        
        Args:
            texts: 文本列表
            
        Returns:
            向量列表，每个元素为 1024 维浮点数列表
        """
        try:
            instruction = "为这个句子生成表示以用于检索相关段落："
            full_texts = [instruction + t for t in texts]

            embeddings = _embedding_model.encode(
                full_texts,
                normalize_embeddings=EMBEDDING_NORMALIZE,
                batch_size=EMBEDDING_BATCH_SIZE,
                show_progress_bar=False,
            )
            return embeddings.tolist()
        except Exception as e:
            print(f"批量Embedding推理异常: {str(e)}")
            return None


# 创建全局分析器实例
analyzer = AIAnalyzer()


def analyze_message(text: str) -> Dict[str, Any]:
    """
    分析消息内容的便捷函数
    
    Args:
        text: 待分析的文本内容
        
    Returns:
        分析结果字典
    """
    return analyzer.analyze_message(text)


def detect_rumor(text: str) -> Dict[str, Any]:
    """
    检测谣言的便捷函数
    
    Args:
        text: 待检测的文本内容
        
    Returns:
        谣言检测结果字典
    """
    return analyzer.detect_rumor(text)


def get_embedding(text: str) -> Optional[List[float]]:
    """
    获取文本向量的便捷函数
    
    Args:
        text: 待向量化的文本内容
        
    Returns:
        文本向量
    """
    return analyzer.get_embedding(text)


def calculate_similarity(vec1: List[float], vec2: List[float]) -> float:
    """
    计算相似度的便捷函数
    
    Args:
        vec1: 第一个向量
        vec2: 第二个向量
        
    Returns:
        相似度值
    """
    return analyzer.calculate_similarity(vec1, vec2)


def get_batch_embeddings(texts: List[str]) -> Optional[List[List[float]]]:
    """
    批量文本向量化便捷函数（GPU 并行推理）
    
    Args:
        texts: 文本列表
        
    Returns:
        向量列表
    """
    return analyzer.get_batch_embeddings(texts)
