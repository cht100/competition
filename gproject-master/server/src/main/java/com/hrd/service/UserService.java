package com.hrd.service;

import com.hrd.dto.CreateResponderDTO;
import com.hrd.entity.User;

public interface UserService {

    void addUserTest(User user);

    /**
     * 创建执勤人员
     * 同时创建User和对应的Responder信息
     * @param createResponderDTO 创建执勤人员参数
     * @return 创建的用户信息
     */
    User createResponder(CreateResponderDTO createResponderDTO);
}
