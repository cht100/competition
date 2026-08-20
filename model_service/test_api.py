# -*- coding: utf-8 -*-
"""API测试脚本"""

import requests
import json

BASE_URL = 'http://127.0.0.1:5001'

def test_analyze():
    """测试文本研判接口"""
    print("=" * 50)
    print("测试文本研判接口 /api/analyze")
    print("=" * 50)
    
    text = "我们村被洪水淹了，有人被困在一楼，急需救援！"
    response = requests.post(f'{BASE_URL}/api/analyze', json={'text': text})
    result = response.json()
    print(f"输入文本: {text}")
    print(f"返回结果: {json.dumps(result, ensure_ascii=False, indent=2)}")
    return result

def test_health():
    """测试健康检查接口"""
    print("=" * 50)
    print("测试健康检查接口 /health")
    print("=" * 50)
    
    response = requests.get(f'{BASE_URL}/health')
    result = response.json()
    print(f"返回结果: {json.dumps(result, ensure_ascii=False, indent=2)}")
    return result

if __name__ == '__main__':
    test_health()
    print()
    test_analyze()
