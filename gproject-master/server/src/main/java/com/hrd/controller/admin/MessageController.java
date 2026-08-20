package com.hrd.controller.admin;

import com.hrd.constant.MessageConstant;
import com.hrd.entity.Message;
import com.hrd.exception.BaseException;
import com.hrd.result.Result;
import com.hrd.service.MessageService;
import com.hrd.vo.MessageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息管理控制器
 */
@RestController
@RequestMapping("/admin/message")
@Slf4j
@Api(tags = "消息管理")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询消息")
    public Result<MessageVO> getById(@PathVariable @ApiParam(value = "消息ID", example = "1") Long id) {
        MessageVO  messageVO = messageService.getById(id);
        return Result.success(messageVO);
    }

    @GetMapping("/list")
    @ApiOperation("查询所有消息")
    public Result<List<Message>> list() {
        log.info("【消息控制器】获取消息列表");
        List<Message> messages = messageService.listAll();
        return Result.success(messages);
    }

    @GetMapping("/recent")
    public Result<List<Message>> listRecent(@RequestParam(defaultValue = "50") int limit) {
        log.info("【消息控制器】获取最近{}条消息", limit);
        List<Message> messages = messageService.listRecent(limit);
        return Result.success(messages);
    }

}
