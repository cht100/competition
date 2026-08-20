package com.hrd.service;

import com.hrd.dto.AcceptTaskDTO;
import com.hrd.dto.SubmitFeedbackDTO;
import com.hrd.entity.Incident;
import com.hrd.vo.IncidentDetailVO;
import java.util.List;
import java.util.Map;

/**
 * 事件服务接口
 */
public interface IncidentService {

    void save(Incident incident);

    void updateById(Incident incident);

    Incident getById(Long id);

    /**
     * 根据ID查询事件详情（包含证据链）
     * @param id 事件ID
     * @return 事件详情（包含关联的消息列表）
     */
    IncidentDetailVO getDetailById(Long id);

    List<Incident> listAll();

    List<Incident> list();

    List<Incident> listByStatus(Integer status);

    List<Incident> listByDisasterType(String disasterType);

    List<Incident> listByType(String type);

    /**
     * 查询活跃事件（待审核和已确认状态的事件）
     * @return 活跃事件列表
     */
    List<Incident> listActive();

    void confirm(Long id);

    void confirm(Long id, Long operatorId);

    void reject(Long id, String reason);

    void reject(Long id, Long operatorId, String reason);

    Map<String, Object> getStatistics();

    /**
     * 查询可接受的事件列表（已确认状态的事件）
     * @return 可接受的事件列表
     */
    List<Incident> listAcceptable();

    /**
     * 执勤人员接受任务
     * @param responderId 执勤人员ID
     * @param acceptTaskDTO 接受任务参数
     */
    void acceptTask(Long responderId, AcceptTaskDTO acceptTaskDTO);

    /**
     * 执勤人员提交反馈
     * @param responderId 执勤人员ID
     * @param submitFeedbackDTO 提交反馈参数
     */
    void submitFeedback(Long responderId, SubmitFeedbackDTO submitFeedbackDTO);

    /**
     * 根据执勤人员ID查询正在处理的事件
     * @param responderId 执勤人员ID
     * @return 正在处理的事件
     */
    Incident getProcessingIncidentByResponderId(Long responderId);

    /**
     * 根据执勤人员ID查询关联的所有事件
     * @param responderId 执勤人员ID
     * @return 关联的事件列表
     */
    List<Incident> getIncidentsByResponderId(Long responderId);
}
