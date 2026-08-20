package com.hrd.controller.admin;

import com.hrd.result.Result;
import com.hrd.service.ClusteringService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 聚合可视化控制器
 * 提供聚合过程的可视化数据接口
 */
@RestController
@RequestMapping("/admin/aggregation")
@Slf4j
@Api(tags = "聚合可视化")
public class AggregationVisualController {

    @Autowired
    private ClusteringService clusteringService;

    @GetMapping("/visual")
    @ApiOperation("获取聚合可视化数据")
    public Result<String> getVisualData() {
        log.info("【聚合可视化】获取当前聚合可视化数据");
        return Result.success("聚合可视化功能已启用，请通过WebSocket接收实时数据");
    }

    @PostMapping("/simulate/{messageId}")
    @ApiOperation("模拟聚合过程")
    public Result<String> simulateAggregation(@PathVariable Long messageId) {
        log.info("【聚合可视化】模拟聚合过程，消息ID：{}", messageId);
        return Result.success("聚合过程已开始，请查看WebSocket实时推送数据");
    }
}