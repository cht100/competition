package com.hrd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrd.entity.Message;
import com.hrd.mapper.MessageMapper;
import com.hrd.service.MessageService;
import com.hrd.vo.MessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public boolean save(Message simulateMsg) {
        messageMapper.save(simulateMsg);
        log.info("保存原始信息成功");
        return true;
    }

    @Override
    public void updateById(Message cleanedMsg) {
        messageMapper.updateById(cleanedMsg);
        log.info("保存清洗信息成功");
    }

    @Override
    public MessageVO getById(Long id) {
        MessageVO messageVO =messageMapper.getById(id);
        log.info("查询message：{}", messageVO);
        return messageVO;
    }

    @Override
    public List<Message> listAll() {
        log.info("查询所有消息");
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Message::getCreateTime);
        return messageMapper.selectList(wrapper);
    }

    @Override
    public List<Message> listRecent(int limit) {
        log.info("查询最近{}条消息", limit);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Message::getCreateTime)
                .last("LIMIT " + limit);
        return messageMapper.selectList(wrapper);
    }

    @Override
    public List<Message> listByIncidentId(Long incidentId) {
        log.info("根据事件ID查询消息，事件ID：{}", incidentId);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getIncidentId, incidentId)
                .orderByAsc(Message::getCreateTime);
        return messageMapper.selectList(wrapper);
    }
}
