package com.hrd.service;

import com.hrd.dto.LoginDTO;
import com.hrd.entity.User;

public interface LoginService {

    User login(LoginDTO loginDTO);
}
