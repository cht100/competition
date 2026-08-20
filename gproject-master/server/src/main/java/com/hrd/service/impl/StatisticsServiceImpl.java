package com.hrd.service.impl;

import com.alibaba.fastjson.JSON;
import com.hrd.entity.DispatchTask;
import com.hrd.entity.Incident;
import com.hrd.entity.Message;
import com.hrd.entity.Responder;
import com.hrd.mapper.DispatchTaskMapper;
import com.hrd.mapper.IncidentMapper;
import com.hrd.mapper.MessageMapper;
import com.hrd.mapper.ResponderMapper;
import com.hrd.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 统计数据服务实现类
 * 实现各类统计数据的计算和查询
 */
@Service
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private IncidentMapper incidentMapper;
    
    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;

    @Autowired
    private ResponderMapper responderMapper;

    /**
     * 获取今日统计数据
     */
    @Override
    public Map<String, Object> getTodayStatistics() {
        Map<String, Object> result = new HashMap<>();

        // 获取今日开始时间
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        // 查询今日事件总数
        List<Incident> todayIncidents = incidentMapper.selectList(null);
        long todayCount = todayIncidents.stream()
                .filter(i -> i.getCreateTime() != null && i.getCreateTime().isAfter(todayStart))
                .count();

        // 统计各状态数量
        long pendingCount = todayIncidents.stream()
                .filter(i -> i.getCreateTime() != null && i.getCreateTime().isAfter(todayStart))
                .filter(i -> i.getStatus() != null && i.getStatus() == Incident.STATUS_PENDING)
                .count();

        long confirmedCount = todayIncidents.stream()
                .filter(i -> i.getCreateTime() != null && i.getCreateTime().isAfter(todayStart))
                .filter(i -> i.getStatus() != null && i.getStatus() >= Incident.STATUS_CONFIRMED && i.getStatus() <= Incident.STATUS_COMPLETED)
                .count();

        long completedCount = todayIncidents.stream()
                .filter(i -> i.getCreateTime() != null && i.getCreateTime().isAfter(todayStart))
                .filter(i -> i.getStatus() != null && i.getStatus() == Incident.STATUS_COMPLETED)
                .count();

        long rejectedCount = todayIncidents.stream()
                .filter(i -> i.getCreateTime() != null && i.getCreateTime().isAfter(todayStart))
                .filter(i -> i.getStatus() != null && i.getStatus() == Incident.STATUS_REJECTED)
                .count();

        // 总事件数（不限今日）
        long totalCount = todayIncidents.size();

        // 谣言消息数量（消息状态为RUMOR）
        List<Message> allMessages = messageMapper.selectList(null);
        long rumorMessageCount = allMessages.stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == Message.STATUS_RUMOR)
                .count();

        // 谣言总数 = AI谣言消息 + 管理员驳回的事件
        long rumorTotalCount = rumorMessageCount + rejectedCount;

        // 计算完成率
        double completedRate = todayCount > 0 ? (double) completedCount / todayCount * 100 : 0;

        result.put("todayCount", todayCount);
        result.put("pendingCount", pendingCount);
        result.put("confirmedCount", confirmedCount);
        result.put("completedCount", completedCount);
        result.put("rejectedCount", rejectedCount);
        result.put("totalCount", totalCount);
        result.put("rumorMessageCount", rumorMessageCount);
        result.put("rumorTotalCount", rumorTotalCount);
        result.put("completedRate", String.format("%.1f", completedRate));

        // 动态计算平均处置耗时
        List<DispatchTask> allTasks = dispatchTaskMapper.selectList(null);
        double avgMinutes = calculateAvgProcessTime(
                todayIncidents.stream()
                        .filter(i -> i.getStatus() != null && i.getStatus() == Incident.STATUS_COMPLETED)
                        .filter(i -> i.getCompleteTime() != null && i.getCreateTime() != null)
                        .collect(Collectors.toList()),
                allTasks);
        if (avgMinutes <= 0) {
            result.put("avgProcessTime", "0分钟");
        } else if (avgMinutes < 60) {
            result.put("avgProcessTime", String.format("%.0f分钟", avgMinutes));
        } else {
            result.put("avgProcessTime", String.format("%.1f小时", avgMinutes / 60));
        }

        return result;
    }

    /**
     * 获取事件趋势数据
     */
    @Override
    public List<Map<String, Object>> getTrend(Integer days) {
        log.info("获取事件趋势数据，天数：{}", days);

        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MM-dd");

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        // 直接查询所有事件在Java端按日期分组统计，避免SQL日期比较和@MapKey类型问题
        List<Incident> allIncidents = incidentMapper.selectList(null);

        for (int i = 0; i < days; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            String displayDate = currentDate.format(displayFormatter);

            long count = allIncidents.stream()
                    .filter(inc -> inc.getCreateTime() != null)
                    .filter(inc -> inc.getCreateTime().toLocalDate().equals(currentDate))
                    .count();

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", displayDate);
            dayData.put("count", (int) count);
            result.add(dayData);
        }

        log.info("事件趋势数据查询完成，返回 {} 天数据", result.size());
        return result;
    }

    /**
     * 获取处置效率数据
     */
    @Override
    public Map<String, Object> getEfficiency() {
        Map<String, Object> result = new HashMap<>();

        // 查询所有已完成的事件
        List<Incident> completedIncidents = incidentMapper.selectList(null).stream()
                .filter(i -> i.getStatus() != null && i.getStatus() == Incident.STATUS_COMPLETED)
                .filter(i -> i.getCreateTime() != null && i.getCompleteTime() != null)
                .collect(Collectors.toList());

        // 查询所有派单任务
        List<DispatchTask> allDispatchTasks = dispatchTaskMapper.selectList(null);

        // 计算平均响应时间（从事件创建到派单的时间）
        double avgResponseTime = calculateAvgResponseTime(completedIncidents, allDispatchTasks);
        
        // 计算平均处理时间（从派单到完成的时间）
        double avgProcessTime = calculateAvgProcessTime(completedIncidents, allDispatchTasks);
        
        // 计算完成率
        double completedRate = calculateCompletedRate();
        
        // 计算派单率
        double dispatchRate = calculateDispatchRate();
        
        // 计算各时段效率
        List<Map<String, Object>> hourlyEfficiency = calculateHourlyEfficiency();

        result.put("avgResponseTime", formatTime(avgResponseTime));
        result.put("avgProcessTime", formatTime(avgProcessTime));
        result.put("completedRate", String.format("%.1f%%", completedRate));
        result.put("dispatchRate", String.format("%.1f%%", dispatchRate));
        result.put("hourlyEfficiency", hourlyEfficiency);
        result.put("totalCompleted", completedIncidents.size());
        result.put("totalDispatched", allDispatchTasks.size());

        return result;
    }

    /**
     * 计算平均响应时间
     */
    private double calculateAvgResponseTime(List<Incident> completedIncidents, List<DispatchTask> allDispatchTasks) {
        if (completedIncidents.isEmpty()) return 0;
        
        double totalResponseTime = 0;
        int count = 0;
        
        for (Incident incident : completedIncidents) {
            // 查找该事件的最早派单时间
            Optional<DispatchTask> earliestDispatch = allDispatchTasks.stream()
                    .filter(task -> task.getIncidentId().equals(incident.getId()))
                    .min((t1, t2) -> t1.getCreateTime().compareTo(t2.getCreateTime()));
                    
            if (earliestDispatch.isPresent()) {
                long responseMinutes = java.time.Duration.between(
                        incident.getCreateTime(), earliestDispatch.get().getCreateTime()
                ).toMinutes();
                
                if (responseMinutes > 0) {
                    totalResponseTime += responseMinutes;
                    count++;
                }
            }
        }
        
        return count > 0 ? totalResponseTime / count : 0;
    }

    /**
     * 计算平均处理时间
     */
    private double calculateAvgProcessTime(List<Incident> completedIncidents, List<DispatchTask> allDispatchTasks) {
        if (completedIncidents.isEmpty()) return 0;
        
        double totalProcessTime = 0;
        int count = 0;
        
        for (Incident incident : completedIncidents) {
            // 查找该事件的派单任务
            Optional<DispatchTask> dispatchTask = allDispatchTasks.stream()
                    .filter(task -> task.getIncidentId().equals(incident.getId()))
                    .filter(task -> task.getStatus() == DispatchTask.STATUS_ACCEPTED || 
                                   task.getStatus() == DispatchTask.STATUS_COMPLETED)
                    .findFirst();
                    
            if (dispatchTask.isPresent() && incident.getCompleteTime() != null && 
                dispatchTask.get().getCreateTime() != null) {
                long processMinutes = java.time.Duration.between(
                        dispatchTask.get().getCreateTime(), incident.getCompleteTime()
                ).toMinutes();
                
                if (processMinutes > 0) {
                    totalProcessTime += processMinutes;
                    count++;
                }
            } else if (incident.getCompleteTime() != null && incident.getCreateTime() != null) {
                // 无派单任务时，使用事件创建到完成的时间差
                long processMinutes = java.time.Duration.between(
                        incident.getCreateTime(), incident.getCompleteTime()
                ).toMinutes();
                
                if (processMinutes > 0) {
                    totalProcessTime += processMinutes;
                    count++;
                }
            }
        }
        
        return count > 0 ? totalProcessTime / count : 0;
    }

    /**
     * 计算完成率
     */
    private double calculateCompletedRate() {
        List<Incident> allIncidents = incidentMapper.selectList(null);
        if (allIncidents.isEmpty()) return 0;
        
        long completedCount = allIncidents.stream()
                .filter(i -> i.getStatus() != null && i.getStatus() == Incident.STATUS_COMPLETED)
                .count();
        
        return (double) completedCount / allIncidents.size() * 100;
    }

    /**
     * 计算派单率
     */
    private double calculateDispatchRate() {
        List<Incident> allIncidents = incidentMapper.selectList(null);
        if (allIncidents.isEmpty()) return 0;
        
        long dispatchedCount = allIncidents.stream()
                .filter(i -> i.getStatus() != null && 
                           (i.getStatus() == Incident.STATUS_DISPATCHED || 
                            i.getStatus() == Incident.STATUS_PROCESSING || 
                            i.getStatus() == Incident.STATUS_COMPLETED))
                .count();
        
        return (double) dispatchedCount / allIncidents.size() * 100;
    }

    /**
     * 计算各时段效率
     */
    private List<Map<String, Object>> calculateHourlyEfficiency() {
        List<Map<String, Object>> hourlyEfficiency = new ArrayList<>();
        
        // 查询最近7天的事件数据
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Incident> recentIncidents = incidentMapper.selectList(null).stream()
                .filter(i -> i.getCreateTime() != null && i.getCreateTime().isAfter(sevenDaysAgo))
                .collect(Collectors.toList());
        
        // 按小时统计事件数量
        int[] hourlyCounts = new int[24];
        for (Incident incident : recentIncidents) {
            int hour = incident.getCreateTime().getHour();
            hourlyCounts[hour]++;
        }
        
        // 生成各时段效率数据
        for (int i = 0; i < 24; i++) {
            Map<String, Object> hourData = new HashMap<>();
            hourData.put("hour", i);
            hourData.put("count", hourlyCounts[i]);
            hourData.put("efficiency", calculateHourlyEfficiency(i, recentIncidents));
            hourlyEfficiency.add(hourData);
        }
        
        return hourlyEfficiency;
    }

    /**
     * 计算指定时段的效率
     */
    private double calculateHourlyEfficiency(int hour, List<Incident> incidents) {
        // 计算该时段内完成的事件比例作为效率指标
        long hourlyCompleted = incidents.stream()
                .filter(i -> i.getCreateTime().getHour() == hour)
                .filter(i -> i.getStatus() == Incident.STATUS_COMPLETED)
                .count();
                
        long hourlyTotal = incidents.stream()
                .filter(i -> i.getCreateTime().getHour() == hour)
                .count();
                
        return hourlyTotal > 0 ? (double) hourlyCompleted / hourlyTotal * 100 : 0;
    }

    /**
     * 格式化时间显示
     */
    private String formatTime(double minutes) {
        if (minutes < 60) {
            return String.format("%.0f分钟", minutes);
        } else {
            double hours = minutes / 60;
            if (hours < 24) {
                return String.format("%.1f小时", hours);
            } else {
                double days = hours / 24;
                return String.format("%.1f天", days);
            }
        }
    }

    /**
     * 获取谣言占比数据
     */
    @Override
    public Map<String, Object> getRumorRatio() {
        Map<String, Object> result = new HashMap<>();

        // 查询所有消息
        List<Message> allMessages = messageMapper.selectList(null);
        
        // 只统计已处理的消息数（status > 0，即已推送到前端经过处理的消息）
        long processedTotal = allMessages.stream()
                .filter(m -> m.getStatus() != null && m.getStatus() > Message.STATUS_TO_CLEAN)
                .count();
        
        // 统计谣言数量（状态为STATUS_RUMOR）
        long rumorCount = allMessages.stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == Message.STATUS_RUMOR)
                .count();

        // 统计管理员驳回的事件数量
        List<Incident> allIncidents = incidentMapper.selectList(null);
        long rejectedIncidentCount = allIncidents.stream()
                .filter(i -> i.getStatus() != null && i.getStatus() == Incident.STATUS_REJECTED)
                .count();

        // 谣言总数 = AI识别谣言消息 + 管理员驳回事件
        long totalRumor = rumorCount + rejectedIncidentCount;
        
        // 统计已验证的真实事件数量（已聚合的消息）
        long verifiedCount = allMessages.stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == Message.STATUS_AGGREGATED)
                .count();
        
        // 统计待处理数量（已清洗但未研判的消息）
        long pendingCount = allMessages.stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == Message.STATUS_CLEANED)
                .count();
        
        // 计算谣言占比（谣言总数 / 已处理消息数）
        double rumorRatio = processedTotal > 0 ? (double) totalRumor / processedTotal * 100 : 0;
        
        result.put("total", (int) processedTotal);
        result.put("rumor", (int) totalRumor);
        result.put("rumorMessages", (int) rumorCount);
        result.put("rejectedIncidents", (int) rejectedIncidentCount);
        result.put("verified", (int) verifiedCount);
        result.put("pending", (int) pendingCount);
        result.put("rumorRatio", String.format("%.1f%%", rumorRatio));

        //暂时无法获取谣言类型占比
        result.put("rumorTypes", null);

        return result;
    }

    /**
     * 获取执勤人员效率排名
     */
    @Override
    public List<Map<String, Object>> getResponderRanking() {
        List<Map<String, Object>> result = new ArrayList<>();

        // 查询所有执勤人员
        List<Responder> allResponders = responderMapper.selectList(null);
        
        // 查询所有已完成的事件
        List<Incident> completedIncidents = incidentMapper.selectList(null).stream()
                .filter(i -> i.getStatus() != null && i.getStatus() == Incident.STATUS_COMPLETED)
                .filter(i -> i.getResponderId() != null)
                .collect(Collectors.toList());

        // 查询所有派单任务
        List<DispatchTask> allDispatchTasks = dispatchTaskMapper.selectList(null);

        // 统计每个执勤人员的处理数据
        for (Responder responder : allResponders) {
            Map<String, Object> responderData = new HashMap<>();
            
            // 统计该执勤人员完成的事件数量
            long completedCount = completedIncidents.stream()
                    .filter(i -> i.getResponderId().equals(responder.getId()))
                    .count();
            
            // 统计该执勤人员的派单任务数量
            long totalTasks = allDispatchTasks.stream()
                    .filter(task -> task.getResponderId().equals(responder.getId()))
                    .count();
            
            // 计算平均处理时间
            double avgProcessTime = calculateResponderAvgProcessTime(responder.getId(), completedIncidents, allDispatchTasks);
            
            // 计算完成率
            double completionRate = totalTasks > 0 ? (double) completedCount / totalTasks * 100 : 0;
            
            responderData.put("responderId", responder.getId());
            responderData.put("responderName", responder.getName());
            responderData.put("position", responder.getPosition());
            responderData.put("completedCount", (int) completedCount);
            responderData.put("totalTasks", (int) totalTasks);
            responderData.put("completionRate", String.format("%.1f%%", completionRate));
            responderData.put("avgProcessTime", formatTime(avgProcessTime));
            
            result.add(responderData);
        }

        // 按完成数量降序排序
        result.sort((a, b) -> {
            int countA = (int) a.get("completedCount");
            int countB = (int) b.get("completedCount");
            return Integer.compare(countB, countA);
        });

        return result;
    }

    /**
     * 计算执勤人员的平均处理时间
     */
    private double calculateResponderAvgProcessTime(Long responderId, List<Incident> completedIncidents, List<DispatchTask> allDispatchTasks) {
        List<Incident> responderIncidents = completedIncidents.stream()
                .filter(i -> i.getResponderId().equals(responderId))
                .collect(Collectors.toList());
        
        if (responderIncidents.isEmpty()) return 0;
        
        double totalProcessTime = 0;
        int count = 0;
        
        for (Incident incident : responderIncidents) {
            Optional<DispatchTask> dispatchTask = allDispatchTasks.stream()
                    .filter(task -> task.getIncidentId().equals(incident.getId()))
                    .filter(task -> task.getResponderId().equals(responderId))
                    .filter(task -> task.getStatus() == DispatchTask.STATUS_ACCEPTED || 
                                   task.getStatus() == DispatchTask.STATUS_COMPLETED)
                    .findFirst();
                    
            if (dispatchTask.isPresent() && incident.getCompleteTime() != null && 
                dispatchTask.get().getCreateTime() != null) {
                long processMinutes = java.time.Duration.between(
                        dispatchTask.get().getCreateTime(), incident.getCompleteTime()
                ).toMinutes();
                
                if (processMinutes > 0) {
                    totalProcessTime += processMinutes;
                    count++;
                }
            } else if (incident.getCompleteTime() != null && incident.getCreateTime() != null) {
                // 无派单任务时，使用事件创建到完成的时间差
                long processMinutes = java.time.Duration.between(
                        incident.getCreateTime(), incident.getCompleteTime()
                ).toMinutes();
                
                if (processMinutes > 0) {
                    totalProcessTime += processMinutes;
                    count++;
                }
            }
        }
        
        return count > 0 ? totalProcessTime / count : 0;
    }

    /**
     * 获取灾种类型统计
     */
    @Override
    public Map<String, Object> getDisasterTypeStatistics() {
        Map<String, Object> result = new HashMap<>();

        // 查询所有事件
        List<Incident> allIncidents = incidentMapper.selectList(null);
        
        // 按灾种类型统计事件数量
        Map<String, Long> disasterTypeCounts = allIncidents.stream()
                .filter(i -> i.getDisasterType() != null && !i.getDisasterType().isEmpty())
                .collect(Collectors.groupingBy(Incident::getDisasterType, Collectors.counting()));
        
        // 按灾种类型统计完成率
        Map<String, Double> disasterTypeCompletionRates = new HashMap<>();
        for (String disasterType : disasterTypeCounts.keySet()) {
            long total = allIncidents.stream()
                    .filter(i -> disasterType.equals(i.getDisasterType()))
                    .count();
            
            long completed = allIncidents.stream()
                    .filter(i -> disasterType.equals(i.getDisasterType()))
                    .filter(i -> i.getStatus() == Incident.STATUS_COMPLETED)
                    .count();
            
            double completionRate = total > 0 ? (double) completed / total * 100 : 0;
            disasterTypeCompletionRates.put(disasterType, completionRate);
        }
        
        // 按事件数量降序排序
        List<Map<String, Object>> disasterTypeList = new ArrayList<>();
        for (Map.Entry<String, Long> entry : disasterTypeCounts.entrySet()) {
            Map<String, Object> typeData = new HashMap<>();
            typeData.put("disasterType", entry.getKey());
            typeData.put("count", entry.getValue());
            typeData.put("completionRate", String.format("%.1f%%", disasterTypeCompletionRates.get(entry.getKey())));
            disasterTypeList.add(typeData);
        }
        
        disasterTypeList.sort((a, b) -> {
            long countA = (long) a.get("count");
            long countB = (long) b.get("count");
            return Long.compare(countB, countA);
        });
        
        result.put("disasterTypes", disasterTypeList);
        result.put("totalTypes", disasterTypeCounts.size());
        result.put("totalIncidents", allIncidents.size());
        
        return result;
    }
}
