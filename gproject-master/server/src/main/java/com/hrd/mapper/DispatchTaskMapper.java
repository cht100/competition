package com.hrd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrd.entity.DispatchTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 派单任务Mapper接口
 */
@Mapper
public interface DispatchTaskMapper extends BaseMapper<DispatchTask> {

    /**
     * 根据事件ID查询派单任务
     * @param incidentId 事件ID
     * @return 派单任务列表
     */
    @Select("SELECT * FROM dispatch_tasks WHERE incident_id = #{incidentId} ORDER BY create_time DESC")
    List<DispatchTask> findByIncidentId(Long incidentId);

    /**
     * 根据执勤人员ID查询活动任务（状态小于4的任务）
     * @param responderId 执勤人员ID
     * @return 活动任务列表
     */
    @Select("SELECT * FROM dispatch_tasks WHERE responder_id = #{responderId} AND status < 4 ORDER BY create_time DESC")
    List<DispatchTask> findActiveByResponderId(Long responderId);
}