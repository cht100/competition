package com.hrd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrd.annotation.AutoFill;
import com.hrd.entity.User;
import com.hrd.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Insert("insert into users(username,password,phone,role,status,duty_status) values(#{username},#{password},#{phone},#{role},#{status},#{dutyStatus})")
    @AutoFill(value = OperationType.INSERT)
    public void addUserTest(User user);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    @Select("select * from users where username = #{username}")
    User getByUsername(String username);
}
