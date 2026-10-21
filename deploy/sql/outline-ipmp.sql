-- ===============================================================
-- 软考在线学习平台 · 大纲章节树 · 信息系统项目管理师（IPMP）
--
-- 前置：schema.sql + init-data.sql（本文件不建表、不建科目）。
-- 执行：mysql -h127.0.0.1 -uroot -p ruankao < deploy/sql/outline-ipmp.sql
--       或按 09 §9.7 的循环一次导入全部：for f in sql/outline-*.sql; do ...
--
-- 唯一事实来源：docs/04-database.md §4.8 清单 + docs/02 §2.3「大纲章节树」维度。
-- 文件结构、幂等语义、版权边界与 outline-sd.sql 完全一致——**改一处要同时改三处**，
-- 三份 outline-*.sql 的唯一差异是数据。
--
-- ---------------------------------------------------------------
-- 数据来源与版权边界（GR-1 / docs/02 §2.3「大纲数据边界」）
-- ---------------------------------------------------------------
-- 全部 name 取自《信息系统项目管理师考试大纲》的**编号标题**（`1.` / `1.1` / `6.2.1`）。
-- 本大纲的细目是「·」条目（如「信息系统概念」），属大纲正文性质的展开描述，
-- **一律未录入**。本树是全站唯一用到 3 层的科目（`6.2 软件工程的国家标准`
-- 下面还有 `6.2.1 基础标准` 等 4 个），因为大纲自己就编到了第三级。
--
-- ⚠️ 大纲原文里 `2.7 项目进度管理` 的编号处有一处多余空格（其他都是 `2.7 ` 后
--    直接跟标题）。本文件按**规范写法**去掉该空格，不逐字保留排版瑕疵。
--
-- 【取证说明】来源是一份公开的考纲 PDF（页脚印「信息系统项目管理师大纲 第 N 页（共 13 页）」），
--   文本层缺 CMap（pdftotext 只吐出编号骨架），故逐页读**渲染图像**人工转录。
--   未与官方单行本逐条比对。出入记于 docs/24。
--
-- ---------------------------------------------------------------
-- 覆盖范围：科目 1 与科目 2；**科目 3（论文）不收**
-- ---------------------------------------------------------------
-- 科目 3 的标题与科目 1 重名（「信息系统项目管理」在科目 1 是 L1、在科目 3 也是 L1；
-- 「信息系统工程监理」「项目绩效考核与绩效管理」在科目 1 已作为 L2 存在）。
-- 重名会让「同科目重名」自检失败，也会让大纲树页出现两个同名节点。
-- SAD 出于同一理由也丢掉科目 3——三份文件用的是同一条规则。
--
-- 科目 2（案例分析）的 5 个标题与科目 1 不重名，故收；它们在大纲里只有「·」层，
-- 因此是 5 个 L1 叶子。
--
-- ---------------------------------------------------------------
-- ⚠️ 导入后要清掉运行中后端的大纲缓存
-- ---------------------------------------------------------------
-- 考生端读大纲走 Redis 缓存 `rk:cache:outline:<subjectId>`（TTL 1 小时），
-- 而**只有管理端的写接口会 evict 它**。用 mysql 直连导入绕过了管理端：
-- 后端在跑、且该科目大纲此前被读过时，缓存里那份旧树（很可能是空数组）
-- 会继续被返回，最长 1 小时——库里明明有 55 行，页面却是「共 0 个章节」。
-- 导入后执行（或直接重启后端）：
--   redis-cli -h 127.0.0.1 -p 6379 -a <REDIS_PASSWORD> del rk:cache:outline:3
-- 09 §9.7 的导入在应用启动前执行，那种顺序下没有此问题。详见 docs/24。
-- ===============================================================

SET NAMES utf8mb4;
USE ruankao;
START TRANSACTION;

SET @ipmp := (SELECT id FROM rk_subject WHERE code = 'IPMP');

-- 科目不存在时立刻中断。子查询返回多行 → ERROR 1242；不要改成 `WHERE 1/0`
-- 之类的式子，MySQL 的除零只出 NULL 与 warning，不报错（见 outline-sd.sql 的长注）。
SET @guard := IF(@ipmp IS NULL, (SELECT 1 UNION ALL SELECT 2), 0);


-- ===============================================================
-- L1 · 14 个知识领域（科目 1 的 9 个 + 科目 2 的 5 个）
-- ===============================================================
INSERT INTO rk_outline_node (subject_id, parent_id, level, name, sort_no, origin)
SELECT @ipmp, 0, 1, t.name, t.sort_no, 'OFFICIAL_TITLE'
FROM (
  -- 考试科目 1：信息系统项目管理综合知识
            SELECT '信息系统基础' AS name, 1 AS sort_no
  UNION ALL SELECT '信息系统项目管理',                    2
  UNION ALL SELECT '信息系统项目管理高级知识',            3
  UNION ALL SELECT '信息化基础知识',                      4
  UNION ALL SELECT '信息安全知识',                        5
  UNION ALL SELECT '法律法规和标准规范',                  6
  UNION ALL SELECT '管理科学基础知识',                    7
  UNION ALL SELECT '项目管理师职业道德',                  8
  UNION ALL SELECT '专业英语',                            9
  -- 考试科目 2：信息系统项目管理案例分析
  UNION ALL SELECT '项目启动',                           10
  UNION ALL SELECT '项目管理计划',                       11
  UNION ALL SELECT '项目实施',                           12
  UNION ALL SELECT '项目监督与控制',                     13
  UNION ALL SELECT '项目收尾',                           14
) t
LEFT JOIN rk_outline_node n
       ON n.subject_id = @ipmp AND n.parent_id = 0 AND n.name = t.name
WHERE n.id IS NULL;


-- ===============================================================
-- L2 · 37 个节（全部属于科目 1；第 5/7/8/9 章与整个科目 2 都没有 `n.m` 标题）
-- ===============================================================
INSERT INTO rk_outline_node (subject_id, parent_id, level, name, sort_no, origin)
SELECT @ipmp, p.id, 2, t.name, t.sort_no, 'OFFICIAL_TITLE'
FROM (
  -- 1. 信息系统基础
            SELECT '信息系统'                                   AS name, '信息系统基础'               AS parent_name, 1 AS sort_no
  UNION ALL SELECT '信息系统建设',                                   '信息系统基础',                                2
  UNION ALL SELECT '软件工程知识',                                   '信息系统基础',                                3
  UNION ALL SELECT '软件构件技术知识',                               '信息系统基础',                                4
  UNION ALL SELECT '软件体系结构',                                   '信息系统基础',                                5
  UNION ALL SELECT '面向对象系统分析与设计',                         '信息系统基础',                                6
  UNION ALL SELECT '典型应用集成技术',                               '信息系统基础',                                7
  UNION ALL SELECT '软件工具',                                       '信息系统基础',                                8
  UNION ALL SELECT '计算机网络知识',                                 '信息系统基础',                                9
  -- 2. 信息系统项目管理
  UNION ALL SELECT '信息系统项目管理基础',                           '信息系统项目管理',                            1
  UNION ALL SELECT '项目生命周期和组织',                             '信息系统项目管理',                            2
  UNION ALL SELECT '项目管理过程',                                   '信息系统项目管理',                            3
  UNION ALL SELECT '项目立项与招投标管理',                           '信息系统项目管理',                            4
  UNION ALL SELECT '项目整体管理',                                   '信息系统项目管理',                            5
  UNION ALL SELECT '项目范围管理',                                   '信息系统项目管理',                            6
  UNION ALL SELECT '项目进度管理',                                   '信息系统项目管理',                            7
  UNION ALL SELECT '项目成本管理',                                   '信息系统项目管理',                            8
  UNION ALL SELECT '项目质量管理',                                   '信息系统项目管理',                            9
  UNION ALL SELECT '项目人力资源管理',                               '信息系统项目管理',                           10
  UNION ALL SELECT '项目沟通管理',                                   '信息系统项目管理',                           11
  UNION ALL SELECT '项目风险管理',                                   '信息系统项目管理',                           12
  UNION ALL SELECT '项目采购和合同管理',                             '信息系统项目管理',                           13
  UNION ALL SELECT '文档与配置管理',                                 '信息系统项目管理',                           14
  UNION ALL SELECT '需求管理',                                       '信息系统项目管理',                           15
  -- 3. 信息系统项目管理高级知识
  UNION ALL SELECT '大型、复杂项目和多项目管理',                     '信息系统项目管理高级知识',                    1
  UNION ALL SELECT '战略管理',                                       '信息系统项目管理高级知识',                    2
  UNION ALL SELECT '用户业务流程管理',                               '信息系统项目管理高级知识',                    3
  UNION ALL SELECT '知识管理',                                       '信息系统项目管理高级知识',                    4
  UNION ALL SELECT '项目绩效考核与绩效管理',                         '信息系统项目管理高级知识',                    5
  UNION ALL SELECT '信息系统工程监理',                               '信息系统项目管理高级知识',                    6
  -- 4. 信息化基础知识
  UNION ALL SELECT '信息与信息化',                                   '信息化基础知识',                              1
  UNION ALL SELECT '政府信息化与电子政务',                           '信息化基础知识',                              2
  UNION ALL SELECT '企业信息化与电子商务',                           '信息化基础知识',                              3
  UNION ALL SELECT '信息资源管理',                                   '信息化基础知识',                              4
  UNION ALL SELECT 'CIO 的职责、条件和重要性',                       '信息化基础知识',                              5
  -- 6. 法律法规和标准规范
  UNION ALL SELECT '法律',                                           '法律法规和标准规范',                          1
  UNION ALL SELECT '软件工程的国家标准',                             '法律法规和标准规范',                          2
) t
JOIN rk_outline_node p
  ON p.subject_id = @ipmp AND p.level = 1 AND p.name = t.parent_name
LEFT JOIN rk_outline_node n
       ON n.subject_id = @ipmp AND n.parent_id = p.id AND n.name = t.name
WHERE n.id IS NULL;


-- ===============================================================
-- L3 · 4 个三级节点（大纲自己编到了 `6.2.1`，是全站唯一用到第 3 层的地方）
-- ===============================================================
INSERT INTO rk_outline_node (subject_id, parent_id, level, name, sort_no, origin)
SELECT @ipmp, p.id, 3, t.name, t.sort_no, 'OFFICIAL_TITLE'
FROM (
            SELECT '基础标准' AS name, '软件工程的国家标准' AS parent_name, 1 AS sort_no
  UNION ALL SELECT '开发标准',                                   '软件工程的国家标准', 2
  UNION ALL SELECT '文档标准',                                   '软件工程的国家标准', 3
  UNION ALL SELECT '管理标准',                                   '软件工程的国家标准', 4
) t
JOIN rk_outline_node p
  ON p.subject_id = @ipmp AND p.level = 2 AND p.name = t.parent_name
LEFT JOIN rk_outline_node n
       ON n.subject_id = @ipmp AND n.parent_id = p.id AND n.name = t.name
WHERE n.id IS NULL;


COMMIT;


-- ===============================================================
-- 自检（只读，不改数据）
--
-- 判据一：三层节点数量应为 14 / 37 / 4，合计 55。
-- 判据二：下面两条查询都必须返回空集（父子错位与重名）。
-- ===============================================================
SELECT level, COUNT(*) AS node_count
FROM rk_outline_node
WHERE subject_id = @ipmp AND status = 'ENABLED'
GROUP BY level
ORDER BY level;

SELECT '父子错位：parent_id 不存在' AS problem, COUNT(*) AS rows_found
FROM rk_outline_node c
WHERE c.subject_id = @ipmp AND c.parent_id <> 0
  AND NOT EXISTS (SELECT 1 FROM rk_outline_node p WHERE p.id = c.parent_id AND p.subject_id = c.subject_id)

UNION ALL

SELECT '父子错位：level 与父节点不连续', COUNT(*)
FROM rk_outline_node c
JOIN rk_outline_node p ON p.id = c.parent_id
WHERE c.subject_id = @ipmp AND c.level <> p.level + 1

UNION ALL

SELECT '同科目重名', COUNT(*)
FROM (
  SELECT subject_id, parent_id, name
  FROM rk_outline_node
  WHERE subject_id = @ipmp
  GROUP BY subject_id, parent_id, name
  HAVING COUNT(*) > 1
) dup;
