package com.hrd.controller.admin;

import com.hrd.constant.MessageConstant;
import com.hrd.exception.BaseException;
import com.hrd.result.Result;
import com.hrd.task.TextSimulateTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟数据控制器
 */
@RestController
@RequestMapping("/admin/simulate")
@Slf4j
@Api(tags = "模拟数据管理")
public class SimulateController {

    @Autowired
    private TextSimulateTask textSimulateTask;

    /**
     * 开启模拟消息推送
     */
    @PostMapping("/start")
    @ApiOperation("开启模拟消息推送")
    public Result startSimulate() {
        if (textSimulateTask.isRunning()) {
            throw new BaseException(MessageConstant.IS_RUNNING);
        }
        textSimulateTask.start();
        return Result.success();
    }

    //停止模拟消息推送
    @PostMapping("/stop")
    public Result stopSimulate() {
        if (!textSimulateTask.isRunning()) {
            throw new BaseException(MessageConstant.NOT_RUNNING);
        }
        textSimulateTask.stop();
        return Result.success();
    }
}
