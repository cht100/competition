#!/bin/bash
# ==========================================
# 智瞰危局 - 阿里云宝塔一键部署脚本
# 使用方法: bash setup.sh
# ==========================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_DIR="/www/wwwroot/zhikan"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo ""
echo "============================================"
echo "   智瞰危局 - 一键部署 (宝塔版)"
echo "============================================"
echo ""

# ====== 配置信息 (已预填) ======
SERVER_HOST="your_server_ip"
MYSQL_ROOT_PASS="your_mysql_root_password"
DASHSCOPE_KEY="your_dashscope_api_key_here"

DB_NAME="gproject"
DB_PASS="zhikan$(date +%s | tail -c 6)"

echo -e "${GREEN}开始部署...${NC}"
echo ""

# ====== 1. 安装依赖 ======
echo "[1/7] 检查并安装系统依赖..."

# 检查 Java 17
if ! java -version 2>&1 | grep -q "17\|18\|19\|20\|21"; then
    echo "  安装 JDK 17..."
    if command -v yum &>/dev/null; then
        yum install -y java-17-openjdk java-17-openjdk-devel >/dev/null 2>&1
    elif command -v apt &>/dev/null; then
        apt update -qq && apt install -y openjdk-17-jdk >/dev/null 2>&1
    fi
    if ! java -version 2>&1 | grep -q "17\|18\|19\|20\|21"; then
        echo -e "${RED}  JDK 17 安装失败! 请手动安装: yum install java-17-openjdk${NC}"
        exit 1
    fi
fi
echo "  Java: $(java -version 2>&1 | head -1)"

# 检查 Python >= 3.8 (Flask 3.x, torch, sentence-transformers 都需要)
PY_OK=0
if command -v python3.9 &>/dev/null; then
    PY_CMD=python3.9
    PY_OK=1
elif command -v python3.11 &>/dev/null; then
    PY_CMD=python3.11
    PY_OK=1
elif command -v python3.10 &>/dev/null; then
    PY_CMD=python3.10
    PY_OK=1
elif command -v python3 &>/dev/null; then
    PY_VER=$(python3 -c 'import sys; print(sys.version_info.minor)' 2>/dev/null || echo 0)
    if [ "$PY_VER" -ge 8 ]; then
        PY_CMD=python3
        PY_OK=1
    fi
fi

if [ "$PY_OK" -eq 0 ]; then
    echo "  系统 Python 版本过低，安装 Python 3.9..."
    if command -v yum &>/dev/null; then
        # CentOS 7/8
        yum install -y gcc openssl-devel bzip2-devel libffi-devel zlib-devel >/dev/null 2>&1
        cd /tmp
        if [ ! -f Python-3.9.18.tgz ]; then
            curl -sO https://www.python.org/ftp/python/3.9.18/Python-3.9.18.tgz
        fi
        tar xzf Python-3.9.18.tgz
        cd Python-3.9.18
        ./configure --enable-optimizations --prefix=/usr/local/python39 >/dev/null 2>&1
        make -j$(nproc) >/dev/null 2>&1
        make altinstall >/dev/null 2>&1
        ln -sf /usr/local/python39/bin/python3.9 /usr/local/bin/python3.9
        ln -sf /usr/local/python39/bin/pip3.9 /usr/local/bin/pip3.9
        PY_CMD=python3.9
        cd $SCRIPT_DIR
    elif command -v apt &>/dev/null; then
        apt update -qq && apt install -y python3.9 python3.9-venv python3-pip >/dev/null 2>&1
        PY_CMD=python3.9
    fi
    if ! command -v $PY_CMD &>/dev/null; then
        echo -e "${RED}  Python 3.9 安装失败! 请手动安装后重试${NC}"
        echo -e "${RED}  CentOS: yum install python39 或从源码编译${NC}"
        exit 1
    fi
fi

PIP_CMD="$PY_CMD -m pip"
echo "  Python: $($PY_CMD --version 2>&1)"

echo "  系统依赖检查完成"

# ====== 2. 部署文件 ======
echo "[2/7] 部署项目文件..."
mkdir -p $PROJECT_DIR/logs

if [ -f "$SCRIPT_DIR/backend.jar" ]; then
    # 从打包目录运行 (pack.bat 生成的 zhikan_deploy/)
    cp -f "$SCRIPT_DIR/backend.jar" "$PROJECT_DIR/backend.jar"
    cp -rf "$SCRIPT_DIR/platform" "$PROJECT_DIR/"
    cp -rf "$SCRIPT_DIR/model_service" "$PROJECT_DIR/"
    cp -f "$SCRIPT_DIR/init_db.sql" "$PROJECT_DIR/"
    [ -f "$SCRIPT_DIR/init_simulated_data.py" ] && cp -f "$SCRIPT_DIR/init_simulated_data.py" "$PROJECT_DIR/"
    cp -f "$SCRIPT_DIR/start.sh" "$PROJECT_DIR/" 2>/dev/null || true
    cp -f "$SCRIPT_DIR/stop.sh" "$PROJECT_DIR/" 2>/dev/null || true
else
    echo -e "${RED}  未找到 backend.jar!${NC}"
    echo -e "${RED}  请先在本地运行 pack.bat 打包后再上传${NC}"
    exit 1
fi

chmod +x $PROJECT_DIR/start.sh $PROJECT_DIR/stop.sh 2>/dev/null || true
echo "  文件部署到 $PROJECT_DIR"

# ====== 3. 写入环境变量 ======
echo "[3/7] 配置环境变量..."
cat > $PROJECT_DIR/.env << EOF
DASHSCOPE_API_KEY=$DASHSCOPE_KEY
EOF
echo "  .env 写入完成"

# ====== 4. 初始化数据库 ======
echo "[4/7] 初始化数据库..."
mysql -uroot -p"$MYSQL_ROOT_PASS" -e "
  CREATE DATABASE IF NOT EXISTS $DB_NAME DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE USER IF NOT EXISTS '${DB_NAME}'@'localhost' IDENTIFIED BY '${DB_PASS}';
  CREATE USER IF NOT EXISTS '${DB_NAME}'@'127.0.0.1' IDENTIFIED BY '${DB_PASS}';
  GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_NAME}'@'localhost';
  GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_NAME}'@'127.0.0.1';
  FLUSH PRIVILEGES;
" 2>/dev/null

mysql -uroot -p"$MYSQL_ROOT_PASS" $DB_NAME < $PROJECT_DIR/init_db.sql 2>/dev/null
echo "  数据库 $DB_NAME 初始化完成"

# ====== 5. 创建 Spring Boot 生产配置 ======
echo "[5/7] 写入后端配置..."
mkdir -p $PROJECT_DIR/config
cat > $PROJECT_DIR/config/application-prod.yml << EOFYML
hrd:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    host: 127.0.0.1
    port: 3306
    database: ${DB_NAME}
    username: ${DB_NAME}
    password: "${DB_PASS}"
EOFYML
echo "  application-prod.yml 完成"

# ====== 6. 安装 Python 依赖 ======
echo "[6/7] 安装 Python 依赖 (可能较慢)..."
cd $PROJECT_DIR/model_service
$PIP_CMD install --upgrade pip >/dev/null 2>&1
$PIP_CMD install -r requirements.txt -q 2>&1 | tail -5
echo "  Python 依赖安装完成"

# ====== 7. 配置 Nginx ======
echo "[7/7] 配置 Nginx..."

NGINX_CONF_CONTENT="server {
    listen 80;
    server_name ${SERVER_HOST};

    root ${PROJECT_DIR}/platform/dist;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 120;
    }

    location /ws/ {
        proxy_pass http://127.0.0.1:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection \"upgrade\";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_read_timeout 3600;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
        expires 7d;
        add_header Cache-Control \"public, immutable\";
    }

    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml;
}"

# 写入 Nginx 配置
BT_VHOST="/www/server/panel/vhost/nginx"
NGINX_VHOST="/www/server/nginx/conf/vhost"

if [ -d "$BT_VHOST" ]; then
    echo "$NGINX_CONF_CONTENT" > "$BT_VHOST/${SERVER_HOST}.conf"
elif [ -d "$NGINX_VHOST" ]; then
    echo "$NGINX_CONF_CONTENT" > "$NGINX_VHOST/${SERVER_HOST}.conf"
else
    mkdir -p /etc/nginx/conf.d
    echo "$NGINX_CONF_CONTENT" > "/etc/nginx/conf.d/zhikan.conf"
fi

# 重载 Nginx
if [ -f "/etc/init.d/nginx" ]; then
    /etc/init.d/nginx reload 2>/dev/null || true
elif command -v nginx &>/dev/null; then
    nginx -t 2>/dev/null && nginx -s reload 2>/dev/null || true
fi
echo "  Nginx 配置完成并已重载"

# ====== 创建 systemd 服务 (开机自启+自动重启) ======
echo ""
echo "配置系统服务 (开机自启)..."

JAVA_PATH=$(which java)

cat > /etc/systemd/system/zhikan-backend.service << EOF
[Unit]
Description=ZhiKan Backend (Spring Boot)
After=network.target mysqld.service

[Service]
Type=simple
User=root
WorkingDirectory=$PROJECT_DIR
ExecStart=$JAVA_PATH -jar -Dspring.profiles.active=prod -Dspring.config.additional-location=file:$PROJECT_DIR/config/ -Xms256m -Xmx512m $PROJECT_DIR/backend.jar
Restart=on-failure
RestartSec=10
StandardOutput=append:$PROJECT_DIR/logs/backend.log
StandardError=append:$PROJECT_DIR/logs/backend.log

[Install]
WantedBy=multi-user.target
EOF

# 检测 GPU
GPU_ENV=""
if ! command -v nvidia-smi &>/dev/null; then
    GPU_ENV="Environment=EMBEDDING_DEVICE=cpu"
fi

GUNICORN_PATH=$($PY_CMD -c "import shutil; print(shutil.which('gunicorn') or '')" 2>/dev/null)
if [ -z "$GUNICORN_PATH" ]; then
    # gunicorn 可能装在 python 对应的 bin 目录
    GUNICORN_PATH=$(dirname $(which $PY_CMD))/gunicorn
fi
if [ ! -f "$GUNICORN_PATH" ]; then
    GUNICORN_PATH="/usr/local/bin/gunicorn"
fi

cat > /etc/systemd/system/zhikan-model.service << EOF
[Unit]
Description=ZhiKan AI Model Service (Flask)
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=$PROJECT_DIR/model_service
$GPU_ENV
ExecStart=$GUNICORN_PATH --bind 127.0.0.1:5001 --workers 1 --timeout 120 app:app
Restart=on-failure
RestartSec=10
StandardOutput=append:$PROJECT_DIR/logs/model.log
StandardError=append:$PROJECT_DIR/logs/model.log

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable zhikan-backend zhikan-model >/dev/null 2>&1

# ====== 启动 ======
echo "启动服务..."
systemctl start zhikan-backend
echo "  后端服务启动中..."
systemctl start zhikan-model
echo "  模型服务启动中..."

sleep 5
BACKEND_STATUS=$(systemctl is-active zhikan-backend 2>/dev/null || echo "starting")
MODEL_STATUS=$(systemctl is-active zhikan-model 2>/dev/null || echo "starting")

echo ""
echo -e "============================================"
echo -e "  ${GREEN}部署完成!${NC}"
echo -e "============================================"
echo ""
echo -e "  访问地址:    ${GREEN}http://${SERVER_HOST}${NC}"
echo ""
echo "  管理员登录:  admin / 123456"
echo "  执勤人员:    zhangwei / 123456"
echo ""
echo "  后端状态:    $BACKEND_STATUS"
echo "  模型状态:    $MODEL_STATUS"
echo ""
echo "  数据库用户:  $DB_NAME"
echo "  数据库密码:  $DB_PASS  (请记住!)"
echo ""
echo -e "  ${YELLOW}常用命令:${NC}"
echo "  查看后端日志: tail -f $PROJECT_DIR/logs/backend.log"
echo "  查看模型日志: tail -f $PROJECT_DIR/logs/model.log"
echo "  重启全部:     systemctl restart zhikan-backend zhikan-model"
echo "  停止全部:     systemctl stop zhikan-backend zhikan-model"
echo ""

# 如果状态不是 active，给出排查提示
if [ "$BACKEND_STATUS" != "active" ]; then
    echo -e "${YELLOW}  后端可能还在启动中(Java启动较慢)，等30秒后检查:${NC}"
    echo "  systemctl status zhikan-backend"
fi
if [ "$MODEL_STATUS" != "active" ]; then
    echo -e "${YELLOW}  模型服务可能还在加载模型，等一会检查:${NC}"
    echo "  systemctl status zhikan-model"
fi
echo ""
