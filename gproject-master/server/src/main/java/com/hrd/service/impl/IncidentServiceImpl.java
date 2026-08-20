package com.hrd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrd.dto.AcceptTaskDTO;
import com.hrd.dto.SubmitFeedbackDTO;
import com.hrd.entity.DispatchTask;
import com.hrd.entity.Incident;
import com.hrd.entity.Message;
import com.hrd.entity.Responder;
import com.hrd.entity.ReviewLog;
import com.hrd.mapper.DispatchTaskMapper;
import com.hrd.mapper.IncidentMapper;
import com.hrd.mapper.MessageMapper;
import com.hrd.mapper.ResponderMapper;
import com.hrd.mapper.ReviewLogMapper;
import com.hrd.service.IncidentService;
import com.hrd.service.MessageService;
import com.hrd.service.ResponderService;
import com.hrd.vo.IncidentDetailVO;
import com.hrd.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 事件服务实现类
 * 负责事件相关的业务逻辑处理
 */
@Service
@Slf4j
public class IncidentServiceImpl implements IncidentService {

    @Autowired
    private IncidentMapper incidentMapper;

    @Autowired
    private ReviewLogMapper reviewLogMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ResponderService responderService;

    @Autowired
    private ResponderMapper responderMapper;

    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 创建事件
     * 设置初始状态为待审核，记录创建时间
     * @param incident 事件对象
     */
    @Override
    public void save(Incident incident) {
        log.info("创建事件: {}", incident.getSummary());
        incident.setStatus(Incident.STATUS_PENDING);
        incident.setCreateTime(LocalDateTime.now());
        incident.setUpdateTime(LocalDateTime.now());
        incidentMapper.insert(incident);
    }

    /**
     * 更新事件
     * @param incident 事件对象
     */
    @Override
    public void updateById(Incident incident) {
        log.info("更新事件: id={}", incident.getId());
        incident.setUpdateTime(LocalDateTime.now());
        incidentMapper.updateById(incident);
    }

    /**
     * 根据ID查询事件
     * @param id 事件ID
     * @return 事件对象
     */
    @Override
    public Incident getById(Long id) {
        log.info("查询事件: id={}", id);
        return incidentMapper.selectById(id);
    }

    /**
     * 根据ID查询事件详情（包含证据链）
     * @param id 事件ID
     * @return 事件详情（包含关联的消息列表）
     */
    @Override
    public IncidentDetailVO getDetailById(Long id) {
        log.info("查询事件详情（包含证据链）: id={}", id);
        
        // 查询事件基本信息
        Incident incident = incidentMapper.selectById(id);
        if (incident == null) {
            throw new RuntimeException("事件不存在，ID: " + id);
        }
        
        // 查询事件关联的消息列表（证据链）
        List<Message> messages = messageService.listByIncidentId(id);
        
        // 构建事件详情VO
        return IncidentDetailVO.fromIncidentAndMessages(incident, messages);
    }

    /**
     * 查询所有事件
     * @return 事件列表
     */
    @Override
    public List<Incident> listAll() {
        log.info("查询所有事件");
        return incidentMapper.selectList(null);
    }

    /**
     * 查询所有事件（list方法）
     * @return 事件列表
     */
    @Override
    public List<Incident> list() {
        log.info("查询所有事件");
        return incidentMapper.selectList(null);
    }

    /**
     * 按状态查询事件
     * @param status 状态值
     * @return 事件列表
     */
    @Override
    public List<Incident> listByStatus(Integer status) {
        log.info("按状态查询事件: status={}", status);
        LambdaQueryWrapper<Incident> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Incident::getStatus, status)
                .orderByDesc(Incident::getCreateTime);
        return incidentMapper.selectList(wrapper);
    }

    /**
     * 按灾种查询事件
     * @param disasterType 灾种类型
     * @return 事件列表
     */
    @Override
    public List<Incident> listByDisasterType(String disasterType) {
        log.info("按灾种查询事件: disasterType={}", disasterType);
        LambdaQueryWrapper<Incident> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Incident::getDisasterType, disasterType)
                .orderByDesc(Incident::getCreateTime);
        return incidentMapper.selectList(wrapper);
    }

    /**
     * 按灾种查询事件（别名方法）
     * @param type 灾种类型
     * @return 事件列表
     */
    @Override
    public List<Incident> listByType(String type) {
        return listByDisasterType(type);
    }

    /**
     * 查询活跃事件（待审核和已确认状态的事件）
     * @return 活跃事件列表
     */
    @Override
    public List<Incident> listActive() {
        log.info("查询活跃事件");
        LambdaQueryWrapper<Incident> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Incident::getStatus, Incident.STATUS_PENDING, Incident.STATUS_CONFIRMED)
                .orderByDesc(Incident::getCreateTime);
        return incidentMapper.selectList(wrapper);
    }

    /**
     * 确认事件（简化版本，operatorId默认为1L）
     * @param id 事件ID
     */
    @Override
    @Transactional
    public void confirm(Long id) {
        confirm(id, 1L);
    }

    /**
     * 确认事件
     * 更新事件状态为已确认，记录确认时间，并保存审核记录
     * @param id 事件ID
     * @param operatorId 操作人ID
     */
    @Override
    @Transactional
    public void confirm(Long id, Long operatorId) {
        log.info("确认事件: id={}, operatorId={}", id, operatorId);

        Incident incident = incidentMapper.selectById(id);
        if (incident == null) {
            throw new RuntimeException("事件不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        Integer oldStatus = incident.getStatus();
        incident.setStatus(Incident.STATUS_CONFIRMED);
        incident.setConfirmTime(now);
        incident.setUpdateTime(now);
        incidentMapper.updateById(incident);

        ReviewLog reviewLog = ReviewLog.builder()
                .incidentId(id)
                .operation("确认")
                .operatorId(operatorId)
                .createTime(now)
                .build();
        reviewLogMapper.insert(reviewLog);

        // 推送WebSocket事件状态更新
        try {
            webSocketServer.sendIncidentUpdate(incident, oldStatus, Incident.STATUS_CONFIRMED);
        } catch (Exception e) {
            log.warn("推送事件确认通知失败: {}", e.getMessage());
        }
    }

    /**
     * 驳回事件（简化版本，operatorId默认为1L）
     * @param id 事件ID
     * @param reason 驳回原因
     */
    @Override
    @Transactional
    public void reject(Long id, String reason) {
        reject(id, 1L, reason);
    }

    /**
     * 驳回事件
     * 更新事件状态为已驳回，记录驳回原因，并保存审核记录
     * @param id 事件ID
     * @param operatorId 操作人ID
     * @param reason 驳回原因
     */
    @Override
    @Transactional
    public void reject(Long id, Long operatorId, String reason) {
        log.info("驳回事件: id={}, operatorId={}, reason={}", id, operatorId, reason);

        Incident incident = incidentMapper.selectById(id);
        if (incident == null) {
            throw new RuntimeException("事件不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        Integer oldStatus = incident.getStatus();
        incident.setStatus(Incident.STATUS_REJECTED);
        incident.setUpdateTime(now);
        incidentMapper.updateById(incident);

        ReviewLog reviewLog = ReviewLog.builder()
                .incidentId(id)
                .operation("驳回")
                .operatorId(operatorId)
                .remark(reason)
                .createTime(now)
                .build();
        reviewLogMapper.insert(reviewLog);

        // 推送WebSocket事件状态更新
        try {
            webSocketServer.sendIncidentUpdate(incident, oldStatus, Incident.STATUS_REJECTED);
        } catch (Exception e) {
            log.warn("推送事件驳回通知失败: {}", e.getMessage());
        }
    }

    /**
     * 获取统计数据
     * 包含今日事件数、各状态数量、各灾种数量等
     * @return 统计数据Map
     */
    @Override
    public Map<String, Object> getStatistics() {
        log.info("获取事件统计数据");
        Map<String, Object> statistics = new HashMap<>();

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        LambdaQueryWrapper<Incident> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.between(Incident::getCreateTime, todayStart, todayEnd);
        Long todayCount = incidentMapper.selectCount(todayWrapper);
        statistics.put("todayCount", todayCount);

        Long totalCount = incidentMapper.selectCount(null);
        statistics.put("totalCount", totalCount);

        List<Map<String, Object>> statusCounts = incidentMapper.countByStatus();
        Map<Integer, Long> statusMap = new HashMap<>();
        for (Map<String, Object> item : statusCounts) {
            Integer status = (Integer) item.get("status");
            Long count = ((Number) item.get("count")).longValue();
            statusMap.put(status, count);
        }
        statistics.put("statusCounts", statusMap);

        List<Map<String, Object>> disasterTypeCounts = incidentMapper.countByDisasterType();
        statistics.put("disasterTypeCounts", disasterTypeCounts);

        List<Incident> pendingIncidents = incidentMapper.findPendingIncidents();
        statistics.put("pendingCount", pendingIncidents.size());

        return statistics;
    }

    /**
     * 查询可接受的事件列表（已确认状态的事件）
     * @return 可接受的事件列表
     */
    @Override
    public List<Incident> listAcceptable() {
        log.info("查询可接受的事件列表");
        LambdaQueryWrapper<Incident> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Incident::getStatus, Incident.STATUS_CONFIRMED)
                .isNull(Incident::getResponderId) // 尚未分配处理人员
                .orderByDesc(Incident::getCreateTime);
        return incidentMapper.selectList(wrapper);
    }

    /**
     * 执勤人员接受任务
     * @param responderId 执勤人员ID
     * @param acceptTaskDTO 接受任务参数
     */
    @Override
    @Transactional
    public void acceptTask(Long responderId, AcceptTaskDTO acceptTaskDTO) {
        log.info("执勤人员接受任务，执勤人员ID：{}，事件ID：{}", responderId, acceptTaskDTO.getIncidentId());

        // 1. 检查执勤人员状态
        Responder responder = responderMapper.selectById(responderId);
        if (responder == null) {
            throw new RuntimeException("执勤人员不存在，ID：" + responderId);
        }
        if (responder.getCurrentIncidentId() != null) {
            throw new RuntimeException("执勤人员正在处理其他事件，无法接受新任务");
        }
        if (responder.getStatus() != Responder.STATUS_IDLE) {
            throw new RuntimeException("执勤人员当前状态无法接受任务");
        }

        // 2. 检查事件状态
        Incident incident = incidentMapper.selectById(acceptTaskDTO.getIncidentId());
        if (incident == null) {
            throw new RuntimeException("事件不存在，ID：" + acceptTaskDTO.getIncidentId());
        }
        if (incident.getStatus() != Incident.STATUS_CONFIRMED) {
            throw new RuntimeException("事件状态不可接受");
        }
        if (incident.getResponderId() != null) {
            throw new RuntimeException("事件已被其他执勤人员接受");
        }

        LocalDateTime now = LocalDateTime.now();

        // 3. 志愿者需要管理员审批，其他岗位直接接受
        if ("志愿者".equals(responder.getPosition())) {
            // 创建志愿者申请记录（dispatch_task with VOLUNTEER_PENDING status）
            DispatchTask volunteerTask = DispatchTask.builder()
                    .incidentId(acceptTaskDTO.getIncidentId())
                    .responderId(responderId)
                    .description("志愿者主动申请：" + incident.getSummary())
                    .location(incident.getLocationText())
                    .deadline(now.plusHours(4))
                    .notes("志愿者主动接单，等待管理员审批")
                    .status(DispatchTask.STATUS_VOLUNTEER_PENDING)
                    .createTime(now)
                    .build();
            dispatchTaskMapper.insert(volunteerTask);

            ReviewLog reviewLog = ReviewLog.builder()
                    .incidentId(acceptTaskDTO.getIncidentId())
                    .operation("志愿者申请接单")
                    .operatorId(responderId)
                    .remark("志愿者[" + responder.getName() + "]申请接受任务，等待管理员审批")
                    .createTime(now)
                    .build();
            reviewLogMapper.insert(reviewLog);

            log.info("志愿者申请接单成功，等待管理员审批，事件ID：{}，志愿者ID：{}", acceptTaskDTO.getIncidentId(), responderId);
            return;
        }

        // 非志愿者：直接接受任务
        // 3. 更新事件信息
        Integer oldIncidentStatus = incident.getStatus();
        incident.setStatus(Incident.STATUS_PROCESSING);
        incident.setResponderId(responderId);
        incident.setUpdateTime(now);
        incidentMapper.updateById(incident);

        // 4. 更新执勤人员信息
        Integer oldResponderStatus = responder.getStatus();
        responder.setStatus(Responder.STATUS_EXECUTING);
        responder.setCurrentIncidentId(acceptTaskDTO.getIncidentId());
        responder.setUpdateTime(now);
        responderMapper.updateById(responder);

        // 5. 记录审核日志
        ReviewLog reviewLog = ReviewLog.builder()
                .incidentId(acceptTaskDTO.getIncidentId())
                .operation("接受任务")
                .operatorId(responderId)
                .remark("执勤人员接受任务处理")
                .createTime(now)
                .build();
        reviewLogMapper.insert(reviewLog);

        // 6. 推送WebSocket通知
        try {
            webSocketServer.sendIncidentUpdate(incident, oldIncidentStatus, Incident.STATUS_PROCESSING);
            webSocketServer.sendResponderUpdate(responder, oldResponderStatus, Responder.STATUS_EXECUTING);
        } catch (Exception e) {
            log.warn("推送接受任务通知失败: {}", e.getMessage());
        }

        log.info("执勤人员接受任务成功，事件ID：{}，执勤人员ID：{}", acceptTaskDTO.getIncidentId(), responderId);
    }

    /**
     * 执勤人员提交反馈
     * @param responderId 执勤人员ID
     * @param submitFeedbackDTO 提交反馈参数
     */
    @Override
    @Transactional
    public void submitFeedback(Long responderId, SubmitFeedbackDTO submitFeedbackDTO) {
        log.info("执勤人员提交反馈，执勤人员ID：{}，事件ID：{}", responderId, submitFeedbackDTO.getIncidentId());

        // 1. 检查事件状态和处理人员
        Incident incident = incidentMapper.selectById(submitFeedbackDTO.getIncidentId());
        if (incident == null) {
            throw new RuntimeException("事件不存在，ID：" + submitFeedbackDTO.getIncidentId());
        }
        if (!responderId.equals(incident.getResponderId())) {
            throw new RuntimeException("该事件不是由当前执勤人员处理的");
        }
        if (incident.getStatus() != Incident.STATUS_PROCESSING) {
            throw new RuntimeException("事件状态不可提交反馈");
        }

        // 2. 检查执勤人员状态
        Responder responder = responderMapper.selectById(responderId);
        if (responder == null) {
            throw new RuntimeException("执勤人员不存在，ID：" + responderId);
        }
        if (!submitFeedbackDTO.getIncidentId().equals(responder.getCurrentIncidentId())) {
            throw new RuntimeException("执勤人员当前未处理该事件");
        }

        // 3. 更新事件信息
        LocalDateTime now = LocalDateTime.now();
        Integer oldIncidentStatus = incident.getStatus();
        incident.setStatus(Incident.STATUS_COMPLETED);
        incident.setFeedback(submitFeedbackDTO.getFeedback());
        incident.setCompleteTime(now);
        incident.setUpdateTime(now);
        incidentMapper.updateById(incident);

        // 4. 更新执勤人员信息
        Integer oldResponderStatus = responder.getStatus();
        responder.setStatus(Responder.STATUS_IDLE);
        responder.setCurrentIncidentId(null);
        responder.setUpdateTime(now);
        responderMapper.updateById(responder);

        // 5. 记录审核日志
        ReviewLog reviewLog = ReviewLog.builder()
                .incidentId(submitFeedbackDTO.getIncidentId())
                .operation("完成处理")
                .operatorId(responderId)
                .remark("执勤人员完成事件处理并提交反馈")
                .createTime(now)
                .build();
        reviewLogMapper.insert(reviewLog);

        // 6. 推送WebSocket通知 - 事件完成、人员空闲
        try {
            webSocketServer.sendIncidentUpdate(incident, oldIncidentStatus, Incident.STATUS_COMPLETED);
            webSocketServer.sendResponderUpdate(responder, oldResponderStatus, Responder.STATUS_IDLE);
        } catch (Exception e) {
            log.warn("推送反馈完成通知失败: {}", e.getMessage());
        }

        log.info("执勤人员提交反馈成功，事件ID：{}，执勤人员ID：{}", submitFeedbackDTO.getIncidentId(), responderId);
    }

    /**
     * 根据执勤人员ID查询正在处理的事件
     * @param responderId 执勤人员ID
     * @return 正在处理的事件
     */
    @Override
    public Incident getProcessingIncidentByResponderId(Long responderId) {
        log.info("根据执勤人员ID查询正在处理的事件，执勤人员ID：{}", responderId);
        
        LambdaQueryWrapper<Incident> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Incident::getResponderId, responderId)
                .eq(Incident::getStatus, Incident.STATUS_PROCESSING);
        
        return incidentMapper.selectOne(wrapper);
    }

    /**
     * 根据执勤人员ID查询关联的所有事件
     * @param responderId 执勤人员ID
     * @return 关联的事件列表
     */
    @Override
    public List<Incident> getIncidentsByResponderId(Long responderId) {
        log.info("根据执勤人员ID查询关联的所有事件，执勤人员ID：{}", responderId);
        return incidentMapper.findByResponderId(responderId);
    }
}
