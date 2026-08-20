package com.hrd.service.impl;

import com.hrd.dto.CreateResponderDTO;
import com.hrd.entity.Responder;
import com.hrd.entity.User;
import com.hrd.mapper.ResponderMapper;
import com.hrd.mapper.UserMapper;
import com.hrd.service.ResponderService;
import com.hrd.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ResponderMapper responderMapper;

    @Autowired
    private ResponderService responderService;

    @Override
    public void addUserTest(User user) {
        userMapper.addUserTest(user);
    }

    @Override
    @Transactional
    public User createResponder(CreateResponderDTO createResponderDTO) {
        log.info("创建执勤人员：{}", createResponderDTO);

        // 1. 检查用户名是否已存在
        User existingUser = userMapper.getByUsername(createResponderDTO.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在：" + createResponderDTO.getUsername());
        }

        // 2. 创建User
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .username(createResponderDTO.getUsername())
                .password(DigestUtils.md5DigestAsHex(createResponderDTO.getPassword().getBytes()))
                .phone(createResponderDTO.getPhone())
                .role(2) // 执勤人员角色
                .status(1) // 正常状态
                .dutyStatus(0) // 休息中
                .createTime(now)
                .updateTime(now)
                .build();

        userMapper.insert(user);

        // 3. 创建对应的Responder
        Responder responder = Responder.builder()
                .name(createResponderDTO.getName())
                .phone(createResponderDTO.getPhone())
                .position(createResponderDTO.getPosition() != null ? createResponderDTO.getPosition() : "志愿者")
                .status(Responder.STATUS_OFFLINE) // 离线状态
                .userId(user.getId())
                .createTime(now)
                .updateTime(now)
                .build();

        responderService.save(responder);

        log.info("执勤人员创建成功，用户ID：{}，执勤人员ID：{}", user.getId(), responder.getId());
        return user;
    }
}
