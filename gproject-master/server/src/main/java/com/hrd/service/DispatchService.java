package com.hrd.service;

import com.hrd.dto.DispatchRequestDTO;
import com.hrd.dto.DispatchResultDTO;
import com.hrd.dto.RespondDispatchDTO;
import com.hrd.entity.DispatchTask;
import com.hrd.entity.Incident;
import com.hrd.entity.Responder;

import java.util.List;

/**
 * 派单服务接口
 */
public interface DispatchService {

    /**
     * 智能派单
     * 根据事件类型和执勤人员岗位进行匹配
     * @param dispatchRequestDTO 派单请求
     * @return 派单结果
     */
    DispatchResultDTO smartDispatch(DispatchRequestDTO dispatchRequestDTO);

    /**
     * 执勤人员响应派单
     * @param responderId 执勤人员ID
     * @param respondDispatchDTO 响应参数
     */
    void respondDispatch(Long responderId, RespondDispatchDTO respondDispatchDTO);

    /**
     * 根据事件ID获取派单任务
     * @param incidentId 事件ID
     * @return 派单任务列表
     */
    List<DispatchTask> getDispatchTasksByIncidentId(Long incidentId);

    /**
     * 根据执勤人员ID获取派单任务
     * @param responderId 执勤人员ID
     * @return 派单任务列表
     */
    List<DispatchTask> getDispatchTasksByResponderId(Long responderId);

    /**
     * 获取待响应的派单任务
     * @param responderId 执勤人员ID
     * @return 待响应的派单任务
     */
    List<DispatchTask> getPendingDispatchTasks(Long responderId);

    /**
     * 取消派单任务
     * @param dispatchTaskId 派单任务ID
     */
    void cancelDispatchTask(Long dispatchTaskId);

    /**
     * 重新派单
     * 当执勤人员拒绝时，重新派单给其他人员
     * @param dispatchTaskId 原派单任务ID
     * @return 新的派单结果
     */
    DispatchResultDTO redispatch(Long dispatchTaskId);

    /**
     * 获取岗位与灾种的匹配规则
     * @param disasterType 灾种类型
     * @return 匹配的岗位列表
     */
    List<String> getMatchingPositions(String disasterType);

    /**
     * 根据灾种类型匹配执勤人员
     * @param incident 事件信息
     * @return 匹配的执勤人员列表
     */
    List<Responder> matchRespondersByDisasterType(Incident incident);

    /**
     * 批量智能派单 - 对所有已确认事件执行智能派单
     * @return 批量派单结果列表
     */
    List<DispatchResultDTO> batchSmartDispatch();
}