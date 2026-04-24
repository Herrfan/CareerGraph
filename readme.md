# career-agent-plus

面向职业规划、岗位匹配与成长报告的全栈应用。项目采用 Spring Boot + Vue 3，结合 MySQL、Neo4j 以及 Spring AI，支持简历解析、岗位画像、职业路径分析、聊天式职业辅导、报告生成和岗位数据预处理。

## 技术栈

- 后端：Java 21、Spring Boot 4、Spring AI、Spring Data JPA、Spring Data Neo4j
- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router
- 数据库：MySQL、Neo4j
- 模型接入：Ollama、本地或兼容 OpenAI 的接口

## 项目结构

```text
career-agent-plus
├─ src/main/java/com/zust/qyf/careeragent
│  ├─ application      业务编排与工作流服务
│  ├─ ai               模型调用与 AI 相关能力
│  ├─ config           Spring 配置
│  ├─ domain           DTO 与领域服务
│  ├─ infrastructure   MySQL、Neo4j、知识库与数据访问
│  └─ web              REST 接口
├─ src/main/resources
│  ├─ application.yml  主配置
│  ├─ prompts          Prompt 模板
│  └─ knowledge        内置知识文件
├─ frontend            Vue 前端
├─ scripts/job_data    岗位数据预处理脚本
├─ docs                辅助文档与示例
└─ .env.example        本地环境变量示例
```

## 主要能力

- 简历上传、解析与用户画像生成
- 岗位匹配与评分
- 职业路径与图谱分析
- 职业成长报告生成与润色
- 聊天式职业辅导
- 企业岗位知识导入与图谱初始化
- 岗位原始数据预处理与人工审查辅助

## 环境要求

- Java 21
- Node.js 20+
- MySQL 8+
- Neo4j 5+
- 可选：Ollama
- 可选：兼容 OpenAI 的大模型接口

## 配置

项目支持从根目录 `.env` 加载本地配置。建议先复制示例文件：

```powershell
Copy-Item .env.example .env
```

常用配置项如下：

```env
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/career_agent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

SPRING_NEO4J_URI=bolt://localhost:7687
SPRING_NEO4J_AUTHENTICATION_USERNAME=neo4j
SPRING_NEO4J_AUTHENTICATION_PASSWORD=your_password

SPRING_AI_OPENAI_API_KEY=your_api_key
SPRING_AI_OPENAI_BASE_URL=https://api.moonshot.cn
SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL=moonshot-v1-32k

OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_CHAT_MODEL=qwen3:8b
OLLAMA_EMBEDDING_MODEL=nomic-embed-text
APP_GRAPH_BOOTSTRAP_ON_STARTUP=true
```

## 启动后端

```powershell
.\mvnw.cmd spring-boot:run
```

默认端口：`8080`

## 启动前端

```powershell
cd frontend
npm install
npm run dev
```

常用命令：

- `npm run dev`：启动前端开发环境
- `npm run build`：构建前端产物
- `npm run preview`：本地预览构建结果
- `npm run typecheck`：执行前端类型检查

## 构建

后端：

```powershell
.\mvnw.cmd clean package
```

前端：

```powershell
cd frontend
npm run build
```

## 数据与提交约定

- 仓库中不提交 `frontend/node_modules/`、`frontend/dist/`、`target/` 等可再生成内容
- 根目录 `.env` 仅用于本地开发，不应提交
- `data/processed/`、`data/legacy-processed/`、`.compilecheck-*` 属于本地实验或处理中间产物，不作为分发内容
- 如需导入岗位数据，可参考 `docs/normalized-job.example.jsonl` 与 `scripts/job_data/`

## 建议启动顺序

1. 启动 MySQL 与 Neo4j
2. 准备 `.env`
3. 启动后端
4. 进入 `frontend/` 安装依赖并启动前端

如果只想快速验证接口层，至少需要先准备好 MySQL、Neo4j 和基础模型配置。
