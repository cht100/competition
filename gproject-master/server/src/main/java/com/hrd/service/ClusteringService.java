package com.hrd.service;

import com.hrd.entity.Message;
import java.util.List;

/**
 * 消息聚类服务接口
 * 负责将新消息与现有事件进行匹配聚类，实现事件的自动聚合
 */
public interface ClusteringService {

    /**
     * 处理新消息，判断是否需要聚类到现有事件
     * 核心聚类方法，根据消息内容计算与现有事件的相似度，决定是否合并
     *
     * @param message 新消息对象
     * @return 关联的事件ID，如果新建事件则返回新ID，如果聚类失败返回null
     */
    Long processMessage(Message message);

    /**
     * 计算两条消息的相似度
     * 基于文本向量计算语义相似度
     *
     * @param m1 消息1
     * @param m2 消息2
     * @return 相似度分数（0-1之间）
     */
    float calculateSimilarity(Message m1, Message m2);

    /**
     * 计算消息与事件的相似度
     * 综合考虑事件中所有证据消息的相似度
     *
     * @param message 消息对象
     * @param incidentId 事件ID
     * @return 相似度分数（0-1之间）
     */
    float calculateIncidentSimilarity(Message message, Long incidentId);

    /**
     * 获取事件的所有证据消息
     *
     * @param incidentId 事件ID
     * @return 证据消息列表
     */
    List<Message> getIncidentEvidences(Long incidentId);

    /**
     * 重新聚类所有未聚合的消息
     * 批量处理历史消息，用于系统初始化或数据修复
     *
     * @return 成功聚类的消息数量
     */
    int reclusterAllMessages();

    /**
     * 更新事件的摘要信息
     * 当事件有新消息加入时，更新事件的描述和统计信息
     *
     * @param incidentId 事件ID
     */
    void updateIncidentSummary(Long incidentId);

    /**
     * 清除事件向量缓存
     */
    void clearVectorCache();
}
