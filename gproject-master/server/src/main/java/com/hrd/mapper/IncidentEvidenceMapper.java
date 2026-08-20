package com.hrd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrd.entity.IncidentEvidence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 事件证据链Mapper接口
 * 用于事件证据链数据的持久化操作
 */
@Mapper
public interface IncidentEvidenceMapper extends BaseMapper<IncidentEvidence> {

    /**
     * 按事件ID查询证据链
     * @param incidentId 事件ID
     * @return 该事件的证据链列表（按顺序排列）
     */
    @Select("SELECT * FROM incident_evidences WHERE incident_id = #{incidentId} ORDER BY sort_order")
    List<IncidentEvidence> findByIncidentId(@Param("incidentId") Long incidentId);
}
