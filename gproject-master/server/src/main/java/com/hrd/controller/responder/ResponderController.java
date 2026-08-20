package com.hrd.controller.responder;

import com.hrd.context.BaseContext;
import com.hrd.dto.ResponderUpdateDTO;
import com.hrd.entity.Responder;
import com.hrd.result.Result;
import com.hrd.service.ResponderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 执勤人员控制器
 * 处理执勤人员自己的操作，如更新状态、岗位等
 */
@RestController
@RequestMapping("/responder")
@Slf4j
@Api(tags = "执勤人员操作")
@Controller("responderResponderController")
public class ResponderController {

    @Autowired
    private ResponderService responderService;

    /**
     * 获取当前执勤人员信息
     * @return 执勤人员信息
     */
    @GetMapping("/info")
    @ApiOperation("获取当前执勤人员信息")
    public Result<Responder> getCurrentInfo() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取当前执勤人员信息，用户ID：{}", userId);
        
        Responder responder = responderService.getByUserId(userId);
        if (responder == null) {
            return Result.error("未找到对应的执勤人员信息");
        }
        
        return Result.success(responder);
    }

    /**
     * 执勤人员更新自己的信息
     * 可以更新状态、岗位、手机号等
     * @param updateDTO 更新信息
     * @return 操作结果
     */
    @PutMapping("/info")
    @ApiOperation("执勤人员更新自己的信息")
    public Result<String> updateSelfInfo(@RequestBody ResponderUpdateDTO updateDTO) {
        Long userId = BaseContext.getCurrentId();
        log.info("执勤人员更新自己的信息，用户ID：{}，更新内容：{}", userId, updateDTO);
        
        // 根据用户ID获取执勤人员信息
        Responder responder = responderService.getByUserId(userId);
        if (responder == null) {
            return Result.error("未找到对应的执勤人员信息");
        }
        
        try {
            responderService.updateSelfInfo(responder.getId(), updateDTO);
            return Result.success("信息更新成功");
        } catch (Exception e) {
            log.error("执勤人员更新信息失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 执勤人员上线（设置为空闲状态）
     * @return 操作结果
     */
    @PostMapping("/online")
    @ApiOperation("执勤人员上线")
    public Result<String> goOnline() {
        Long userId = BaseContext.getCurrentId();
        log.info("执勤人员上线，用户ID：{}", userId);
        
        ResponderUpdateDTO updateDTO = new ResponderUpdateDTO();
        updateDTO.setStatus(Responder.STATUS_IDLE); // 设置为空闲状态
        
        return updateSelfInfo(updateDTO);
    }

    /**
     * 执勤人员下线（设置为离线状态）
     * @return 操作结果
     */
    @PostMapping("/offline")
    @ApiOperation("执勤人员下线")
    public Result<String> goOffline() {
        Long userId = BaseContext.getCurrentId();
        log.info("执勤人员下线，用户ID：{}", userId);
        
        ResponderUpdateDTO updateDTO = new ResponderUpdateDTO();
        updateDTO.setStatus(Responder.STATUS_OFFLINE); // 设置为离线状态
        
        return updateSelfInfo(updateDTO);
    }
}