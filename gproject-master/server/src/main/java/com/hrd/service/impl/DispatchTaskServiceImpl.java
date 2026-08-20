package com.hrd.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hrd.dto.DispatchDTO;
import com.hrd.dto.TaskStatusDTO;
import com.hrd.entity.DispatchTask;
import com.hrd.entity.Incident;
import com.hrd.entity.Responder;
import com.hrd.mapper.DispatchTaskMapper;
import com.hrd.mapper.IncidentMapper;
import com.hrd.mapper.ResponderMapper;
import com.hrd.service.DispatchTaskService;
import com.hrd.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 派单任务服务实现类
 * 负责派单任务相关的业务逻辑处理
 */
@Service
@Slf4j
public class DispatchTaskServiceImpl implements DispatchTaskService {

    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;

    @Autowired
    private IncidentMapper incidentMapper;

    @Autowired
    private ResponderMapper responderMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 创建任务
     * 设置初始状态为待出发
     * @param task 任务对象
     */
    @Override
    public void save(DispatchTask task) {
        log.info("创建派单任务: incidentId={}, responderId={}", task.getIncidentId(), task.getResponderId());
        task.setStatus(DispatchTask.STATUS_PENDING);
        task.setCreateTime(LocalDateTime.now());
        dispatchTaskMapper.insert(task);
    }

    /**
     * 更新任务
     * @param task 任务对象
     */
    @Override
    public void updateById(DispatchTask task) {
        log.info("更新派单任务: id={}", task.getId());
        dispatchTaskMapper.updateById(task);
    }

    /**
     * 根据ID查询任务
     * @param id 任务ID
     * @return 任务对象
     */
    @Override
    public DispatchTask getById(Long id) {
        log.info("查询派单任务: id={}", id);
        return dispatchTaskMapper.selectById(id);
    }

    /**
     * 按事件ID查询任务
     * @param incidentId 事件ID
     * @return 任务列表
     */
    @Override
    public List<DispatchTask> listByIncidentId(Long incidentId) {
        log.info("按事件ID查询任务: incidentId={}", incidentId);
        return dispatchTaskMapper.findByIncidentId(incidentId);
    }

    /**
     * 按人员ID查询活动任务
     * 活动任务指状态小于4（已完成）的任务
     * @param responderId 人员ID
     * @return 活动任务列表
     */
    @Override
    public List<DispatchTask> listActiveByResponderId(Long responderId) {
        log.info("按人员ID查询活动任务: responderId={}", responderId);
        return dispatchTaskMapper.findActiveByResponderId(responderId);
    }

    /**
     * 派单
     * 创建任务并更新事件和人员状态
     * @param incidentId 事件ID
     * @param responderId 人员ID
     * @param description 任务描述
     * @param notes 注意事项
     */
    @Override
    @Transactional
    public void dispatch(Long incidentId, Long responderId, String description, String notes) {
        log.info("派单: incidentId={}, responderId={}", incidentId, responderId);

        Incident incident = incidentMapper.selectById(incidentId);
        if (incident == null) {
            throw new RuntimeException("事件不存在");
        }

        Responder responder = responderMapper.selectById(responderId);
        if (responder == null) {
            throw new RuntimeException("执勤人员不存在");
        }

        if (!Responder.STATUS_IDLE.equals(responder.getStatus())) {
            throw new RuntimeException("该执勤人员当前非空闲状态，无法派单");
        }

        DispatchTask task = DispatchTask.builder()
                .incidentId(incidentId)
                .responderId(responderId)
                .description(description)
                .location(incident.getLocationText())
                .notes(notes)
                .status(DispatchTask.STATUS_PENDING)
                .createTime(LocalDateTime.now())
                .build();
        dispatchTaskMapper.insert(task);

        LambdaUpdateWrapper<Incident> incidentUpdate = new LambdaUpdateWrapper<>();
        incidentUpdate.eq(Incident::getId, incidentId)
                .set(Incident::getStatus, Incident.STATUS_DISPATCHED)
                .set(Incident::getUpdateTime, LocalDateTime.now());
        incidentMapper.update(null, incidentUpdate);

        LambdaUpdateWrapper<Responder> responderUpdate = new LambdaUpdateWrapper<>();
        responderUpdate.eq(Responder::getId, responderId)
                .set(Responder::getStatus, Responder.STATUS_DISPATCHED)
                .set(Responder::getUpdateTime, LocalDateTime.now());
        responderMapper.update(null, responderUpdate);

        // 推送WebSocket通知
        try {
            Incident updatedInc = incidentMapper.selectById(incidentId);
            Responder updatedResp = responderMapper.selectById(responderId);
            webSocketServer.sendIncidentUpdate(updatedInc, incident.getStatus(), Incident.STATUS_DISPATCHED);
            webSocketServer.sendResponderUpdate(updatedResp, responder.getStatus(), Responder.STATUS_DISPATCHED);
            webSocketServer.sendTaskUpdate(task, null, DispatchTask.STATUS_PENDING);
        } catch (Exception e) {
            log.warn("推送手动派单通知失败: {}", e.getMessage());
        }
    }

    /**
     * 派单（使用DTO）
     * @param dispatchDTO 派单请求参数
     */
    @Override
    @Transactional
    public void dispatch(DispatchDTO dispatchDTO) {
        dispatch(dispatchDTO.getIncidentId(), dispatchDTO.getResponderId(),
                dispatchDTO.getDescription(), dispatchDTO.getNotes());
    }

    /**
     * 更新任务状态（使用DTO）
     * @param taskId 任务ID
     * @param taskStatusDTO 任务状态更新参数
     */
    @Override
    @Transactional
    public void updateStatus(Long taskId, TaskStatusDTO taskStatusDTO) {
        updateStatus(taskId, taskStatusDTO.getStatus(), taskStatusDTO.getFeedback());
    }

    /**
     * 更新任务状态
     * 根据不同状态更新相应的时间字段
     * @param taskId 任务ID
     * @param status 新状态
     * @param feedback 反馈备注
     */
    @Override
    @Transactional
    public void updateStatus(Long taskId, Integer status, String feedback) {
        log.info("更新任务状态: taskId={}, status={}", taskId, status);

        DispatchTask task = dispatchTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<DispatchTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DispatchTask::getId, taskId)
                .set(DispatchTask::getStatus, status)
                .set(DispatchTask::getFeedback, feedback);

        switch (status) {
            case 1:
                updateWrapper.set(DispatchTask::getDepartTime, now);
                updateResponderStatus(task.getResponderId(), Responder.STATUS_EXECUTING);
                updateIncidentStatus(task.getIncidentId(), Incident.STATUS_PROCESSING);
                break;
            case 2:
                updateWrapper.set(DispatchTask::getArriveTime, now);
                break;
            case 3:
                break;
            case 4:
                updateWrapper.set(DispatchTask::getCompleteTime, now);
                updateResponderStatus(task.getResponderId(), Responder.STATUS_IDLE);
                updateIncidentStatus(task.getIncidentId(), Incident.STATUS_COMPLETED);
                break;
            case 5:
                break;
            default:
                break;
        }

        dispatchTaskMapper.update(null, updateWrapper);
    }

    /**
     * 更新执勤人员状态
     * @param responderId 人员ID
     * @param status 新状态
     */
    private void updateResponderStatus(Long responderId, Integer status) {
        LambdaUpdateWrapper<Responder> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Responder::getId, responderId)
                .set(Responder::getStatus, status)
                .set(Responder::getUpdateTime, LocalDateTime.now());
        responderMapper.update(null, updateWrapper);
    }

    /**
     * 更新事件状态
     * @param incidentId 事件ID
     * @param status 新状态
     */
    private void updateIncidentStatus(Long incidentId, Integer status) {
        LambdaUpdateWrapper<Incident> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Incident::getId, incidentId)
                .set(Incident::getStatus, status)
                .set(Incident::getUpdateTime, LocalDateTime.now());

        if (Incident.STATUS_COMPLETED.equals(status)) {
            updateWrapper.set(Incident::getCompleteTime, LocalDateTime.now());
        }

        incidentMapper.update(null, updateWrapper);
    }
}
