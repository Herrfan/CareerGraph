# 阿里云快速部署指南 - 一键部署版本

## 推荐方案：阿里云函数计算（FC）+ RDS MySQL

这是最适合您的"一键部署"方案！

## 部署步骤

### 第一步：准备 MySQL 数据库

1. 登录阿里云控制台，进入 **RDS MySQL**
2. 创建 MySQL 8.0 实例（推荐配置：2核4GB，50GB存储）
3. 创建数据库 `career_agent`
4. 设置白名单，允许函数计算访问

### 第二步：使用阿里云函数计算部署

#### 方式 A：使用控制台界面部署（最简单）

1. 登录阿里云控制台，进入 **函数计算 FC**
2. 创建服务（服务名：`career-agent-service`）
3. 创建函数：
   - 函数名称：`career-agent`
   - 运行环境：**自定义容器（Custom Container）**
   - 代码上传方式：**使用容器镜像**
   - 监听端口：`8080`

4. 构建并推送镜像到阿里云容器镜像服务（ACR）：
   ```bash
   # 1. 登录阿里云容器镜像服务
   docker login --username=<您的阿里云账号> registry.cn-<region>.aliyuncs.com
   
   # 2. 构建镜像
   docker build -t career-agent:latest .
   
   # 3. 标记镜像
   docker tag career-agent:latest registry.cn-<region>.aliyuncs.com/<您的命名空间>/career-agent:latest
   
   # 4. 推送镜像
   docker push registry.cn-<region>.aliyuncs.com/<您的命名空间>/career-agent:latest
   ```

5. 在函数配置中设置镜像地址：
   ```
   registry.cn-<region>.aliyuncs.com/<您的命名空间>/career-agent:latest
   ```

6. 配置环境变量（在函数配置 -> 环境变量中添加）：
   - `SPRING_DATASOURCE_URL`: 您的 RDS 连接地址
   - `SPRING_DATASOURCE_USERNAME`: 数据库用户名
   - `SPRING_DATASOURCE_PASSWORD`: 数据库密码
   - 其他环境变量见 `.env.example`

7. 配置触发器：
   - 添加 HTTP 触发器
   - 认证方式：匿名（或根据需要配置）
   - 支持的方法：GET, POST, PUT, DELETE

8. 点击部署！

#### 方式 B：使用 Serverless Devs 工具部署

1. 安装 Serverless Devs：
   ```bash
   npm install @serverless-devs/s -g
   ```

2. 配置阿里云账号：
   ```bash
   s config add
   ```

3. 修改 `template.yml` 中的配置：
   - 将 `<account-id>` 替换为您的阿里云账号 ID
   - 将 `<your-rds-endpoint>` 替换为您的 RDS 地址
   - 将数据库用户名和密码替换为实际值
   - 配置 ACR 镜像地址

4. 部署：
   ```bash
   s deploy
   ```

### 第三步：配置前端

在您的前端项目（已部署到 vertical）中，修改 API 地址：

找到前端的 API 配置文件（通常在 `frontend/src/services/http.ts` 或类似文件），将后端地址改为：

```typescript
const API_BASE_URL = 'https://<您的函数计算域名>';
```

## 成本参考

阿里云函数计算（FC）有免费额度，非常适合前期测试和中小流量：
- 每月前 100 万次调用免费
- 每月前 400,000 GB-秒免费
- RDS MySQL：按量付费或包年包月

## 监控和日志

- 在函数计算控制台查看函数执行日志
- 配置云监控监控应用健康状态
- 设置告警规则

## 常见问题

**Q: 构建镜像太慢怎么办？**
A: 使用阿里云容器镜像服务的构建功能，或使用本地构建后推送

**Q: 数据库连接不上？**
A: 检查 RDS 的白名单设置，确保允许函数计算的访问

**Q: 函数冷启动太慢？**
A: 配置预留实例，或者使用单实例多并发模式

需要更详细的帮助？请查看 `DEPLOYMENT.md` 文件！
