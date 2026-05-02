# 项目部署指南

## 项目架构

- **前端**: Vue 3 + Vite
- **后端**: Spring Boot 4 + Java 17
- **数据库**: MySQL 8 + Neo4j

---

## 一、前端部署到 Vercel

### 1. 准备工作

确保您已经：
- [ ] 注册 [Vercel 账号](https://vercel.com)
- [ ] 安装 [Git](https://git-scm.com/)
- [ ] 将代码推送到 GitHub/GitLab/Bitbucket

### 2. 部署步骤

#### 方式一：通过 Vercel 官网部署（推荐）

1. 访问 [Vercel 官网](https://vercel.com) 并登录
2. 点击 **"New Project"**
3. 导入您的 GitHub 仓库
4. 在项目设置中配置：
   - **Framework Preset**: 选择 `Vite`
   - **Root Directory**: 设置为 `frontend`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
5. 添加环境变量（在 Environment Variables 中）：
   ```
   VITE_API_URL = http://121.41.50.68:8080
   ```
6. 点击 **"Deploy"** 开始部署

#### 方式二：通过 Vercel CLI 部署

```bash
# 1. 安装 Vercel CLI
npm i -g vercel

# 2. 登录 Vercel
vercel login

# 3. 部署（在 frontend 目录下执行）
cd frontend
vercel

# 4. 部署到生产环境
vercel --prod
```

### 3. 自定义域名（可选）

部署成功后，在 Vercel 项目设置中：
1. 进入 **"Settings"** → **"Domains"**
2. 添加您的域名
3. 根据提示配置 DNS 解析

---

## 二、后端部署到阿里云

### 1. 准备工作

确保您已经：
- [ ] 拥有阿里云服务器（推荐配置：2核4G以上）
- [ ] 服务器已安装 Docker（可选，推荐）
- [ ] 阿里云 MySQL 数据库已创建（数据库名：`career_agent`）
- [ ] 获取 Vercel 分配的域名（用于 CORS 配置）

### 2. 部署步骤

#### 方式一：使用 Docker 部署（推荐）

```bash
# 1. SSH 登录到阿里云服务器
ssh root@your-server-ip

# 2. 克隆代码
git clone your-repo-url
cd career-agent-plus-mian/源码

# 3. 构建 Docker 镜像
docker build -t career-agent:latest .

# 4. 创建并启动容器（配置环境变量）
docker run -d \
  --name career-agent \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://your-mysql-host:3306/career_agent?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8&characterEncoding=utf-8" \
  -e SPRING_DATASOURCE_USERNAME="root" \
  -e SPRING_DATASOURCE_PASSWORD="your-mysql-password" \
  -e APP_CORS_ALLOWED_ORIGINS="http://localhost:5173,https://your-vercel-domain.vercel.app" \
  career-agent:latest

# 5. 查看日志
docker logs -f career-agent
```

#### 方式二：使用 JAR 包直接部署

```bash
# 1. 在本地构建 JAR 包
cd 源码
.\mvnw.cmd clean package -DskipTests

# 2. 上传 JAR 包到服务器
scp target/career-agent-0.0.1-SNAPSHOT.jar root@your-server-ip:/root/

# 3. SSH 登录服务器
ssh root@your-server-ip

# 4. 确保服务器已安装 Java 17
java -version

# 5. 创建启动脚本
cat > start.sh << 'EOF'
#!/bin/bash
nohup java -jar career-agent-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url="jdbc:mysql://your-mysql-host:3306/career_agent?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8&characterEncoding=utf-8" \
  --spring.datasource.username="root" \
  --spring.datasource.password="your-mysql-password" \
  --app.cors.allowed-origins="http://localhost:5173,https://your-vercel-domain.vercel.app" \
  > app.log 2>&1 &
EOF

# 6. 给脚本执行权限
chmod +x start.sh

# 7. 启动应用
./start.sh

# 8. 查看日志
tail -f app.log
```

### 3. 配置阿里云安全组

1. 登录阿里云控制台
2. 进入 **"云服务器 ECS"** → **"安全组"**
3. 找到您的安全组，点击 **"配置规则"**
4. 添加入方向规则：
   - **端口范围**: `8080/8080`
   - **授权对象**: `0.0.0.0/0`（或限制为特定 IP）

### 4. 使用 Nginx 反向代理（可选，推荐）

```bash
# 1. 安装 Nginx
apt update && apt install nginx -y

# 2. 编辑 Nginx 配置
vim /etc/nginx/sites-available/career-agent

# 3. 添加以下配置
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# 4. 创建软链接
ln -s /etc/nginx/sites-available/career-agent /etc/nginx/sites-enabled/

# 5. 测试配置
nginx -t

# 6. 重启 Nginx
systemctl restart nginx

# 7. 配置 HTTPS（使用 Let's Encrypt）
apt install certbot python3-certbot-nginx -y
certbot --nginx -d your-domain.com
```

---

## 三、环境变量配置

### 后端环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SPRING_DATASOURCE_URL` | MySQL 数据库连接地址 | `jdbc:mysql://localhost:3306/career_agent` |
| `SPRING_DATASOURCE_USERNAME` | MySQL 用户名 | `root` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL 密码 | - |
| `SPRING_NEO4J_URI` | Neo4j 连接地址 | `bolt://localhost:7687` |
| `SPRING_NEO4J_AUTHENTICATION_USERNAME` | Neo4j 用户名 | `neo4j` |
| `SPRING_NEO4J_AUTHENTICATION_PASSWORD` | Neo4j 密码 | `12345678` |
| `APP_CORS_ALLOWED_ORIGINS` | 允许的 CORS 源地址（逗号分隔） | `http://localhost:5173,http://127.0.0.1:5173` |
| `SPRING_AI_OPENAI_API_KEY` | OpenAI API Key | - |

### 前端环境变量

| 变量名 | 说明 |
|--------|------|
| `VITE_API_URL` | 后端 API 地址 |

---

## 四、部署后验证

### 1. 验证后端

访问：`http://your-server-ip:8080/`

### 2. 验证前端

访问您的 Vercel 域名，检查：
- [ ] 页面正常加载
- [ ] 可以正常登录/注册
- [ ] API 请求正常（无 Network Error）
- [ ] 文件上传功能正常

---

## 五、常见问题

### Q1: 前端显示 Network Error

**A**: 检查以下几点：
1. 后端是否正常启动
2. `VITE_API_URL` 是否正确配置
3. 后端 CORS 配置是否包含前端域名
4. 阿里云安全组是否开放 8080 端口

### Q2: 后端无法连接数据库

**A**: 检查以下几点：
1. 数据库地址、用户名、密码是否正确
2. 数据库是否允许远程连接
3. 数据库安全组是否开放 3306 端口

### Q3: 如何更新部署

**前端更新**:
```bash
# 提交代码到 GitHub
git add .
git commit -m "update"
git push
# Vercel 会自动重新部署
```

**后端更新**:
```bash
# 方式一：Docker
docker stop career-agent
docker rm career-agent
docker build -t career-agent:latest .
# 重新运行容器（参考前面的命令）

# 方式二：JAR 包
# 重新构建并上传 JAR 包
# 重启应用
```

---

## 六、快速部署检查清单

- [ ] 前端代码推送到 GitHub
- [ ] 前端环境变量配置完成
- [ ] 前端部署到 Vercel
- [ ] 后端数据库配置完成
- [ ] 后端 CORS 配置包含前端域名
- [ ] 后端部署到阿里云
- [ ] 阿里云安全组配置完成
- [ ] 前后端联调测试通过

祝您部署顺利！🎉
