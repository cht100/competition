package com.hrd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrd.entity.Responder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 执勤人员Mapper接口
 * 用于执勤人员数据的持久化操作
 */
@Mapper
public interface ResponderMapper extends BaseMapper<Responder> {

    /**
     * 查询空闲人员
     * @return 空闲状态的执勤人员列表
     */
    @Select("SELECT * FROM responders WHERE status = 1")
    List<Responder> findIdleResponders();

    /**
     * 统计各状态人员数量
     * @return 各状态的人员数量列表
     */
    @Select("SELECT status, COUNT(*) as count FROM responders GROUP BY status")
    List<Map<String, Object>> countByStatus();
}
