package com.hrd.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hrd.service.ModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * AI模型服务实现类
 * 通过HTTP调用Python Flask服务，实现消息分析、谣言检测、向量计算等功能
 */
@Service
@Slf4j
public class ModelServiceImpl implements ModelService {

    /**
     * Python模型服务地址，默认为本地5001端口
     */
    @Value("${model.service.url:http://127.0.0.1:5001}")
    private String modelServiceUrl;

    /**
     * HTTP请求超时时间（毫秒）
     */
    private static final int REQUEST_TIMEOUT = 30000;

    /**
     * RestTemplate实例，用于发送HTTP请求
     */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 分析消息文本
     * 调用Python模型的/api/analyze接口，对文本进行深度分析
     *
     * @param text 清洗后的文本内容
     * @return 分析结果Map，包含disaster_type, info_type, severity, confidence, suggestion等字段
     */
    @Override
    public Map<String, Object> analyzeMessage(String text) {
        log.info("【AI模型服务】开始分析消息文本，长度：{}", text.length());

        // 构建请求URL
        String url = modelServiceUrl + "/api/analyze";

        // 构建请求体
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", text);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 发送POST请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());

                // 构建返回结果
                Map<String, Object> result = new HashMap<>();
                result.put("disaster_type", jsonResponse.getString("disaster_type"));
                result.put("info_type", jsonResponse.getString("info_type"));
                result.put("severity", jsonResponse.getInteger("severity"));
                result.put("confidence", jsonResponse.getInteger("confidence"));
                result.put("suggestion", jsonResponse.getString("suggestion"));

                log.info("【AI模型服务】消息分析完成，灾害类型：{}，严重程度：{}，置信度：{}",
                        result.get("disaster_type"), result.get("severity"), result.get("confidence"));

                return result;
            } else {
                log.error("【AI模型服务】分析请求失败，状态码：{}", response.getStatusCode());
                return getDefaultAnalysisResult();
            }
        } catch (RestClientException e) {
            log.error("【AI模型服务】调用分析接口异常：{}", e.getMessage(), e);
            return getDefaultAnalysisResult();
        }
    }

    /**
     * 检测谣言
     * 调用Python模型的/api/rumor接口，判断文本是否为谣言
     *
     * @param text 文本内容
     * @return 谣言检测结果Map，包含is_rumor, confidence, reason等字段
     */
    @Override
    public Map<String, Object> detectRumor(String text) {
        log.info("【AI模型服务】开始谣言检测，文本长度：{}", text.length());

        // 构建请求URL
        String url = modelServiceUrl + "/api/rumor";

        // 构建请求体
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", text);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 发送POST请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());

                // 构建返回结果
                Map<String, Object> result = new HashMap<>();
                result.put("is_rumor", jsonResponse.getBoolean("is_rumor"));
                result.put("confidence", jsonResponse.getInteger("confidence"));
                result.put("reason", jsonResponse.getString("reason"));

                log.info("【AI模型服务】谣言检测完成，是否谣言：{}，置信度：{}",
                        result.get("is_rumor"), result.get("confidence"));

                return result;
            } else {
                log.error("【AI模型服务】谣言检测请求失败，状态码：{}", response.getStatusCode());
                return getDefaultRumorResult();
            }
        } catch (RestClientException e) {
            log.error("【AI模型服务】调用谣言检测接口异常：{}", e.getMessage(), e);
            return getDefaultRumorResult();
        }
    }

    /**
     * 获取文本向量
     * 调用Python模型的/api/embedding接口，将文本转换为向量表示
     *
     * @param text 文本内容
     * @return 向量数组
     */
    @Override
    public float[] getEmbedding(String text) {
        log.debug("【AI模型服务】开始获取文本向量，文本长度：{}", text.length());

        // 构建请求URL
        String url = modelServiceUrl + "/api/embedding";

        // 构建请求体
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", text);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 发送POST请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                JSONArray embeddingArray = jsonResponse.getJSONArray("embedding");

                // 将JSONArray转换为float数组
                float[] embedding = new float[embeddingArray.size()];
                for (int i = 0; i < embeddingArray.size(); i++) {
                    embedding[i] = embeddingArray.getFloatValue(i);
                }

                log.debug("【AI模型服务】获取文本向量完成，维度：{}", embedding.length);
                return embedding;
            } else {
                log.error("【AI模型服务】获取向量请求失败，状态码：{}", response.getStatusCode());
                return new float[0];
            }
        } catch (RestClientException e) {
            log.error("【AI模型服务】调用向量接口异常：{}", e.getMessage(), e);
            return new float[0];
        }
    }

    /**
     * 计算文本相似度
     * 调用Python模型的/api/similarity接口，计算两段文本的语义相似度
     *
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度分数（0-1之间）
     */
    @Override
    public float calculateSimilarity(String text1, String text2) {
        log.debug("【AI模型服务】开始计算文本相似度");

        // 构建请求URL
        String url = modelServiceUrl + "/api/similarity";

        // 构建请求体
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text1", text1);
        requestBody.put("text2", text2);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 发送POST请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                float similarity = jsonResponse.getFloatValue("similarity");

                log.debug("【AI模型服务】相似度计算完成，结果：{}", similarity);
                return similarity;
            } else {
                log.error("【AI模型服务】相似度计算请求失败，状态码：{}", response.getStatusCode());
                return 0.0f;
            }
        } catch (RestClientException e) {
            log.error("【AI模型服务】调用相似度接口异常：{}", e.getMessage(), e);
            return 0.0f;
        }
    }

    /**
     * 批量获取文本向量
     * 调用Python模型的/api/embedding/batch接口，批量获取多个文本的向量表示
     *
     * @param texts 文本数组
     * @return 向量数组列表
     */
    @Override
    public float[][] getBatchEmbeddings(String[] texts) {
        log.info("【AI模型服务】开始批量获取文本向量，数量：{}", texts.length);

        // 构建请求URL
        String url = modelServiceUrl + "/api/embedding/batch";

        // 构建请求体
        Map<String, String[]> requestBody = new HashMap<>();
        requestBody.put("texts", texts);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String[]>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 发送POST请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                JSONArray embeddingsArray = jsonResponse.getJSONArray("embeddings");

                // 将JSONArray转换为二维float数组
                float[][] embeddings = new float[embeddingsArray.size()][];
                for (int i = 0; i < embeddingsArray.size(); i++) {
                    JSONArray singleEmbedding = embeddingsArray.getJSONArray(i);
                    embeddings[i] = new float[singleEmbedding.size()];
                    for (int j = 0; j < singleEmbedding.size(); j++) {
                        embeddings[i][j] = singleEmbedding.getFloatValue(j);
                    }
                }

                log.info("【AI模型服务】批量获取向量完成，数量：{}", embeddings.length);
                return embeddings;
            } else {
                log.error("【AI模型服务】批量获取向量请求失败，状态码：{}", response.getStatusCode());
                return new float[0][0];
            }
        } catch (RestClientException e) {
            log.error("【AI模型服务】调用批量向量接口异常：{}", e.getMessage(), e);
            return new float[0][0];
        }
    }

    /**
     * 获取默认的分析结果
     * 当模型服务不可用时，返回默认值
     *
     * @return 默认分析结果
     */
    private Map<String, Object> getDefaultAnalysisResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("disaster_type", "未知");
        result.put("info_type", "未知");
        result.put("severity", 0);
        result.put("confidence", 0);
        result.put("suggestion", "模型服务暂时不可用，请稍后重试");
        return result;
    }

    /**
     * 获取默认的谣言检测结果
     * 当模型服务不可用时，返回默认值
     *
     * @return 默认谣言检测结果
     */
    private Map<String, Object> getDefaultRumorResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("is_rumor", false);
        result.put("confidence", 0);
        result.put("reason", "模型服务暂时不可用，无法检测");
        return result;
    }
}
