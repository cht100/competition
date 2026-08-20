package com.hrd.service.impl;

import com.hrd.constant.MessageConstant;
import com.hrd.constant.StatusConstant;
import com.hrd.dto.LoginDTO;
import com.hrd.entity.User;
import com.hrd.exception.AccountDisableException;
import com.hrd.exception.AccountNotFoundException;
import com.hrd.exception.PasswordErrorException;
import com.hrd.mapper.LoginMapper;
import com.hrd.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    @Autowired
    private LoginMapper loginMapper;

    @Override
    public User login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        User user = loginMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (user == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(user.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (user.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountDisableException(MessageConstant.ACCOUNT_DISABLE);
        }

        //3、返回实体对象
        return user;
    }




}
