package com.hrd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrd.entity.ReviewLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 审核记录Mapper接口
 * 用于审核记录数据的持久化操作
 */
@Mapper
public interface ReviewLogMapper extends BaseMapper<ReviewLog> {

    /**
     * 按事件ID查询审核记录
     * @param incidentId 事件ID
     * @return 该事件的审核记录列表（按时间倒序）
     */
    @Select("SELECT * FROM review_logs WHERE incident_id = #{incidentId} ORDER BY create_time DESC")
    List<ReviewLog> findByIncidentId(@Param("incidentId") Long incidentId);
}
