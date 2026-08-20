#!/bin/bash
# ==========================================
# 智瞰危局 - 阿里云宝塔一键部署脚本
# 在服务器上执行: bash deploy.sh
# ==========================================

set -e

# === 配置区 (按需修改) ===
PROJECT_DIR="/www/wwwroot/zhikan"
DB_NAME="gproject"
DB_USER="gproject"
DB_PASS="your_db_password_here"    # 改成你的数据库密码
DASHSCOPE_KEY="your_dashscope_api_key_here"  # 改成你的阿里百炼API Key
JAVA_JAR="$PROJECT_DIR/gproject-master/server/target/server-0.0.1-SNAPSHOT.jar"

echo "=========================================="
echo "  智瞰危局 - 部署开始"
echo "=========================================="

# 1. 创建项目目录
echo "[1/6] 创建项目目录..."
mkdir -p $PROJECT_DIR
cd $PROJECT_DIR

# 2. 创建 .env 文件
echo "[2/6] 写入环境变量..."
cat > $PROJECT_DIR/.env << EOF
DASHSCOPE_API_KEY=$DASHSCOPE_KEY
EOF

# 3. 初始化数据库
echo "[3/6] 初始化数据库..."
mysql -u root -e "CREATE DATABASE IF NOT EXISTS $DB_NAME DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -e "CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';"
mysql -u root -e "GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost'; FLUSH PRIVILEGES;"
mysql -u $DB_USER -p"$DB_PASS" $DB_NAME < $PROJECT_DIR/init_db.sql
echo "  数据库初始化完成"

# 4. 构建前端
echo "[4/6] 构建前端..."
cd $PROJECT_DIR/platform
npm install
npm run build
echo "  前端构建完成 -> platform/dist/"

# 5. 构建后端 JAR
echo "[5/6] 构建后端..."
cd $PROJECT_DIR/gproject-master
mvn clean package -DskipTests -Pprod 2>&1 | tail -5
echo "  后端构建完成 -> server/target/server-0.0.1-SNAPSHOT.jar"

# 6. 安装 Python 依赖
echo "[6/6] 安装Python依赖..."
cd $PROJECT_DIR/model_service
pip3 install -r requirements.txt
echo "  Python依赖安装完成"

echo ""
echo "=========================================="
echo "  部署完成！请继续以下步骤："
echo "=========================================="
echo ""
echo "  1. 修改 deploy/nginx.conf 中的域名"
echo "  2. 在宝塔创建网站并粘贴 nginx 配置"
echo "  3. 启动服务: bash deploy/start.sh"
echo ""
