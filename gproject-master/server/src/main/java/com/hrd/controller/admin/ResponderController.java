package com.hrd.controller.admin;

import com.hrd.entity.Responder;
import com.hrd.result.Result;
import com.hrd.service.ResponderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 执勤人员管理控制器
 * 负责处理执勤人员的查询、状态更新等操作
 */
@RestController
@RequestMapping("/admin/responder")
@Slf4j
@Api(tags = "执勤人员管理")
@Controller("adminResponderController")
public class ResponderController {

    @Autowired
    private ResponderService responderService;

    /**
     * 查询所有执勤人员
     * @return 执勤人员列表
     */
    @GetMapping("/list")
    @ApiOperation("查询所有执勤人员")
    public Result<List<Responder>> list() {
        log.info("查询所有执勤人员");
        List<Responder> responders = responderService.list();
        return Result.success(responders);
    }

    /**
     * 查询空闲状态的执勤人员
     * 空闲状态的人员可以被派单
     * @return 空闲人员列表
     */
    @GetMapping("/idle")
    @ApiOperation("查询空闲执勤人员")
    public Result<List<Responder>> listIdle() {
        log.info("查询空闲执勤人员");
        List<Responder> responders = responderService.listByStatus(Responder.STATUS_IDLE);
        return Result.success(responders);
    }

    /**
     * 更新执勤人员状态
     * @param id 执勤人员ID
     * @param status 新状态：0-离线,1-空闲,2-已派单,3-执行中
     * @return 操作结果
     */
    @PutMapping("/status/{id}")
    public Result updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        log.info("更新执勤人员状态，ID：{}，新状态：{}", id, status);
        responderService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 统计各状态人数
     * 返回离线、空闲、已派单、执行中各状态的人数
     * @return 各状态人数统计
     */
    @GetMapping("/count")
    public Result<Map<String, Integer>> countByStatus() {
        log.info("统计各状态人数");
        Map<String, Integer> countMap = responderService.countByStatusMap();
        return Result.success(countMap);
    }
}
