-- ===============================================================
-- 软考在线学习平台 · 大纲章节树 · 软件设计师（SD）
--
-- 前置：schema.sql + init-data.sql（本文件不建表、不建科目）。
-- 执行：mysql -h127.0.0.1 -uroot -p ruankao < deploy/sql/outline-sd.sql
--       或按 09 §9.7 的循环一次导入全部：for f in sql/outline-*.sql; do ...
--
-- 唯一事实来源：docs/04-database.md §4.8 清单 + docs/02 §2.3「大纲章节树」维度。
-- 本文件是 SD 章节树的**唯一**写入方，见下方「为什么是增量而不是重建」。
--
-- ---------------------------------------------------------------
-- 数据来源与版权边界（GR-1 / docs/02 §2.3「大纲数据边界」）
-- ---------------------------------------------------------------
-- 全部 name 取自《软件设计师考试大纲》的**章节标题短语**，即大纲里带编号的行
-- （`1.` / `1.1` / `2.1.1` 三种层级）。**不含任何大纲正文**——编号行下面的
-- 描述性条目（如「二进制、八进制、十进制和十六进制等常用数制及其相互转换」）
-- 一律未录入。这正是 docs/02 §2.3 允许的用途：「使用官方大纲的章节标题作为
-- 事实性索引（标题短语、功能性信息）」，而禁止项是「复制大纲正文的描述性内容」。
--
-- 编号（`1.1` 这些）**不写进 name**：层级由 level 表达、次序由 sort_no 表达，
-- 大纲树页与 docs/02 §2.3 的示例（`计算机系统基础知识 → 操作系统 → 进程管理`）
-- 都不带编号。把编号塞进名字会让它同时出现在两个字段里，改名时必然打架。
--
-- 【取证说明】取标题时用的是一份第三方（希赛网）排版的大纲 PDF，其文本层不含
--   CJK 字形（pdftotext 只吐出编号骨架），故改为逐页读**渲染图像**人工转录。
--   转录后与公开的软考大纲结构一致，但**未与工信部官方单行本逐条比对**。
--   出入记于 docs/24；若日后拿到权威单行本，核对的是 name 字符串本身。
--
-- ---------------------------------------------------------------
-- 覆盖范围：两个考试科目都收
-- ---------------------------------------------------------------
-- 软件设计师的卷面（docs/04 §4.8.2）是 JUNIOR_MID = 150 总分 / 75 客观，
-- 对应真实的「基础知识」（75 单选）+「应用技术」（5 案例）两卷。所以「科目 1」
-- 与「科目 2」的考点都属于本课程范围，组卷两边都要能取到，故 13 个 L1 全收。
-- 两个科目在大纲里各自从 `1.` 起编号，合并后**不写编号**正好避开重号；
-- L1 的次序为科目 1 的 7 个在前、科目 2 的 6 个在后，`sort_no` 按此排。
--
-- ---------------------------------------------------------------
-- 为什么是「增量」而不是「先 DELETE 再 INSERT」
-- ---------------------------------------------------------------
-- `rk_question.outline_id` 与 `stat_user_outline.outline_id` 都指向本表。
-- 一旦 P3 的题目落库，`DELETE FROM rk_outline_node WHERE subject_id = ?`
-- 会把在线题目与用户章节统计一次打断（本表无外键，数据库不会拦）。
-- 所以本文件的语义是**幂等新增**：每条 INSERT 都带 `WHERE NOT EXISTS` 守卫，
-- 重复执行是空操作；已存在的节点**不改名、不改序**。
-- 要改名或改序，走管理端（`PUT /admin/outline` / `/sort`），那会写操作日志。
--
-- ---------------------------------------------------------------
-- ⚠️ 导入后要清掉运行中后端的大纲缓存
-- ---------------------------------------------------------------
-- 考生端读大纲走 Redis 缓存 `rk:cache:outline:<subjectId>`（TTL 1 小时，
-- 见 CacheDao 与 06 §6.11），而 **只有管理端的写接口会 evict 它**。
-- 本文件用 mysql 直连导入，绕过了管理端，于是：后端正在运行、且该科目
-- 的大纲此前被读过时，缓存里那份旧树（很可能是空数组）会继续被返回，
-- 最长 1 小时——**库里明明有数据，页面上却是「该科目还没有录入章节」**。
--
-- 09 §9.7 的导入是在应用启动**之前**执行的，那种顺序下没有这个问题。
-- 本轮验证时踩到了它：SAD 导入后页面显示「共 0 个章节」，直到
--   redis-cli -h 127.0.0.1 -p 6379 -a <REDIS_PASSWORD> del rk:cache:outline:2
-- 才恢复（陈旧值就是 `[]`）。凡在后端运行期间导数据，导入后删掉该科目的键，
-- 或直接重启后端。出入与完整取证见 docs/24。
--
-- 层级上限 3 层与大纲自身的编号层级（n / n.m / n.m.k）恰好对齐，无压缩。
-- ===============================================================

SET NAMES utf8mb4;
USE ruankao;
START TRANSACTION;

-- 科目 id 每次现查，不假设 AUTO_INCREMENT 的取值（重复导入 init-data.sql 会变）。
SET @sd := (SELECT id FROM rk_subject WHERE code = 'SD');

-- 兜底：科目不存在时立刻中断，而不是继续往下插孤儿节点。
--
-- 用"子查询返回多行"（ERROR 1242）来中断：MySQL 不允许在脚本里直接用 SIGNAL
-- （那只能在存储程序里）。**不要**写成 `WHERE 1 / 0` 之类的式子——MySQL 的
-- 除零只产生 NULL 与一条 warning，不报错，那句守卫会一声不响地放行，
-- 变成一条"看起来在断言、其实永远通过"的假守卫。
-- SET 而不是 SELECT：成功时也不打印无意义的一行。
SET @guard := IF(@sd IS NULL, (SELECT 1 UNION ALL SELECT 2), 0);


-- ===============================================================
-- L1 · 13 个知识领域（科目 1 的 7 个 + 科目 2 的 6 个）
-- ===============================================================
INSERT INTO rk_outline_node (subject_id, parent_id, level, name, sort_no, origin)
SELECT @sd, 0, 1, t.name, t.sort_no, 'OFFICIAL_TITLE'
FROM (
  -- 考试科目 1：计算机与软件工程知识
            SELECT '计算机科学基础知识'                 AS name, 1 AS sort_no
  UNION ALL SELECT '计算机系统知识',                        2
  UNION ALL SELECT '系统开发和运行知识',                    3
  UNION ALL SELECT '面向对象基础知识',                      4
  UNION ALL SELECT '信息安全知识',                          5
  UNION ALL SELECT '标准化、信息化和知识产权基础知识',      6
  UNION ALL SELECT '计算机专业英语',                        7
  -- 考试科目 2：软件设计
  UNION ALL SELECT '外部设计',                              8
  UNION ALL SELECT '内部设计',                              9
  UNION ALL SELECT '数据库应用分析与设计',                 10
  UNION ALL SELECT '程序设计',                             11
  UNION ALL SELECT '系统实施',                             12
  UNION ALL SELECT '软件工程应用',                         13
) t
LEFT JOIN rk_outline_node n
       ON n.subject_id = @sd AND n.parent_id = 0 AND n.name = t.name
WHERE n.id IS NULL;


-- ===============================================================
-- L2 · 46 个子领域
-- parent_name 必须与上面 L1 的 name 逐字相同（含顿号），这里是唯一的关联键。
-- ===============================================================
INSERT INTO rk_outline_node (subject_id, parent_id, level, name, sort_no, origin)
SELECT @sd, p.id, 2, t.name, t.sort_no, 'OFFICIAL_TITLE'
FROM (
  -- 1. 计算机科学基础知识
            SELECT '数制及其转换'                                   AS name, '计算机科学基础知识'                 AS parent_name, 1 AS sort_no
  UNION ALL SELECT '计算机内数据的表示',                               '计算机科学基础知识',                          2
  UNION ALL SELECT '算术运算和逻辑运算',                               '计算机科学基础知识',                          3
  UNION ALL SELECT '其他数学基础知识',                                 '计算机科学基础知识',                          4
  -- 2. 计算机系统知识
  UNION ALL SELECT '计算机硬件基础知识',                               '计算机系统知识',                              1
  UNION ALL SELECT '计算机软件知识',                                   '计算机系统知识',                              2
  UNION ALL SELECT '计算机网络知识',                                   '计算机系统知识',                              3
  UNION ALL SELECT '多媒体基础知识',                                   '计算机系统知识',                              4
  -- 3. 系统开发和运行知识
  UNION ALL SELECT '软件工程知识',                                     '系统开发和运行知识',                          1
  UNION ALL SELECT '系统分析基础知识',                                 '系统开发和运行知识',                          2
  UNION ALL SELECT '系统设计基础知识',                                 '系统开发和运行知识',                          3
  UNION ALL SELECT '系统实施基础知识',                                 '系统开发和运行知识',                          4
  UNION ALL SELECT '系统运行和维护基础知识',                           '系统开发和运行知识',                          5
  UNION ALL SELECT '软件质量管理基础知识',                             '系统开发和运行知识',                          6
  UNION ALL SELECT '软件过程改进基础知识',                             '系统开发和运行知识',                          7
  -- 6. 标准化、信息化和知识产权基础知识
  UNION ALL SELECT '标准化基础知识',                                   '标准化、信息化和知识产权基础知识',            1
  UNION ALL SELECT '信息化基础知识',                                   '标准化、信息化和知识产权基础知识',            2
  UNION ALL SELECT '知识产权基础知识',                                 '标准化、信息化和知识产权基础知识',            3
  -- 科目 2 · 1. 外部设计
  UNION ALL SELECT '理解系统需求说明',                                 '外部设计',                                    1
  UNION ALL SELECT '准备进行系统开发',                                 '外部设计',                                    2
  UNION ALL SELECT '设计系统功能',                                     '外部设计',                                    3
  UNION ALL SELECT '设计数据模型',                                     '外部设计',                                    4
  UNION ALL SELECT '编写外部设计文档',                                 '外部设计',                                    5
  UNION ALL SELECT '外部设计的评审',                                   '外部设计',                                    6
  -- 科目 2 · 2. 内部设计
  UNION ALL SELECT '设计软件结构',                                     '内部设计',                                    1
  UNION ALL SELECT '设计输入输出',                                     '内部设计',                                    2
  UNION ALL SELECT '设计物理数据',                                     '内部设计',                                    3
  UNION ALL SELECT '构件的创建和重用',                                 '内部设计',                                    4
  UNION ALL SELECT '编写内部设计文档',                                 '内部设计',                                    5
  UNION ALL SELECT '内部设计的评审',                                   '内部设计',                                    6
  -- 科目 2 · 4. 程序设计
  UNION ALL SELECT '模块划分',                                         '程序设计',                                    1
  UNION ALL SELECT '编写程序设计文档',                                 '程序设计',                                    2
  UNION ALL SELECT '程序设计评审',                                     '程序设计',                                    3
  -- 科目 2 · 5. 系统实施
  UNION ALL SELECT '配置计算机系统及环境',                             '系统实施',                                    1
  UNION ALL SELECT '选择合适的程序设计语言',                           '系统实施',                                    2
  UNION ALL SELECT '用 C 程序设计语言以及 C++、Java 中的任一种程序设计语言进行程序设计', '系统实施',          3
  UNION ALL SELECT '系统测试',                                         '系统实施',                                    4
  -- 科目 2 · 6. 软件工程应用
  UNION ALL SELECT '软件开发周期模型',                                 '软件工程应用',                                1
  UNION ALL SELECT '需求分析',                                         '软件工程应用',                                2
  UNION ALL SELECT '软件设计',                                         '软件工程应用',                                3
  UNION ALL SELECT '软件测试的原则与方法',                             '软件工程应用',                                4
  UNION ALL SELECT '软件质量（软件质量特性、软件质量控制）',           '软件工程应用',                                5
  UNION ALL SELECT '软件过程评估基本方法、软件能力成熟度评估基本方法', '软件工程应用',                                6
  UNION ALL SELECT '软件开发和开发工具',                               '软件工程应用',                                7
  UNION ALL SELECT '面向对象技术',                                     '软件工程应用',                                8
  UNION ALL SELECT '网络环境软件技术',                                 '软件工程应用',                                9
) t
JOIN rk_outline_node p
  ON p.subject_id = @sd AND p.level = 1 AND p.name = t.parent_name
LEFT JOIN rk_outline_node n
       ON n.subject_id = @sd AND n.parent_id = p.id AND n.name = t.name
WHERE n.id IS NULL;


-- ===============================================================
-- L3 · 7 个三级节点（只有科目 1 的 `2.1` / `2.2` 往下再分了一层）
-- 这些才是 `rk_question.outline_id` 的落点（docs/02 §2.3：题目挂叶子章节）。
-- ===============================================================
INSERT INTO rk_outline_node (subject_id, parent_id, level, name, sort_no, origin)
SELECT @sd, p.id, 3, t.name, t.sort_no, 'OFFICIAL_TITLE'
FROM (
            SELECT '计算机系统的组成、体系结构分类及特性' AS name, '计算机硬件基础知识' AS parent_name, 1 AS sort_no
  UNION ALL SELECT '存储系统',                                   '计算机硬件基础知识', 2
  UNION ALL SELECT '可靠性与系统性能评测基础知识',               '计算机硬件基础知识', 3
  UNION ALL SELECT '数据结构与算法知识',                         '计算机软件知识',     1
  UNION ALL SELECT '操作系统知识',                               '计算机软件知识',     2
  UNION ALL SELECT '程序设计语言和语言处理程序知识',             '计算机软件知识',     3
  UNION ALL SELECT '数据库知识',                                 '计算机软件知识',     4
) t
JOIN rk_outline_node p
  ON p.subject_id = @sd AND p.level = 2 AND p.name = t.parent_name
LEFT JOIN rk_outline_node n
       ON n.subject_id = @sd AND n.parent_id = p.id AND n.name = t.name
WHERE n.id IS NULL;


COMMIT;


-- ===============================================================
-- 自检（只读，不改数据）
--
-- 判据一：三层节点的数量应为 13 / 46 / 7，合计 66。
-- 判据二：不应存在 parent_id 指向"别的科目"或"不存在的节点"的行——
--         下面两条查询都必须返回空集。这一条比数量更重要：数量对得上
--         而父子错位，大纲树页会渲染成一棵看似正常、实则位置错乱的树。
-- ===============================================================
SELECT level, COUNT(*) AS node_count
FROM rk_outline_node
WHERE subject_id = @sd AND status = 'ENABLED'
GROUP BY level
ORDER BY level;

SELECT '父子错位：parent_id 不存在' AS problem, COUNT(*) AS rows_found
FROM rk_outline_node c
WHERE c.subject_id = @sd AND c.parent_id <> 0
  AND NOT EXISTS (SELECT 1 FROM rk_outline_node p WHERE p.id = c.parent_id AND p.subject_id = c.subject_id)

UNION ALL

SELECT '父子错位：level 与父节点不连续', COUNT(*)
FROM rk_outline_node c
JOIN rk_outline_node p ON p.id = c.parent_id
WHERE c.subject_id = @sd AND c.level <> p.level + 1

UNION ALL

SELECT '同科目重名（与上面的 NOT EXISTS 守卫同罪）', COUNT(*)
FROM (
  SELECT subject_id, parent_id, name
  FROM rk_outline_node
  WHERE subject_id = @sd
  GROUP BY subject_id, parent_id, name
  HAVING COUNT(*) > 1
) dup;
