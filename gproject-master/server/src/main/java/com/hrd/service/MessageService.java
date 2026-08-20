package com.hrd.service;

import com.hrd.entity.Message;
import com.hrd.vo.MessageVO;

import java.util.List;

public interface MessageService {
    boolean save(Message simulateMsg);

    void updateById(Message cleanedMsg);

    MessageVO getById(Long id);

    List<Message> listAll();

    List<Message> listRecent(int limit);

    /**
     * 根据事件ID查询消息列表
     * @param incidentId 事件ID
     * @return 消息列表
     */
    List<Message> listByIncidentId(Long incidentId);
}
