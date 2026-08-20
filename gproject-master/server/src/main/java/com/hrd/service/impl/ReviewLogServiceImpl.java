package com.hrd.service.impl;

import com.hrd.entity.ReviewLog;
import com.hrd.mapper.ReviewLogMapper;
import com.hrd.service.ReviewLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审核记录服务实现类
 * 负责审核记录相关的业务逻辑处理
 */
@Service
@Slf4j
public class ReviewLogServiceImpl implements ReviewLogService {

    @Autowired
    private ReviewLogMapper reviewLogMapper;

    /**
     * 保存审核记录
     * 记录操作时间
     * @param reviewLog 审核记录对象
     */
    @Override
    public void save(ReviewLog reviewLog) {
        log.info("保存审核记录: incidentId={}, operation={}", reviewLog.getIncidentId(), reviewLog.getOperation());

        if (reviewLog.getCreateTime() == null) {
            reviewLog.setCreateTime(LocalDateTime.now());
        }

        reviewLogMapper.insert(reviewLog);
    }

    /**
     * 按事件ID查询审核记录
     * 返回该事件的所有审核记录，按时间倒序排列
     * @param incidentId 事件ID
     * @return 审核记录列表
     */
    @Override
    public List<ReviewLog> listByIncidentId(Long incidentId) {
        log.info("按事件ID查询审核记录: incidentId={}", incidentId);
        return reviewLogMapper.findByIncidentId(incidentId);
    }
}
