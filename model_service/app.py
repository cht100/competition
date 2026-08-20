# -*- coding: utf-8 -*-
"""
Flask API服务入口
提供文本研判、谣言检测、向量化和相似度计算接口
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
from typing import List

from config import SERVICE_PORT, SERVICE_HOST
from ai_analyzer import (
    analyze_message,
    detect_rumor,
    get_embedding,
    get_batch_embeddings,
    calculate_similarity
)

# 创建Flask应用
app = Flask(__name__)

# 启用跨域支持
CORS(app)


@app.route('/api/analyze', methods=['POST'])
def api_analyze():
    """
    文本研判接口
    
    请求体JSON格式：
    {
        "text": "待分析的文本内容"
    }
    
    返回JSON格式：
    {
        "disaster_type": "灾种类型",
        "info_type": "信息类型",
        "severity": 严重程度(1-3),
        "confidence": 置信度(0-100),
        "suggestion": "建议处置",
        "keywords": ["关键词列表"],
        "is_rumor": 是否为谣言,
        "rumor_reason": "谣言判定原因"
    }
    """
    try:
        data = request.get_json()
        
        if not data or 'text' not in data:
            return jsonify({
                'error': '请求参数错误',
                'message': '请提供text参数'
            }), 400
        
        text = data['text']
        
        if not text or not isinstance(text, str):
            return jsonify({
                'error': '请求参数错误',
                'message': 'text参数不能为空且必须为字符串'
            }), 400
        
        result = analyze_message(text)
        return jsonify(result)
    
    except Exception as e:
        return jsonify({
            'error': '服务器内部错误',
            'message': str(e)
        }), 500


@app.route('/api/rumor', methods=['POST'])
def api_rumor():
    """
    谣言检测接口
    
    请求体JSON格式：
    {
        "text": "待检测的文本内容"
    }
    
    返回JSON格式：
    {
        "is_rumor": 是否为谣言,
        "confidence": 置信度,
        "reason": "判定原因",
        "suggestion": "处理建议"
    }
    """
    try:
        data = request.get_json()
        
        if not data or 'text' not in data:
            return jsonify({
                'error': '请求参数错误',
                'message': '请提供text参数'
            }), 400
        
        text = data['text']
        
        if not text or not isinstance(text, str):
            return jsonify({
                'error': '请求参数错误',
                'message': 'text参数不能为空且必须为字符串'
            }), 400
        
        result = detect_rumor(text)
        return jsonify(result)
    
    except Exception as e:
        return jsonify({
            'error': '服务器内部错误',
            'message': str(e)
        }), 500


@app.route('/api/embedding', methods=['POST'])
def api_embedding():
    """
    文本向量接口
    
    请求体JSON格式：
    {
        "text": "待向量化的文本内容"
    }
    
    返回JSON格式：
    {
        "embedding": [向量数组],
        "dimension": 向量维度
    }
    """
    try:
        data = request.get_json()
        
        if not data or 'text' not in data:
            return jsonify({
                'error': '请求参数错误',
                'message': '请提供text参数'
            }), 400
        
        text = data['text']
        
        if not text or not isinstance(text, str):
            return jsonify({
                'error': '请求参数错误',
                'message': 'text参数不能为空且必须为字符串'
            }), 400
        
        embedding = get_embedding(text)
        
        if embedding is None:
            return jsonify({
                'error': '向量化失败',
                'message': '无法获取文本向量'
            }), 500
        
        return jsonify({
            'embedding': embedding,
            'dimension': len(embedding)
        })
    
    except Exception as e:
        return jsonify({
            'error': '服务器内部错误',
            'message': str(e)
        }), 500


@app.route('/api/similarity', methods=['POST'])
def api_similarity():
    """
    相似度计算接口
    
    请求体JSON格式：
    {
        "vec1": [向量1],
        "vec2": [向量2]
    }
    
    或直接计算两段文本的相似度：
    {
        "text1": "文本1",
        "text2": "文本2"
    }
    
    返回JSON格式：
    {
        "similarity": 相似度值(-1到1)
    }
    """
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({
                'error': '请求参数错误',
                'message': '请求体不能为空'
            }), 400
        
        # 方式1：直接传入向量
        if 'vec1' in data and 'vec2' in data:
            vec1 = data['vec1']
            vec2 = data['vec2']
            
            if not isinstance(vec1, list) or not isinstance(vec2, list):
                return jsonify({
                    'error': '请求参数错误',
                    'message': 'vec1和vec2必须为数组'
                }), 400
            
            similarity = calculate_similarity(vec1, vec2)
            return jsonify({'similarity': similarity})
        
        # 方式2：传入文本，自动向量化后计算
        elif 'text1' in data and 'text2' in data:
            text1 = data['text1']
            text2 = data['text2']
            
            if not isinstance(text1, str) or not isinstance(text2, str):
                return jsonify({
                    'error': '请求参数错误',
                    'message': 'text1和text2必须为字符串'
                }), 400
            
            embedding1 = get_embedding(text1)
            embedding2 = get_embedding(text2)
            
            if embedding1 is None or embedding2 is None:
                return jsonify({
                    'error': '向量化失败',
                    'message': '无法获取文本向量'
                }), 500
            
            similarity = calculate_similarity(embedding1, embedding2)
            return jsonify({'similarity': similarity})
        
        else:
            return jsonify({
                'error': '请求参数错误',
                'message': '请提供vec1和vec2参数，或text1和text2参数'
            }), 400
    
    except Exception as e:
        return jsonify({
            'error': '服务器内部错误',
            'message': str(e)
        }), 500


@app.route('/health', methods=['GET'])
def health_check():
    """
    健康检查接口
    
    返回服务状态
    """
    return jsonify({
        'status': 'healthy',
        'service': 'model-service',
        'version': '1.0.0'
    })


@app.route('/api/embedding/batch', methods=['POST'])
def api_batch_embedding():
    """批量文本向量接口（GPU 并行推理）"""
    try:
        data = request.get_json()
        if not data or 'texts' not in data:
            return jsonify({'error': '请求参数错误', 'message': '请提供texts参数'}), 400
        
        texts = data['texts']
        if not isinstance(texts, list):
            return jsonify({'error': '请求参数错误', 'message': 'texts必须为数组'}), 400
        
        embeddings = get_batch_embeddings(texts)
        if embeddings is None:
            return jsonify({'error': '向量化失败', 'message': '批量推理失败'}), 500
        
        return jsonify({
            'embeddings': embeddings,
            'count': len(embeddings)
        })
    except Exception as e:
        return jsonify({'error': '服务器内部错误', 'message': str(e)}), 500


def main():
    """启动Flask服务"""
    print(f"模型服务启动中...")
    print(f"服务地址: http://{SERVICE_HOST}:{SERVICE_PORT}")
    print(f"API文档:")
    print(f"  - POST /api/analyze    文本研判接口")
    print(f"  - POST /api/rumor      谣言检测接口")
    print(f"  - POST /api/embedding  文本向量接口")
    print(f"  - POST /api/similarity 相似度计算接口")
    print(f"  - GET  /health         健康检查接口")
    
    app.run(
        host=SERVICE_HOST,
        port=SERVICE_PORT,
        debug=False
    )


if __name__ == '__main__':
    main()
