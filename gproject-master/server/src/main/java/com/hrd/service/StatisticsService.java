package com.hrd.service;

import java.util.List;
import java.util.Map;

/**
 * 统计数据服务接口
 * 负责提供各类统计数据
 */
public interface StatisticsService {

    /**
     * 获取今日统计数据
     * 包括今日事件总数、已处理数、处理中数、待处理数等
     * @return 今日统计数据
     */
    Map<String, Object> getTodayStatistics();

    /**
     * 获取事件趋势数据
     * 返回最近一段时间内每天的事件数量
     * @param days 查询天数
     * @return 事件趋势数据
     */
    List<Map<String, Object>> getTrend(Integer days);

    /**
     * 获取处置效率数据
     * 包括平均响应时间、平均处理时间、完成率等
     * @return 处置效率数据
     */
    Map<String, Object> getEfficiency();

    /**
     * 获取谣言占比数据
     * 返回真实事件与谣言的比例
     * @return 谣言占比数据
     */
    Map<String, Object> getRumorRatio();

    /**
     * 获取执勤人员效率排名
     * 返回执勤人员处理事件的数量和效率排名
     * @return 执勤人员效率排名数据
     */
    List<Map<String, Object>> getResponderRanking();

    /**
     * 获取灾种类型统计
     * 返回各灾种类型的事件数量分布
     * @return 灾种类型统计数据
     */
    Map<String, Object> getDisasterTypeStatistics();
}
