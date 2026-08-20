package com.hrd.controller.admin;

import com.hrd.dto.DispatchDTO;
import com.hrd.dto.TaskStatusDTO;
import com.hrd.entity.DispatchTask;
import com.hrd.result.Result;
import com.hrd.service.DispatchTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 派单任务控制器
 * 负责处理派单任务的查询、派单、状态更新等操作
 */
@RestController
@RequestMapping("/admin/task")
@Slf4j
@Api(tags = "派单任务管理")
public class DispatchTaskController {

    @Autowired
    private DispatchTaskService dispatchTaskService;

    /**
     * 按事件ID查询任务
     * 获取指定事件的所有派单任务
     * @param incidentId 事件ID
     * @return 任务列表
     */
    @GetMapping("/incident/{incidentId}")
    @ApiOperation("按事件ID查询任务")
    public Result<List<DispatchTask>> listByIncidentId(@PathVariable @ApiParam(value = "事件ID", example = "1") Long incidentId) {
        log.info("按事件ID查询任务，事件ID：{}", incidentId);
        List<DispatchTask> tasks = dispatchTaskService.listByIncidentId(incidentId);
        return Result.success(tasks);
    }

    /**
     * 按人员ID查询活动任务
     * 获取指定执勤人员当前未完成的任务
     * @param responderId 执勤人员ID
     * @return 任务列表
     */
    @GetMapping("/responder/{responderId}")
    @ApiOperation("按人员ID查询活动任务")
    public Result<List<DispatchTask>> listByResponderId(@PathVariable @ApiParam(value = "执勤人员ID", example = "1") Long responderId) {
        log.info("按人员ID查询活动任务，人员ID：{}", responderId);
        List<DispatchTask> tasks = dispatchTaskService.listActiveByResponderId(responderId);
        return Result.success(tasks);
    }

    /**
     * 派单
     * 为指定事件分配执勤人员
     * @param dispatchDTO 派单请求参数
     * @return 操作结果
     */
    @PostMapping("/dispatch")
    public Result dispatch(@RequestBody DispatchDTO dispatchDTO) {
        log.info("派单，事件ID：{}，人员ID：{}", dispatchDTO.getIncidentId(), dispatchDTO.getResponderId());
        dispatchTaskService.dispatch(dispatchDTO);
        return Result.success();
    }

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param taskStatusDTO 任务状态更新参数
     * @return 操作结果
     */
    @PutMapping("/status/{taskId}")
    public Result updateStatus(@PathVariable Long taskId, @RequestBody TaskStatusDTO taskStatusDTO) {
        log.info("更新任务状态，任务ID：{}，新状态：{}", taskId, taskStatusDTO.getStatus());
        dispatchTaskService.updateStatus(taskId, taskStatusDTO);
        return Result.success();
    }
}
