package com.hrd.controller.responder;

import com.hrd.context.BaseContext;
import com.hrd.dto.RespondDispatchDTO;
import com.hrd.entity.DispatchTask;
import com.hrd.entity.Responder;
import com.hrd.result.Result;
import com.hrd.service.DispatchService;
import com.hrd.service.ResponderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 执勤人员端派单控制器
 */
@RestController
@RequestMapping("/responder/dispatch")
@Slf4j
@Api(tags = "执勤人员端派单管理")
public class ResponderDispatchController {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private ResponderService responderService;

    /**
     * 获取待响应的派单任务
     * @return 待响应的派单任务列表
     */
    @GetMapping("/tasks/pending")
    @ApiOperation("获取待响应的派单任务")
    public Result<List<DispatchTask>> getPendingDispatchTasks() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取待响应的派单任务，用户ID：{}", userId);
        
        // 根据用户ID获取执勤人员信息
        Responder responder = responderService.getByUserId(userId);
        if (responder == null) {
            return Result.error("未找到对应的执勤人员信息");
        }
        
        List<DispatchTask> tasks = dispatchService.getPendingDispatchTasks(responder.getId());
        return Result.success(tasks);
    }

    /**
     * 获取当前执勤人员的所有派单任务
     * @return 派单任务列表
     */
    @GetMapping("/tasks")
    @ApiOperation("获取当前执勤人员的所有派单任务")
    public Result<List<DispatchTask>> getDispatchTasks() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取当前执勤人员的所有派单任务，用户ID：{}", userId);
        
        // 根据用户ID获取执勤人员信息
        Responder responder = responderService.getByUserId(userId);
        if (responder == null) {
            return Result.error("未找到对应的执勤人员信息");
        }
        
        List<DispatchTask> tasks = dispatchService.getDispatchTasksByResponderId(responder.getId());
        return Result.success(tasks);
    }

    /**
     * 响应派单任务
     * @param respondDispatchDTO 响应参数
     * @return 操作结果
     */
    @PostMapping("/respond")
    @ApiOperation("响应派单任务")
    public Result<String> respondDispatch(@RequestBody RespondDispatchDTO respondDispatchDTO) {
        Long userId = BaseContext.getCurrentId();
        log.info("响应派单任务，用户ID：{}，派单任务ID：{}", 
                userId, respondDispatchDTO.getDispatchTaskId());
        
        // 根据用户ID获取执勤人员信息
        Responder responder = responderService.getByUserId(userId);
        if (responder == null) {
            return Result.error("未找到对应的执勤人员信息");
        }
        
        try {
            dispatchService.respondDispatch(responder.getId(), respondDispatchDTO);
            
            if (respondDispatchDTO.getAccept()) {
                return Result.success("派单任务接受成功");
            } else {
                return Result.success("派单任务拒绝成功");
            }
        } catch (Exception e) {
            log.error("响应派单任务失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取岗位与灾种的匹配规则
     * @param disasterType 灾种类型
     * @return 匹配的岗位列表
     */
    @GetMapping("/matching-positions/{disasterType}")
    @ApiOperation("获取岗位与灾种的匹配规则")
    public Result<List<String>> getMatchingPositions(@PathVariable String disasterType) {
        log.info("获取岗位与灾种的匹配规则，灾种类型：{}", disasterType);
        
        List<String> positions = dispatchService.getMatchingPositions(disasterType);
        return Result.success(positions);
    }
}