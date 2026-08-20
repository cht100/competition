package com.hrd.mapper;

import com.hrd.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LoginMapper {

    /**
     * 根据用户名查询用户
     *
     * @param username
     * @return
     */
    @Select("select * from users where username = #{username}")
    User getByUsername(String username);
}
