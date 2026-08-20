package com.hrd.controller.responder;

import com.hrd.context.BaseContext;
import com.hrd.dto.AcceptTaskDTO;
import com.hrd.dto.SubmitFeedbackDTO;
import com.hrd.entity.Incident;
import com.hrd.entity.Responder;
import com.hrd.result.Result;
import com.hrd.service.IncidentService;
import com.hrd.service.ResponderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 执勤人员端事件控制器
 * 处理执勤人员对事件的操作，如查看任务、接受任务、提交反馈等
 */
@RestController
@RequestMapping("/responder/incidents")
@Slf4j
@Api(tags = "执勤人员端事件管理")
public class ResponderIncidentController {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private ResponderService responderService;

    /**
     * 获取可接受的事件列表
     * @return 可接受的事件列表
     */
    @GetMapping("/available")
    @ApiOperation("获取可接受的事件列表")
    public Result<List<Incident>> getAvailableTasks() {
        log.info("获取可接受的事件列表");
        
        List<Incident> incidents = incidentService.listAcceptable();
        return Result.success(incidents);
    }

    /**
     * 获取当前执勤人员正在处理的事件
     * @return 正在处理的事件
     */
    @GetMapping("/current")
    @ApiOperation("获取当前执勤人员正在处理的事件")
    public Result<Incident> getCurrentTask() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取当前执勤人员正在处理的事件，用户ID：{}", userId);
        
        // 根据用户ID获取执勤人员信息
        Responder responder = responderService.getByUserId(userId);
        if (responder == null) {
            return Result.error("未找到对应的执勤人员信息");
        }
        
        Incident incident = incidentService.getProcessingIncidentByResponderId(responder.getId());
        return Result.success(incident);
    }

    /**
     * 接受任务
     * @param acceptTaskDTO 接受任务参数
     * @return 操作结果
     */
    @PostMapping("/accept")
    @ApiOperation("接受任务")
    public Result<String> acceptTask(@RequestBody AcceptTaskDTO acceptTaskDTO) {
        Long userId = BaseContext.getCurrentId();
        log.info("执勤人员接受任务，用户ID：{}，事件ID：{}", userId, acceptTaskDTO.getIncidentId());
        
        // 根据用户ID获取执勤人员信息
        Responder responder = responderService.getByUserId(userId);
        if (responder == null) {
            return Result.error("未找到对应的执勤人员信息");
        }
        
        try {
            incidentService.acceptTask(responder.getId(), acceptTaskDTO);
            return Result.success("任务接受成功");
        } catch (Exception e) {
            log.error("接受任务失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 提交反馈
     * @param submitFeedbackDTO 提交反馈参数
     * @return 操作结果
     */
    @PostMapping("/feedback")
    @ApiOperation("提交反馈")
    public Result<String> submitFeedback(@RequestBody SubmitFeedbackDTO submitFeedbackDTO) {
        Long userId = BaseContext.getCurrentId();
        log.info("执勤人员提交反馈，用户ID：{}，事件ID：{}", userId, submitFeedbackDTO.getIncidentId());
        
        // 根据用户ID获取执勤人员信息
        Responder responder = responderService.getByUserId(userId);
        if (responder == null) {
            return Result.error("未找到对应的执勤人员信息");
        }
        
        try {
            incidentService.submitFeedback(responder.getId(), submitFeedbackDTO);
            return Result.success("反馈提交成功");
        } catch (Exception e) {
            log.error("提交反馈失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前执勤人员关联的所有事件（用于地图展示）
     * @return 关联的事件列表
     */
    @GetMapping("/my-incidents")
    @ApiOperation("获取当前执勤人员关联的所有事件")
    public Result<List<Incident>> getMyIncidents() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取执勤人员关联事件，用户ID：{}", userId);

        Responder responder = responderService.getByUserId(userId);
        if (responder == null) {
            return Result.error("未找到对应的执勤人员信息");
        }

        List<Incident> incidents = incidentService.getIncidentsByResponderId(responder.getId());
        return Result.success(incidents);
    }
}