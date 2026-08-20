package com.hrd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrd.entity.Incident;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 事件Mapper接口
 * 用于事件数据的持久化操作
 */
@Mapper
public interface IncidentMapper extends BaseMapper<Incident> {

    /**
     * 按状态统计事件数量
     * @return 各状态的事件数量列表
     */
    @Select("SELECT status, COUNT(*) as count FROM incidents GROUP BY status")
    List<Map<String, Object>> countByStatus();

    /**
     * 按灾种统计事件数量
     * @return 各灾种的事件数量列表
     */
    @Select("SELECT disaster_type, COUNT(*) as count FROM incidents GROUP BY disaster_type")
    List<Map<String, Object>> countByDisasterType();

    /**
     * 查询待处理事件（待审核或已确认未派单）
     * @return 待处理事件列表
     */
    @Select("SELECT * FROM incidents WHERE status IN (0, 1) ORDER BY create_time DESC")
    List<Incident> findPendingIncidents();

    /**
     * 按日期范围统计事件数量
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日事件数量统计
     */
    @MapKey("date")
    Map<String, Integer> countByDate(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 根据执勤人员ID查询关联的事件（通过派单任务表关联）
     * @param responderId 执勤人员ID
     * @return 关联的事件列表
     */
    @Select("SELECT DISTINCT i.* FROM incidents i JOIN dispatch_tasks dt ON i.id = dt.incident_id WHERE dt.responder_id = #{responderId} ORDER BY i.create_time DESC")
    List<Incident> findByResponderId(@Param("responderId") Long responderId);
}
