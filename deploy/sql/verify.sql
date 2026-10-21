-- ===============================================================
-- 软考在线学习平台 · 初始化结果校验
--
-- 执行：mysql -h127.0.0.1 -uroot -p ruankao < deploy/sql/verify.sql
--
-- 用法：每一节给出「期望值」。凡是**列出多余行**的就是失败——
--   设计成"违规即出行为空表"，比打印 OK 更不容易自欺。
--
-- 对应 10-roadmap.md §10.3 P1 验收第 2、3 条。
-- ===============================================================

SET NAMES utf8mb4;
USE ruankao;

SELECT '========== 1. 表数量 ==========' AS section;

-- 期望：27
SELECT COUNT(*) AS table_count_期望27
FROM information_schema.tables
WHERE table_schema = 'ruankao';

-- 期望：无输出（多表/缺表都会在这里暴露）
SELECT expected.name AS 缺失的表
FROM (
            SELECT 'sys_user' AS name              UNION ALL SELECT 'user_wrong_question'
  UNION ALL SELECT 'user_favorite'                 UNION ALL SELECT 'user_answer_record'
  UNION ALL SELECT 'user_practice_progress'        UNION ALL SELECT 'rk_subject'
  UNION ALL SELECT 'rk_outline_node'               UNION ALL SELECT 'rk_knowledge_point'
  UNION ALL SELECT 'rk_question'                   UNION ALL SELECT 'rk_question_option'
  UNION ALL SELECT 'rk_question_kp'                UNION ALL SELECT 'rk_question_review_log'
  UNION ALL SELECT 'rk_generation_batch'           UNION ALL SELECT 'rk_question_feedback'
  UNION ALL SELECT 'exam_paper_template'           UNION ALL SELECT 'exam_paper_sitting'
  UNION ALL SELECT 'exam_paper_section'            UNION ALL SELECT 'exam_session'
  UNION ALL SELECT 'exam_session_sitting'          UNION ALL SELECT 'exam_session_section'
  UNION ALL SELECT 'exam_session_item'             UNION ALL SELECT 'stat_user_subject'
  UNION ALL SELECT 'stat_user_outline'             UNION ALL SELECT 'stat_user_kp'
  UNION ALL SELECT 'stat_daily'                    UNION ALL SELECT 'sys_config'
  UNION ALL SELECT 'sys_operation_log'
) expected
LEFT JOIN information_schema.tables t
       ON t.table_schema = 'ruankao' AND t.table_name = expected.name
WHERE t.table_name IS NULL;


SELECT '========== 2. 行数 ==========' AS section;

-- 期望：20
SELECT COUNT(*) AS 科目数_期望20 FROM rk_subject;
-- 期望：20
SELECT COUNT(*) AS 模板数_期望20 FROM exam_paper_template;
-- 期望：25（高级 5×2 + 初/中级 15×1）
SELECT COUNT(*) AS 场次数_期望25 FROM exam_paper_sitting;
-- 期望：45（高级 5×3 + 初/中级 15×2）
SELECT COUNT(*) AS 分节数_期望45 FROM exam_paper_section;
-- 期望：15
SELECT COUNT(*) AS 配置键数_期望15 FROM sys_config;

-- FULL 科目应为 3 个（SAD / IPMP / SD），期望输出 3 行
SELECT code, name, outline_status, content_status
FROM rk_subject WHERE outline_status = 'FULL' ORDER BY sort_no;


SELECT '========== 3. G3 必填列断言（期望：两个 0） ==========' AS section;

SELECT
  (SELECT COUNT(*) FROM exam_paper_section    WHERE duration_min IS NULL OR duration_max IS NULL) AS 模板分节时长缺失_期望0,
  (SELECT COUNT(*) FROM exam_session_section  WHERE duration_min IS NULL OR duration_max IS NULL) AS 会话分节时长缺失_期望0,
  (SELECT COUNT(*) FROM exam_session_section  WHERE sitting_no IS NULL OR support_status IS NULL) AS 会话分节快照列缺失_期望0;


SELECT '========== 4. 时长不变式（期望：无 VIOLATION 行） ==========' AS section;

SELECT s.code, ps.sitting_no, ps.total_duration,
       SUM(sec.duration_min) AS sum_min,
       SUM(sec.duration_max) AS sum_max,
       'VIOLATION' AS 结果
FROM exam_paper_sitting ps
JOIN exam_paper_template t ON t.id = ps.template_id
JOIN rk_subject s          ON s.id = t.subject_id
JOIN exam_paper_section sec ON sec.template_id = t.id AND sec.sitting_no = ps.sitting_no
GROUP BY s.code, ps.sitting_no, ps.total_duration
HAVING NOT (SUM(sec.duration_min) <= ps.total_duration AND ps.total_duration <= SUM(sec.duration_max));


SELECT '========== 5. 满分口径一致性（期望：均无输出） ==========' AS section;

-- 5.1 模板满分 = 各场次之和（期望：无行）
SELECT s.code, t.total_max_score AS 模板满分, SUM(ps.total_max_score) AS 场次之和
FROM exam_paper_template t
JOIN rk_subject s           ON s.id = t.subject_id
JOIN exam_paper_sitting ps  ON ps.template_id = t.id
GROUP BY s.code, t.total_max_score
HAVING t.total_max_score <> SUM(ps.total_max_score);

-- 5.2 模板可自动判分满分 = 各场次之和（期望：无行）
SELECT s.code, t.auto_scored_max_score AS 模板auto, SUM(ps.auto_scored_max_score) AS 场次之和
FROM exam_paper_template t
JOIN rk_subject s           ON s.id = t.subject_id
JOIN exam_paper_sitting ps  ON ps.template_id = t.id
GROUP BY s.code, t.auto_scored_max_score
HAVING t.auto_scored_max_score <> SUM(ps.auto_scored_max_score);

-- 5.3 场次满分 = 该场次 SUPPORTED 分节之和（期望：无行）
SELECT s.code, ps.sitting_no, ps.total_max_score AS 场次满分,
       SUM(sec.question_count * sec.per_question_score) AS 分节之和
FROM exam_paper_sitting ps
JOIN exam_paper_template t  ON t.id = ps.template_id
JOIN rk_subject s           ON s.id = t.subject_id
JOIN exam_paper_section sec ON sec.template_id = t.id AND sec.sitting_no = ps.sitting_no
WHERE sec.support_status = 'SUPPORTED'
GROUP BY s.code, ps.sitting_no, ps.total_max_score
HAVING ps.total_max_score <> SUM(sec.question_count * sec.per_question_score);

-- 5.4 场次 auto 满分 = 该场次客观题分节之和（期望：无行）
SELECT s.code, ps.sitting_no, ps.auto_scored_max_score AS 场次auto,
       SUM(sec.question_count * sec.per_question_score) AS 客观节之和
FROM exam_paper_sitting ps
JOIN exam_paper_template t  ON t.id = ps.template_id
JOIN rk_subject s           ON s.id = t.subject_id
JOIN exam_paper_section sec ON sec.template_id = t.id AND sec.sitting_no = ps.sitting_no
WHERE sec.section_type = 'OBJECTIVE' AND sec.support_status = 'SUPPORTED'
GROUP BY s.code, ps.sitting_no, ps.auto_scored_max_score
HAVING ps.auto_scored_max_score <> SUM(sec.question_count * sec.per_question_score);


SELECT '========== 6. IPT 覆盖（期望：恰好 1 行 NOT_SUPPORTED） ==========' AS section;

SELECT s.code, t.total_max_score AS 模板满分, t.auto_scored_max_score AS 模板auto,
       ps.sitting_no, ps.total_max_score AS 场次满分, ps.auto_scored_max_score AS 场次auto,
       sec.section_no, sec.section_name, sec.question_count, sec.per_question_score,
       sec.duration_min, sec.duration_max, sec.support_status
FROM rk_subject s
JOIN exam_paper_template t  ON t.subject_id = s.id
JOIN exam_paper_sitting ps  ON ps.template_id = t.id
JOIN exam_paper_section sec ON sec.template_id = t.id AND sec.sitting_no = ps.sitting_no
WHERE s.code = 'IPT'
ORDER BY ps.sitting_no, sec.section_no;

-- 期望：无输出（除 IPT 外不得有 NOT_SUPPORTED）
SELECT s.code, sec.section_name, sec.support_status AS 意外的不支持分节
FROM rk_subject s
JOIN exam_paper_template t  ON t.subject_id = s.id
JOIN exam_paper_section sec ON sec.template_id = t.id
WHERE sec.support_status <> 'SUPPORTED' AND s.code <> 'IPT';


SELECT '========== 7. 管理员与配置（期望：must_change_password = 1，15 个键） ==========' AS section;

SELECT username, role, status, must_change_password FROM sys_user WHERE role = 'ADMIN';

SELECT GROUP_CONCAT(config_key ORDER BY config_key SEPARATOR ', ') AS 全部配置键 FROM sys_config;


SELECT '========== 8. 蓝图对照：高级科目与初级科目的卷面 ==========' AS section;

SELECT s.code, s.level, t.paper_type, ps.sitting_no, ps.sitting_name, ps.total_duration,
       sec.section_no, sec.section_name, sec.section_type, sec.question_type,
       sec.question_count, sec.per_question_score, sec.duration_min, sec.duration_max,
       sec.allow_early_submit, sec.support_status
FROM rk_subject s
JOIN exam_paper_template t  ON t.subject_id = s.id
JOIN exam_paper_sitting ps  ON ps.template_id = t.id
JOIN exam_paper_section sec ON sec.template_id = t.id AND sec.sitting_no = ps.sitting_no
WHERE s.code IN ('SAD', 'SD', 'IPT')
ORDER BY s.sort_no, ps.sitting_no, sec.section_no;
