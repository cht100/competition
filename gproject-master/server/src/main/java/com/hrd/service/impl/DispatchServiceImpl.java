package com.hrd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hrd.dto.DispatchRequestDTO;
import com.hrd.dto.DispatchResultDTO;
import com.hrd.dto.RespondDispatchDTO;
import com.hrd.entity.DispatchTask;
import com.hrd.entity.Incident;
import com.hrd.entity.Responder;
import com.hrd.mapper.DispatchTaskMapper;
import com.hrd.mapper.IncidentMapper;
import com.hrd.mapper.ResponderMapper;
import com.hrd.service.DispatchService;
import com.hrd.service.IncidentService;
import com.hrd.service.ResponderService;
import com.hrd.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 派单服务实现类
 */
@Service
@Slf4j
public class DispatchServiceImpl implements DispatchService {

    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;

    @Autowired
    private IncidentMapper incidentMapper;

    @Autowired
    private ResponderMapper responderMapper;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private ResponderService responderService;

    @Autowired
    private WebSocketServer webSocketServer;

    // 岗位与灾种的匹配规则
    private static final Map<String, List<String>> POSITION_DISASTER_MATCH = new HashMap<>();
    
    static {
        // 火灾：消防、医疗、警务
        POSITION_DISASTER_MATCH.put("火灾", Arrays.asList("消防", "医疗", "警务"));
        // 洪涝：消防、医疗、志愿者
        POSITION_DISASTER_MATCH.put("洪涝", Arrays.asList("消防", "医疗", "志愿者"));
        // 地震：消防、医疗、警务
        POSITION_DISASTER_MATCH.put("地震", Arrays.asList("消防", "医疗", "警务"));
        // 滑坡：消防、医疗
        POSITION_DISASTER_MATCH.put("滑坡", Arrays.asList("消防", "医疗"));
        // 交通事故：医疗、警务
        POSITION_DISASTER_MATCH.put("交通事故", Arrays.asList("医疗", "警务"));
        // 燃气泄漏：消防、医疗
        POSITION_DISASTER_MATCH.put("燃气泄漏", Arrays.asList("消防", "医疗"));
        // 电梯困人：消防、志愿者
        POSITION_DISASTER_MATCH.put("电梯困人", Arrays.asList("消防", "志愿者"));
        // 道路塌陷：消防、医疗、警务
        POSITION_DISASTER_MATCH.put("道路塌陷", Arrays.asList("消防", "医疗", "警务"));
        // 暴雪：消防、志愿者、医疗
        POSITION_DISASTER_MATCH.put("暴雪", Arrays.asList("消防", "志愿者", "医疗"));
        // 台风：消防、医疗、警务、志愿者
        POSITION_DISASTER_MATCH.put("台风", Arrays.asList("消防", "医疗", "警务", "志愿者"));
        // 默认匹配：所有岗位
        POSITION_DISASTER_MATCH.put("默认", Arrays.asList("消防", "医疗", "警务", "志愿者"));
    }

    @Override
    @Transactional
    public DispatchResultDTO smartDispatch(DispatchRequestDTO dispatchRequestDTO) {
        log.info("智能派单，事件ID：{}", dispatchRequestDTO.getIncidentId());

        // 1. 检查事件是否存在
        Incident incident = incidentMapper.selectById(dispatchRequestDTO.getIncidentId());
        if (incident == null) {
            throw new RuntimeException("事件不存在，ID：" + dispatchRequestDTO.getIncidentId());
        }

        // 2. 检查事件状态
        if (incident.getStatus() != Incident.STATUS_CONFIRMED) {
            throw new RuntimeException("事件状态不可派单");
        }

        // 3. 强制派单逻辑
        if (dispatchRequestDTO.getForceDispatch() && dispatchRequestDTO.getSpecifiedResponderId() != null) {
            return forceDispatch(incident, dispatchRequestDTO.getSpecifiedResponderId());
        }

        // 4. 智能匹配逻辑
        return matchAndDispatch(incident);
    }

    /**
     * 强制派单
     */
    private DispatchResultDTO forceDispatch(Incident incident, Long responderId) {
        Responder responder = responderMapper.selectById(responderId);
        if (responder == null) {
            throw new RuntimeException("指定的执勤人员不存在，ID：" + responderId);
        }

        if (responder.getStatus() != Responder.STATUS_IDLE) {
            throw new RuntimeException("指定的执勤人员当前不可用");
        }

        return createDispatchTask(incident, responder, "强制派单");
    }

    /**
     * 智能匹配并派单 - 基于距离+岗位匹配度综合评分
     */
    private DispatchResultDTO matchAndDispatch(Incident incident) {
        // 1. 根据灾种类型匹配执勤人员
        List<Responder> matchedResponders = matchRespondersByDisasterType(incident);
        boolean fallback = false;
        
        if (matchedResponders.isEmpty()) {
            // 如果没有匹配的人员，尝试匹配所有在线人员
            matchedResponders = getAllIdleResponders();
            fallback = true;
            
            if (matchedResponders.isEmpty()) {
                return DispatchResultDTO.builder()
                        .success(false)
                        .message("当前没有可用的执勤人员")
                        .matchedResponderCount(0)
                        .build();
            }
        }

        // 2. 综合评分选择最优执勤人员
        Responder selectedResponder = selectBestResponder(matchedResponders, incident);
        String reason = buildMatchReason(selectedResponder, incident, matchedResponders.size(), fallback);
        
        DispatchResultDTO result = createDispatchTask(incident, selectedResponder, "智能匹配派单");
        result.setResponderName(selectedResponder.getName());
        result.setReason(reason);
        return result;
    }

    /**
     * 综合评分选择最优执勤人员
     * 评分维度：距离(60%) + 岗位优先级(40%)
     */
    private Responder selectBestResponder(List<Responder> candidates, Incident incident) {
        if (candidates.size() == 1) return candidates.get(0);
        
        double incLat = incident.getLat() != null ? incident.getLat().doubleValue() : 0;
        double incLng = incident.getLng() != null ? incident.getLng().doubleValue() : 0;
        boolean hasLocation = incLat != 0 && incLng != 0;
        
        // 岗位优先级
        List<String> priorityPositions = getMatchingPositions(incident.getDisasterType());
        
        Responder best = null;
        double bestScore = -1;
        
        for (Responder r : candidates) {
            double score = 0;
            
            // 距离评分 (0-60分，越近越高)
            if (hasLocation && r.getLat() != null && r.getLng() != null) {
                double dist = haversineDistance(incLat, incLng, r.getLat().doubleValue(), r.getLng().doubleValue());
                // 0km=60分, 10km=30分, 50km=0分
                score += Math.max(0, 60 - dist * 1.2);
            } else {
                score += 30; // 无位置信息给中间分
            }
            
            // 岗位匹配度评分 (0-40分)
            int posIdx = priorityPositions.indexOf(r.getPosition());
            if (posIdx == 0) score += 40;        // 首选岗位
            else if (posIdx == 1) score += 28;   // 次选岗位
            else if (posIdx >= 2) score += 16;   // 其他匹配岗位
            else score += 5;                     // 未匹配岗位(fallback)
            
            if (score > bestScore) {
                bestScore = score;
                best = r;
            }
        }
        
        return best != null ? best : candidates.get(0);
    }

    /**
     * 计算两点间距离(km) - Haversine公式
     */
    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 构建匹配理由说明
     */
    private String buildMatchReason(Responder responder, Incident incident, int candidateCount, boolean fallback) {
        StringBuilder sb = new StringBuilder();
        sb.append("从").append(candidateCount).append("名候选人员中智能匹配。");
        
        if (!fallback) {
            sb.append("岗位[").append(responder.getPosition()).append("]与灾种[")
              .append(incident.getDisasterType()).append("]高度匹配；");
        } else {
            sb.append("无专业岗位匹配，从全部空闲人员中择优选择；");
        }
        
        if (incident.getLat() != null && responder.getLat() != null) {
            double dist = haversineDistance(
                incident.getLat().doubleValue(), incident.getLng().doubleValue(),
                responder.getLat().doubleValue(), responder.getLng().doubleValue());
            sb.append("距事发地约").append(String.format("%.1f", dist)).append("km。");
        }
        
        return sb.toString();
    }

    /**
     * 创建派单任务
     */
    private DispatchResultDTO createDispatchTask(Incident incident, Responder responder, String dispatchType) {
        LocalDateTime now = LocalDateTime.now();
        Integer oldIncidentStatus = incident.getStatus();
        Integer oldResponderStatus = responder.getStatus();
        
        // 1. 创建派单任务
        DispatchTask dispatchTask = DispatchTask.builder()
                .incidentId(incident.getId())
                .responderId(responder.getId())
                .description("处理事件：" + incident.getSummary())
                .location(incident.getLocationText())
                .deadline(now.plusHours(2)) // 默认2小时内完成
                .notes("请尽快前往现场处理")
                .status(DispatchTask.STATUS_PENDING)
                .createTime(now)
                .build();

        dispatchTaskMapper.insert(dispatchTask);

        // 2. 更新事件状态
        incident.setStatus(Incident.STATUS_DISPATCHED);
        incident.setUpdateTime(now);
        incidentMapper.updateById(incident);

        // 3. 更新执勤人员状态
        responder.setStatus(Responder.STATUS_DISPATCHED);
        responder.setUpdateTime(now);
        responderMapper.updateById(responder);

        log.info("{}成功，事件ID：{}，执勤人员ID：{}，派单任务ID：{}", 
                dispatchType, incident.getId(), responder.getId(), dispatchTask.getId());

        // 4. 推送WebSocket通知
        try {
            webSocketServer.sendIncidentUpdate(incident, oldIncidentStatus, Incident.STATUS_DISPATCHED);
            webSocketServer.sendResponderUpdate(responder, oldResponderStatus, Responder.STATUS_DISPATCHED);
            webSocketServer.sendTaskUpdate(dispatchTask, null, DispatchTask.STATUS_PENDING);
        } catch (Exception e) {
            log.warn("推送派单通知失败: {}", e.getMessage());
        }

        return DispatchResultDTO.builder()
                .success(true)
                .message(dispatchType + "成功，已通知执勤人员")
                .dispatchTask(dispatchTask)
                .matchedResponderCount(1)
                .build();
    }

    @Override
    @Transactional
    public void respondDispatch(Long responderId, RespondDispatchDTO respondDispatchDTO) {
        log.info("执勤人员响应派单，执勤人员ID：{}，派单任务ID：{}", 
                responderId, respondDispatchDTO.getDispatchTaskId());

        // 1. 检查派单任务
        DispatchTask dispatchTask = dispatchTaskMapper.selectById(respondDispatchDTO.getDispatchTaskId());
        if (dispatchTask == null) {
            throw new RuntimeException("派单任务不存在，ID：" + respondDispatchDTO.getDispatchTaskId());
        }

        if (!responderId.equals(dispatchTask.getResponderId())) {
            throw new RuntimeException("该派单任务不是指派给当前执勤人员的");
        }

        if (dispatchTask.getStatus() != DispatchTask.STATUS_PENDING) {
            throw new RuntimeException("派单任务状态不可响应");
        }

        // 2. 更新派单任务状态
        LocalDateTime now = LocalDateTime.now();
        if (respondDispatchDTO.getAccept()) {
            // 接受任务
            dispatchTask.setStatus(DispatchTask.STATUS_ACCEPTED);
            
            // 更新事件状态
            Incident incident = incidentMapper.selectById(dispatchTask.getIncidentId());
            Integer oldIncidentStatus = incident.getStatus();
            incident.setStatus(Incident.STATUS_PROCESSING);
            incident.setResponderId(responderId);
            incident.setUpdateTime(now);
            incidentMapper.updateById(incident);

            // 更新执勤人员状态
            Responder responder = responderMapper.selectById(responderId);
            Integer oldResponderStatus = responder.getStatus();
            responder.setStatus(Responder.STATUS_EXECUTING);
            responder.setCurrentIncidentId(dispatchTask.getIncidentId());
            responder.setUpdateTime(now);
            responderMapper.updateById(responder);

            dispatchTaskMapper.updateById(dispatchTask);

            // 推送WebSocket通知
            try {
                webSocketServer.sendIncidentUpdate(incident, oldIncidentStatus, Incident.STATUS_PROCESSING);
                webSocketServer.sendResponderUpdate(responder, oldResponderStatus, Responder.STATUS_EXECUTING);
                webSocketServer.sendTaskUpdate(dispatchTask, DispatchTask.STATUS_PENDING, DispatchTask.STATUS_ACCEPTED);
            } catch (Exception e) {
                log.warn("推送接受派单通知失败: {}", e.getMessage());
            }

            log.info("执勤人员接受派单任务，派单任务ID：{}", dispatchTask.getId());
        } else {
            // 拒绝任务
            dispatchTask.setStatus(DispatchTask.STATUS_REJECTED);
            dispatchTask.setRejectReason(respondDispatchDTO.getRejectReason());
            
            // 更新执勤人员状态
            Responder responder = responderMapper.selectById(responderId);
            Integer oldResponderStatus = responder.getStatus();
            responder.setStatus(Responder.STATUS_IDLE);
            responder.setUpdateTime(now);
            responderMapper.updateById(responder);

            // 拒绝后将事件状态回退为已确认，以便重新派单
            Incident incident = incidentMapper.selectById(dispatchTask.getIncidentId());
            Integer oldIncidentStatus = incident.getStatus();
            if (incident.getStatus() == Incident.STATUS_DISPATCHED) {
                incident.setStatus(Incident.STATUS_CONFIRMED);
                incident.setUpdateTime(now);
                incidentMapper.updateById(incident);
            }

            dispatchTaskMapper.updateById(dispatchTask);

            // 推送WebSocket通知
            try {
                webSocketServer.sendIncidentUpdate(incident, oldIncidentStatus, incident.getStatus());
                webSocketServer.sendResponderUpdate(responder, oldResponderStatus, Responder.STATUS_IDLE);
                webSocketServer.sendTaskUpdate(dispatchTask, DispatchTask.STATUS_PENDING, DispatchTask.STATUS_REJECTED);
            } catch (Exception e) {
                log.warn("推送拒绝派单通知失败: {}", e.getMessage());
            }

            log.info("执勤人员拒绝派单任务，派单任务ID：{}，拒绝原因：{}", 
                    dispatchTask.getId(), respondDispatchDTO.getRejectReason());
        }
    }

    @Override
    public List<DispatchTask> getDispatchTasksByIncidentId(Long incidentId) {
        LambdaQueryWrapper<DispatchTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DispatchTask::getIncidentId, incidentId)
                .orderByDesc(DispatchTask::getCreateTime);
        return dispatchTaskMapper.selectList(wrapper);
    }

    @Override
    public List<DispatchTask> getDispatchTasksByResponderId(Long responderId) {
        LambdaQueryWrapper<DispatchTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DispatchTask::getResponderId, responderId)
                .orderByDesc(DispatchTask::getCreateTime);
        return dispatchTaskMapper.selectList(wrapper);
    }

    @Override
    public List<DispatchTask> getPendingDispatchTasks(Long responderId) {
        LambdaQueryWrapper<DispatchTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DispatchTask::getResponderId, responderId)
                .eq(DispatchTask::getStatus, DispatchTask.STATUS_PENDING)
                .orderByDesc(DispatchTask::getCreateTime);
        return dispatchTaskMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void cancelDispatchTask(Long dispatchTaskId) {
        DispatchTask dispatchTask = dispatchTaskMapper.selectById(dispatchTaskId);
        if (dispatchTask == null) {
            throw new RuntimeException("派单任务不存在，ID：" + dispatchTaskId);
        }

        LocalDateTime now = LocalDateTime.now();
        dispatchTask.setStatus(DispatchTask.STATUS_REJECTED);
        dispatchTask.setRejectReason("管理员取消派单");
        dispatchTaskMapper.updateById(dispatchTask);

        // 更新执勤人员状态
        Responder responder = responderMapper.selectById(dispatchTask.getResponderId());
        if (responder != null) {
            responder.setStatus(Responder.STATUS_IDLE);
            responder.setUpdateTime(now);
            responderMapper.updateById(responder);
        }

        log.info("取消派单任务，派单任务ID：{}", dispatchTaskId);
    }

    @Override
    @Transactional
    public DispatchResultDTO redispatch(Long dispatchTaskId) {
        DispatchTask originalTask = dispatchTaskMapper.selectById(dispatchTaskId);
        if (originalTask == null) {
            throw new RuntimeException("原派单任务不存在，ID：" + dispatchTaskId);
        }

        if (originalTask.getStatus() != DispatchTask.STATUS_REJECTED) {
            throw new RuntimeException("只有被拒绝的派单任务才能重新派单");
        }

        Incident incident = incidentMapper.selectById(originalTask.getIncidentId());
        if (incident == null) {
            throw new RuntimeException("关联的事件不存在");
        }

        // 排除已拒绝的执勤人员
        List<Long> excludedResponderIds = getDispatchTasksByIncidentId(incident.getId())
                .stream()
                .filter(task -> task.getStatus() == DispatchTask.STATUS_REJECTED)
                .map(DispatchTask::getResponderId)
                .collect(Collectors.toList());

        // 重新匹配执勤人员
        List<Responder> matchedResponders = matchRespondersByDisasterType(incident)
                .stream()
                .filter(responder -> !excludedResponderIds.contains(responder.getId()))
                .collect(Collectors.toList());

        if (matchedResponders.isEmpty()) {
            return DispatchResultDTO.builder()
                    .success(false)
                    .message("没有其他可用的执勤人员")
                    .matchedResponderCount(0)
                    .build();
        }

        Responder selectedResponder = matchedResponders.get(0);
        return createDispatchTask(incident, selectedResponder, "重新派单");
    }

    @Override
    public List<String> getMatchingPositions(String disasterType) {
        return POSITION_DISASTER_MATCH.getOrDefault(disasterType, POSITION_DISASTER_MATCH.get("默认"));
    }

    @Override
    public List<Responder> matchRespondersByDisasterType(Incident incident) {
        List<String> matchingPositions = getMatchingPositions(incident.getDisasterType());
        
        LambdaQueryWrapper<Responder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Responder::getPosition, matchingPositions)
                .eq(Responder::getStatus, Responder.STATUS_IDLE)
                .orderByDesc(Responder::getUpdateTime);
        
        return responderMapper.selectList(wrapper);
    }

    /**
     * 获取所有空闲的执勤人员
     */
    private List<Responder> getAllIdleResponders() {
        LambdaQueryWrapper<Responder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Responder::getStatus, Responder.STATUS_IDLE)
                .orderByDesc(Responder::getUpdateTime);
        return responderMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public List<DispatchResultDTO> batchSmartDispatch() {
        log.info("批量智能派单开始");
        // 查询所有已确认待派单的事件
        LambdaQueryWrapper<Incident> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Incident::getStatus, Incident.STATUS_CONFIRMED)
                .orderByDesc(Incident::getSeverity) // 高严重度优先
                .orderByAsc(Incident::getCreateTime); // 早发生的优先
        List<Incident> confirmedIncidents = incidentMapper.selectList(wrapper);

        List<DispatchResultDTO> results = new ArrayList<>();
        for (Incident incident : confirmedIncidents) {
            try {
                DispatchResultDTO result = matchAndDispatch(incident);
                results.add(result);
            } catch (Exception e) {
                log.warn("批量派单中事件{}派单失败: {}", incident.getId(), e.getMessage());
                results.add(DispatchResultDTO.builder()
                        .success(false)
                        .message("事件[" + incident.getSummary() + "]派单失败: " + e.getMessage())
                        .matchedResponderCount(0)
                        .build());
            }
        }
        log.info("批量智能派单完成，共处理{}个事件", confirmedIncidents.size());
        return results;
    }
}