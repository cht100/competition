#!/bin/bash
# ==========================================
# 智瞰危局 - 启动全部服务
# ==========================================

echo "启动服务..."
systemctl start zhikan-backend
systemctl start zhikan-model
echo "后端: $(systemctl is-active zhikan-backend)"
echo "模型: $(systemctl is-active zhikan-model)"
echo "日志: tail -f /www/wwwroot/zhikan/logs/backend.log"
