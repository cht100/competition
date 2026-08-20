# -*- coding: utf-8 -*-
"""
配置文件模块
加载环境变量并提供配置信息
"""

import os
from dotenv import load_dotenv

# 获取当前目录
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# 加载.env文件（先尝试上级目录，再尝试当前目录）
env_path = os.path.join(os.path.dirname(BASE_DIR), '.env')
if os.path.exists(env_path):
    load_dotenv(env_path)
else:
    load_dotenv()

# 阿里百炼API配置
DASHSCOPE_API_KEY = os.getenv('DASHSCOPE_API_KEY')

# LLM 模型配置（云端）
MODEL_NAME = 'qwen-plus'

# 本地 Embedding 模型配置（BAAI/bge-large-zh-v1.5）
EMBEDDING_MODEL_NAME = 'BAAI/bge-large-zh-v1.5'
EMBEDDING_MODEL_PATH = os.path.join(BASE_DIR, 'models', 'bge-large-zh-v1.5')  # 本地模型路径
EMBEDDING_DEVICE = os.getenv('EMBEDDING_DEVICE', 'cuda')  # 'cuda' | 'cpu'
EMBEDDING_MAX_LENGTH = 512  # 最大输入 token 数
EMBEDDING_BATCH_SIZE = 32   # 批量推理批大小
EMBEDDING_NORMALIZE = True  # L2 归一化

# LoRA 应急领域微调适配器
LORA_ADAPTER_PATH = os.path.join(BASE_DIR, 'models', 'bge-lora-emergency', 'lora_adapter')  # LoRA 权重路径
LORA_ENABLED = True  # 是否启用 LoRA 适配器（热插拔开关）

# 服务配置
SERVICE_PORT = 5001
SERVICE_HOST = '0.0.0.0'

# 灾种类型定义
DISASTER_TYPES = [
    '洪涝', '地震', '台风', '火灾', '泥石流', 
    '山体滑坡', '干旱', '暴雪', '雷电', '其他'
]

# 信息类型定义
INFO_TYPES = [
    '求助', '报平安', '灾情通报', '物资需求', 
    '人员失踪', '道路阻断', '其他'
]

# 严重程度定义 (1-3级)
SEVERITY_LEVELS = {
    1: '轻微',
    2: '中等',
    3: '严重'
}
