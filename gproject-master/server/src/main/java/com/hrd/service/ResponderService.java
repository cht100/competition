package com.hrd.service;

import com.hrd.dto.ResponderUpdateDTO;
import com.hrd.entity.Responder;
import java.util.List;
import java.util.Map;

/**
 * 执勤人员服务接口
 */
public interface ResponderService {

    void save(Responder responder);

    void updateById(Responder responder);

    Responder getById(Long id);

    List<Responder> listAll();

    List<Responder> list();

    List<Responder> listIdle();

    List<Responder> listByStatus(Integer status);

    void updateStatus(Long id, Integer status);

    Map<Integer, Long> countByStatus();

    Map<String, Integer> countByStatusMap();

    /**
     * 执勤人员更新自己的信息
     * @param responderId 执勤人员ID
     * @param updateDTO 更新信息
     */
    void updateSelfInfo(Long responderId, ResponderUpdateDTO updateDTO);

    /**
     * 根据用户ID获取执勤人员信息
     * @param userId 用户ID
     * @return 执勤人员信息
     */
    Responder getByUserId(Long userId);
}
