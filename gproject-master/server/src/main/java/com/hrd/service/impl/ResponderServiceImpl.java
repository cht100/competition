package com.hrd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hrd.dto.ResponderUpdateDTO;
import com.hrd.entity.Responder;
import com.hrd.mapper.ResponderMapper;
import com.hrd.service.ResponderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执勤人员服务实现类
 * 负责执勤人员相关的业务逻辑处理
 */
@Service
@Slf4j
public class ResponderServiceImpl implements ResponderService {

    @Autowired
    private ResponderMapper responderMapper;

    /**
     * 保存人员
     * 设置创建时间和更新时间
     * @param responder 人员对象
     */
    @Override
    public void save(Responder responder) {
        log.info("保存执勤人员: {}", responder.getName());
        responder.setCreateTime(LocalDateTime.now());
        responder.setUpdateTime(LocalDateTime.now());
        responderMapper.insert(responder);
    }

    /**
     * 更新人员
     * @param responder 人员对象
     */
    @Override
    public void updateById(Responder responder) {
        log.info("更新执勤人员: id={}", responder.getId());
        responder.setUpdateTime(LocalDateTime.now());
        responderMapper.updateById(responder);
    }

    /**
     * 根据ID查询人员
     * @param id 人员ID
     * @return 人员对象
     */
    @Override
    public Responder getById(Long id) {
        log.info("查询执勤人员: id={}", id);
        return responderMapper.selectById(id);
    }

    /**
     * 查询所有人员
     * @return 人员列表
     */
    @Override
    public List<Responder> listAll() {
        log.info("查询所有执勤人员");
        return responderMapper.selectList(null);
    }

    /**
     * 查询所有人员（list方法）
     * @return 人员列表
     */
    @Override
    public List<Responder> list() {
        log.info("查询所有执勤人员");
        return responderMapper.selectList(null);
    }

    /**
     * 查询空闲人员
     * 空闲状态为1，表示可以接受派单
     * @return 空闲人员列表
     */
    @Override
    public List<Responder> listIdle() {
        log.info("查询空闲执勤人员");
        return responderMapper.findIdleResponders();
    }

    /**
     * 按状态查询人员
     * @param status 状态值
     * @return 人员列表
     */
    @Override
    public List<Responder> listByStatus(Integer status) {
        log.info("按状态查询执勤人员: status={}", status);
        LambdaQueryWrapper<Responder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Responder::getStatus, status);
        return responderMapper.selectList(wrapper);
    }

    /**
     * 更新人员状态
     * @param id 人员ID
     * @param status 新状态值
     */
    @Override
    public void updateStatus(Long id, Integer status) {
        log.info("更新执勤人员状态: id={}, status={}", id, status);

        LambdaUpdateWrapper<Responder> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Responder::getId, id)
                .set(Responder::getStatus, status)
                .set(Responder::getUpdateTime, LocalDateTime.now());
        responderMapper.update(null, updateWrapper);
    }

    /**
     * 统计各状态人数
     * @return 状态与人数的映射
     */
    @Override
    public Map<Integer, Long> countByStatus() {
        log.info("统计各状态执勤人员数量");

        List<Map<String, Object>> statusCounts = responderMapper.countByStatus();
        Map<Integer, Long> result = new HashMap<>();

        for (Map<String, Object> item : statusCounts) {
            Integer status = (Integer) item.get("status");
            Long count = ((Number) item.get("count")).longValue();
            result.put(status, count);
        }

        return result;
    }

    /**
     * 统计各状态人数（返回Map<String, Integer>格式）
     * @return 状态与人数的映射
     */
    @Override
    public Map<String, Integer> countByStatusMap() {
        log.info("统计各状态执勤人员数量（Map格式）");

        Map<Integer, Long> countMap = countByStatus();
        Map<String, Integer> result = new HashMap<>();

        result.put("offline", countMap.getOrDefault(Responder.STATUS_OFFLINE, 0L).intValue());
        result.put("idle", countMap.getOrDefault(Responder.STATUS_IDLE, 0L).intValue());
        result.put("dispatched", countMap.getOrDefault(Responder.STATUS_DISPATCHED, 0L).intValue());
        result.put("executing", countMap.getOrDefault(Responder.STATUS_EXECUTING, 0L).intValue());

        return result;
    }

    /**
     * 执勤人员更新自己的信息
     * @param responderId 执勤人员ID
     * @param updateDTO 更新信息
     */
    @Override
    public void updateSelfInfo(Long responderId, ResponderUpdateDTO updateDTO) {
        log.info("执勤人员更新自己的信息，ID：{}，更新内容：{}", responderId, updateDTO);

        Responder responder = responderMapper.selectById(responderId);
        if (responder == null) {
            throw new RuntimeException("执勤人员不存在，ID：" + responderId);
        }

        // 构建更新条件
        LambdaUpdateWrapper<Responder> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Responder::getId, responderId);

        // 只更新非空字段
        if (updateDTO.getStatus() != null) {
            // 验证状态值是否合法（只能更新为离线或空闲）
            if (updateDTO.getStatus() != Responder.STATUS_OFFLINE && 
                updateDTO.getStatus() != Responder.STATUS_IDLE) {
                throw new RuntimeException("执勤人员只能更新为离线或空闲状态");
            }
            updateWrapper.set(Responder::getStatus, updateDTO.getStatus());
        }

        if (updateDTO.getPosition() != null && !updateDTO.getPosition().trim().isEmpty()) {
            updateWrapper.set(Responder::getPosition, updateDTO.getPosition().trim());
        }

        if (updateDTO.getPhone() != null && !updateDTO.getPhone().trim().isEmpty()) {
            updateWrapper.set(Responder::getPhone, updateDTO.getPhone().trim());
        }

        updateWrapper.set(Responder::getUpdateTime, LocalDateTime.now());

        int result = responderMapper.update(null, updateWrapper);
        if (result == 0) {
            throw new RuntimeException("更新执勤人员信息失败");
        }

        log.info("执勤人员信息更新成功，ID：{}", responderId);
    }

    /**
     * 根据用户ID获取执勤人员信息
     * @param userId 用户ID
     * @return 执勤人员信息
     */
    @Override
    public Responder getByUserId(Long userId) {
        log.info("根据用户ID查询执勤人员，用户ID：{}", userId);
        
        LambdaQueryWrapper<Responder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Responder::getUserId, userId);
        
        return responderMapper.selectOne(queryWrapper);
    }
}
