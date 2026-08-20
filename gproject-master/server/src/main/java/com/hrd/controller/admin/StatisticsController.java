package com.hrd.controller.admin;

import com.hrd.result.Result;
import com.hrd.service.StatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统计数据控制器
 * 负责提供各类统计数据接口
 */
@RestController
@RequestMapping("/admin/statistics")
@Slf4j
@Api(tags = "统计管理")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 获取今日统计数据
     * 包括今日事件总数、已处理数、处理中数、待处理数等
     * @return 今日统计数据
     */
    @GetMapping("/today")
    @ApiOperation("获取今日统计数据")
    public Result<Map<String, Object>> getTodayStatistics() {
        log.info("获取今日统计数据");
        Map<String, Object> statistics = statisticsService.getTodayStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取事件趋势数据
     * 返回最近一段时间内每天的事件数量
     * @param days 查询天数，默认7天
     * @return 事件趋势数据
     */
    @GetMapping("/trend")
    @ApiOperation("获取事件趋势数据")
    public Result<List<Map<String, Object>>> getTrend(@RequestParam(defaultValue = "7") Integer days) {
        log.info("获取事件趋势数据，天数：{}", days);
        List<Map<String, Object>> trend = statisticsService.getTrend(days);
        return Result.success(trend);
    }

    /**
     * 获取处置效率数据
     * 包括平均响应时间、平均处理时间、完成率、派单率等
     * @return 处置效率数据
     */
    @GetMapping("/efficiency")
    @ApiOperation("获取处置效率数据")
    public Result<Map<String, Object>> getEfficiency() {
        log.info("获取处置效率数据");
        Map<String, Object> efficiency = statisticsService.getEfficiency();
        return Result.success(efficiency);
    }

    /**
     * 获取谣言占比数据
     * 返回真实事件与谣言的比例
     * @return 谣言占比数据
     */
    @GetMapping("/rumor-ratio")
    @ApiOperation("获取谣言占比数据")
    public Result<Map<String, Object>> getRumorRatio() {
        log.info("获取谣言占比数据");
        Map<String, Object> ratio = statisticsService.getRumorRatio();
        return Result.success(ratio);
    }

    /**
     * 获取执勤人员效率排名
     * 返回执勤人员处理事件的数量和效率排名
     * @return 执勤人员效率排名数据
     */
    @GetMapping("/responder-ranking")
    @ApiOperation("获取执勤人员效率排名")
    public Result<List<Map<String, Object>>> getResponderRanking() {
        log.info("获取执勤人员效率排名");
        List<Map<String, Object>> ranking = statisticsService.getResponderRanking();
        return Result.success(ranking);
    }

    /**
     * 获取灾种类型统计
     * 返回各灾种类型的事件数量分布
     * @return 灾种类型统计数据
     */
    @GetMapping("/disaster-types")
    @ApiOperation("获取灾种类型统计")
    public Result<Map<String, Object>> getDisasterTypeStatistics() {
        log.info("获取灾种类型统计");
        Map<String, Object> statistics = statisticsService.getDisasterTypeStatistics();
        return Result.success(statistics);
    }
}
