package com.hrd.controller.admin;

import com.hrd.constant.MessageConstant;
import com.hrd.exception.BaseException;
import com.hrd.mapper.*;
import com.hrd.result.Result;
import com.hrd.service.ClusteringService;
import com.hrd.task.TextSimulateTask;
import com.hrd.websocket.WebSocketServer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统设置控制器
 * 提供数据源控制、阈值配置、系统状态检查等接口
 */
@RestController
@RequestMapping("/admin/settings")
@Api(tags = "系统设置相关接口")
@Slf4j
public class SettingsController {

    @Autowired
    private TextSimulateTask textSimulateTask;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ClusteringService clusteringService;

    @Autowired
    private IncidentMapper incidentMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;

    @Autowired
    private IncidentEvidenceMapper incidentEvidenceMapper;

    @Autowired
    private ReviewLogMapper reviewLogMapper;

    @Autowired
    private ResponderMapper responderMapper;

    /**
     * 置信度阈值配置
     * 低于此值的事件将进入待复核池
     */
    private volatile int confidenceThreshold = 60;

    /**
     * 聚类相似度阈值配置
     * 超过此值的消息将合并到现有事件
     */
    private volatile int similarityThreshold = 60;

    /**
     * 推送间隔配置（毫秒）
     */
    private volatile long pushIntervalMs = 5000;

    /**
     * 切换数据源开关
     * @param params 请求参数，包含 enabled 字段
     * @return 操作结果
     */
    @PostMapping("/simulate/toggle")
    @ApiOperation("切换数据源开关")
    public Result<String> toggleDataSource(@RequestBody Map<String, Boolean> params) {
        Boolean enabled = params.get("enabled");
        if (enabled == null) {
            throw new BaseException("参数错误：enabled 不能为空");
        }

        if (enabled) {
            // 启动数据源
            if (textSimulateTask.isRunning()) {
                throw new BaseException(MessageConstant.IS_RUNNING);
            }
            textSimulateTask.start();
            log.info("【系统设置】模拟数据源已启动");
        } else {
            // 停止数据源
            if (!textSimulateTask.isRunning()) {
                throw new BaseException(MessageConstant.NOT_RUNNING);
            }
            textSimulateTask.stop();
            log.info("【系统设置】模拟数据源已停止");
        }

        return Result.success(enabled ? "数据源已启动" : "数据源已停止");
    }

    /**
     * 更新推送间隔
     * @param params 请求参数，包含 interval 字段（单位：秒）
     * @return 操作结果
     */
    @PostMapping("/simulate/interval")
    @ApiOperation("更新推送间隔")
    public Result<String> updateInterval(@RequestBody Map<String, Integer> params) {
        Integer interval = params.get("interval");
        if (interval == null || interval < 1 || interval > 60) {
            throw new BaseException("参数错误：interval 必须在 1-60 之间");
        }

        // 更新推送间隔（转换为毫秒）
        this.pushIntervalMs = interval * 1000L;
        log.info("【系统设置】推送间隔已更新为 {} 秒", interval);

        return Result.success("推送间隔已更新");
    }

    /**
     * 更新置信度阈值
     * @param params 请求参数，包含 threshold 字段
     * @return 操作结果
     */
    @PostMapping("/threshold")
    @ApiOperation("更新置信度阈值")
    public Result<String> updateThreshold(@RequestBody Map<String, Integer> params) {
        Integer threshold = params.get("threshold");
        if (threshold == null || threshold < 0 || threshold > 100) {
            throw new BaseException("参数错误：threshold 必须在 0-100 之间");
        }

        this.confidenceThreshold = threshold;
        log.info("【系统设置】置信度阈值已更新为 {}", threshold);

        return Result.success("阈值已更新");
    }

    /**
     * 更新聚类相似度阈值
     * @param params 请求参数，包含 threshold 字段
     * @return 操作结果
     */
    @PostMapping("/similarity")
    @ApiOperation("更新聚类相似度阈值")
    public Result<String> updateSimilarity(@RequestBody Map<String, Integer> params) {
        Integer threshold = params.get("threshold");
        if (threshold == null || threshold < 0 || threshold > 100) {
            throw new BaseException("参数错误：threshold 必须在 0-100 之间");
        }

        this.similarityThreshold = threshold;
        log.info("【系统设置】聚类相似度阈值已更新为 {}", threshold);

        return Result.success("阈值已更新");
    }

    /**
     * 检查系统状态
     * 返回各组件的运行状态
     * @return 系统状态信息
     */
    @GetMapping("/status")
    @ApiOperation("检查系统状态")
    public Result<Map<String, Boolean>> checkStatus() {
        Map<String, Boolean> status = new HashMap<>();

        // 后端服务状态（能调用此接口说明后端正常）
        status.put("backend", true);

        // 模型服务状态（暂时返回 true，实际可调用模型服务检查）
        status.put("model", checkModelService());

        // WebSocket 状态（检查是否有活跃连接）
        status.put("websocket", checkWebSocketStatus());

        // 数据库状态（暂时返回 true，实际可执行简单查询检查）
        status.put("database", checkDatabaseStatus());

        // 数据源运行状态
        status.put("dataSource", textSimulateTask.isRunning());

        log.info("【系统设置】系统状态检查完成：{}", status);

        return Result.success(status);
    }

    /**
     * 获取当前设置
     * @return 当前系统设置
     */
    @GetMapping("/current")
    @ApiOperation("获取当前设置")
    public Result<Map<String, Object>> getCurrentSettings() {
        Map<String, Object> settings = new HashMap<>();

        // 数据源设置
        settings.put("dataSourceEnabled", textSimulateTask.isRunning());
        settings.put("pushInterval", pushIntervalMs / 1000);

        // 阈值设置
        settings.put("confidenceThreshold", confidenceThreshold);
        settings.put("similarityThreshold", similarityThreshold);

        return Result.success(settings);
    }

    /**
     * 获取置信度阈值
     * @return 置信度阈值
     */
    public int getConfidenceThreshold() {
        return confidenceThreshold;
    }

    /**
     * 获取聚类相似度阈值
     * @return 聚类相似度阈值
     */
    public int getSimilarityThreshold() {
        return similarityThreshold;
    }

    /**
     * 获取推送间隔（毫秒）
     * @return 推送间隔
     */
    public long getPushIntervalMs() {
        return pushIntervalMs;
    }

    /**
     * 检查模型服务状态
     * @return 模型服务是否正常
     */
    private boolean checkModelService() {
        // TODO: 实际调用模型服务健康检查接口
        // 这里暂时返回 true
        return true;
    }

    /**
     * 一键重置所有演示数据
     * 清空所有处理产生的数据，将消息恢复为初始未处理状态
     */
    @PostMapping("/reset-data")
    @ApiOperation("一键重置所有演示数据")
    public Result<String> resetAllData() {
        log.info("【系统设置】开始一键重置所有演示数据...");

        // 1. 如果模拟正在运行，先停止
        if (textSimulateTask.isRunning()) {
            textSimulateTask.stop();
            log.info("【数据重置】已停止模拟数据推送");
        }

        try {
            // 2. 清空派单任务表
            dispatchTaskMapper.delete(null);
            log.info("【数据重置】已清空 dispatch_tasks 表");

            // 3. 清空事件证据表
            incidentEvidenceMapper.delete(null);
            log.info("【数据重置】已清空 incident_evidences 表");

            // 4. 清空审核日志表
            reviewLogMapper.delete(null);
            log.info("【数据重置】已清空 review_logs 表");

            // 5. 清空事件表
            incidentMapper.delete(null);
            log.info("【数据重置】已清空 incidents 表");

            // 6. 将所有消息重置为初始状态
            try (Connection conn = dataSource.getConnection()) {
                var stmt = conn.prepareStatement(
                    "UPDATE messages SET status = 0, cleaned_text = NULL, ai_analysis = NULL, incident_id = NULL"
                );
                int rows = stmt.executeUpdate();
                log.info("【数据重置】已重置 {} 条消息为初始状态", rows);
            }

            // 7. 将所有执勤人员状态重置为空闲
            try (Connection conn = dataSource.getConnection()) {
                var stmt = conn.prepareStatement(
                    "UPDATE responders SET status = 1"
                );
                int rows = stmt.executeUpdate();
                log.info("【数据重置】已重置 {} 名执勤人员状态为空闲", rows);
            }

            // 8. 清除聚类向量缓存
            clusteringService.clearVectorCache();

            // 9. 广播数据重置通知，让前端清空本地状态
            webSocketServer.sendDataReset();

            log.info("【系统设置】一键重置所有演示数据完成");
            return Result.success("所有演示数据已重置");
        } catch (Exception e) {
            log.error("【系统设置】数据重置失败: {}", e.getMessage(), e);
            throw new BaseException("数据重置失败: " + e.getMessage());
        }
    }

    /**
     * 检查 WebSocket 连接状态
     * @return 是否有活跃的 WebSocket 连接
     */
    private boolean checkWebSocketStatus() {
        return webSocketServer.hasConnections();
    }

    /**
     * 检查数据库连接状态
     * @return 数据库是否正常
     */
    private boolean checkDatabaseStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(3);
        } catch (Exception e) {
            log.error("【系统设置】数据库连接检查失败: {}", e.getMessage());
            return false;
        }
    }
}
