# -*- coding: utf-8 -*-
"""
model_service 包初始化文件
"""

from .config import (
    DASHSCOPE_API_KEY,
    MODEL_NAME,
    SERVICE_PORT,
    SERVICE_HOST,
    DISASTER_TYPES,
    INFO_TYPES,
    SEVERITY_LEVELS
)

from .ai_analyzer import (
    AIAnalyzer,
    analyze_message,
    detect_rumor,
    get_embedding,
    calculate_similarity
)

from .app import app, main

__all__ = [
    'DASHSCOPE_API_KEY',
    'MODEL_NAME',
    'SERVICE_PORT',
    'SERVICE_HOST',
    'DISASTER_TYPES',
    'INFO_TYPES',
    'SEVERITY_LEVELS',
    'AIAnalyzer',
    'analyze_message',
    'detect_rumor',
    'get_embedding',
    'calculate_similarity',
    'app',
    'main'
]
