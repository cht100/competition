package com.hrd.task;

import com.alibaba.fastjson.JSON;
import com.hrd.config.SimulateDataConfiguration;
import com.hrd.controller.admin.SettingsController;
import com.hrd.entity.Message;
import com.hrd.service.ClusteringService;
import com.hrd.service.MessageService;
import com.hrd.service.ModelService;
import com.hrd.utils.MessageCleanUtil;
import com.hrd.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class TextSimulateTask {

    @Autowired
    private MessageService messageService;
    @Autowired
    private SimulateDataConfiguration simulateDataConfiguration;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ClusteringService clusteringService;
    @Autowired
    private SettingsController settingsController;
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    private volatile boolean isRunning = false;

    private final AtomicInteger currentIndex = new AtomicInteger(0);

    private List<Message> pendingMessages = null;

    private ScheduledFuture<?> scheduledFuture;

    public synchronized void start() {
        if (this.isRunning) {
            log.warn("【群聊文本模拟器】已在运行中");
            return;
        }
        this.isRunning = true;
        this.currentIndex.set(0);
        loadPendingMessages();
        scheduleNextPush();
        log.info("【群聊文本模拟器】已启动，推送间隔：{}秒", settingsController.getPushIntervalMs() / 1000);
    }

    public synchronized void stop() {
        this.isRunning = false;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
        log.info("【群聊文本模拟器】已停止");
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    private void loadPendingMessages() {
        try {
            pendingMessages = messageService.listAll();
            if (pendingMessages == null || pendingMessages.isEmpty()) {
                log.warn("【群聊文本模拟器】数据库中没有待推送的消息");
            } else {
                log.info("【群聊文本模拟器】已加载 {} 条待推送消息", pendingMessages.size());
            }
        } catch (Exception e) {
            log.error("【群聊文本模拟器】加载消息失败: {}", e.getMessage());
        }
    }

    private void scheduleNextPush() {
        if (!isRunning) return;

        scheduledFuture = taskScheduler.schedule(() -> {
            generateAndPushMessage();
            scheduleNextPush();
        }, new java.util.Date(System.currentTimeMillis() + settingsController.getPushIntervalMs()));
    }

    public void generateAndPushMessage() {
        if (!isRunning) {
            return;
        }

        if (pendingMessages == null || pendingMessages.isEmpty()) {
            loadPendingMessages();
            if (pendingMessages == null || pendingMessages.isEmpty()) {
                return;
            }
        }

        try {
            int idx = currentIndex.getAndIncrement();

            if (idx >= pendingMessages.size()) {
                log.info("【群聊文本模拟器】所有消息已推送完毕，重新开始");
                currentIndex.set(0);
                idx = 0;
            }

            Message msg = pendingMessages.get(idx);

            msg.setPublishTime(LocalDateTime.now());
            msg.setCreateTime(LocalDateTime.now());
            msg.setStatus(Message.STATUS_TO_CLEAN);

            Message cleanedMsg = MessageCleanUtil.cleanMessage(msg);
            log.info("【消息清洗完成】清洗后文本：{}", cleanedMsg.getCleanedText());

            if (cleanedMsg.getCleanedText() != null && !cleanedMsg.getCleanedText().isEmpty()) {
                try {
                    Map<String, Object> analysis = modelService.analyzeMessage(cleanedMsg.getCleanedText());
                    cleanedMsg.setAiAnalysis(JSON.toJSONString(analysis));
                    log.info("【AI研判完成】灾害类型：{}，严重程度：{}，置信度：{}",
                            analysis.get("disaster_type"), analysis.get("severity"), analysis.get("confidence"));
                    // 置信度 <= 30% → 谣言，30%-阈值 → 正常消息（可聚类但不直接生成事件），>= 阈值 → 直接生成事件
                    float conf = analysis.get("confidence") != null ? ((Number) analysis.get("confidence")).floatValue() : 0;
                    if (conf <= 30) {
                        cleanedMsg.setStatus(Message.STATUS_RUMOR);
                    } else {
                        cleanedMsg.setStatus(Message.STATUS_JUDGED);
                        messageService.updateById(cleanedMsg);
                    }
                } catch (Exception e) {
                    log.error("【AI研判失败】消息ID：{}，错误：{}", cleanedMsg.getId(), e.getMessage());
                }
            }

            try {
                Long incidentId = clusteringService.processMessage(cleanedMsg);
                if (incidentId != null) {
                    cleanedMsg.setIncidentId(incidentId);
                    log.info("【聚类完成】消息已聚合到事件，事件ID：{}", incidentId);
                }
            } catch (Exception e) {
                log.error("【聚类失败】消息ID：{}，错误：{}", cleanedMsg.getId(), e.getMessage());
            }

            messageService.updateById(cleanedMsg);

            webSocketServer.sendMessage(cleanedMsg);

            log.info("【消息推送成功】{}/{} | 平台：{} | 文本：{}",
                    idx + 1, pendingMessages.size(),
                    cleanedMsg.getSourcePlatform(),
                    cleanedMsg.getCleanedText() != null && cleanedMsg.getCleanedText().length() > 30
                            ? cleanedMsg.getCleanedText().substring(0, 30) + "..."
                            : cleanedMsg.getCleanedText());

        } catch (Exception e) {
            log.error("【群聊文本模拟器】推送消息异常", e);
        }
    }

    private String generatePublisherName(String sourcePlatform) {
        String prefix = switch (sourcePlatform) {
            case "微信群聊" -> "微信用户";
            case "QQ群聊" -> "QQ用户";
            case "微博" -> "微博博主";
            default -> "匿名用户";
        };
        return prefix + "_" + (1000 + SimulateDataConfiguration.RANDOM.nextInt(9000));
    }

    @PreDestroy
    public void destroy() {
        stop();
    }
}
