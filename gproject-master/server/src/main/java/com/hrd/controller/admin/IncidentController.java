package com.hrd.controller.admin;

import com.hrd.entity.Incident;
import com.hrd.result.Result;
import com.hrd.service.IncidentService;
import com.hrd.vo.IncidentDetailVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 事件管理控制器
 * 负责处理事件的查询、确认、驳回等操作
 */
@RestController
@RequestMapping("/admin/incident")
@Slf4j
@Api(tags = "事件管理")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    /**
     * 查询所有事件
     * @return 事件列表
     */
    @GetMapping("/list")
    @ApiOperation("查询所有事件")
    public Result<List<Incident>> list() {
        log.info("查询所有事件");
        List<Incident> incidents = incidentService.list();
        return Result.success(incidents);
    }

    /**
     * 按状态查询事件
     * @param status 事件状态：0-待审核,1-已确认,2-已派单,3-处理中,4-已完成,5-已驳回
     * @return 符合条件的事件列表
     */
    @GetMapping("/list/status/{status}")
    @ApiOperation("按状态查询事件")
    public Result<List<Incident>> listByStatus(@PathVariable @ApiParam(value = "事件状态", example = "0") Integer status) {
        log.info("按状态查询事件，状态：{}", status);
        List<Incident> incidents = incidentService.listByStatus(status);
        return Result.success(incidents);
    }

    /**
     * 按灾种查询事件
     * @param type 灾害类型：洪涝/火灾/地震/滑坡/交通事故/燃气泄漏/电梯困人/道路塌陷
     * @return 符合条件的事件列表
     */
    @GetMapping("/list/type/{type}")
    @ApiOperation("按灾种查询事件")
    public Result<List<Incident>> listByType(@PathVariable @ApiParam(value = "灾害类型", example = "洪涝") String type) {
        log.info("按灾种查询事件，灾种：{}", type);
        List<Incident> incidents = incidentService.listByType(type);
        return Result.success(incidents);
    }

    /**
     * 根据ID查询事件详情
     * @param id 事件ID
     * @return 事件详情
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询事件基本信息")
    public Result<Incident> getById(@PathVariable Long id) {
        log.info("根据ID查询事件，ID：{}", id);
        Incident incident = incidentService.getById(id);
        return Result.success(incident);
    }

    /**
     * 根据ID查询事件详情（包含证据链）
     * @param id 事件ID
     * @return 事件详情（包含关联的消息列表）
     */
    @GetMapping("/{id}/detail")
    @ApiOperation("根据ID查询事件详情（包含证据链）")
    public Result<IncidentDetailVO> getDetailById(@PathVariable Long id) {
        log.info("根据ID查询事件详情（包含证据链），ID：{}", id);
        IncidentDetailVO incidentDetail = incidentService.getDetailById(id);
        return Result.success(incidentDetail);
    }

    /**
     * 确认事件
     * 将事件状态从待审核变更为已确认
     * @param id 事件ID
     * @return 操作结果
     */
    @PostMapping("/confirm/{id}")
    public Result confirm(@PathVariable Long id) {
        log.info("确认事件，ID：{}", id);
        incidentService.confirm(id);
        return Result.success();
    }

    /**
     * 驳回事件
     * 将事件状态从待审核变更为已驳回
     * @param id 事件ID
     * @param body 请求体，包含驳回原因
     * @return 操作结果
     */
    @PostMapping("/reject/{id}")
    public Result reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        log.info("驳回事件，ID：{}，原因：{}", id, reason);
        incidentService.reject(id, reason);
        return Result.success();
    }

    /**
     * 获取事件统计数据
     * 包括各状态事件数量、各灾种事件数量等
     * @return 统计数据
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        log.info("获取事件统计数据");
        Map<String, Object> statistics = incidentService.getStatistics();
        return Result.success(statistics);
    }
}
