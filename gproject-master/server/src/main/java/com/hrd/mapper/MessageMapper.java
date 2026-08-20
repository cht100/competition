package com.hrd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrd.entity.Message;
import com.hrd.vo.MessageVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Insert("insert into messages(original_text,cleaned_text,images,publisher_name,publisher_id,publish_time,create_time,location_text,lat,lng,source_platform,url,status,ai_analysis,incident_id) " +
            "values(#{originalText},#{cleanedText},#{images},#{publisherName},#{publisherId},#{publishTime},#{createTime},#{locationText},#{lat},#{lng},#{sourcePlatform},#{url},#{status},#{aiAnalysis},#{incidentId})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void save(Message simulateMsg);

    int updateById(Message message);

    @Select("select * from messages where id = #{id}")
    MessageVO getById(Long id);
}
