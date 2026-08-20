package com.hrd.service;

import com.hrd.entity.ReviewLog;
import java.util.List;

/**
 * 审核记录服务接口
 */
public interface ReviewLogService {

    void save(ReviewLog reviewLog);

    List<ReviewLog> listByIncidentId(Long incidentId);
}
