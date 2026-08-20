package com.hrd.service;

import java.util.Map;

/**
 * AI模型服务接口
 * 用于调用Python模型微服务，提供消息分析、谣言检测、文本向量计算等功能
 */
public interface ModelService {

    /**
     * 分析消息文本
     * 调用Python模型的文本分析接口，对清洗后的文本进行深度分析
     *
     * @param text 清洗后的文本内容
     * @return 分析结果Map，包含以下字段：
     *         - disaster_type: 灾害类型（洪涝/火灾/地震/滑坡/交通事故/燃气泄漏/电梯困人/道路塌陷）
     *         - info_type: 信息类型（求助/报警/现场目击/谣言/转载/无关）
     *         - severity: 严重程度（0-轻微,1-一般,2-严重,3-特别严重）
     *         - confidence: 置信度（0-100）
     *         - suggestion: AI建议处置方案
     */
    Map<String, Object> analyzeMessage(String text);

    /**
     * 检测谣言
     * 调用Python模型的谣言检测接口，判断文本是否为谣言
     *
     * @param text 文本内容
     * @return 谣言检测结果Map，包含以下字段：
     *         - is_rumor: 是否为谣言（boolean）
     *         - confidence: 检测置信度（0-100）
     *         - reason: 判断理由说明
     */
    Map<String, Object> detectRumor(String text);

    /**
     * 获取文本向量
     * 调用Python模型的文本向量化接口，将文本转换为向量表示
     * 用于后续的相似度计算和聚类分析
     *
     * @param text 文本内容
     * @return 向量数组（通常为768维或更高维度的float数组）
     */
    float[] getEmbedding(String text);

    /**
     * 计算文本相似度
     * 计算两段文本之间的语义相似度，用于消息聚类
     *
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度分数（0-1之间，1表示完全相同，0表示完全不同）
     */
    float calculateSimilarity(String text1, String text2);

    /**
     * 批量获取文本向量
     * 批量获取多个文本的向量表示，提高处理效率
     *
     * @param texts 文本列表
     * @return 向量数组列表，与输入文本一一对应
     */
    float[][] getBatchEmbeddings(String[] texts);
}
