#!/bin/bash
# ==========================================
# 智瞰危局 - 停止全部服务
# ==========================================

echo "停止服务..."
systemctl stop zhikan-backend 2>/dev/null && echo "  后端已停止" || echo "  后端未运行"
systemctl stop zhikan-model 2>/dev/null && echo "  模型已停止" || echo "  模型未运行"
echo "所有服务已停止"
