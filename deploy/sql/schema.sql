-- ===============================================================
-- 软考在线学习平台 · 建表脚本（共 27 张表）
--
-- 唯一事实来源：docs/04-database.md §4.3 ~ §4.7。
--   与 04 冲突时以 04 为准；改动本文件必须同步回 04。
--
-- 执行：mysql -h127.0.0.1 -uroot -p < deploy/sql/schema.sql
--   本文件可一次性执行（首部含 CREATE DATABASE 与 USE，见 04 §4.8）。
--
-- 设计约定（04 §4.1）：
--   · 主键 id BIGINT UNSIGNED AUTO_INCREMENT
--   · 时间用 DATETIME（不用 TIMESTAMP，避免 2038 与时区隐式转换）
--   · 枚举用 VARCHAR + 注释列出取值，不用 MySQL ENUM
--   · 不建物理外键，只建索引
--   · 布尔用 TINYINT(1)，以 is_ / allow_ 开头
--
-- 注意：本脚本只建表、不删表（非破坏性）。重复执行会因表已存在而报错，
--   这是有意的——静默跳过会掩盖 schema 漂移。需要重建时请先自行 DROP。
-- ===============================================================

CREATE DATABASE IF NOT EXISTS ruankao DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE ruankao;
SET NAMES utf8mb4;


-- ===============================================================
-- §4.3 用户域（5 张）
-- ===============================================================

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


-- ===============================================================
-- §4.4 题库域（9 张）
-- ===============================================================

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


-- ===============================================================
-- §4.5 考试与模板域（7 张）
-- ===============================================================

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


-- ===============================================================
-- §4.6 统计域（4 张）
-- 统计表是 user_answer_record 的聚合缓存，可随时由流水重算。
-- ===============================================================

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


-- ===============================================================
-- §4.7 系统域（2 张）
-- ===============================================================

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
