# 数据预处理与增量更新路线

## 目标

把当前的人工 `Excel -> JSON` 过程改成稳定的数据管道：

`原始 Excel / 爬虫原始数据 -> 规则清洗 -> 标准化 JSONL -> MySQL + Neo4j 导入 -> LangChain4j 检索`

运行时不直接读 Excel。
运行时也不直接读原始大 JSON。

## 最终会产出什么

不是只产出一个文件，而是三层产物：

1. `raw`
原始文件层。
保留 Excel、爬虫原始 JSON、HTML、CSV。

2. `normalized`
标准化后的 `JSONL`。
每一行是一条结构化岗位记录。
这是导入 MySQL 和 Neo4j 的中间层，不是最终查询层。

3. `serving`
系统实际使用的数据层：

- MySQL: 详细岗位明细、审计、增量状态
- Neo4j: 公司、岗位、技能、城市、类别等图关系

## 为什么用 JSONL

相比一个超大的 JSON，`JSONL` 更适合后续扩展：

- 适合 1 万条以上大文件
- 适合逐行写入和逐行导入
- 适合 Python 爬虫增量追加
- 单条坏数据不影响整包
- 更方便做抽检和重跑

## 清洗原则

默认不用 AI 全量清洗。
主流程以规则清洗为主，AI 只处理少量低置信度记录。

### 规则清洗负责

- 去空值
- 去乱码和控制字符
- 去 HTML
- 统一空白和全角半角
- 城市标准化
- 薪资解析
- 学历和经验归一
- 技能字典抽取
- 重复数据去重
- 噪声数据过滤

### AI 可选负责

- 低置信度岗位分类
- 长描述中的技能补全
- 难判定的噪声文本复判
- 结构化字段补全

## 推荐处理顺序

### 第一步：字段映射

先确定 Excel 里的原始列和标准字段的对应关系。

标准字段至少包括：

- `source_id`
- `source_platform`
- `source_url`
- `row_hash`
- `company_name`
- `job_title`
- `job_category`
- `industry`
- `city`
- `salary_text`
- `salary_min`
- `salary_max`
- `education_level`
- `experience_text`
- `job_description`
- `skills`
- `published_at`
- `crawled_at`
- `is_valid`
- `invalid_reason`
- `confidence_score`

### 第二步：噪声过滤

直接判无效的典型条件：

- 公司名为空
- 岗位名为空
- 描述为空或极短
- 明显广告页、推广页、课程页
- 关键字段全部缺失
- 批次内重复

保留但降级的典型条件：

- 薪资解析失败
- 城市未识别
- 技能提取很弱
- 学历和经验不完整

### 第三步：标准化

- 公司别名归一
- 城市归一
- 薪资转成数值区间
- 经验转标准标签
- 技能转数组
- 文本去模板噪声

### 第四步：去重

建议同时保留：

- `source_id`
- `row_hash`

`row_hash` 可由以下字段拼接后做哈希：

- 公司名
- 岗位名
- 城市
- 清洗后的描述

### 第五步：低置信度分流

不是所有数据都走 AI。

建议给每条记录打一个 `confidence_score`：

- `>= 0.85`: 直接导入
- `0.60 - 0.85`: 人工抽检或规则复跑
- `< 0.60`: 可选走 AI 二次结构化

## 增量更新策略

未来如果用 Python 爬虫持续补数，流程应该是：

`爬虫原始数据 -> 同一套预处理脚本 -> 新 JSONL -> 增量导入`

更新规则：

- 新 `source_id`: 插入
- 同 `source_id` 且 `row_hash` 变化: 更新
- 同 `source_id` 且 `row_hash` 不变: 跳过
- 历史记录不再出现: 标记 inactive，不做硬删除

## MySQL 与 Neo4j 分工

### MySQL

适合保存：

- 岗位明细
- 原始来源字段
- 清洗状态
- 有效性标记
- 哈希和更新时间

### Neo4j

适合保存：

- `Company`
- `JobPosting`
- `Skill`
- `City`
- `Category`

关系包括：

- `Company-POSTS->JobPosting`
- `JobPosting-REQUIRES->Skill`
- `JobPosting-LOCATED_IN->City`
- `JobPosting-BELONGS_TO->Category`

## LangChain4j 使用建议

不要一开始就做复杂 Agent。
先做检索增强。

优先顺序：

1. Neo4j 图谱检索
2. MySQL 结构化明细补充
3. 长描述向量检索

适合第一批落地的能力：

- 解释岗位之间为什么相似
- 根据学生画像找目标岗位缺失技能
- 回答“某城市有哪些同类岗位、常见技能是什么”

## 当前仓库已落的骨架

已经补入：

- 标准化数据契约
- JSONL 读取器
- Neo4j `MERGE` 导入服务
- 导入应用服务
- 导入接口

接下来最应该补的是：

1. Python 预处理脚本
2. Excel 字段映射配置
3. MySQL 明细落库
4. Neo4j 查询服务
5. LangChain4j 检索入口
