package com.hrd.service;

import com.hrd.dto.DispatchDTO;
import com.hrd.dto.TaskStatusDTO;
import com.hrd.entity.DispatchTask;
import java.util.List;

/**
 * 派单任务服务接口
 */
public interface DispatchTaskService {

    void save(DispatchTask task);

    void updateById(DispatchTask task);

    DispatchTask getById(Long id);

    List<DispatchTask> listByIncidentId(Long incidentId);

    List<DispatchTask> listActiveByResponderId(Long responderId);

    void dispatch(Long incidentId, Long responderId, String description, String notes);

    void dispatch(DispatchDTO dispatchDTO);

    void updateStatus(Long taskId, Integer status, String feedback);

    void updateStatus(Long taskId, TaskStatusDTO taskStatusDTO);
}
