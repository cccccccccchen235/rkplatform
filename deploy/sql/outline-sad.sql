-- ===============================================================
-- 软考在线学习平台 · 大纲章节树 · 系统架构设计师（SAD）
--
-- 前置：schema.sql + init-data.sql（本文件不建表、不建科目）。
-- 执行：mysql -h127.0.0.1 -uroot -p ruankao < deploy/sql/outline-sad.sql
--       或按 09 §9.7 的循环一次导入全部：for f in sql/outline-*.sql; do ...
--
-- 唯一事实来源：docs/04-database.md §4.8 清单 + docs/02 §2.3「大纲章节树」维度。
-- 文件结构、幂等语义、版权边界与 outline-sd.sql 完全一致——**改一处要同时改三处**，
-- 三份 outline-*.sql 的唯一差异是数据。
--
-- ---------------------------------------------------------------
-- 数据来源与版权边界（GR-1 / docs/02 §2.3「大纲数据边界」）
-- ---------------------------------------------------------------
-- 全部 name 取自《系统架构设计师考试大纲》的**编号标题**（`1.` / `1.1`）。
-- 本大纲的第三层是「●」条目（如「操作系统的类型和结构」），那是大纲正文性质的
-- 展开描述，**一律未录入**——这正是 docs/02 §2.3 划的红线。所以本树的层级是
-- L1（章）+ L2（节），比 SD 少一层；docs/02 §2.3 允许「若某学科只有 2 层则为 leaf」。
--
-- 【取证说明】来源是一份公开的考纲 PDF（每页页脚印「系统架构设计师考试大纲 第 N 页（共 11 页）」），
--   其文本层缺 CMap（pdftotext 报 Unknown character collection 'Adobe-GB1'，只吐出编号骨架），
--   故逐页读**渲染图像**人工转录，并用另一份第三方 Markdown 转录交叉核对章节名。
--   未与官方单行本逐条比对。出入记于 docs/24。
--
-- ---------------------------------------------------------------
-- 覆盖范围：科目 1 与科目 2；**科目 3（论文）不收**
-- ---------------------------------------------------------------
-- 科目 1（信息系统综合知识）与科目 2（系统架构设计案例分析）收；科目 3（系统架构设计论文）
-- 不收，理由是它的标题与科目 2 大面积重名（软件架构设计 / 系统设计 / 分布式系统设计 /
-- 系统的可靠性分析与设计 / 系统的安全性和保密性设计 五个标题在科目 2、3 中各出现一次），
-- 而重名节点会让「同科目重名」自检失败，也会让大纲树页出现两个同名节点。
-- SD 没有这个问题（它的科目 2 有独立的 `n.m` 编号且标题不重复），所以 SD 收两个科目。
--
-- 科目 2 的条目在大纲里只有「●」层，故这 9 个节点是 L1 叶子。
-- 层级上限 3 层，本树只用到 2 层，无压缩。
--
-- ---------------------------------------------------------------
-- ⚠️ 导入后要清掉运行中后端的大纲缓存
-- ---------------------------------------------------------------
-- 考生端读大纲走 Redis 缓存 `rk:cache:outline:<subjectId>`（TTL 1 小时），
-- 而**只有管理端的写接口会 evict 它**。用 mysql 直连导入绕过了管理端：
-- 后端在跑、且该科目大纲此前被读过时，缓存里那份旧树（很可能是空数组）
-- 会继续被返回，最长 1 小时——库里明明有 39 行，页面却是「共 0 个章节」。
-- 本文件**正是**踩过这个坑的那一个（SAD 是 subject_id = 2）：
--   redis-cli -h 127.0.0.1 -p 6379 -a <REDIS_PASSWORD> del rk:cache:outline:2
-- 09 §9.7 的导入在应用启动前执行，那种顺序下没有此问题。详见 docs/24。
-- ===============================================================

SET NAMES utf8mb4;
USE ruankao;
START TRANSACTION;

SET @sad := (SELECT id FROM rk_subject WHERE code = 'SAD');

-- 科目不存在时立刻中断。子查询返回多行 → ERROR 1242；不要改成 `WHERE 1/0`
-- 之类的式子，MySQL 的除零只出 NULL 与 warning，不报错（见 outline-sd.sql 的长注）。
SET @guard := IF(@sad IS NULL, (SELECT 1 UNION ALL SELECT 2), 0);


-- ===============================================================
-- L1 · 17 个知识领域（科目 1 的 8 个 + 科目 2 的 9 个）
-- ===============================================================
INSERT INTO rk_outline_node (subject_id, parent_id, level, name, sort_no, origin)
SELECT @sad, 0, 1, t.name, t.sort_no, 'OFFICIAL_TITLE'
FROM (
  -- 考试科目 1：信息系统综合知识
            SELECT '计算机软件与网络基础知识' AS name, 1 AS sort_no
  UNION ALL SELECT '信息化基础知识',                          2
  UNION ALL SELECT '系统开发基础知识',                        3
  UNION ALL SELECT '软件架构基础知识',                        4
  UNION ALL SELECT '安全性与可靠性技术',                      5
  UNION ALL SELECT '标准化与知识产权',                        6
  UNION ALL SELECT '应用数学',                                7
  UNION ALL SELECT '专业英语',                                8
  -- 考试科目 2：系统架构设计案例分析（这些在大纲里只有 ● 层，故各自是叶子）
  UNION ALL SELECT '系统计划',                                9
  UNION ALL SELECT '软件架构设计',                           10
  UNION ALL SELECT '设计模式',                               11
  UNION ALL SELECT '系统设计',                               12
  UNION ALL SELECT '软件系统建模',                           13
  UNION ALL SELECT '分布式系统设计',                         14
  UNION ALL SELECT '嵌入式系统设计',                         15
  UNION ALL SELECT '系统的可靠性分析与设计',                 16
  UNION ALL SELECT '系统的安全性和保密性设计',               17
) t
LEFT JOIN rk_outline_node n
       ON n.subject_id = @sad AND n.parent_id = 0 AND n.name = t.name
WHERE n.id IS NULL;


-- ===============================================================
-- L2 · 22 个节（全部属于科目 1；科目 1 的 4/6/7/8 章与整个科目 2 都没有 `n.m` 标题）
-- ===============================================================
INSERT INTO rk_outline_node (subject_id, parent_id, level, name, sort_no, origin)
SELECT @sad, p.id, 2, t.name, t.sort_no, 'OFFICIAL_TITLE'
FROM (
  -- 1. 计算机软件与网络基础知识
            SELECT '操作系统'                                       AS name, '计算机软件与网络基础知识' AS parent_name, 1 AS sort_no
  UNION ALL SELECT '数据库系统',                                       '计算机软件与网络基础知识',                2
  UNION ALL SELECT '嵌入式系统',                                       '计算机软件与网络基础知识',                3
  UNION ALL SELECT '数据通信与计算机网络',                             '计算机软件与网络基础知识',                4
  UNION ALL SELECT '多媒体',                                           '计算机软件与网络基础知识',                5
  UNION ALL SELECT '系统配置与性能评价',                               '计算机软件与网络基础知识',                6
  -- 2. 信息化基础知识
  UNION ALL SELECT '信息系统工程总体规划',                             '信息化基础知识',                          1
  UNION ALL SELECT '政府信息化与电子政务',                             '信息化基础知识',                          2
  UNION ALL SELECT '企业信息化与电子商务',                             '信息化基础知识',                          3
  UNION ALL SELECT '信息资源管理',                                     '信息化基础知识',                          4
  UNION ALL SELECT '国际和国内有关信息化的标准、法律和规定',           '信息化基础知识',                          5
  -- 3. 系统开发基础知识
  UNION ALL SELECT '开发管理',                                         '系统开发基础知识',                        1
  UNION ALL SELECT '需求管理',                                         '系统开发基础知识',                        2
  UNION ALL SELECT '软件开发方法',                                     '系统开发基础知识',                        3
  UNION ALL SELECT '软件开发环境与工具',                               '系统开发基础知识',                        4
  UNION ALL SELECT '设计方法',                                         '系统开发基础知识',                        5
  UNION ALL SELECT '基于构件的开发',                                   '系统开发基础知识',                        6
  UNION ALL SELECT '应用系统构建',                                     '系统开发基础知识',                        7
  UNION ALL SELECT '测试与评审',                                       '系统开发基础知识',                        8
  -- 5. 安全性与可靠性技术
  UNION ALL SELECT '信息安全与保密',                                   '安全性与可靠性技术',                      1
  UNION ALL SELECT '系统可靠性',                                       '安全性与可靠性技术',                      2
  UNION ALL SELECT '安全性规章与保护私有信息规则',                     '安全性与可靠性技术',                      3
) t
JOIN rk_outline_node p
  ON p.subject_id = @sad AND p.level = 1 AND p.name = t.parent_name
LEFT JOIN rk_outline_node n
       ON n.subject_id = @sad AND n.parent_id = p.id AND n.name = t.name
WHERE n.id IS NULL;


COMMIT;


-- ===============================================================
-- 自检（只读，不改数据）
--
-- 判据一：两层节点数量应为 17 / 22，合计 39；**没有 level = 3 的行**。
-- 判据二：下面两条查询都必须返回空集（父子错位与重名）。
-- ===============================================================
SELECT level, COUNT(*) AS node_count
FROM rk_outline_node
WHERE subject_id = @sad AND status = 'ENABLED'
GROUP BY level
ORDER BY level;

SELECT '父子错位：parent_id 不存在' AS problem, COUNT(*) AS rows_found
FROM rk_outline_node c
WHERE c.subject_id = @sad AND c.parent_id <> 0
  AND NOT EXISTS (SELECT 1 FROM rk_outline_node p WHERE p.id = c.parent_id AND p.subject_id = c.subject_id)

UNION ALL

SELECT '父子错位：level 与父节点不连续', COUNT(*)
FROM rk_outline_node c
JOIN rk_outline_node p ON p.id = c.parent_id
WHERE c.subject_id = @sad AND c.level <> p.level + 1

UNION ALL

SELECT '同科目重名', COUNT(*)
FROM (
  SELECT subject_id, parent_id, name
  FROM rk_outline_node
  WHERE subject_id = @sad
  GROUP BY subject_id, parent_id, name
  HAVING COUNT(*) > 1
) dup;
