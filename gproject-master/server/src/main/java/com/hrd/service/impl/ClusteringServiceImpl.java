package com.hrd.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrd.controller.admin.SettingsController;
import com.hrd.entity.Incident;
import com.hrd.entity.Message;
import com.hrd.mapper.IncidentMapper;
import com.hrd.mapper.MessageMapper;
import com.hrd.service.ClusteringService;
import com.hrd.service.IncidentService;
import com.hrd.service.ModelService;
import com.hrd.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

/**
 * 消息聚类服务实现类
 * 实现消息与事件的智能聚类，基于文本相似度进行事件聚合
 */
@Service
@Slf4j
public class ClusteringServiceImpl implements ClusteringService {

    @Autowired
    private ModelService modelService;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private IncidentMapper incidentMapper;

    @Autowired
    private SettingsController settingsController;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 聚类相似度阈值，超过此阈值则合并到现有事件
     */
    @Value("${clustering.similarity.threshold:0.75}")
    private float similarityThresholdDefault;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 事件向量缓存，用于加速相似度计算
     * Key: 事件ID, Value: 事件代表性向量
     */
    private final Map<Long, float[]> incidentVectorCache = new ConcurrentHashMap<>();

    /**
     * 处理新消息，判断是否需要聚类到现有事件
     * 核心聚类逻辑：
     * 1. 检查消息置信度是否达到阈值
     * 2. 获取消息的文本向量
     * 3. 计算与所有待审核事件的相似度
     * 4. 如果最高相似度超过阈值，合并到该事件
     * 5. 否则创建新事件
     *
     * @param message 新消息对象
     * @return 关联的事件ID
     */
    @Override
    @Transactional
    public Long processMessage(Message message) {
        log.info("【聚类服务】开始处理消息，消息ID：{}", message.getId());

        if (message.getIncidentId() != null) {
            log.info("【聚类服务】消息已关联事件，事件ID：{}", message.getIncidentId());
            return message.getIncidentId();
        }

        String text = message.getCleanedText();
        if (text == null || text.isEmpty()) {
            log.warn("【聚类服务】消息清洗文本为空，无法聚类");
            return null;
        }

        int messageConfidence = getMessageConfidence(message);
        int confidenceThreshold = settingsController.getConfidenceThreshold();
        log.info("【聚类服务】消息置信度：{}，阈值：{}", messageConfidence, confidenceThreshold);

        // 置信度 <= 30% 的消息为谣言，不参与聚类
        if (messageConfidence <= 30) {
            log.info("【聚类服务】消息置信度<=30%，判定为谣言，不聚类");
            return null;
        }

        float[] messageVector = modelService.getEmbedding(text);
        if (messageVector.length == 0) {
            log.warn("【聚类服务】获取消息向量失败，无法聚类");
            return null;
        }

        List<Incident> activeIncidents = getActiveIncidents();

        if (activeIncidents.isEmpty()) {
            log.info("【聚类服务】无活跃事件，创建新事件");
            return createNewIncident(message);
        }

        Long bestMatchIncidentId = null;
        float bestSimilarity = 0.0f;

        for (Incident incident : activeIncidents) {
            float similarity = calculateIncidentVectorSimilarity(messageVector, incident.getId());
            log.debug("【聚类服务】消息与事件{}的相似度：{}", incident.getId(), similarity);

            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatchIncidentId = incident.getId();
            }
        }

        float similarityThreshold = settingsController.getSimilarityThreshold() / 100.0f;

        if (bestSimilarity >= similarityThreshold && bestMatchIncidentId != null) {
            log.info("【聚类服务】消息聚类到现有事件，事件ID：{}，相似度：{}",
                    bestMatchIncidentId, bestSimilarity);

            // 推送聚合过程可视化数据
            pushAggregationVisualData(message, bestMatchIncidentId, bestSimilarity, similarityThreshold, "MERGE_TO_EVENT");

            message.setIncidentId(bestMatchIncidentId);
            message.setStatus(Message.STATUS_AGGREGATED);
            messageMapper.updateById(message);

            // 更新事件置信度（累积证据链提升置信度）
            updateIncidentConfidence(bestMatchIncidentId, messageConfidence);
            updateIncidentSummary(bestMatchIncidentId);

            incidentVectorCache.remove(bestMatchIncidentId);

            // 推送事件更新WebSocket
            Incident updatedIncident = incidentMapper.selectById(bestMatchIncidentId);
            if (updatedIncident != null) {
                webSocketServer.sendIncidentUpdate(updatedIncident, updatedIncident.getStatus(), updatedIncident.getStatus());
            }

            return bestMatchIncidentId;
        } else if (messageConfidence >= confidenceThreshold) {
            // 置信度 >= 阈值：可以直接创建新事件
            log.info("【聚类服务】高置信度消息，创建新事件，最高相似度：{}", bestSimilarity);
            
            // 推送新事件创建可视化数据
            pushAggregationVisualData(message, null, bestSimilarity, similarityThreshold, "NEW_EVENT");
            
            Long newIncidentId = createNewIncident(message);
            // 推送新事件WebSocket通知
            Incident newIncident = incidentMapper.selectById(newIncidentId);
            if (newIncident != null) {
                webSocketServer.sendNewIncident(newIncident);
            }
            return newIncidentId;
        } else {
            // 置信度 20%-阈值 且无相似事件：暂不创建事件，等待更多证据
            log.info("【聚类服务】中等置信度消息且无相似事件，暂不创建事件，最高相似度：{}", bestSimilarity);
            return null;
        }
    }

    /**
     * 从消息的AI分析结果中获取置信度
     */
    private int getMessageConfidence(Message message) {
        if (message.getAiAnalysis() != null && !message.getAiAnalysis().isEmpty()) {
            try {
                Map<String, Object> analysis = JSON.parseObject(message.getAiAnalysis(), Map.class);
                Object confidenceObj = analysis.get("confidence");
                if (confidenceObj instanceof Integer) {
                    return (Integer) confidenceObj;
                } else if (confidenceObj instanceof Number) {
                    return ((Number) confidenceObj).intValue();
                }
            } catch (Exception e) {
                log.warn("【聚类服务】解析AI分析结果失败", e);
            }
        }
        return 0;
    }

    /**
     * 计算两条消息的相似度
     * 基于文本向量计算余弦相似度
     *
     * @param m1 消息1
     * @param m2 消息2
     * @return 相似度分数（0-1之间）
     */
    @Override
    public float calculateSimilarity(Message m1, Message m2) {
        // 如果任一消息没有清洗文本，返回0
        if (m1.getCleanedText() == null || m2.getCleanedText() == null) {
            return 0.0f;
        }

        // 调用模型服务计算相似度
        return modelService.calculateSimilarity(m1.getCleanedText(), m2.getCleanedText());
    }

    /**
     * 计算消息与事件的相似度
     * 综合考虑事件中所有证据消息的相似度，取最大值
     *
     * @param message 消息对象
     * @param incidentId 事件ID
     * @return 相似度分数（0-1之间）
     */
    @Override
    public float calculateIncidentSimilarity(Message message, Long incidentId) {
        // 获取消息向量
        float[] messageVector = modelService.getEmbedding(message.getCleanedText());
        if (messageVector.length == 0) {
            return 0.0f;
        }

        return calculateIncidentVectorSimilarity(messageVector, incidentId);
    }

    /**
     * 获取事件的所有证据消息
     *
     * @param incidentId 事件ID
     * @return 证据消息列表
     */
    @Override
    public List<Message> getIncidentEvidences(Long incidentId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getIncidentId, incidentId)
                .orderByAsc(Message::getPublishTime);
        return messageMapper.selectList(wrapper);
    }

    /**
     * 重新聚类所有未聚合的消息
     * 批量处理历史消息，用于系统初始化或数据修复
     *
     * @return 成功聚类的消息数量
     */
    @Override
    @Transactional
    public int reclusterAllMessages() {
        log.info("【聚类服务】开始重新聚类所有未聚合消息");

        // 查询所有未聚合的消息（状态为已研判但未聚合）
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getStatus, Message.STATUS_JUDGED)
                .isNull(Message::getIncidentId);

        List<Message> unclusteredMessages = messageMapper.selectList(wrapper);
        log.info("【聚类服务】找到{}条未聚合消息", unclusteredMessages.size());

        int successCount = 0;
        for (Message message : unclusteredMessages) {
            try {
                Long incidentId = processMessage(message);
                if (incidentId != null) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("【聚类服务】聚类消息失败，消息ID：{}", message.getId(), e);
            }
        }

        log.info("【聚类服务】重新聚类完成，成功{}条", successCount);
        return successCount;
    }

    /**
     * 更新事件的摘要信息
     * 当事件有新消息加入时，更新事件的描述和统计信息
     *
     * @param incidentId 事件ID
     */
    @Override
    @Transactional
    public void updateIncidentSummary(Long incidentId) {
        log.info("【聚类服务】更新事件摘要，事件ID：{}", incidentId);

        // 获取事件
        Incident incident = incidentMapper.selectById(incidentId);
        if (incident == null) {
            log.warn("【聚类服务】事件不存在，ID：{}", incidentId);
            return;
        }

        // 获取事件的所有证据消息
        List<Message> evidences = getIncidentEvidences(incidentId);
        if (evidences.isEmpty()) {
            return;
        }

        // 更新事件的位置信息（取最新消息的位置）
        Message latestMessage = evidences.get(evidences.size() - 1);
        if (latestMessage.getLocationText() != null) {
            incident.setLocationText(latestMessage.getLocationText());
        }
        if (latestMessage.getLat() != null && latestMessage.getLng() != null) {
            incident.setLat(latestMessage.getLat());
            incident.setLng(latestMessage.getLng());
        }

        // 更新事件的更新时间
        incident.setUpdateTime(LocalDateTime.now());

        // 生成新的摘要（取第一条消息的清洗文本作为基础摘要）
        if (incident.getSummary() == null || incident.getSummary().isEmpty()) {
            String summary = evidences.get(0).getCleanedText();
            if (summary != null && summary.length() > 200) {
                summary = summary.substring(0, 200) + "...";
            }
            incident.setSummary(summary);
        }

        incidentMapper.updateById(incident);
        log.info("【聚类服务】事件摘要更新完成，事件ID：{}", incidentId);
    }

    /**
     * 更新事件置信度（证据链累积）
     * 每增加一条证据消息，事件置信度按加权平均提升
     */
    private void updateIncidentConfidence(Long incidentId, int newMessageConfidence) {
        Incident incident = incidentMapper.selectById(incidentId);
        if (incident == null) return;

        List<Message> evidences = getIncidentEvidences(incidentId);
        int evidenceCount = evidences.size();

        // 加权平均：新消息权重 30%，已有平均权重 70%
        int currentConfidence = incident.getConfidence() != null ? incident.getConfidence() : 0;
        int newConfidence;
        if (evidenceCount <= 1) {
            newConfidence = Math.max(currentConfidence, newMessageConfidence);
        } else {
            // 每增加一条证据，置信度提升：原值 * 0.7 + 新消息 * 0.3，且至少提升3点
            newConfidence = (int) Math.min(100, Math.max(currentConfidence + 3,
                    currentConfidence * 0.7 + newMessageConfidence * 0.3));
        }

        if (newConfidence != currentConfidence) {
            incident.setConfidence(newConfidence);
            incident.setUpdateTime(LocalDateTime.now());
            incidentMapper.updateById(incident);
            log.info("【聚类服务】事件置信度更新：{} -> {}，事件ID：{}，证据数：{}",
                    currentConfidence, newConfidence, incidentId, evidenceCount);
        }
    }

    /**
     * 获取活跃事件列表
     * 包括待审核和已确认状态的事件
     *
     * @return 活跃事件列表
     */
    private List<Incident> getActiveIncidents() {
        return incidentService.listActive();
    }

    /**
     * 创建新事件
     * 根据消息内容和AI分析结果创建新事件
     *
     * @param message 消息对象
     * @return 新创建的事件ID
     */
    @Transactional
    private Long createNewIncident(Message message) {
        log.info("【聚类服务】创建新事件，消息ID：{}", message.getId());

        // 解析AI分析结果
        String disasterType = "未知";
        String infoType = "未知";
        Integer severity = 0;
        Integer confidence = 0;
        String suggestion = "";

        if (message.getAiAnalysis() != null && !message.getAiAnalysis().isEmpty()) {
            try {
                Map<String, Object> analysis = JSON.parseObject(message.getAiAnalysis(), Map.class);
                disasterType = (String) analysis.getOrDefault("disaster_type", "未知");
                infoType = (String) analysis.getOrDefault("info_type", "未知");
                severity = (Integer) analysis.getOrDefault("severity", 0);
                confidence = (Integer) analysis.getOrDefault("confidence", 0);
                suggestion = (String) analysis.getOrDefault("suggestion", "");
            } catch (Exception e) {
                log.warn("【聚类服务】解析AI分析结果失败", e);
            }
        }

        // 创建事件
        Incident incident = Incident.builder()
                .summary(message.getCleanedText() != null && message.getCleanedText().length() > 200
                        ? message.getCleanedText().substring(0, 200) + "..."
                        : message.getCleanedText())
                .disasterType(disasterType)
                .infoType(infoType)
                .severity(severity)
                .confidence(confidence)
                .locationText(message.getLocationText())
                .lat(message.getLat())
                .lng(message.getLng())
                .status(Incident.STATUS_PENDING)
                .aiSuggestion(suggestion)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        incidentMapper.insert(incident);
        Long incidentId = incident.getId();

        // 更新消息的事件ID和状态
        message.setIncidentId(incidentId);
        message.setStatus(Message.STATUS_AGGREGATED);
        messageMapper.updateById(message);

        log.info("【聚类服务】新事件创建成功，事件ID：{}", incidentId);
        return incidentId;
    }

    /**
     * 计算消息向量与事件的相似度
     * 使用事件的代表性向量进行计算
     *
     * @param messageVector 消息向量
     * @param incidentId 事件ID
     * @return 相似度分数
     */
    private float calculateIncidentVectorSimilarity(float[] messageVector, Long incidentId) {
        // 尝试从缓存获取事件向量
        float[] incidentVector = incidentVectorCache.get(incidentId);

        if (incidentVector == null) {
            // 获取事件的所有证据消息
            List<Message> evidences = getIncidentEvidences(incidentId);
            if (evidences.isEmpty()) {
                return 0.0f;
            }

            // 计算事件的代表性向量（取所有消息向量的平均值）
            incidentVector = calculateAverageVector(evidences);

            // 缓存事件向量
            if (incidentVector.length > 0) {
                incidentVectorCache.put(incidentId, incidentVector);
            }
        }

        if (incidentVector.length == 0) {
            return 0.0f;
        }

        // 计算余弦相似度
        return cosineSimilarity(messageVector, incidentVector);
    }

    /**
     * 计算消息列表的平均向量
     * 用于生成事件的代表性向量
     *
     * @param messages 消息列表
     * @return 平均向量
     */
    private float[] calculateAverageVector(List<Message> messages) {
        if (messages.isEmpty()) {
            return new float[0];
        }

        // 获取第一条消息的向量作为基准
        float[] firstVector = modelService.getEmbedding(messages.get(0).getCleanedText());
        if (firstVector.length == 0) {
            return new float[0];
        }

        // 累加所有向量
        float[] sumVector = Arrays.copyOf(firstVector, firstVector.length);
        int validCount = 1;

        for (int i = 1; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (msg.getCleanedText() != null) {
                float[] vector = modelService.getEmbedding(msg.getCleanedText());
                if (vector.length == sumVector.length) {
                    for (int j = 0; j < sumVector.length; j++) {
                        sumVector[j] += vector[j];
                    }
                    validCount++;
                }
            }
        }

        // 计算平均值
        for (int i = 0; i < sumVector.length; i++) {
            sumVector[i] /= validCount;
        }

        return sumVector;
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * @param v1 向量1
     * @param v2 向量2
     * @return 余弦相似度（0-1之间）
     */
    private float cosineSimilarity(float[] v1, float[] v2) {
        if (v1.length != v2.length || v1.length == 0) {
            return 0.0f;
        }

        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0f;
        }

        return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }

    /**
     * 推送聚合过程数据
     * 用于前端展示 AI 聚合过程的实时动画
     *
     * @param message 新消息
     * @param targetIncidentId 目标事件ID
     * @param similarityScore 相似度分数
     * @param similarityThreshold 相似度阈值
     * @param operationType 操作类型
     */
    private void pushAggregationVisualData(Message message, Long targetIncidentId, 
                                          float similarityScore, float similarityThreshold, 
                                          String operationType) {
        try {
            // 构建简化的聚合过程数据
            Map<String, Object> aggregationData = new HashMap<>();
            aggregationData.put("operationType", operationType);
            aggregationData.put("newMessageId", message.getId());
            aggregationData.put("newMessageContent", message.getCleanedText());
            aggregationData.put("targetIncidentId", targetIncidentId);
            aggregationData.put("similarityScore", similarityScore);
            aggregationData.put("similarityThreshold", similarityThreshold);
            aggregationData.put("exceedThreshold", similarityScore >= similarityThreshold);
            aggregationData.put("timestamp", LocalDateTime.now().format(formatter));

            // 通过 WebSocket 推送聚合过程数据
            webSocketServer.sendAggregationVisualData(aggregationData);
            
            log.info("【聚合可视化】推送聚合过程数据，操作类型：{}，消息ID：{}，相似度：{}", 
                    operationType, message.getId(), similarityScore);
                    
        } catch (Exception e) {
            log.error("【聚合可视化】推送聚合过程数据失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 生成事件颜色
     */
    private String generateColor(Long incidentId) {
        String[] colors = {"#ff6b6b", "#4ecdc4", "#45b7d1", "#96ceb4", "#feca57", "#ff9ff3", "#54a0ff", "#5f27cd"};
        int index = Math.abs(incidentId.hashCode()) % colors.length;
        return colors[index];
    }

    /**
     * 生成消息颜色
     */
    private String generateMessageColor(String infoType) {
        if (infoType == null) return "#95afc0";
        switch (infoType) {
            case "求助": return "#ff6b6b";
            case "报警": return "#ff9ff3";
            case "现场目击": return "#54a0ff";
            case "转载": return "#5f27cd";
            default: return "#95afc0";
        }
    }

    /**
     * 计算事件半径
     */
    private int calculateRadius(int messageCount) {
        return Math.min(50 + (messageCount * 5), 150);
    }

    /**
     * 计算消息大小
     */
    private int calculateMessageSize(Float confidence) {
        if (confidence == null) return 8;
        return Math.max(6, Math.min(15, (int)(confidence * 15)));
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已确认";
            case 2 -> "已派单";
            case 3 -> "处理中";
            case 4 -> "已完成";
            case 5 -> "已驳回";
            default -> "未知";
        };
    }

    /**
     * 从 AI 分析结果中提取信息类型
     *
     * @param aiAnalysis AI 分析结果（JSON格式）
     * @return 信息类型
     */
    private String extractInfoTypeFromAnalysis(String aiAnalysis) {
        if (aiAnalysis == null || aiAnalysis.isEmpty()) {
            return "未知";
        }
        try {
            Map<String, Object> analysis = JSON.parseObject(aiAnalysis, Map.class);
            Object infoTypeObj = analysis.get("infoType");
            return infoTypeObj != null ? infoTypeObj.toString() : "未知";
        } catch (Exception e) {
            return "未知";
        }
    }

    /**
     * 从 AI 分析结果中提取置信度
     *
     * @param aiAnalysis AI 分析结果（JSON格式）
     * @return 置信度
     */
    private Float extractConfidenceFromAnalysis(String aiAnalysis) {
        if (aiAnalysis == null || aiAnalysis.isEmpty()) {
            return 0.0f;
        }
        try {
            Map<String, Object> analysis = JSON.parseObject(aiAnalysis, Map.class);
            Object confidenceObj = analysis.get("confidence");
            if (confidenceObj instanceof Number) {
                return ((Number) confidenceObj).floatValue();
            }
            return 0.0f;
        } catch (Exception e) {
            return 0.0f;
        }
    }

    @Override
    public void clearVectorCache() {
        incidentVectorCache.clear();
        log.info("【聚类服务】事件向量缓存已清除");
    }
}
