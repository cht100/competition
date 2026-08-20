package com.hrd.websocket;

import com.alibaba.fastjson.JSON;
import com.hrd.entity.DispatchTask;
import com.hrd.entity.Incident;
import com.hrd.entity.Message;
import com.hrd.entity.Responder;
import com.hrd.vo.MessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务端
 * 支持多种消息类型的实时推送
 *
 * 消息类型包括：
 * - new_message: 新消息推送
 * - new_incident: 新事件创建
 * - incident_update: 事件状态更新
 * - responder_update: 人员状态变化
 * - task_update: 任务状态更新
 * - system_notice: 系统通知
 * - alert: 告警消息
 */
@Component
@ServerEndpoint("/ws/message")
@Slf4j
public class WebSocketServer {

    /**
     * 存放会话对象
     * 使用线程安全的 ConcurrentHashMap
     */
    private static ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 消息类型常量
     */
    private static final String TYPE_NEW_MESSAGE = "new_message";
    private static final String TYPE_NEW_INCIDENT = "new_incident";
    private static final String TYPE_INCIDENT_UPDATE = "incident_update";
    private static final String TYPE_RESPONDER_UPDATE = "responder_update";
    private static final String TYPE_TASK_UPDATE = "task_update";
    private static final String TYPE_SYSTEM_NOTICE = "system_notice";
    private static final String TYPE_ALERT = "alert";
    private static final String TYPE_PONG = "pong";
    private static final String TYPE_AGGREGATION_VISUAL = "aggregation_visual";
    private static final String TYPE_DATA_RESET = "data_reset";

    /**
     * 建立连接时调用
     * @param session WebSocket 会话
     */
    @OnOpen
    public void onOpen(Session session) {
        sessionMap.put(session.getId(), session);
        log.info("【WebSocket】新连接建立，sessionId: {}，当前连接数: {}", session.getId(), sessionMap.size());

        // 发送连接成功通知
        sendMessageToSession(session, createMessage(TYPE_SYSTEM_NOTICE, "连接成功"));
    }

    /**
     * 接收消息时调用
     * @param message 接收到的消息
     * @param session WebSocket 会话
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("【WebSocket】收到消息，sessionId: {}，消息: {}", session.getId(), message);

        try {
            // 解析消息
            Map<String, Object> msgMap = JSON.parseObject(message, Map.class);
            String type = (String) msgMap.get("type");

            // 处理心跳消息
            if ("ping".equals(type)) {
                sendMessageToSession(session, createMessage(TYPE_PONG, null));
            }
        } catch (Exception e) {
            log.error("【WebSocket】消息解析失败: {}", e.getMessage());
        }
    }

    /**
     * 连接关闭时调用
     * @param session WebSocket 会话
     */
    @OnClose
    public void onClose(Session session) {
        sessionMap.remove(session.getId());
        log.info("【WebSocket】连接关闭，sessionId: {}，当前连接数: {}", session.getId(), sessionMap.size());
    }

    /**
     * 发送消息给指定会话
     * @param session 目标会话
     * @param message 消息内容
     */
    private void sendMessageToSession(Session session, String message) {
        try {
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(message);
            }
        } catch (IOException e) {
            log.error("【WebSocket】发送消息失败: {}", e.getMessage());
        }
    }

    /**
     * 广播消息给所有连接的客户端
     * @param message 消息内容
     */
    private void broadcast(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            sendMessageToSession(session, message);
        }
    }

    /**
     * 创建标准格式的消息
     * @param type 消息类型
     * @param payload 消息负载
     * @return JSON 格式的消息字符串
     */
    private String createMessage(String type, Object payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("payload", payload);
        message.put("timestamp", System.currentTimeMillis());
        return JSON.toJSONString(message);
    }

    // ==================== 消息推送方法 ====================

    /**
     * 推送新消息（原始消息入库后推送）
     * @param message 消息实体
     */
    public void sendMessage(Message message) {
        MessageVO messageVO = new MessageVO();
        BeanUtils.copyProperties(message, messageVO);

        String jsonMessage = createMessage(TYPE_NEW_MESSAGE, messageVO);
        broadcast(jsonMessage);

        log.info("【WebSocket推送】新消息推送成功，消息ID: {}", message.getId());
    }

    /**
     * 推送新事件（消息聚类后创建新事件）
     * @param incident 事件实体
     */
    public void sendNewIncident(Incident incident) {
        Map<String, Object> incidentData = new HashMap<>();
        incidentData.put("id", incident.getId());
        incidentData.put("summary", incident.getSummary());
        incidentData.put("disasterType", incident.getDisasterType());
        incidentData.put("infoType", incident.getInfoType());
        incidentData.put("severity", incident.getSeverity());
        incidentData.put("confidence", incident.getConfidence());
        incidentData.put("locationText", incident.getLocationText());
        incidentData.put("lat", incident.getLat());
        incidentData.put("lng", incident.getLng());
        incidentData.put("status", incident.getStatus());
        incidentData.put("createTime", incident.getCreateTime());

        String jsonMessage = createMessage(TYPE_NEW_INCIDENT, incidentData);
        broadcast(jsonMessage);

        log.info("【WebSocket推送】新事件推送成功，事件ID: {}，类型: {}，严重程度: {}",
                incident.getId(), incident.getDisasterType(), incident.getSeverity());
    }

    /**
     * 推送事件状态更新
     * @param incident 更新后的事件实体
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    public void sendIncidentUpdate(Incident incident, Integer oldStatus, Integer newStatus) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("id", incident.getId());
        updateData.put("oldStatus", oldStatus);
        updateData.put("newStatus", newStatus);
        updateData.put("statusText", getStatusText(newStatus));
        updateData.put("updateTime", incident.getUpdateTime());
        updateData.put("summary", incident.getSummary());

        String jsonMessage = createMessage(TYPE_INCIDENT_UPDATE, updateData);
        broadcast(jsonMessage);

        log.info("【WebSocket推送】事件状态更新推送成功，事件ID: {}，状态: {} -> {}",
                incident.getId(), oldStatus, newStatus);
    }

    /**
     * 推送人员状态变化
     * @param responder 人员实体
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    public void sendResponderUpdate(Responder responder, Integer oldStatus, Integer newStatus) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("id", responder.getId());
        updateData.put("name", responder.getName());
        updateData.put("phone", responder.getPhone());
        updateData.put("position", responder.getPosition());
        updateData.put("oldStatus", oldStatus);
        updateData.put("newStatus", newStatus);
        updateData.put("statusText", getResponderStatusText(newStatus));
        updateData.put("lat", responder.getLat());
        updateData.put("lng", responder.getLng());
        updateData.put("updateTime", responder.getUpdateTime());

        String jsonMessage = createMessage(TYPE_RESPONDER_UPDATE, updateData);
        broadcast(jsonMessage);

        log.info("【WebSocket推送】人员状态更新推送成功，人员ID: {}，姓名: {}，状态: {} -> {}",
                responder.getId(), responder.getName(), oldStatus, newStatus);
    }

    /**
     * 推送任务状态更新
     * @param task 任务实体
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    public void sendTaskUpdate(DispatchTask task, Integer oldStatus, Integer newStatus) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("id", task.getId());
        updateData.put("incidentId", task.getIncidentId());
        updateData.put("responderId", task.getResponderId());
        updateData.put("description", task.getDescription());
        updateData.put("location", task.getLocation());
        updateData.put("oldStatus", oldStatus);
        updateData.put("newStatus", newStatus);
        updateData.put("statusText", getTaskStatusText(newStatus));
        updateData.put("updateTime", task.getCompleteTime() != null ? task.getCompleteTime() : task.getCreateTime());

        String jsonMessage = createMessage(TYPE_TASK_UPDATE, updateData);
        broadcast(jsonMessage);

        log.info("【WebSocket推送】任务状态更新推送成功，任务ID: {}，状态: {} -> {}",
                task.getId(), oldStatus, newStatus);
    }

    /**
     * 推送系统通知
     * @param notice 通知内容
     */
    public void sendSystemNotice(String notice) {
        Map<String, Object> noticeData = new HashMap<>();
        noticeData.put("message", notice);
        noticeData.put("level", "info");

        String jsonMessage = createMessage(TYPE_SYSTEM_NOTICE, noticeData);
        broadcast(jsonMessage);

        log.info("【WebSocket推送】系统通知推送成功: {}", notice);
    }

    /**
     * 推送告警消息
     * @param incident 高严重度事件
     */
    public void sendAlert(Incident incident) {
        Map<String, Object> alertData = new HashMap<>();
        alertData.put("id", incident.getId());
        alertData.put("summary", incident.getSummary());
        alertData.put("disasterType", incident.getDisasterType());
        alertData.put("severity", incident.getSeverity());
        alertData.put("locationText", incident.getLocationText());
        alertData.put("message", String.format("【高严重度告警】%s - %s",
                incident.getDisasterType(), incident.getSummary()));

        String jsonMessage = createMessage(TYPE_ALERT, alertData);
        broadcast(jsonMessage);

        log.warn("【WebSocket推送】告警消息推送成功，事件ID: {}，严重程度: {}",
                incident.getId(), incident.getSeverity());
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取事件状态文本
     * @param status 状态码
     * @return 状态文本
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
     * 获取人员状态文本
     * @param status 状态码
     * @return 状态文本
     */
    private String getResponderStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "离线";
            case 1 -> "空闲";
            case 2 -> "已派单";
            case 3 -> "执行中";
            default -> "未知";
        };
    }

    /**
     * 获取任务状态文本
     * @param status 状态码
     * @return 状态文本
     */
    private String getTaskStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待出发";
            case 1 -> "已出发";
            case 2 -> "已到达";
            case 3 -> "处理中";
            case 4 -> "已完成";
            case 5 -> "需增援";
            default -> "未知";
        };
    }

    /**
     * 获取当前连接数
     * @return 连接数
     */
    public int getConnectionCount() {
        return sessionMap.size();
    }

    /**
     * 检查是否有活跃连接
     * @return 是否有连接
     */
    public boolean hasConnections() {
        return !sessionMap.isEmpty();
    }

    /**
     * 推送聚合过程可视化数据
     * 用于前端展示 AI 聚合过程的实时动画
     *
     * @param visualData 聚合可视化数据
     */
    public void sendAggregationVisualData(Object visualData) {
        if (!hasConnections()) {
            log.warn("【WebSocket推送】无活跃连接，跳过聚合可视化数据推送");
            return;
        }

        try {
            String jsonMessage = createMessage(TYPE_AGGREGATION_VISUAL, visualData);
            broadcast(jsonMessage);

            log.info("【WebSocket推送】聚合可视化数据推送成功");
        } catch (Exception e) {
            log.error("【WebSocket推送】聚合可视化数据推送失败: {}", e.getMessage());
        }
    }

    /**
     * 广播数据重置通知
     * 通知所有前端客户端清空本地缓存数据
     */
    public void sendDataReset() {
        String jsonMessage = createMessage(TYPE_DATA_RESET, null);
        broadcast(jsonMessage);
        log.info("【WebSocket推送】数据重置通知已广播");
    }
}
