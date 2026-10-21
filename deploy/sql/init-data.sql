-- ===============================================================
-- 软考在线学习平台 · 初始化数据
--
-- 前置：必须先执行 schema.sql（本文件不建表）。
-- 执行：mysql -h127.0.0.1 -uroot -p ruankao < deploy/sql/init-data.sql
--
-- 唯一事实来源：docs/04-database.md §4.8。
-- 幂等：全部 UPSERT，可重复执行。
--
-- 落地了三项遗留（见 docs/14-changes-round2.md §5）：
--   L1  20 个科目的完整 INSERT（含场次与分节）——此前只有蓝图注释
--   L2  管理员初始记录以 must_change_password = 1 插入
--   L3  exam_paper_section / exam_session_section 的 4 个时长列全部填值
--       （两表该列已是 NOT NULL，漏填在建库期即报错，这是期望行为）
--
-- 关于 ON DUPLICATE KEY UPDATE 的 VALUES() 写法：
--   MySQL 8.0.20+ 已将其标记为 deprecated（未来版本移除）。此处仍采用，因为它
--   是本文件唯一能在 INSERT ... SELECT 上表达"按业务键 upsert"的写法（行别名
--   形式不支持 SELECT 源）。批量执行时该警告不显示，且不影响结果。
--   将来若要消除，需把派生表改写成显式 VALUES 行列表。
-- ===============================================================

SET NAMES utf8mb4;
USE ruankao;
START TRANSACTION;


-- ===============================================================
-- §4.8.1 科目（20 条）
-- ===============================================================
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
('JUNIOR','NA',  '网络管理员',            'JUNIOR_MID', 'SECOND_HALF', 'SKELETON','SAMPLE',43)
ON DUPLICATE KEY UPDATE
  level          = VALUES(level),
  name           = VALUES(name),
  paper_type     = VALUES(paper_type),
  exam_period    = VALUES(exam_period),
  outline_status = VALUES(outline_status),
  content_status = VALUES(content_status),
  sort_no        = VALUES(sort_no);


-- ===============================================================
-- §4.8.2 卷面模板（20 份，每科目一份）
--
-- 满分口径（权威定义见 02 §2.4.4）：
--   SENIOR      total=225, auto=75   （场次 1：150/75 + 场次 2：75/0）
--   JUNIOR_MID  total=150, auto=75   （场次 1：150/75）
--   IPT 覆盖    total=75,  auto=75   （「应用技术」节 NOT_SUPPORTED，不计入任何满分）
-- ===============================================================
INSERT INTO exam_paper_template (subject_id, name, paper_type, total_max_score, auto_scored_max_score, status)
SELECT s.id,
       CONCAT(s.name, ' 模拟卷'),
       s.paper_type,
       CASE WHEN s.code = 'IPT' THEN 75
            WHEN s.paper_type = 'SENIOR' THEN 225
            ELSE 150 END,
       75,
       'ENABLED'
FROM rk_subject s
ON DUPLICATE KEY UPDATE
  name                  = VALUES(name),
  paper_type            = VALUES(paper_type),
  total_max_score       = VALUES(total_max_score),
  auto_scored_max_score = VALUES(auto_scored_max_score),
  status                = VALUES(status);


-- ===============================================================
-- §4.8.2 卷面场次（25 条 = SENIOR 5×2 + JUNIOR_MID 15×1）
--
-- 场次之间不自动衔接：高级卷面午休前是场次 1（连考 240 分钟），
-- 午休后由用户主动开始场次 2（论文 120 分钟），各有独立倒计时。
-- IPT 的场次 1 total_max_score 随其「应用技术」节被覆盖而降为 75。
-- ===============================================================
INSERT INTO exam_paper_sitting (template_id, sitting_no, sitting_name, total_duration, total_max_score, auto_scored_max_score, sort_no)
SELECT t.id,
       b.sitting_no,
       b.sitting_name,
       b.total_duration,
       CASE WHEN s.code = 'IPT' AND b.sitting_no = 1 THEN 75 ELSE b.total_max_score END,
       b.auto_scored_max_score,
       b.sort_no
FROM rk_subject s
JOIN exam_paper_template t ON t.subject_id = s.id
JOIN (
            SELECT 'SENIOR'     AS paper_type, 1 AS sitting_no, '连考场次' AS sitting_name,
                   240 AS total_duration, 150 AS total_max_score, 75 AS auto_scored_max_score, 10 AS sort_no
  UNION ALL SELECT 'SENIOR',     2, '论文章次', 120,  75,  0, 20
  UNION ALL SELECT 'JUNIOR_MID', 1, '连考场次', 240, 150, 75, 10
) b ON b.paper_type = s.paper_type
ON DUPLICATE KEY UPDATE
  sitting_name          = VALUES(sitting_name),
  total_duration        = VALUES(total_duration),
  total_max_score       = VALUES(total_max_score),
  auto_scored_max_score = VALUES(auto_scored_max_score),
  sort_no               = VALUES(sort_no);


-- ===============================================================
-- §4.8.2 卷面分节（45 条 = SENIOR 5×3 + JUNIOR_MID 15×2）
--
-- 不变式（必须成立，校验见 deploy/sql/verify.sql）：
--   Σ(duration_min) ≤ sitting.total_duration ≤ Σ(duration_max)
--   初级/中级 90+120=210 ≤ 240 ≤ 120+150=270   ✓
--   高级场次一 120+90=210 ≤ 240 ≤ 150+120=270  ✓
--   高级场次二 120 ≤ 120 ≤ 120                 ✓
--
-- IPT 覆盖（对应评审 B13）：其「应用技术」节为 NOT_SUPPORTED，
--   question_count ⇒ 0、per_question_score ⇒ 0、不参与组卷与满分口径。
--   注意 duration_min / duration_max **不变**（仍为基线 120/150）——
--   本节在时长模型里照常存在，只是不可作答（这也是这两列能 NOT NULL 的原因，见 04 §4.8.2）。
-- ===============================================================
INSERT INTO exam_paper_section (template_id, sitting_no, section_no, section_name, section_type, question_type,
                                question_count, per_question_score, duration_min, duration_max,
                                allow_early_submit, support_status, sort_no)
SELECT t.id,
       b.sitting_no,
       b.section_no,
       b.section_name,
       b.section_type,
       b.question_type,
       CASE WHEN s.code = 'IPT' AND b.section_no = 2 THEN 0 ELSE b.question_count END,
       CASE WHEN s.code = 'IPT' AND b.section_no = 2 THEN 0 ELSE b.per_question_score END,
       b.duration_min,
       b.duration_max,
       b.allow_early_submit,
       CASE WHEN s.code = 'IPT' AND b.section_no = 2 THEN 'NOT_SUPPORTED' ELSE 'SUPPORTED' END,
       b.sort_no
FROM rk_subject s
JOIN exam_paper_template t ON t.subject_id = s.id
JOIN (
            SELECT 'SENIOR'     AS paper_type, 1 AS sitting_no, 1 AS section_no, '综合知识' AS section_name,
                   'OBJECTIVE'  AS section_type, 'SINGLE_CHOICE' AS question_type,
                   75 AS question_count, 1  AS per_question_score, 120 AS duration_min, 150 AS duration_max,
                   1  AS allow_early_submit, 10 AS sort_no
  UNION ALL SELECT 'SENIOR',     1, 2, '案例分析', 'SUBJECTIVE', 'SUBJECTIVE',   3, 25,  90, 120, 1, 20
  UNION ALL SELECT 'SENIOR',     2, 3, '论文',     'ESSAY',      'ESSAY',        1, 75, 120, 120, 0, 30
  UNION ALL SELECT 'JUNIOR_MID', 1, 1, '基础知识', 'OBJECTIVE',  'SINGLE_CHOICE',75,  1,  90, 120, 1, 10
  UNION ALL SELECT 'JUNIOR_MID', 1, 2, '应用技术', 'SUBJECTIVE', 'SUBJECTIVE',   5, 15, 120, 150, 1, 20
) b ON b.paper_type = s.paper_type
ON DUPLICATE KEY UPDATE
  sitting_no         = VALUES(sitting_no),
  section_name       = VALUES(section_name),
  section_type       = VALUES(section_type),
  question_type      = VALUES(question_type),
  question_count     = VALUES(question_count),
  per_question_score = VALUES(per_question_score),
  duration_min       = VALUES(duration_min),
  duration_max       = VALUES(duration_max),
  allow_early_submit = VALUES(allow_early_submit),
  support_status     = VALUES(support_status),
  sort_no            = VALUES(sort_no);


-- ===============================================================
-- §4.8.3 管理员账号
--
-- ⚠️ 这里刻意保留占位哈希。**不要**把真实哈希写进本文件，也不要指望
--    "跑完初始化就能用初始密码登录"：
--   · 本文件会随仓库公开，写在这里的哈希等于把管理员口令的校验值公开，
--     任何拿到源码的人都能离线爆破（09 §9.9 的决策）。
--   · 真实哈希在上线时一次性写入，不入库、不入版本控制：
--       java -jar rk-boot.jar --rk.password-gen='ChangeMe@2026'   # 输出 60 字符 $2a$10$ 哈希
--       UPDATE sys_user SET password_hash='<上一步输出>' WHERE username='admin';
--     然后核查 LENGTH(password_hash) = 60（09 §9.9 第 7 步）。
--   · 未替换前该账号**登录必然失败**（占位串格式非法，比对返回 false，不是 500）；
--     登录接口上线后需先完成上面这次 UPDATE，才能执行 09 的"初始密码登录"核查项。
--   · 首登强制改密（must_change_password = 1，落地遗留 L2）保持不变。
--   · 重复执行本文件**不会**把 must_change_password 重置为 1，
--     否则管理员改密后每次重跑初始化都会被再次强制改密（故 upsert 分支为无实义写）。
--   · upsert 分支只写 username，**不覆盖 password_hash**：重跑本文件不会
--     把已生效的真实哈希打回占位串。
-- ===============================================================
INSERT INTO sys_user (username, password_hash, nickname, role, status, must_change_password) VALUES
('admin', '<BCrypt占位，待 rk-boot 的哈希生成器产出>', '管理员', 'ADMIN', 'ACTIVE', 1)
ON DUPLICATE KEY UPDATE
  username = VALUES(username);


-- ===============================================================
-- §4.8.3 系统配置（15 个键）
-- 约定（对应评审 C3）：06/08 新增配置键时必须同步回本清单，否则键在 04 无登记。
-- ===============================================================
INSERT INTO sys_config (config_key, config_value, remark) VALUES
('site.name',                       '软考在线',  '站点名称'),
('site.icp',                        '',          '备案号，如无可留空'),
('question.repeat.window',          '30',        '组卷防重复窗口（天）'),
('mastery.half.life',               '30',        '掌握度时间衰减半衰期（天）'),
('mastery.prior',                   '0.5',       '新题掌握度先验'),
('judge.max.duration.ms',           '600000',    '单题耗时上限，超出按此值截断'),
('wrongbook.master.threshold',      '2',         '错题本连续答对几次移出（06 §6.8）'),
('admin.must.change.password',      'true',      '管理员首次登录强制改密（06 §6.5）'),
('task.enabled.exam.timeout',       'true',      '定时任务开关：超时会话结算（06 §6.13）'),
('task.enabled.stat.rebuild',       'true',      '定时任务开关：统计全量重算（06 §6.13）'),
('task.enabled.count.check',        'true',      '定时任务开关：冗余计数校验（06 §6.13）'),
('task.enabled.session.clean',      'true',      '定时任务开关：过期数据清理（06 §6.13）'),
('task.enabled.batch.consistency',  'true',      '定时任务开关：批次一致性巡检（06 §6.13）'),
('task.enabled.session.stale',      'true',      '定时任务开关：会话级滞留结算（06 §6.13）'),
('exam.session.stale.hours',        '6',         '会话滞留自动交卷阈值（小时）；判据是上一场次的 finished_at，对应评审 G1')
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  remark       = VALUES(remark);


COMMIT;
