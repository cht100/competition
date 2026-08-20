package com.hrd.controller.admin;

import com.hrd.dto.DispatchRequestDTO;
import com.hrd.dto.DispatchResultDTO;
import com.hrd.entity.DispatchTask;
import com.hrd.entity.Incident;
import com.hrd.entity.Responder;
import com.hrd.mapper.DispatchTaskMapper;
import com.hrd.mapper.IncidentMapper;
import com.hrd.mapper.ResponderMapper;
import com.hrd.result.Result;
import com.hrd.service.DispatchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 派单控制器（管理端）
 */
@RestController
@RequestMapping("/admin/dispatch")
@Slf4j
@Api(tags = "智能派单管理")
public class DispatchController {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;

    @Autowired
    private IncidentMapper incidentMapper;

    @Autowired
    private ResponderMapper responderMapper;

    /**
     * 智能派单
     * @param dispatchRequestDTO 派单请求
     * @return 派单结果
     */
    @PostMapping("/smart")
    @ApiOperation("智能派单")
    public Result<DispatchResultDTO> smartDispatch(@RequestBody DispatchRequestDTO dispatchRequestDTO) {
        log.info("智能派单，事件ID：{}", dispatchRequestDTO.getIncidentId());
        
        try {
            DispatchResultDTO result = dispatchService.smartDispatch(dispatchRequestDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("智能派单失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据事件ID获取派单任务列表
     * @param incidentId 事件ID
     * @return 派单任务列表
     */
    @GetMapping("/tasks/incident/{incidentId}")
    @ApiOperation("根据事件ID获取派单任务列表")
    public Result<List<DispatchTask>> getDispatchTasksByIncidentId(@PathVariable Long incidentId) {
        log.info("根据事件ID获取派单任务列表，事件ID：{}", incidentId);
        
        List<DispatchTask> tasks = dispatchService.getDispatchTasksByIncidentId(incidentId);
        return Result.success(tasks);
    }

    /**
     * 取消派单任务
     * @param dispatchTaskId 派单任务ID
     * @return 操作结果
     */
    @PostMapping("/tasks/{dispatchTaskId}/cancel")
    @ApiOperation("取消派单任务")
    public Result<String> cancelDispatchTask(@PathVariable Long dispatchTaskId) {
        log.info("取消派单任务，派单任务ID：{}", dispatchTaskId);
        
        try {
            dispatchService.cancelDispatchTask(dispatchTaskId);
            return Result.success("派单任务取消成功");
        } catch (Exception e) {
            log.error("取消派单任务失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重新派单
     * @param dispatchTaskId 原派单任务ID
     * @return 重新派单结果
     */
    @PostMapping("/tasks/{dispatchTaskId}/redispatch")
    @ApiOperation("重新派单")
    public Result<DispatchResultDTO> redispatch(@PathVariable Long dispatchTaskId) {
        log.info("重新派单，原派单任务ID：{}", dispatchTaskId);
        
        try {
            DispatchResultDTO result = dispatchService.redispatch(dispatchTaskId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("重新派单失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量智能派单 - 一键对所有已确认事件执行智能派单
     */
    @PostMapping("/batch-smart")
    @ApiOperation("批量智能派单")
    public Result<Map<String, Object>> batchSmartDispatch() {
        log.info("批量智能派单");
        try {
            List<DispatchResultDTO> results = dispatchService.batchSmartDispatch();
            long successCount = results.stream().filter(DispatchResultDTO::getSuccess).count();
            long failCount = results.size() - successCount;
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("total", results.size());
            data.put("success", successCount);
            data.put("fail", failCount);
            data.put("details", results);
            return Result.success(data);
        } catch (Exception e) {
            log.error("批量智能派单失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取志愿者待审批的申请列表
     */
    @GetMapping("/volunteer-applications")
    @ApiOperation("获取志愿者待审批申请")
    public Result<List<Map<String, Object>>> getVolunteerApplications() {
        log.info("获取志愿者待审批申请");
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DispatchTask> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(DispatchTask::getStatus, DispatchTask.STATUS_VOLUNTEER_PENDING)
                .orderByDesc(DispatchTask::getCreateTime);
        List<DispatchTask> tasks = dispatchTaskMapper.selectList(wrapper);
        
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (DispatchTask task : tasks) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("task", task);
            Responder responder = responderMapper.selectById(task.getResponderId());
            if (responder != null) {
                item.put("responderName", responder.getName());
                item.put("responderPosition", responder.getPosition());
            }
            Incident incident = incidentMapper.selectById(task.getIncidentId());
            if (incident != null) {
                item.put("incidentSummary", incident.getSummary());
                item.put("disasterType", incident.getDisasterType());
            }
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 审批志愿者申请 - 同意
     */
    @PostMapping("/volunteer-applications/{taskId}/approve")
    @ApiOperation("同意志愿者申请")
    public Result<String> approveVolunteerApplication(@PathVariable Long taskId) {
        log.info("同意志愿者申请，任务ID：{}", taskId);
        try {
            DispatchTask task = dispatchTaskMapper.selectById(taskId);
            if (task == null || !DispatchTask.STATUS_VOLUNTEER_PENDING.equals(task.getStatus())) {
                return Result.error("申请不存在或已处理");
            }
            LocalDateTime now = LocalDateTime.now();
            
            // 更新派单任务状态为已接受
            task.setStatus(DispatchTask.STATUS_ACCEPTED);
            dispatchTaskMapper.updateById(task);
            
            // 更新事件状态为处理中
            Incident incident = incidentMapper.selectById(task.getIncidentId());
            if (incident != null) {
                incident.setStatus(Incident.STATUS_PROCESSING);
                incident.setResponderId(task.getResponderId());
                incident.setUpdateTime(now);
                incidentMapper.updateById(incident);
            }
            
            // 更新执勤人员状态为执行中
            Responder responder = responderMapper.selectById(task.getResponderId());
            if (responder != null) {
                responder.setStatus(Responder.STATUS_EXECUTING);
                responder.setCurrentIncidentId(task.getIncidentId());
                responder.setUpdateTime(now);
                responderMapper.updateById(responder);
            }
            
            return Result.success("已同意志愿者申请");
        } catch (Exception e) {
            log.error("审批志愿者申请失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审批志愿者申请 - 驳回
     */
    @PostMapping("/volunteer-applications/{taskId}/reject")
    @ApiOperation("驳回志愿者申请")
    public Result<String> rejectVolunteerApplication(@PathVariable Long taskId) {
        log.info("驳回志愿者申请，任务ID：{}", taskId);
        try {
            DispatchTask task = dispatchTaskMapper.selectById(taskId);
            if (task == null || !DispatchTask.STATUS_VOLUNTEER_PENDING.equals(task.getStatus())) {
                return Result.error("申请不存在或已处理");
            }
            
            task.setStatus(DispatchTask.STATUS_REJECTED);
            task.setRejectReason("管理员驳回志愿者申请");
            dispatchTaskMapper.updateById(task);
            
            return Result.success("已驳回志愿者申请");
        } catch (Exception e) {
            log.error("驳回志愿者申请失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}