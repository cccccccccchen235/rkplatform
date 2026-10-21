# 04 · 数据库设计

> 数据库：MySQL 8.0，字符集 `utf8mb4`，排序规则 `utf8mb4_0900_ai_ci`。
> 本文档的**表名、字段名、枚举值**是全项目唯一事实来源，`05/06/07/08` 必须与本文件一致。

## 4.1 设计约定

| 约定 | 规则 |
| --- | --- |
| 主键 | `id BIGINT UNSIGNED AUTO_INCREMENT` |
| 业务编号 | 仅对外可读的对象需要（`rk_generation_batch.batch_no`、`exam_session.session_no`），规则：前缀 + `yyyyMMdd` + 6 位随机，如 `EX20260925000183` |
| 命名 | 表名、字段名全小写下划线；布尔字段用 `tinyint(1)` 且以 `is_` / `allow_` 开头 |
| 时间 | `DATETIME`（不用 timestamp，避免 2038 与时区隐式转换）；`created_at` / `updated_at` 由数据库默认值维护 |
| 枚举 | **用 `VARCHAR` + 注释列出取值，不用 MySQL `ENUM` 类型**。理由：`ENUM` 增删值需要 `ALTER TABLE` 且顺序敏感，迁移成本高于收益；取值合法性在应用层用 Java 枚举保证 |
| 软删除 | 仅对"必须保留历史引用"的对象使用状态位（题目用 `status`，用户用 `status`）；其余表物理删除 |
| 金额/分值 | `DECIMAL(5,1)`（分值最大 999.9） |
| 大文本 | `MEDIUMTEXT`（题干、解析、论文作答）；`JSON` 用于结构化小数据（选项快照、评分要点） |
| 外键 | **不建物理外键**，只建索引。理由：单机应用层已保证引用完整性，物理外键会让数据修复与批量导入变得困难 |

**分域总览**（共 27 张表）：

| 域 | 表 |
| --- | --- |
| 用户域（5） | `sys_user`、`user_wrong_question`、`user_favorite`、`user_answer_record`、`user_practice_progress` |
| 题库域（9） | `rk_subject`、`rk_outline_node`、`rk_knowledge_point`、`rk_question`、`rk_question_option`、`rk_question_kp`、`rk_question_review_log`、`rk_generation_batch`、`rk_question_feedback` |
| 考试域（7） | `exam_paper_template`、`exam_paper_sitting`、`exam_paper_section`、`exam_session`、`exam_session_sitting`、`exam_session_section`、`exam_session_item` |
| 统计域（4） | `stat_user_subject`、`stat_user_outline`、`stat_user_kp`、`stat_daily` |
| 系统域（2） | `sys_config`、`sys_operation_log` |

## 4.2 ER 概览

```
sys_user ──1:N── user_answer_record ──N:1── rk_question
    │                                              │
    ├──1:N── user_wrong_question ──────────────────┤
    ├──1:N── user_favorite ────────────────────────┤
    ├──1:N── exam_session ──1:N── exam_session_sitting ──1:N── exam_session_section
    │                    ──1:N── exam_session_item ─────────────────────────────┘
    └──1:N── stat_user_subject / stat_user_outline / stat_user_kp / stat_daily

rk_subject ──1:N── rk_outline_node (自引用树)
           ──1:N── rk_knowledge_point
           ──1:N── rk_question ──1:N── rk_question_option
           │                  ──N:N── rk_knowledge_point (rk_question_kp)
           │                  ──1:N── rk_question_review_log
           │                  ──N:1── rk_generation_batch
           ──1:1── exam_paper_template ──1:N── exam_paper_sitting ──1:N── exam_paper_section
```

## 4.3 用户域

```sql
-- ---------------------------------------------------------------
-- 用户
-- 登录失败计数与锁定不使用数据库字段：15 分钟窗口的计数天然是 TTL 语义，
-- 放在 Redis（rk:login:fail:{username}）即可自动解锁，避免两处状态不一致。
-- ---------------------------------------------------------------
CREATE TABLE sys_user (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username           VARCHAR(32)  NOT NULL                COMMENT '登录名',
  password_hash      CHAR(60)     NOT NULL                COMMENT 'BCrypt(strength=10) 结果，固定 60 字符',
  nickname           VARCHAR(32)  NOT NULL                COMMENT '昵称',
  role               VARCHAR(16)  NOT NULL DEFAULT 'USER'  COMMENT 'USER / ADMIN',
  status             VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / DISABLED',
  current_subject_id BIGINT UNSIGNED NULL                 COMMENT '当前备考科目，影响首页与统计默认口径',
  must_change_password TINYINT(1) NOT NULL DEFAULT 0      COMMENT '首次登录强制改密（管理员重置密码后置 1，改密成功后置 0；对应评审 C2）',
  last_login_at      DATETIME     NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_current_subject (current_subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ---------------------------------------------------------------
-- 错题本
-- 收录规则：答错即收录（wrong_count+1）；答对 consecutive_correct+1，
-- 连续答对 2 次置为 MASTERED（移出可用列表但保留数据）；用户可手动移除（REMOVED）。
-- ---------------------------------------------------------------
CREATE TABLE user_wrong_question (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id            BIGINT UNSIGNED NOT NULL,
  question_id        BIGINT UNSIGNED NOT NULL,
  subject_id         BIGINT UNSIGNED NOT NULL             COMMENT '冗余，避免列表查询回表 rk_question',
  wrong_count        INT          NOT NULL DEFAULT 0,
  correct_count      INT          NOT NULL DEFAULT 0,
  consecutive_correct TINYINT     NOT NULL DEFAULT 0,
  last_answered_at   DATETIME     NULL,
  status             VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / MASTERED / REMOVED',
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_question (user_id, question_id),
  KEY idx_user_subject_status (user_id, subject_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';

-- ---------------------------------------------------------------
-- 收藏
-- ---------------------------------------------------------------
CREATE TABLE user_favorite (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  subject_id  BIGINT UNSIGNED NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_question (user_id, question_id),
  KEY idx_user_subject (user_id, subject_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目收藏';

-- ---------------------------------------------------------------
-- 答题记录（最大表，唯一的事实流水）
-- 所有统计口径均由本表派生，stat_* 表只是它的聚合缓存。
-- ---------------------------------------------------------------
CREATE TABLE user_answer_record (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  subject_id  BIGINT UNSIGNED NOT NULL,
  outline_id  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '冗余叶子章节，统计按章节聚合时免回表',
  question_id BIGINT UNSIGNED NOT NULL,
  source      VARCHAR(24) NOT NULL COMMENT 'PRACTICE_CHAPTER/PRACTICE_KP/PRACTICE_SEQUENCE/PRACTICE_FAVORITE/WRONG_REDO/EXAM',
  session_id  BIGINT UNSIGNED NULL     COMMENT '仅 source=EXAM 时有值',
  user_answer VARCHAR(512) NULL        COMMENT '客观题为选项 key；主观题为空（不判分）',
  is_correct  TINYINT(1)  NULL         COMMENT '主观题固定为 NULL',
  duration_ms INT         NOT NULL DEFAULT 0 COMMENT '单题作答耗时，超过 10 分钟按 10 分钟截断（防挂机拉高均值）',
  answered_at DATETIME    NOT NULL,
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_created  (user_id, created_at),
  KEY idx_user_subject  (user_id, subject_id, answered_at),
  KEY idx_user_outline  (user_id, outline_id),
  KEY idx_question      (question_id),
  KEY idx_session       (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录';

-- ---------------------------------------------------------------
-- 练习进度（顺序练习"继续上次"）
-- outline_id 用 NOT NULL DEFAULT 0 而非 NULL：MySQL 唯一索引允许多个 NULL，
-- 会造成同一用户同一练习类型产生多条进度记录。
-- ---------------------------------------------------------------
CREATE TABLE user_practice_progress (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id         BIGINT UNSIGNED NOT NULL,
  subject_id      BIGINT UNSIGNED NOT NULL,
  practice_type   VARCHAR(24) NOT NULL COMMENT 'CHAPTER/KP/SEQUENCE/FAVORITE',
  outline_id      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0 表示不按章节',
  last_sequence   INT NOT NULL DEFAULT 0 COMMENT '顺序练习游标',
  last_question_id BIGINT UNSIGNED NULL,
  answered_count  INT NOT NULL DEFAULT 0,
  correct_count   INT NOT NULL DEFAULT 0,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_subject_type_ref (user_id, subject_id, practice_type, outline_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习进度';
```

## 4.4 题库域

```sql
-- ---------------------------------------------------------------
-- 科目（20 条固定数据，见 4.8 初始化）
-- ---------------------------------------------------------------
CREATE TABLE rk_subject (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  level          VARCHAR(16) NOT NULL COMMENT 'JUNIOR / MIDDLE / SENIOR',
  code           VARCHAR(16) NOT NULL COMMENT '科目代码，如 SD / SAD / IPT',
  name           VARCHAR(64) NOT NULL,
  paper_type     VARCHAR(16) NOT NULL COMMENT '卷面类型 JUNIOR_MID / SENIOR',
  exam_period    VARCHAR(16) NOT NULL COMMENT 'BOTH / FIRST_HALF / SECOND_HALF',
  outline_status VARCHAR(16) NOT NULL DEFAULT 'SKELETON' COMMENT 'FULL 完整章节树 / SKELETON 仅科目',
  content_status VARCHAR(16) NOT NULL DEFAULT 'SAMPLE'   COMMENT 'SAMPLE 样例 / BULK 200~300 题',
  intro          VARCHAR(500) NULL COMMENT '科目简介，自行撰写，不得抄大纲正文',
  question_count INT NOT NULL DEFAULT 0 COMMENT '冗余统计，发布/下架题目时更新',
  sort_no        INT NOT NULL DEFAULT 0,
  status         VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED / DISABLED',
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code),
  KEY idx_level_sort (level, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试科目';

-- ---------------------------------------------------------------
-- 大纲章节树（自引用，最多 3 层）
-- name 只取官方大纲的章节标题短语；origin 用于审计来源合规性。
-- ---------------------------------------------------------------
CREATE TABLE rk_outline_node (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  subject_id     BIGINT UNSIGNED NOT NULL,
  parent_id      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0 为根',
  level          TINYINT NOT NULL COMMENT '1/2/3',
  name           VARCHAR(128) NOT NULL,
  sort_no        INT NOT NULL DEFAULT 0,
  origin         VARCHAR(24) NOT NULL DEFAULT 'MANUAL' COMMENT 'OFFICIAL_TITLE / MANUAL',
  question_count INT NOT NULL DEFAULT 0 COMMENT '含子节点的已发布题量，冗余',
  status         VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_subject_parent (subject_id, parent_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大纲章节节点';

-- ---------------------------------------------------------------
-- 知识点标签（扁平，不做层级）
-- ---------------------------------------------------------------
CREATE TABLE rk_knowledge_point (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  subject_id     BIGINT UNSIGNED NOT NULL,
  name           VARCHAR(64) NOT NULL,
  aliases        VARCHAR(255) NULL COMMENT '同义词，逗号分隔，用于合并检索',
  question_count INT NOT NULL DEFAULT 0,
  status         VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_subject_name (subject_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点标签';

-- ---------------------------------------------------------------
-- 题目（核心表）
-- 组卷查询的唯一入口条件：subject_id + status='PUBLISHED' + question_type
-- GR-2 非空契约由应用层强制（不靠 DDL 守这条契约）：写入时校验，且 PUBLISHED 前
--   source_type / source_ref / source_license / generation_batch_id 四字段必须非空。
--   校验点见 05 的发布接口与 08 §8.2.3；因 source_type 各取值所需字段不同，
--   DDL 无法用统一 NOT NULL 表达，故以下三列保持可空（对应评审 B7）。
-- ---------------------------------------------------------------
CREATE TABLE rk_question (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  subject_id          BIGINT UNSIGNED NOT NULL,
  outline_id          BIGINT UNSIGNED NOT NULL COMMENT '所属叶子章节',
  question_type       VARCHAR(24)  NOT NULL COMMENT 'SINGLE_CHOICE/MULTI_CHOICE/TRUE_FALSE/SUBJECTIVE/ESSAY',
  stem                MEDIUMTEXT   NOT NULL COMMENT '题干，Markdown + KaTeX',
  stem_format         VARCHAR(16)  NOT NULL DEFAULT 'MARKDOWN',
  images              JSON         NULL COMMENT '题干插图的 JSON 数组 [{url, alt}]，解析内插图直接用 Markdown',
  correct_answer      VARCHAR(32)  NULL COMMENT '客观题选项 key（多选按字典序，如 ABD）；主观题为 NULL',
  reference_answer    MEDIUMTEXT   NULL COMMENT '主观题/论文参考答案（Markdown）',
  analysis            MEDIUMTEXT   NULL COMMENT '解析（Markdown）',
  score_points        JSON         NULL COMMENT '主观题评分要点字符串数组',
  difficulty          TINYINT      NOT NULL DEFAULT 3 COMMENT '1~5，1 最易，5 最难（以 02 文档为准）',
  source_type         VARCHAR(16)  NOT NULL COMMENT 'AI_GENERATED / ORIGINAL / LICENSED（约束 C1，必填）',
  source_ref          VARCHAR(255) NULL COMMENT '来源说明（如批次号或编写人），AI 生成时记录考点范围；应用层强制 PUBLISHED 前非空（B7）',
  source_license      VARCHAR(64)  NULL COMMENT '许可标识；AI_GENERATED / ORIGINAL 填 SELF；应用层强制 PUBLISHED 前非空（B7）',
  generation_batch_id BIGINT UNSIGNED NULL COMMENT 'AI 生成时关联批次；应用层强制 PUBLISHED 前非空（B7）',
  status              VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING_REVIEW/APPROVED/REJECTED/PUBLISHED/OFFLINE',
  version             INT          NOT NULL DEFAULT 1 COMMENT '每次内容编辑 +1，供审计',
  created_by          BIGINT UNSIGNED NOT NULL,
  reviewed_by         BIGINT UNSIGNED NULL,
  reviewed_at         DATETIME     NULL,
  published_at        DATETIME     NULL,
  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_subject_status_type (subject_id, status, question_type),
  KEY idx_outline_status (outline_id, status),
  KEY idx_batch (generation_batch_id),
  KEY idx_status_updated (status, updated_at),
  KEY idx_source_type (source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目';

-- ---------------------------------------------------------------
-- 选项（仅客观题）
-- ---------------------------------------------------------------
CREATE TABLE rk_question_option (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  question_id BIGINT UNSIGNED NOT NULL,
  option_key  CHAR(1) NOT NULL COMMENT 'A~F',
  content     TEXT NOT NULL COMMENT 'Markdown + KaTeX',
  sort_no     INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_question_key (question_id, option_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目选项';

-- ---------------------------------------------------------------
-- 题目-知识点 多对多
-- ---------------------------------------------------------------
CREATE TABLE rk_question_kp (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  question_id BIGINT UNSIGNED NOT NULL,
  kp_id       BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_question_kp (question_id, kp_id),
  KEY idx_kp (kp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目知识点关联';

-- ---------------------------------------------------------------
-- 题目状态流转日志（审计）
-- ---------------------------------------------------------------
CREATE TABLE rk_question_review_log (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  question_id BIGINT UNSIGNED NOT NULL,
  from_status VARCHAR(24) NULL,
  to_status   VARCHAR(24) NOT NULL,
  action      VARCHAR(24) NOT NULL COMMENT 'SUBMIT/APPROVE/REJECT/REGENERATE/PUBLISH/OFFLINE/EDIT/CREATE',
  remark      VARCHAR(500) NULL,
  operator_id BIGINT UNSIGNED NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_question_created (question_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目状态流转日志';

-- ---------------------------------------------------------------
-- AI 生成批次
-- outline_ids 记录本次生成覆盖的考点 —— 这是"输入不含真题"的证据链。
-- ---------------------------------------------------------------
CREATE TABLE rk_generation_batch (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  batch_no       VARCHAR(32) NOT NULL,
  subject_id     BIGINT UNSIGNED NOT NULL,
  outline_ids    JSON NOT NULL COMMENT '考点节点 ID 数组，生成输入的唯一内容来源',
  question_type  VARCHAR(24) NOT NULL,
  target_count   INT NOT NULL,
  difficulty     TINYINT NULL,
  model          VARCHAR(64) NOT NULL COMMENT '如 deepseek-chat / qwen-plus',
  prompt_version VARCHAR(32) NOT NULL COMMENT 'Prompt 模板版本号，便于复现与回溯',
  status         VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/GENERATING/GENERATED/REVIEWING/COMPLETED/FAILED',
  produced_count INT NOT NULL DEFAULT 0,
  approved_count INT NOT NULL DEFAULT 0,
  rejected_count INT NOT NULL DEFAULT 0,
  fail_reason    VARCHAR(500) NULL,
  operator_id    BIGINT UNSIGNED NOT NULL,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at    DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch_no (batch_no),
  KEY idx_subject_created (subject_id, created_at),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 生成批次';

-- ---------------------------------------------------------------
-- 题目反馈（含版权投诉）
-- ---------------------------------------------------------------
CREATE TABLE rk_question_feedback (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  question_id   BIGINT UNSIGNED NOT NULL,
  user_id       BIGINT UNSIGNED NOT NULL,
  type          VARCHAR(24) NOT NULL COMMENT 'TYPO/WRONG_ANSWER/UNCLEAR_ANALYSIS/COPYRIGHT/OTHER',
  content       VARCHAR(1000) NOT NULL,
  status        VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSED/IGNORED',
  handler_id    BIGINT UNSIGNED NULL,
  handle_remark VARCHAR(500) NULL,
  handled_at    DATETIME NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status_created (status, created_at),
  KEY idx_question (question_id),
  KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目反馈与版权投诉';
```

## 4.5 考试与模板域

```sql
-- ---------------------------------------------------------------
-- 卷面模板（每科目一份，1:1）
-- 时长不再落在模板级：一份卷面可含多个场次（sitting），各场次有独立时长与独立倒计时。
-- 模板级满分为各场次之和（对应评审 A5，权威定义见 02 §2.4）。
-- ---------------------------------------------------------------
CREATE TABLE exam_paper_template (
  id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  subject_id            BIGINT UNSIGNED NOT NULL,
  name                  VARCHAR(64) NOT NULL,
  paper_type            VARCHAR(16) NOT NULL COMMENT 'JUNIOR_MID / SENIOR',
  total_max_score       INT NOT NULL COMMENT '卷面满分 = 各场次 total_max_score 之和（含主观题节，NOT_SUPPORTED 分节不计）',
  auto_scored_max_score INT NOT NULL COMMENT '可自动判分满分 = 各场次之和（仅客观题节）',
  remark                VARCHAR(255) NULL,
  status                VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_subject (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卷面模板';

-- ---------------------------------------------------------------
-- 卷面场次（模板 → 场次 → 分节 的中间层）
-- 高级卷面为「场次 1 = 综合知识 + 案例分析连考（240 分钟）」+「场次 2 = 论文独立（120 分钟）」，
-- 两场次之间隔着午休，故各有独立倒计时、互不自动衔接（对应评审 A5，见 02 §2.4.1）。
-- 场次内所有分节共享本场次的 total_duration。
-- ---------------------------------------------------------------
CREATE TABLE exam_paper_sitting (
  id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_id           BIGINT UNSIGNED NOT NULL,
  sitting_no            TINYINT      NOT NULL COMMENT '场次序号：1/2',
  sitting_name          VARCHAR(64)  NOT NULL COMMENT '如「连考场次」「论文章次」',
  total_duration        INT          NOT NULL COMMENT '本场次总时长（分钟）：240 或 120；场次内分节共享',
  total_max_score       INT          NOT NULL COMMENT '本场次满分（NOT_SUPPORTED 分节不计）',
  auto_scored_max_score INT          NOT NULL COMMENT '本场次可自动判分满分（仅客观题节）',
  sort_no               INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_sitting (template_id, sitting_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卷面场次';

-- ---------------------------------------------------------------
-- 卷面分节
-- 原「节余时长结转」字段已删除：场次内总时长共享，节余时长自动可用，该字段既非判定依据
-- 也不可配置（对应评审 B11，见 02 §2.4.3）。
-- ---------------------------------------------------------------
CREATE TABLE exam_paper_section (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_id        BIGINT UNSIGNED NOT NULL,
  sitting_no         TINYINT NOT NULL COMMENT '所属场次（1/2），对应 exam_paper_sitting.sitting_no',
  section_no         TINYINT NOT NULL COMMENT '1/2/3',
  section_name       VARCHAR(64) NOT NULL COMMENT '基础知识/应用技术/综合知识/案例分析/论文',
  section_type       VARCHAR(16) NOT NULL COMMENT 'OBJECTIVE / SUBJECTIVE / ESSAY',
  question_type      VARCHAR(24) NOT NULL,
  question_count     INT NOT NULL,
  per_question_score DECIMAL(4,1) NOT NULL,
  duration_min       INT NOT NULL COMMENT '本节最早可离开的时间点（分钟）。与 duration_max 同为必填（A6 裁决）：分节不存在"不需要时长约束"的场景，故建库期即用 NOT NULL 拦住漏填（对应评审 G3；与 B7 不同——B7 保持可空是因为 source_type 各取值所需字段本就不同）',
  duration_max       INT NOT NULL COMMENT '本节最晚必须离开的时间点（分钟）；不变式 Σ(duration_min) ≤ sitting.total_duration ≤ Σ(duration_max)，见 02 §2.4.2',
  allow_early_submit TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许提前交卷；论文节为 0',
  support_status     VARCHAR(16) NOT NULL DEFAULT 'SUPPORTED' COMMENT 'SUPPORTED / NOT_SUPPORTED（Office 操作题）',
  sort_no            INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_section_no (template_id, section_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卷面分节';

-- ---------------------------------------------------------------
-- 模拟考会话
-- 计时真值由场次的 started_at 派生（见 exam_session_sitting），本表 remaining_sec 仅为
-- 展示用缓存，由服务端按当前场次重算后回写；Redis 只承载会话状态与短锁
-- （如 rk:exam:creating:{userId}），不承载计时增量。客户端时间戳不可信（约束 C4，对应评审 A1）。
-- ---------------------------------------------------------------
CREATE TABLE exam_session (
  id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_no            VARCHAR(32) NOT NULL,
  user_id               BIGINT UNSIGNED NOT NULL,
  subject_id            BIGINT UNSIGNED NOT NULL,
  template_id           BIGINT UNSIGNED NOT NULL,
  status                VARCHAR(16) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/IN_PROGRESS/SUBMITTED/SCORED/ABANDONED',
  total_duration_sec    INT NOT NULL COMMENT '全场次总时长之和（展示用；各场次真值见 exam_session_sitting.total_duration_sec）',
  remaining_sec         INT NOT NULL COMMENT '当前场次的剩余秒数（展示用缓存；真值由 exam_session_sitting.started_at 派生）',
  current_sitting_no    TINYINT NOT NULL DEFAULT 1 COMMENT '当前进行到的场次（1/2）',
  current_section_no    TINYINT NOT NULL DEFAULT 1,
  question_count        INT NOT NULL,
  objective_score       DECIMAL(5,1) NOT NULL DEFAULT 0,
  auto_scored_max_score INT NOT NULL,
  total_max_score       INT NOT NULL,
  started_at            DATETIME NULL COMMENT '会话首次开始时间；场次级计时真值看 exam_session_sitting.started_at',
  submitted_at          DATETIME NULL,
  scored_at             DATETIME NULL,
  created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_session_no (session_no),
  KEY idx_user_subject_created (user_id, subject_id, created_at),
  KEY idx_status_updated (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟考会话';

-- ---------------------------------------------------------------
-- 会话场次（每个场次一条独立倒计时）
-- 计时真值的唯一来源：PENDING 阶段不倒计时、started_at 为空；IN_PROGRESS 阶段
--   remainingSec = max(0, total_duration_sec - (now - started_at))
-- 场次之间不自动衔接（午休/换场期间不扣时）。
-- （对应评审 A5，权威定义见 02 §2.4.1 与 §2.5.5）
-- ---------------------------------------------------------------
CREATE TABLE exam_session_sitting (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id         BIGINT UNSIGNED NOT NULL,
  sitting_no         TINYINT NOT NULL COMMENT '场次序号：1/2',
  sitting_name       VARCHAR(64) NOT NULL,
  total_duration_sec INT NOT NULL COMMENT '本场次总时长（秒）',
  remaining_sec      INT NOT NULL COMMENT '展示用缓存；IN_PROGRESS 时 = max(0, total_duration_sec - (now - started_at))，FINISHED 后冻结为 0',
  status             VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING（未启动，started_at 为空，不倒计时）/ IN_PROGRESS / FINISHED',
  current_section_no TINYINT NOT NULL DEFAULT 1,
  objective_score    DECIMAL(5,1) NOT NULL DEFAULT 0,
  started_at         DATETIME NULL COMMENT '本场次开始时间；为空即未启动（PENDING）',
  finished_at        DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_session_sitting (session_id, sitting_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟考场次';

-- ---------------------------------------------------------------
-- 会话分节（记录每节的得分与用时）
-- 原「结转」相关两字段已删除：场次内总时长共享，节余自动可用，
-- 结转字段既非判定依据也不可配置（对应评审 B11）。
-- ---------------------------------------------------------------
CREATE TABLE exam_session_section (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id         BIGINT UNSIGNED NOT NULL,
  sitting_no         TINYINT NOT NULL COMMENT '所属场次（1/2）；创建会话时从 exam_paper_section 快照（对应评审 G2：不 JOIN 回模板表，否则管理员修正模板会让历史成绩的场次归属漂移）',
  section_no         TINYINT NOT NULL,
  section_name       VARCHAR(64) NOT NULL,
  section_type       VARCHAR(16) NOT NULL,
  question_count     INT NOT NULL,
  max_score          DECIMAL(5,1) NOT NULL,
  objective_score    DECIMAL(5,1) NOT NULL DEFAULT 0,
  duration_min       INT NOT NULL COMMENT '快照自模板；语义见 02 §2.4.3',
  duration_max       INT NOT NULL COMMENT '快照自模板（模板列已 NOT NULL，故此处同步 NOT NULL，否则快照列仍可能为空）',
  allow_early_submit TINYINT(1) NOT NULL DEFAULT 1,
  support_status     VARCHAR(16) NOT NULL DEFAULT 'SUPPORTED' COMMENT 'SUPPORTED / NOT_SUPPORTED，快照自模板（对应评审 G2：成绩页据此展示"本平台无法模拟"）',
  used_sec           INT NOT NULL DEFAULT 0,
  status             VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/FINISHED/SKIPPED；权威定义见 02 §2.5.6（对应评审 G5）',
  started_at         DATETIME NULL,
  finished_at        DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_session_section (session_id, section_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟考分节';

-- ---------------------------------------------------------------
-- 会话题目快照
-- 快照题干/选项/正确答案，保证历史成绩在题目被编辑或下架后仍可复现（见 02 文档 2.4.3）。
-- reference_snapshot / analysis_snapshot 在交卷时才写入，避免组卷时搬运大字段。
-- user_answer 用 MEDIUMTEXT：论文作答最长约 2500 字；客观题只存选项 key，长度可忽略。
-- ---------------------------------------------------------------
CREATE TABLE exam_session_item (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id         BIGINT UNSIGNED NOT NULL,
  section_no         TINYINT NOT NULL,
  sequence           INT NOT NULL COMMENT '全卷题序，从 1 开始',
  question_id        BIGINT UNSIGNED NOT NULL,
  question_type      VARCHAR(24) NOT NULL,
  stem_snapshot      MEDIUMTEXT NOT NULL,
  options_snapshot   JSON NULL,
  correct_answer     VARCHAR(32) NULL,
  reference_snapshot MEDIUMTEXT NULL COMMENT '交卷时写入',
  analysis_snapshot  MEDIUMTEXT NULL COMMENT '交卷时写入',
  score              DECIMAL(4,1) NOT NULL COMMENT '本题满分',
  user_answer        MEDIUMTEXT NULL,
  is_correct         TINYINT(1) NULL COMMENT '主观题为 NULL',
  duration_ms        INT NULL,
  answered_at        DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_session_sequence (session_id, sequence),
  KEY idx_session_section (session_id, section_no),
  KEY idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟考题目快照';
```

## 4.6 统计域

统计表是 `user_answer_record` 的**聚合缓存**，可随时由流水重算。用途是避免每次打开统计页都全量扫描流水表。

```sql
-- 科目维度汇总
CREATE TABLE stat_user_subject (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id         BIGINT UNSIGNED NOT NULL,
  subject_id      BIGINT UNSIGNED NOT NULL,
  answered_count  INT NOT NULL DEFAULT 0,
  correct_count   INT NOT NULL DEFAULT 0,
  accuracy        DECIMAL(5,4) NOT NULL DEFAULT 0 COMMENT '0~1',
  mastery         DECIMAL(5,4) NOT NULL DEFAULT 0 COMMENT '掌握度，公式见 06 文档',
  exam_count      INT NOT NULL DEFAULT 0 COMMENT '模考次数',
  best_score      DECIMAL(5,1) NULL    COMMENT '最高客观题得分',
  last_answered_at DATETIME NULL,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_subject (user_id, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户科目统计';

-- 章节维度（掌握度按章节聚合）
CREATE TABLE stat_user_outline (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id        BIGINT UNSIGNED NOT NULL,
  subject_id     BIGINT UNSIGNED NOT NULL,
  outline_id     BIGINT UNSIGNED NOT NULL,
  answered_count INT NOT NULL DEFAULT 0,
  correct_count  INT NOT NULL DEFAULT 0,
  accuracy       DECIMAL(5,4) NOT NULL DEFAULT 0,
  mastery        DECIMAL(5,4) NOT NULL DEFAULT 0,
  last_answered_at DATETIME NULL,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_outline (user_id, outline_id),
  KEY idx_user_subject (user_id, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户章节统计';

-- 知识点维度（薄弱点画像）
CREATE TABLE stat_user_kp (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id        BIGINT UNSIGNED NOT NULL,
  subject_id     BIGINT UNSIGNED NOT NULL,
  kp_id          BIGINT UNSIGNED NOT NULL,
  answered_count INT NOT NULL DEFAULT 0,
  correct_count  INT NOT NULL DEFAULT 0,
  accuracy       DECIMAL(5,4) NOT NULL DEFAULT 0,
  mastery        DECIMAL(5,4) NOT NULL DEFAULT 0,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_kp (user_id, kp_id),
  KEY idx_user_subject_mastery (user_id, subject_id, mastery)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户知识点统计';

-- 每日活跃（趋势图与连续学习天数）
CREATE TABLE stat_daily (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id        BIGINT UNSIGNED NOT NULL,
  stat_date      DATE NOT NULL,
  answered_count INT NOT NULL DEFAULT 0,
  correct_count  INT NOT NULL DEFAULT 0,
  duration_sec   INT NOT NULL DEFAULT 0,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_date (user_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日统计';
```

**聚合时机**：答题提交后**增量更新**（同一事务或异步，见 06 文档）；管理员重建统计时全量重算。`stat_daily.duration_sec` 由单题 `duration_ms` 累加，单题超 10 分钟截断。

## 4.7 系统域

```sql
CREATE TABLE sys_config (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  config_key   VARCHAR(64) NOT NULL,
  config_value VARCHAR(2000) NOT NULL,
  remark       VARCHAR(255) NULL,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

-- 高危操作审计（约束 AC-2）
CREATE TABLE sys_operation_log (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  module      VARCHAR(32) NOT NULL COMMENT 'QUESTION/REVIEW/SUBJECT/USER/CONFIG',
  action      VARCHAR(32) NOT NULL COMMENT 'CREATE/UPDATE/DELETE/PUBLISH/OFFLINE/RESET_PASSWORD/DISABLE/ENABLE',
  target_type VARCHAR(32) NULL,
  target_id   BIGINT UNSIGNED NULL,
  detail      VARCHAR(1000) NULL,
  ip          VARCHAR(45) NULL COMMENT '兼容 IPv6',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_created (user_id, created_at),
  KEY idx_module_action (module, action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';
```

## 4.8 初始化数据

初始化 SQL 拆为四个文件，**除 `verify.sql` 外的三个执行顺序不可颠倒**（校验脚本永远最后跑）：

| 文件 | 内容 | 说明 |
| --- | --- | --- |
| `deploy/sql/schema.sql` | 27 张表 DDL | **首部必须包含 `CREATE DATABASE IF NOT EXISTS ruankao DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;` 与 `USE ruankao;`**，使该文件可一次性执行（见 09 文档首次上线步骤） |
| `deploy/sql/init-data.sql` | 20 科目、卷面模板/场次/分节、管理员、系统配置 | 幂等（`INSERT ... ON DUPLICATE KEY UPDATE`），可重复执行 |
| `deploy/sql/outline-*.sql` | 3 个热门科目的章节树 | 按科目一个文件，便于单独更新。**P2 交付**，P1 阶段该文件尚不存在，科目详情返回空大纲（`outlineStatus=SKELETON`）属预期 |
| `deploy/sql/verify.sql` | 建库后的**校验脚本**（不是建库脚本，不会被 init-data 调用） | 用途：在 `schema.sql` + `init-data.sql`（+ 将来的 `outline-*.sql`）跑完后，断言库结构与种子数据完整。执行：`mysql -h127.0.0.1 -uroot -p ruankao < deploy/sql/verify.sql`。**判读方式见下方**。对应 [10 §10.3](10-roadmap.md) 的 P1 验收 |

**`verify.sql` 的判读方式（两类判据，不要混用）**：

| 判据 | 分节 | 通过标准 |
| --- | --- | --- |
| **计数类**：脚本直接打印数字，并与节内注释的「期望值」比对 | 1 表数量、2 行数、3 必填列、7 管理员与配置 | 逐项等于期望值（**27** 张表 / **20** 科目 / **20** 模板 / **25** 场次 / **45** 分节 / **15** 配置键 / 必填列缺失 **0**） |
| **断言类**：脚本只在**违规时**才输出行（"违规即出行"） | 2 缺表检测、4 时长不变式、5 满分口径一致性、6 非 IPT 的不支持分节 | **输出为空即通过，有行输出即失败** |

> 决策与理由
> 为什么计数类不也写成"违规才出行"：能写的话早写了——例如"表数量 = 27"写成断言需要把期望值嵌进 SQL，而那个数字本身就出自本文档 §4.1 的「分域总览（共 27 张表）」；把期望值放在注释里由人比对，比在脚本里再造一份常量更不容易漂移。**断言类是默认写法**，只在"无法用一条 SQL 表达期望值"时才退回计数类。
> 第 6 节有一处**不是**违规输出：IPT 的分节明细会被打印出来用于人工核对，它的期望是「恰好 1 行 `NOT_SUPPORTED`」——这是少见的"有输出才是对的"一节，读之前先看节标题。

### 4.8.1 科目（20 条）

```sql
INSERT INTO rk_subject (level, code, name, paper_type, exam_period, outline_status, content_status, sort_no) VALUES
('SENIOR','SA',  '系统分析师',            'SENIOR',     'BOTH',        'SKELETON','SAMPLE',10),
('SENIOR','SAD', '系统架构设计师',        'SENIOR',     'BOTH',        'FULL',    'SAMPLE',11),
('SENIOR','IPMP','信息系统项目管理师',    'SENIOR',     'FIRST_HALF',  'FULL',    'SAMPLE',12),
('SENIOR','NPD', '网络规划设计师',        'SENIOR',     'SECOND_HALF', 'SKELETON','SAMPLE',13),
('SENIOR','SPM', '系统规划与管理师',      'SENIOR',     'SECOND_HALF', 'SKELETON','SAMPLE',14),
('MIDDLE','SD',  '软件设计师',            'JUNIOR_MID', 'BOTH',        'FULL',    'SAMPLE',20),
('MIDDLE','NE',  '网络工程师',            'JUNIOR_MID', 'BOTH',        'SKELETON','SAMPLE',21),
('MIDDLE','ST',  '软件评测师',            'JUNIOR_MID', 'FIRST_HALF',  'SKELETON','SAMPLE',22),
('MIDDLE','ECD', '电子商务设计师',        'JUNIOR_MID', 'FIRST_HALF',  'SKELETON','SAMPLE',23),
('MIDDLE','ESD', '嵌入式系统设计师',      'JUNIOR_MID', 'FIRST_HALF',  'SKELETON','SAMPLE',24),
('MIDDLE','DSE', '数据库系统工程师',      'JUNIOR_MID', 'FIRST_HALF',  'SKELETON','SAMPLE',25),
('MIDDLE','ISL', '信息系统监理师',        'JUNIOR_MID', 'FIRST_HALF',  'SKELETON','SAMPLE',26),
('MIDDLE','SIPE','系统集成项目管理工程师','JUNIOR_MID', 'BOTH',        'SKELETON','SAMPLE',27),
('MIDDLE','ISE', '信息安全工程师',        'JUNIOR_MID', 'SECOND_HALF', 'SKELETON','SAMPLE',28),
('MIDDLE','ISME','信息系统管理工程师',    'JUNIOR_MID', 'SECOND_HALF', 'SKELETON','SAMPLE',29),
('MIDDLE','MAD', '多媒体应用设计师',      'JUNIOR_MID', 'SECOND_HALF', 'SKELETON','SAMPLE',30),
('JUNIOR','IPT', '信息处理技术员',        'JUNIOR_MID', 'BOTH',        'SKELETON','SAMPLE',40),
('JUNIOR','IOMA','信息系统运行管理员',    'JUNIOR_MID', 'FIRST_HALF',  'SKELETON','SAMPLE',41),
('JUNIOR','PG',  '程序员',                'JUNIOR_MID', 'SECOND_HALF', 'SKELETON','SAMPLE',42),
('JUNIOR','NA',  '网络管理员',            'JUNIOR_MID', 'SECOND_HALF', 'SKELETON','SAMPLE',43);
```

> 注：3 个 `FULL` 科目（软件设计师、系统架构设计师、信息系统项目管理师）的完整章节树由 `deploy/sql/outline-*.sql` 单独导入，章节标题取自官方大纲标题（`origin='OFFICIAL_TITLE'`），不含大纲正文。

> 注（`23` 轮回填第 4 条）：上表 3 门 `FULL` 科目的 `content_status` 是 `SAMPLE`，与 `outline_status = FULL` **不矛盾**——两列语义不同：`outline_status` 说"大纲结构打印到什么程度"，`content_status` 说"**当前**题库规模"（`SAMPLE` 样例 / `BULK` 200~300 题）。当前 20 门 `question_count` 全为 `0`，故 `content_status` 全为 `SAMPLE`；待 P3 真题量到位后再把这 3 门翻为 `BULK`。此前这 3 门误标 `BULK`，与"0 题"自相矛盾（该矛盾由 `18 §6` / `19 §2` 发现，处置登记于 `22 §3.4`、`23 §2.2 第 4 条`）。**不要**因为"FULL 却 SAMPLE"再判为数据错误。

### 4.8.2 卷面模板、场次与分节

卷面按「**模板 → 场次（sitting）→ 分节（section）**」三层初始化，每科目一份模板（权威蓝图见 02 §2.4.2）。同一场次内的分节共享该场次的 `total_duration`；`section_no` 在模板内全局唯一（1/2/3），`sitting_no` 决定分节归属哪个场次。

**校验不变式（必须成立）**：

```
Σ(duration_min) ≤ sitting.total_duration ≤ Σ(duration_max)
```

```sql
-- 【JUNIOR_MID｜初级、中级】每科目一份模板：total_max_score=150、auto_scored_max_score=75
--   场次 1：sitting_no=1, sitting_name='连考场次', total_duration=240,
--           total_max_score=150, auto_scored_max_score=75
--     ① section_no=1 基础知识 SINGLE_CHOICE × 75，per_question_score=1，
--        duration_min=90, duration_max=120, allow_early_submit=1
--     ② section_no=2 应用技术 SUBJECTIVE × 5，per_question_score=15，
--        duration_min=120, duration_max=150, allow_early_submit=1
--   验算：90 + 120 = 210 ≤ 240 ≤ 120 + 150 = 270 ✓

-- 【SENIOR｜高级】每科目一份模板：total_max_score=225、auto_scored_max_score=75
--   场次 1：sitting_no=1, sitting_name='连考场次', total_duration=240,
--           total_max_score=150, auto_scored_max_score=75
--     ① section_no=1 综合知识 SINGLE_CHOICE × 75，per_question_score=1，
--        duration_min=120, duration_max=150, allow_early_submit=1
--     ② section_no=2 案例分析 SUBJECTIVE × 3，per_question_score=25，
--        duration_min=90, duration_max=120, allow_early_submit=1
--   验算：120 + 90 = 210 ≤ 240 ≤ 150 + 120 = 270 ✓
--   场次 2：sitting_no=2, sitting_name='论文章次', total_duration=120,
--           total_max_score=75, auto_scored_max_score=0
--     ③ section_no=3 论文 ESSAY × 1，per_question_score=75，
--        duration_min=120, duration_max=120, allow_early_submit=0
--   验算：120 ≤ 120 ≤ 120 ✓
--   注：场次 2 是下午的独立场次，由用户主动点「开始论文」才启动计时，
--       与场次 1 之间隔着午休，不自动衔接、不结转节余（见 02 §2.4.1）。

-- 【信息处理技术员（code='IPT'）覆盖（对应评审 B13）】
--   它是初级/中级的连考科目，场次 1 的 total_duration=240 照常保留；
--   但其「应用技术」分节覆盖为 support_status='NOT_SUPPORTED'、question_count=0、
--   per_question_score=0，该分节不参与组卷、不计入任何满分口径（见 02 §2.4.4），
--   故该科目模板 total_max_score=75、auto_scored_max_score=75（仅基础知识节）。

-- 约束：exam_paper_sitting.total_duration 为 NOT NULL，初始 SQL 必须为全部 20 个科目
--   覆盖其场次与时长（避免再出现某科目时长未定义）。
```

### 4.8.3 管理员账号

首个管理员由 `deploy/sql/init-data.sql` 插入，密码为**占位哈希**，首次登录强制修改：

```sql
INSERT INTO sys_user (username, password_hash, nickname, role, status, must_change_password) VALUES
('admin', '<BCrypt占位，待 rk-boot 的哈希生成器产出>', '管理员', 'ADMIN', 'ACTIVE', 1)
ON DUPLICATE KEY UPDATE
  username = VALUES(username);      -- 无实义写：重跑不覆盖 password_hash，也不重置 must_change_password
```

> 决策与理由
> **示例必须与 `init-data.sql` 逐字一致（对应 [16](16-p1-code-audit.md) §3 的 D11）。** 这是最容易出事的一类偏差：本节的示例 INSERT 若少了 `must_change_password`，照抄的人会得到 `must_change_password = 0`（列有默认值），于是"首次登录强制改密"这条策略**静默失效**——不报错，只是管理员永远不被要求改密。占位串的文案同理：示例与脚本写得不一样，就没有人能一眼看出"这两个是同一件事"。
> `ON DUPLICATE KEY UPDATE` 分支**有意只写 `username`**：真实哈希在上线时一次性 `UPDATE` 写入，重跑本文件既不能把已生效的哈希打回占位串，也不能把管理员已经改过的密码重新标记为"必须改密"。**无实义写是刻意的**——写成 `password_hash = VALUES(password_hash)` 会让上面两条同时失效。
INSERT INTO sys_config (config_key, config_value, remark) VALUES
('site.name',              '软考在线',                     '站点名称'),
('site.icp',               '',                             '备案号，如无可留空'),
('question.repeat.window', '30',                           '组卷防重复窗口（天）'),
('mastery.half.life',      '30',                           '掌握度时间衰减半衰期（天）'),
('mastery.prior',          '0.5',                          '新题掌握度先验'),
('judge.max.duration.ms',  '600000',                       '单题耗时上限，超出按此值截断'),
('wrongbook.master.threshold', '2',                        '错题本连续答对几次移出（06 §6.8）'),
('admin.must.change.password', 'true',                     '管理员首次登录强制改密（06 §6.5）'),
('task.enabled.exam.timeout',       'true',                '定时任务开关：超时会话结算（06 §6.13）'),
('task.enabled.stat.rebuild',       'true',                '定时任务开关：统计全量重算（06 §6.13）'),
('task.enabled.count.check',        'true',                '定时任务开关：冗余计数校验（06 §6.13）'),
('task.enabled.session.clean',      'true',                '定时任务开关：过期数据清理（06 §6.13）'),
('task.enabled.batch.consistency',  'true',                '定时任务开关：批次一致性巡检（06 §6.13）'),
('task.enabled.session.stale',     'true',                '定时任务开关：会话级滞留结算（06 §6.13）'),
('exam.session.stale.hours',       '6',                   '会话滞留自动交卷阈值（小时）；判据是上一场次的 finished_at，对应评审 G1');
```

> 约定：06/08 新增 `sys_config` 配置键时，必须同步回本小节的初始化清单（对应评审 C3）。

## 4.9 容量与索引策略

**本表是全项目容量数字的唯一出处**：01 §1.5 的「2 万 / 500 万」是**设计容量上限**（非阶段一目标量），03 §3.7 直接引用本表数字（对应评审 A4）。

| 表 | 预估行数（1 年） | 主要压力点 | 应对 |
| --- | --- | --- | --- |
| `rk_question` | ≤ 5000 | 组卷随机取题 | `idx_subject_status_type` + `outline_id` 过滤；随机取题用 `ORDER BY RAND()` **仅在题量小（<2000/科目）时可接受**，超过后改为"按 ID 区间随机偏移"策略，见 06 文档 |
| `user_answer_record` | ≤ 400 万 | 统计聚合 | `idx_user_subject`、`idx_user_outline`；统计结果落到 `stat_*` 表，避免直查流水 |
| `exam_session_item` | ≤ 100 万 | 快照大字段 | 每次模考仅 75~85 行；归档策略：超过 1 年的会话可迁出（不在阶段一实现） |
| `stat_daily` | 用户数 × 天数 | 趋势图 | `uk_user_date` 覆盖 |
| 其他 | 小 | — | — |

**分区、分表、物化视图均不引入**：当前量级下单表 + 合理索引足够，引入分区会提高维护复杂度且无收益。