-- 由 .verify-tmp/import-sd-remote.js 生成：官方导入器的 SQL 生成逻辑 + outlineId 按章节名重映射
-- 未经逐题审核，直接 PUBLISHED；reviewed_by 留 NULL，理由见 sys_operation_log
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO rk_generation_batch
  (batch_no, subject_id, outline_ids, question_type, target_count, model, prompt_version,
   status, produced_count, approved_count, rejected_count, operator_id, finished_at)
VALUES ('EX-SD-001', 6, '[163,164,165,166,226,227,228,229,230,231,232,169,170,171,172,173,174,175,176,177,200,201,202,203,204,205,206,207,208,181,182,183,184,185,186,187,188,189,190,191,192,196,197,198,199,193,194,195,178,179,180,151,152,157]', 'SINGLE_CHOICE', 250, 'external-ai (user-provided)', 'question-spec-sd', 'COMPLETED', 250, 250, 0, 1, NOW());
SET @batch := LAST_INSERT_ID();

-- 知识点（按名称匹配；不存在的新建）
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '二进制转十进制') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_0 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '按权展开求和') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_1 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '十进制转十六进制') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_2 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '除基取余法') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_3 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '进制位权关系') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_4 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '八进制与十六进制的二进制表示') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_5 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '十进制小数转二进制') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_6 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '乘基取整法') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_7 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '十六进制加法') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_8 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '进位规则') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_9 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '补码表示') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_10 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '补码求真值') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_11 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'IEEE 754 浮点数') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_12 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '浮点数格式') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_13 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '汉字编码') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_14 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '机内码') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_15 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'ASCII 编码') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_16 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '字符编码规律') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_17 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '定点数表示') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_18 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '浮点数表示') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_19 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '数值范围与精度') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_20 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '逻辑异或运算') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_21 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '位运算') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_22 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '海明码') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_23 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '纠错编码') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_24 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '校验位计算') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_25 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '奇偶校验码') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_26 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '检错与纠错') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_27 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '补码加法') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_28 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '溢出判断') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_29 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '逻辑与或运算') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_30 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '逻辑等式') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_31 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '排列') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_32 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '计数原理') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_33 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '完全图') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_34 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '图的边数') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_35 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '独立事件概率') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_36 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '概率乘法') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_37 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '满二叉树') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_38 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '结点与叶子计数') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_39 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '集合运算') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_40 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '交集') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_41 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '冯·诺依曼结构') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_42 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '存储程序原理') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_43 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '运算器组成') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_44 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '寄存器功能') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_45 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'RISC') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_46 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'CISC') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_47 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '指令系统') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_48 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '指令流水线') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_49 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '流水线时间计算') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_50 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '总线分类') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_51 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '地址总线') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_52 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'Cache 命中率') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_53 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '平均访问时间') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_54 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'Cache 映射方式') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_55 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '直接映射') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_56 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'Cache') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_57 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '局部性原理') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_58 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '磁盘存取时间') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_59 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '旋转延迟') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_60 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '虚拟存储器') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_61 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '页面调度') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_62 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '串联系统可靠性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_63 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '可靠度计算') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_64 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '并联系统可靠性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_65 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '冗余设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_66 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '系统可用性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_67 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'MTBF 与 MTTR') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_68 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'CPI') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_69 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '性能指标') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_70 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '执行时间计算') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_71 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '主频与 CPI') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_72 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '折半查找') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_73 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '时间复杂度') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_74 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '排序稳定性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_75 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '归并排序') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_76 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '哈夫曼树') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_77 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '带权路径长度') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_78 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '二叉树遍历') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_79 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '遍历序列还原') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_80 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '连通图') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_81 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '生成树与边数') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_82 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '进程状态转换') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_83 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '时间片轮转') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_84 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '死锁') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_85 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '死锁必要条件') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_86 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '银行家算法') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_87 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '死锁避免') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_88 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '页面置换算法') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_89 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'Belady 异常') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_90 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '信号量') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_91 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'PV 操作') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_92 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '编译过程') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_93 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '词法分析') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_94 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '解释方式') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_95 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '编译方式') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_96 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '传引用调用') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_97 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '参数传递方式') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_98 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '正规式') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_99 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '有限自动机') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_100 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '动态绑定') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_101 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '多态') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_102 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '关系代数') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_103 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '选择运算') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_104 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '函数依赖') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_105 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '范式') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_106 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '第三范式') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_107 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'SQL 数据操纵') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_108 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'DELETE 语句') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_109 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'E-R 模型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_110 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '关系模式转换') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_111 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '外键') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_112 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '并发控制') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_113 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '事务隔离性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_114 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '脏读') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_115 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'OSI 参考模型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_116 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '网络层功能') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_117 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '传输层协议') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_118 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'TCP') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_119 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'IP 地址') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_120 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '子网掩码') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_121 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '网络地址计算') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_122 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '应用层协议') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_123 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'DNS 域名解析') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_124 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '子网划分') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_125 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '声音数字化') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_126 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '采样') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_127 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '图像分辨率') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_128 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '图像数据量计算') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_129 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '数据压缩') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_130 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '无损压缩') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_131 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '视频数据量') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_132 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '帧率') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_133 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '颜色空间') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_134 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'YUV 分量') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_135 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '视频压缩') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_136 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件工程三要素') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_137 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件工程概念') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_138 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件生命周期') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_139 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件危机') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_140 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件工程方法学') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_141 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '结构化方法') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_142 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '面向对象方法') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_143 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件危机的对策') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_144 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '需求获取') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_145 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '系统分析') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_146 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '结构化分析方法') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_147 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '数据流图') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_148 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '数据字典') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_149 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'DFD 图元') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_150 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '结构化分析') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_151 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '父子图平衡') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_152 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '分层分解') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_153 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '模块独立性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_154 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '耦合与内聚') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_155 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '耦合') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_156 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '内聚') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_157 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '结构化设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_158 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '模块化') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_159 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件体系结构') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_160 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '构件与连接') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_161 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '系统实施') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_162 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '实施阶段任务') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_163 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '编程规范') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_164 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '程序注释') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_165 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件测试') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_166 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '程序调试') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_167 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '集成测试') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_168 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '自顶向下集成') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_169 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '系统转换') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_170 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '转换方式对比') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_171 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '适应性维护') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_172 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '维护类型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_173 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '改正性维护') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_174 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '完善性维护') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_175 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '可维护性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_176 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件维护') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_177 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '维护成本') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_178 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '维护管理') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_179 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件质量保证') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_180 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '质量管理活动') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_181 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'SQA 活动') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_182 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '技术评审') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_183 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件度量') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_184 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '过程度量') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_185 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '质量成本') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_186 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件过程改进') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_187 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '过程改进意义') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_188 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件过程') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_189 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'IDEAL 模型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_190 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '过程改进步骤') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_191 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '过程改进组织') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_192 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '持续改进') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_193 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '瀑布模型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_194 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件生存周期') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_195 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '原型模型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_196 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '螺旋模型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_197 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '风险分析') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_198 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '增量模型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_199 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '增量交付') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_200 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '统一过程') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_201 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'RUP 阶段') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_202 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '需求分类') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_203 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '非功能需求') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_204 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '需求分析任务') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_205 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件需求规格说明书') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_206 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '需求基线') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_207 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '概要设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_208 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '详细设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_209 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '模块耦合') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_210 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '数据耦合') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_211 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '模块内聚') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_212 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '功能内聚') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_213 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '面向对象设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_214 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '内聚与耦合') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_215 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件测试目的') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_216 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '测试原则') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_217 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '黑盒测试') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_218 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '等价类划分') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_219 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '边界值分析') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_220 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '测试用例设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_221 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '白盒测试') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_222 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '路径覆盖') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_223 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '测试阶段') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_224 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '系统测试') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_225 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件质量特性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_226 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '可靠性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_227 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '易用性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_228 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件质量控制') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_229 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '阶段评审') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_230 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '可移植性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_231 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '能力成熟度模型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_232 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '已管理级') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_233 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'CMMI') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_234 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '能力等级') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_235 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '初始级') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_236 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '已定义级') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_237 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'SPICE') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_238 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '过程能力评定') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_239 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '构建工具') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_240 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '开发工具分类') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_241 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '版本控制') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_242 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '配置管理工具') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_243 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'CASE 工具') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_244 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件工具分类') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_245 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '调试工具') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_246 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '断点调试') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_247 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '代码生成工具') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_248 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'UML') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_249 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '顺序图') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_250 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '用例图') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_251 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'UML 类图') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_252 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '组合关系') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_253 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '面向对象分析') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_254 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '对象与类识别') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_255 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件复用') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_256 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '框架') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_257 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'B/S 结构') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_258 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '分布式应用') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_259 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'C/S 结构') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_260 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '系统结构比较') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_261 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '中间件') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_262 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '分布式系统') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_263 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'Socket 编程') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_264 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '网络应用开发') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_265 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '构件互操作') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_266 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '需求理解') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_267 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '需求确认') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_268 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '需求冲突处理') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_269 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '需求评审') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_270 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '需求可验证性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_271 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '需求质量') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_272 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '需求建模') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_273 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '开发计划') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_274 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '项目启动准备') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_275 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '可行性研究') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_276 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '技术经济与运行可行性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_277 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '网络计划图') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_278 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '关键路径') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_279 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '开发方式选择') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_280 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '自行开发与委托开发') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_281 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '功能设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_282 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '功能分解') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_283 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '功能结构图') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_284 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '功能层次分解') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_285 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '模块划分') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_286 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '高内聚低耦合') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_287 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '功能需求') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_288 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'E-R 图') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_289 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '概念数据模型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_290 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'E-R 图转换') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_291 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '多对多联系') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_292 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '关系模式设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_293 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '关系规范化') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_294 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '数据模型层次') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_295 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '概念与逻辑数据模型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_296 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '物理数据模型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_297 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '外部设计文档') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_298 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '文档内容') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_299 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '文档结构') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_300 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '文档编写要求') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_301 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '需求一致性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_302 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '设计评审') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_303 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '评审目的') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_304 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '走查') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_305 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '外部设计评审') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_306 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '评审检查要点') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_307 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '评审记录') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_308 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '缺陷跟踪') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_309 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件结构图') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_310 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '模块调用关系') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_311 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件结构设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_312 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '体系结构风格') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_313 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '管道-过滤器') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_314 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '数据流') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_315 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '模块层次') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_316 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '调用关系') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_317 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '人机界面设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_318 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '界面设计原则') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_319 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '输入设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_320 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '输入校验') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_321 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '输出设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_322 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '报表设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_323 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '人机交互设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_324 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '直接操纵') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_325 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '界面反馈') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_326 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '数据库物理设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_327 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '存取路径') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_328 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '索引设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_329 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '存取效率') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_330 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '物理设计权衡') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_331 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '文件组织') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_332 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '顺序文件') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_333 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '文件组织与索引') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_334 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '构件化') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_335 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '可复用构件') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_336 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '构件接口设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_337 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '低耦合') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_338 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '类库') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_339 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '控制反转') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_340 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '复用收益') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_341 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '内部设计文档') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_342 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '文档内容侧重点') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_343 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '模块说明') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_344 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '接口说明') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_345 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '内部设计评审') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_346 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '正式审查') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_347 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '评审方法') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_348 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '检查单') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_349 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '评审闭环') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_350 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '计算机硬件配置') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_351 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '配置依据') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_352 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '系统环境搭建') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_353 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '环境准备') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_354 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '系统部署') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_355 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '逐步与并行切换') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_356 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '环境配置管理') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_357 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '配置一致性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_358 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '程序设计语言选择') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_359 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '选型依据') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_360 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '语言适用场景') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_361 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '综合选型决策') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_362 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '程序设计语言选型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_363 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '语言选择依据') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_364 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '指针与取地址') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_365 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '指针间接访问') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_366 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '访问控制') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_367 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '类的封装') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_368 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '友元') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_369 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '字符数组与字符串') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_370 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'sizeof 与 strlen') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_371 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '异常处理') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_372 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'throw 与 catch') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_373 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '系统测试目标') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_374 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '测试类型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_375 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '验收测试') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_376 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '测试依据') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_377 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '压力测试') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_378 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '性能测试') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_379 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '因果图法') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_380 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '黑盒测试用例设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_381 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '模块划分原则') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_382 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '信息隐蔽') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_383 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '模块独立性度量') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_384 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '内聚类型') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_385 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '顺序内聚') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_386 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '程序设计文档') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_387 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '文档作用') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_388 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '注释规范') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_389 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '代码可读性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_390 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '文档质量') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_391 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '代码走查') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_392 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '程序设计评审') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_393 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '评审检查表') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_394 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '缺陷预防') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_395 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '评审闭环管理') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_396 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '缺陷早期发现') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_397 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '标准分级') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_398 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '国家标准') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_399 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '标准的性质') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_400 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '强制性标准与推荐性标准') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_401 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '标准化组织') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_402 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'ISO') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_403 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '标准编号') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_404 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'GB/T') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_405 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '信息化概念') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_406 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '信息资源开发利用') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_407 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'ERP 企业资源计划') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_408 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '信息系统分类') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_409 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'CRM 客户关系管理') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_410 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '数据仓库') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_411 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '数据挖掘') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_412 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件著作权') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_413 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '著作权保护期') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_414 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '专利权') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_415 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '专利权保护期') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_416 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '商标权') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_417 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '注册商标有效期') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_418 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '软件著作权侵权') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_419 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '合理使用') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_420 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '类与对象') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_421 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '抽象与实例') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_422 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '封装') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_423 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '对象') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_424 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '继承') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_425 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '代码复用') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_426 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '方法重写') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_427 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '消息') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_428 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '对象协作') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_429 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '抽象') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_430 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '面向对象基本概念') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_431 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '对称加密') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_432 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'DES') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_433 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '非对称加密') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_434 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '公钥与私钥') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_435 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'RSA') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_436 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '数字签名') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_437 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '消息摘要') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_438 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '完整性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_439 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '身份认证') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_440 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '防火墙') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_441 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '网络访问控制') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_442 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '计算机病毒') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_443 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '病毒防护') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_444 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '数据库设计步骤') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_445 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '逻辑结构设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_446 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '概念结构设计') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_447 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '事务') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_448 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, 'ACID 特性') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_449 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '索引') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_450 := LAST_INSERT_ID();
INSERT INTO rk_knowledge_point (subject_id, name) VALUES (6, '查询优化') ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @kp_451 := LAST_INSERT_ID();

-- SD-01.jsonl:1  SD-01#0001  outline 16 -> 163
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 163, 'SINGLE_CHOICE', '二进制数 $10110101$ 对应的十进制数是（　）。', 'MARKDOWN', 'C', '按权展开：$10110101=1\\times2^7+0\\times2^6+1\\times2^5+1\\times2^4+0\\times2^3+1\\times2^2+0\\times2^1+1\\times2^0=128+32+16+4+1=181$。选项 A、B、D 分别是漏加某一位权值后的结果，均不正确。', 2, 'AI_GENERATED', 'SD-01;outline=16', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0001 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0001, 'A', '$169$', 0), (@q0001, 'B', '$177$', 1), (@q0001, 'C', '$181$', 2), (@q0001, 'D', '$185$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0001, @kp_0);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0001, @kp_1);

-- SD-01.jsonl:2  SD-01#0002  outline 16 -> 163
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 163, 'SINGLE_CHOICE', '十进制数 $45$ 用十六进制表示为（　）。', 'MARKDOWN', 'B', '用 $16$ 除 $45$：$45\\div16=2$ 余 $13$，余数 $13$ 对应十六进制数码 $D$；商 $2$ 作为高位，故结果为 $2D$。选项 A、C 的个位数码偏小或偏大，D 把商写成了 $4$，均错误。', 1, 'AI_GENERATED', 'SD-01;outline=16', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0002 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0002, 'A', '$2C$', 0), (@q0002, 'B', '$2D$', 1), (@q0002, 'C', '$2E$', 2), (@q0002, 'D', '$4D$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0002, @kp_2);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0002, @kp_3);

-- SD-01.jsonl:3  SD-01#0003  outline 16 -> 163
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 163, 'SINGLE_CHOICE', '在计算机中，$1$ 位八进制数需要用（　）位二进制数表示，$1$ 位十六进制数需要用（　）位二进制数表示。', 'MARKDOWN', 'A', '因为 $2^3=8$，所以 $1$ 位八进制数由 $3$ 位二进制数表示；因为 $2^4=16$，所以 $1$ 位十六进制数由 $4$ 位二进制数表示。其余选项把两种进制对应的位数弄反或写错，均不正确。', 1, 'AI_GENERATED', 'SD-01;outline=16', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0003 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0003, 'A', '$3$ 和 $4$', 0), (@q0003, 'B', '$4$ 和 $3$', 1), (@q0003, 'C', '$2$ 和 $4$', 2), (@q0003, 'D', '$3$ 和 $3$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0003, @kp_4);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0003, @kp_5);

-- SD-01.jsonl:4  SD-01#0004  outline 16 -> 163
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 163, 'SINGLE_CHOICE', '十进制小数 $0.625$ 转换为二进制小数是（　）。', 'MARKDOWN', 'A', '小数部分用“乘 $2$ 取整”法：$0.625\\times2=1.25$ 取整 $1$；$0.25\\times2=0.5$ 取整 $0$；$0.5\\times2=1.0$ 取整 $1$。按取整顺序排列得 $0.101$。选项 B、C 的位序或取值不对，D 多算了一位，均不正确。', 2, 'AI_GENERATED', 'SD-01;outline=16', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0004 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0004, 'A', '$0.101$', 0), (@q0004, 'B', '$0.110$', 1), (@q0004, 'C', '$0.011$', 2), (@q0004, 'D', '$0.1001$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0004, @kp_6);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0004, @kp_7);

-- SD-01.jsonl:5  SD-01#0005  outline 16 -> 163
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 163, 'SINGLE_CHOICE', '十六进制数 $3A$ 与 $27$ 相加的结果用十六进制表示为（　）。', 'MARKDOWN', 'B', '按位相加：$A+7=10+7=17=16+1$，本位写 $1$、向高位进 $1$；高位 $3+2+1=6$。故结果为 $61$。选项 A 漏掉进位，C、D 高位数错误，均不正确。', 2, 'AI_GENERATED', 'SD-01;outline=16', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0005 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0005, 'A', '$51$', 0), (@q0005, 'B', '$61$', 1), (@q0005, 'C', '$62$', 2), (@q0005, 'D', '$71$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0005, @kp_8);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0005, @kp_9);

-- SD-01.jsonl:6  SD-01#0006  outline 17 -> 164
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 164, 'SINGLE_CHOICE', '某机器采用 $8$ 位补码表示整数，则补码 $11101110$ 所表示的真值是（　）。', 'MARKDOWN', 'B', '补码最高位为 $1$，说明是负数。求其绝对值：对 $11101110$ 逐位取反得 $00010001$，再加 $1$ 得 $00010010$，即十进制 $18$，故真值为 $-18$。选项 D 把它按无符号数解读；A、C 数值偏差，均不正确。', 3, 'AI_GENERATED', 'SD-01;outline=17', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0006 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0006, 'A', '$-17$', 0), (@q0006, 'B', '$-18$', 1), (@q0006, 'C', '$-19$', 2), (@q0006, 'D', '$238$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0006, @kp_10);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0006, @kp_11);

-- SD-01.jsonl:7  SD-01#0007  outline 17 -> 164
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 164, 'SINGLE_CHOICE', '在 IEEE 754 标准的 $32$ 位单精度浮点数格式中，符号位、阶码和尾数所占位数依次为（　）。', 'MARKDOWN', 'A', 'IEEE 754 单精度格式共 $32$ 位：符号位 $1$ 位，阶码 $8$ 位，尾数 $23$ 位。选项 B 把阶码与尾数位数分配错，C 是错误分配，D 总位数超出 $32$ 位，均不正确。', 2, 'AI_GENERATED', 'SD-01;outline=17', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0007 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0007, 'A', '$1$、$8$、$23$', 0), (@q0007, 'B', '$1$、$7$、$24$', 1), (@q0007, 'C', '$1$、$11$、$20$', 2), (@q0007, 'D', '$1$、$8$、$24$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0007, @kp_12);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0007, @kp_13);

-- SD-01.jsonl:8  SD-01#0008  outline 17 -> 164
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 164, 'SINGLE_CHOICE', '在汉字信息处理系统中，汉字在计算机内部存储、处理和传输时所使用的统一编码是（　）。', 'MARKDOWN', 'B', '输入码用于把汉字录入计算机，如拼音码；机内码是汉字在计算机内部存储和处理时使用的统一编码；字形码用于显示和打印汉字的字形。因此选 B。选项 A、C 用途不符，D 不是汉字编码类型。', 3, 'AI_GENERATED', 'SD-01;outline=17', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0008 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0008, 'A', '输入码', 0), (@q0008, 'B', '机内码', 1), (@q0008, 'C', '字形码', 2), (@q0008, 'D', '地址码', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0008, @kp_14);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0008, @kp_15);

-- SD-01.jsonl:9  SD-01#0009  outline 17 -> 164
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 164, 'SINGLE_CHOICE', '在标准 ASCII 编码中，大写字母 $A$ 的编码为 $65$，则小写字母 $a$ 的编码为（　）。', 'MARKDOWN', 'C', '在 ASCII 码表中，同一字母的小写编码比大写编码大 $32$，即 $65+32=97$，故 $a$ 的编码为 $97$。选项 B、D 分别是 $Z$、$z$ 的编码；A 不是对应字母的编码，均不正确。', 1, 'AI_GENERATED', 'SD-01;outline=17', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0009 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0009, 'A', '$61$', 0), (@q0009, 'B', '$90$', 1), (@q0009, 'C', '$97$', 2), (@q0009, 'D', '$122$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0009, @kp_16);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0009, @kp_17);

-- SD-01.jsonl:10  SD-01#0010  outline 17 -> 164
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 164, 'SINGLE_CHOICE', '关于计算机中定点数与浮点数的表示，下列说法正确的是（　）。', 'MARKDOWN', 'A', '浮点数用阶码表示小数点位置、尾数表示有效数字，因此在相同字长下可表示的范围远大于定点数，故 A 正确。定点数既能表示整数也能表示纯小数，B 错；浮点数位数固定时其有效数字位数并不总是高于定点数，C 错；浮点运算需要对齐阶码等步骤，实现比定点运算更复杂，D 恰好相反。', 4, 'AI_GENERATED', 'SD-01;outline=17', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0010 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0010, 'A', '浮点数由阶码和尾数组成，其可表示的数值范围通常比相同位数的定点数大', 0), (@q0010, 'B', '定点数只能表示整数，不能表示纯小数', 1), (@q0010, 'C', '浮点数的精度在所有情况下都高于定点数', 2), (@q0010, 'D', '定点数运算的实现比浮点数运算更复杂', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0010, @kp_18);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0010, @kp_19);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0010, @kp_20);

-- SD-01.jsonl:11  SD-01#0011  outline 18 -> 165
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 165, 'SINGLE_CHOICE', '设有两个二进制数 $X=1010$、$Y=1100$，则 $X\\oplus Y$（异或）的结果是（　）。', 'MARKDOWN', 'A', '异或运算规则是“相同为 $0$，不同为 $1$”。逐位计算：$1\\oplus1=0$，$0\\oplus1=1$，$1\\oplus0=1$，$0\\oplus0=0$，结果为 $0110$。选项 C 是与运算结果，B、D 位数不对应，均不正确。', 2, 'AI_GENERATED', 'SD-01;outline=18', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0011 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0011, 'A', '$0110$', 0), (@q0011, 'B', '$1000$', 1), (@q0011, 'C', '$1110$', 2), (@q0011, 'D', '$0100$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0011, @kp_21);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0011, @kp_22);

-- SD-01.jsonl:12  SD-01#0012  outline 18 -> 165
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 165, 'SINGLE_CHOICE', '若要用海明码对 $8$ 位数据进行检错和纠错，则至少需要附加（　）位校验位。', 'MARKDOWN', 'B', '海明码校验位数 $k$ 需满足 $2^k\\ge n+k+1$，其中数据位 $n=8$。取 $k=3$ 时 $2^3=8<8+3+1=12$，不满足；取 $k=4$ 时 $2^4=16\\ge13$，满足要求。故至少需要 $4$ 位校验位，选 B。', 4, 'AI_GENERATED', 'SD-01;outline=18', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0012 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0012, 'A', '$3$', 0), (@q0012, 'B', '$4$', 1), (@q0012, 'C', '$5$', 2), (@q0012, 'D', '$8$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0012, @kp_23);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0012, @kp_24);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0012, @kp_25);

-- SD-01.jsonl:13  SD-01#0013  outline 18 -> 165
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 165, 'SINGLE_CHOICE', '关于奇偶校验码，下列说法正确的是（　）。', 'MARKDOWN', 'B', '奇偶校验通过增加一位校验位使 $1$ 的个数为奇或偶，能发现奇数个位出错，但无法定位具体是哪一位，更不能纠错，故 B 正确。A 夸大了其能力；C 说法片面；D 与定义矛盾。', 2, 'AI_GENERATED', 'SD-01;outline=18', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0013 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0013, 'A', '既能发现错误又能确定错误的具体位置', 0), (@q0013, 'B', '能发现一位错误，但不能确定错误发生在哪一位', 1), (@q0013, 'C', '只能检测出偶数个位同时出错的情况', 2), (@q0013, 'D', '不需要附加校验位即可工作', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0013, @kp_26);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0013, @kp_27);

-- SD-01.jsonl:14  SD-01#0014  outline 18 -> 165
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 165, 'SINGLE_CHOICE', '两个 $8$ 位补码数 $01111111$ 与 $00000001$ 相加，其结果是（　）。', 'MARKDOWN', 'A', '$01111111$ 为 $+127$，$00000001$ 为 $+1$，相加真值为 $+128$。$8$ 位补码正数范围为 $+127$，$+128$ 已超出范围，故发生溢出，和的机器数为 $10000000$。因此选 A。其余选项的溢出判断或数值均错误。', 4, 'AI_GENERATED', 'SD-01;outline=18', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0014 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0014, 'A', '$10000000$，且发生溢出', 0), (@q0014, 'B', '$10000000$，且未发生溢出', 1), (@q0014, 'C', '$01111111$，且发生溢出', 2), (@q0014, 'D', '$11111111$，且发生溢出', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0014, @kp_28);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0014, @kp_29);

-- SD-01.jsonl:15  SD-01#0015  outline 18 -> 165
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 165, 'SINGLE_CHOICE', '已知逻辑表达式满足 $A\\land B=0$ 且 $A\\lor B=1$，则 $A$ 与 $B$ 的关系是（　）。', 'MARKDOWN', 'C', '$A\\land B=0$ 说明两者不同时为 $1$；$A\\lor B=1$ 说明两者至少有一个为 $1$。合起来即两者必有一个为 $1$、另一个为 $0$，故 $A$ 与 $B$ 互补，$B=\\bar{A}$。选项 B 只是其中一种情况，A、D 与条件矛盾或不足，均不正确。', 3, 'AI_GENERATED', 'SD-01;outline=18', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0015 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0015, 'A', '$A=B$', 0), (@q0015, 'B', '$A=1$，$B=0$', 1), (@q0015, 'C', '$B=\\bar{A}$（二者互补）', 2), (@q0015, 'D', '无法确定', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0015, @kp_30);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0015, @kp_31);

-- SD-01.jsonl:16  SD-01#0016  outline 19 -> 166
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 166, 'SINGLE_CHOICE', '从 $5$ 名候选人中选出 $3$ 人，分别担任三个不同的职务，共有（　）种不同的安排方法。', 'MARKDOWN', 'B', '三个职务互不相同，与顺序有关，用排列数：$P(5,3)=5\\times4\\times3=60$。选项 A 是组合数 $C(5,3)=10$，把有序问题当成了无序选择；C、D 是重复排列结果，均不正确。', 2, 'AI_GENERATED', 'SD-01;outline=19', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0016 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0016, 'A', '$10$', 0), (@q0016, 'B', '$60$', 1), (@q0016, 'C', '$125$', 2), (@q0016, 'D', '$243$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0016, @kp_32);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0016, @kp_33);

-- SD-01.jsonl:17  SD-01#0017  outline 19 -> 166
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 166, 'SINGLE_CHOICE', '含 $n$ 个顶点的无向完全图共有（　）条边。', 'MARKDOWN', 'B', '无向完全图中任意两个顶点之间恰有一条边，从 $n$ 个顶点中任取 $2$ 个的组合数即边数：$C(n,2)=n(n-1)/2$。选项 A 把无向边当成了有向弧，重复计算；C、D 明显偏大，均不正确。', 2, 'AI_GENERATED', 'SD-01;outline=19', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0017 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0017, 'A', '$n(n-1)$', 0), (@q0017, 'B', '$n(n-1)/2$', 1), (@q0017, 'C', '$n^2$', 2), (@q0017, 'D', '$n(n+1)/2$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0017, @kp_34);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0017, @kp_35);

-- SD-01.jsonl:18  SD-01#0018  outline 19 -> 166
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 166, 'SINGLE_CHOICE', '某系统由两个相互独立的部件组成，只有两个部件同时正常系统才能工作，两部件正常工作的概率分别为 $0.9$ 和 $0.8$，则系统正常工作的概率为（　）。', 'MARKDOWN', 'B', '两部件相互独立且必须同时正常，属于串联关系，概率相乘：$0.9\\times0.8=0.72$。选项 A 是简单相加后取值，C 只取了一个部件，D 是按至少一个正常计算，均不正确。', 2, 'AI_GENERATED', 'SD-01;outline=19', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0018 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0018, 'A', '$0.70$', 0), (@q0018, 'B', '$0.72$', 1), (@q0018, 'C', '$0.80$', 2), (@q0018, 'D', '$0.98$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0018, @kp_36);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0018, @kp_37);

-- SD-01.jsonl:19  SD-01#0019  outline 19 -> 166
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 166, 'SINGLE_CHOICE', '一棵含 $n$ 个结点的满二叉树，其叶子结点的个数为（　）。', 'MARKDOWN', 'B', '满二叉树除叶子外每个结点都有两个子结点。设叶子数为 $m$，则总结点数满足 $n=2m-1$，解得 $m=(n+1)/2$。选项 A、C、D 与二叉树结点计数关系不符，均不正确。', 3, 'AI_GENERATED', 'SD-01;outline=19', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0019 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0019, 'A', '$n/2$', 0), (@q0019, 'B', '$(n+1)/2$', 1), (@q0019, 'C', '$n-1$', 2), (@q0019, 'D', '$2n$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0019, @kp_38);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0019, @kp_39);

-- SD-01.jsonl:20  SD-01#0020  outline 19 -> 166
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 166, 'SINGLE_CHOICE', '设集合 $A=\\{1,2,3\\}$，$B=\\{2,3,4\\}$，则 $A\\cap B$ 中元素的个数为（　）。', 'MARKDOWN', 'B', '交集由同时属于两个集合的元素组成，$A\\cap B=\\{2,3\\}$，共 $2$ 个元素。选项 A、C 计数有误，D 是并集 $A\\cup B$ 的元素个数，均不正确。', 1, 'AI_GENERATED', 'SD-01;outline=19', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0020 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0020, 'A', '$1$', 0), (@q0020, 'B', '$2$', 1), (@q0020, 'C', '$3$', 2), (@q0020, 'D', '$5$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0020, @kp_40);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0020, @kp_41);

-- SD-01.jsonl:21  SD-01#0021  outline 81 -> 226
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 226, 'SINGLE_CHOICE', '冯·诺依曼体系结构计算机的核心设计思想是（　）。', 'MARKDOWN', 'B', '冯·诺依曼结构的核心是把程序和数据一起以二进制形式存放在存储器中，由控制器自动按序取指执行，即“存储程序、程序控制”。并行处理、流水线、虚拟存储都是后来的技术，并非其核心思想，故 A、C、D 不正确。', 1, 'AI_GENERATED', 'SD-01;outline=81', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0021 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0021, 'A', '并行处理', 0), (@q0021, 'B', '存储程序与程序控制', 1), (@q0021, 'C', '流水线执行', 2), (@q0021, 'D', '虚拟存储', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0021, @kp_42);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0021, @kp_43);

-- SD-01.jsonl:22  SD-01#0022  outline 81 -> 226
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 226, 'SINGLE_CHOICE', '在运算器中，用于暂存操作数和运算结果的是（　），用于完成算术与逻辑运算的是（　）。', 'MARKDOWN', 'A', '运算器由算术逻辑单元（ALU）、累加寄存器、数据缓冲寄存器等组成，其中累加寄存器暂存操作数和运算结果，ALU 完成算术与逻辑运算。选项 B、C 中的程序计数器、指令寄存器、控制器属于控制器部件，D 的地址寄存器也属于控制器，均不正确。', 3, 'AI_GENERATED', 'SD-01;outline=81', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0022 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0022, 'A', '累加寄存器、算术逻辑单元', 0), (@q0022, 'B', '程序计数器、指令寄存器', 1), (@q0022, 'C', '指令寄存器、控制器', 2), (@q0022, 'D', '地址寄存器、数据寄存器', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0022, @kp_44);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0022, @kp_45);

-- SD-01.jsonl:23  SD-01#0023  outline 81 -> 226
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 226, 'SINGLE_CHOICE', '与 CISC 相比，RISC 的主要特点是（　）。', 'MARKDOWN', 'B', 'RISC 精简指令系统计算机的指令数量少、格式规整、长度固定、寻址方式简单，便于采用流水线技术，故 B 正确。A 是 CISC 的特征；C 与 RISC 大量使用通用寄存器的事实矛盾；D 说法绝对且与 RISC 多数指令单周期完成相矛盾。', 3, 'AI_GENERATED', 'SD-01;outline=81', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0023 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0023, 'A', '指令数量多，寻址方式复杂', 0), (@q0023, 'B', '指令数量少、长度固定，便于实现流水线', 1), (@q0023, 'C', '完全不使用寄存器', 2), (@q0023, 'D', '所有指令的执行时间都必须为多个时钟周期', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0023, @kp_46);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0023, @kp_47);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0023, @kp_48);

-- SD-01.jsonl:24  SD-01#0024  outline 81 -> 226
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 226, 'SINGLE_CHOICE', '某指令流水线分为取指、分析、执行三段，每段耗时均为 $\\Delta t$。在理想情况下连续执行 $n$ 条指令所需的总时间为（　）。', 'MARKDOWN', 'B', '流水线共 $k=3$ 段，理想情况下第一条指令需 $3\\Delta t$，此后每经过 $\\Delta t$ 完成一条，总时间为 $3\\Delta t+(n-1)\\Delta t=(n+2)\\Delta t$。选项 A 忽略了流水线充满所需的启动时间，C 是串行执行时间，D 多算了一段，均不正确。', 4, 'AI_GENERATED', 'SD-01;outline=81', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0024 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0024, 'A', '$n\\Delta t$', 0), (@q0024, 'B', '$(n+2)\\Delta t$', 1), (@q0024, 'C', '$3n\\Delta t$', 2), (@q0024, 'D', '$(n+3)\\Delta t$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0024, @kp_49);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0024, @kp_50);

-- SD-01.jsonl:25  SD-01#0025  outline 81 -> 226
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 226, 'SINGLE_CHOICE', 'CPU 访问主存或 I/O 端口时，用来传送地址信息的总线是（　）。', 'MARKDOWN', 'B', '系统总线按功能分为数据总线、地址总线和控制总线。地址总线传送 CPU 要访问的存储单元或 I/O 端口的地址，故 B 正确。数据总线传送数据，控制总线传送控制信号，通信总线用于设备间通信，均不符合题意。', 2, 'AI_GENERATED', 'SD-01;outline=81', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0025 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0025, 'A', '数据总线', 0), (@q0025, 'B', '地址总线', 1), (@q0025, 'C', '控制总线', 2), (@q0025, 'D', '通信总线', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0025, @kp_51);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0025, @kp_52);

-- SD-01.jsonl:26  SD-01#0026  outline 80 -> 227
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 227, 'SINGLE_CHOICE', '某计算机 Cache 的命中率为 $0.95$，命中时访问时间为 $1\\text{ns}$，未命中时访问时间为 $5\\text{ns}$，则平均访问时间约为（　）。', 'MARKDOWN', 'B', '平均访问时间 $T=h\\cdot t_c+(1-h)\\cdot t_m=0.95\\times1+0.05\\times5=0.95+0.25=1.20\\text{ns}$。选项 A 漏算了未命中的开销，D 把命中率用反，C 计算有误，均不正确。', 4, 'AI_GENERATED', 'SD-01;outline=80', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0026 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0026, 'A', '$1.05\\text{ns}$', 0), (@q0026, 'B', '$1.20\\text{ns}$', 1), (@q0026, 'C', '$1.25\\text{ns}$', 2), (@q0026, 'D', '$4.80\\text{ns}$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0026, @kp_53);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0026, @kp_54);

-- SD-01.jsonl:27  SD-01#0027  outline 80 -> 227
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 227, 'SINGLE_CHOICE', '在 Cache 的地址映射方式中，硬件实现最简单但块冲突率最高的是（　）。', 'MARKDOWN', 'B', '直接映射把主存块固定映射到唯一的 Cache 行，硬件结构最简单、查找最快，但冲突率最高，故 B 正确。全相联映射冲突最少但比较电路复杂，组相联是二者的折中；段页式属于存储管理方式，不是 Cache 映射，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-01;outline=80', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0027 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0027, 'A', '全相联映射', 0), (@q0027, 'B', '直接映射', 1), (@q0027, 'C', '组相联映射', 2), (@q0027, 'D', '段页式映射', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0027, @kp_55);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0027, @kp_56);

-- SD-01.jsonl:28  SD-01#0028  outline 80 -> 227
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 227, 'SINGLE_CHOICE', 'Cache 能够显著提高系统性能，其主要理论依据是（　）。', 'MARKDOWN', 'A', '程序在一段时间内往往集中访问某一部分地址，即时间局部性和空间局部性，这使得用高速小容量的 Cache 缓存常用数据成为有效，故 A 正确。摩尔定律描述集成电路发展，香农定理关于信道容量，阿姆达尔定律描述加速比上限，均与 Cache 原理无关。', 1, 'AI_GENERATED', 'SD-01;outline=80', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0028 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0028, 'A', '程序访问的局部性原理', 0), (@q0028, 'B', '摩尔定律', 1), (@q0028, 'C', '香农定理', 2), (@q0028, 'D', '阿姆达尔定律', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0028, @kp_57);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0028, @kp_58);

-- SD-01.jsonl:29  SD-01#0029  outline 80 -> 227
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 227, 'SINGLE_CHOICE', '磁盘的存取时间通常由寻道时间和（　）组成。', 'MARKDOWN', 'A', '磁盘存取时间主要由寻道时间、旋转延迟（等待目标扇区转到读写头下）和数据传输时间三部分组成，故 A 正确。中断响应、指令译码属于 CPU 处理过程，动态刷新是 DRAM 的特性，均与磁盘存取无关。', 2, 'AI_GENERATED', 'SD-01;outline=80', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0029 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0029, 'A', '旋转延迟与数据传输时间', 0), (@q0029, 'B', '中断响应时间', 1), (@q0029, 'C', '指令译码时间', 2), (@q0029, 'D', '动态刷新时间', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0029, @kp_59);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0029, @kp_60);

-- SD-01.jsonl:30  SD-01#0030  outline 80 -> 227
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 227, 'SINGLE_CHOICE', '虚拟存储器功能的实现主要依赖于（　）。', 'MARKDOWN', 'B', '虚拟存储器把主存和外存视为一个整体，依据程序访问的局部性原理，只把当前用到的页调入主存，通过页表完成逻辑地址到物理地址的转换并在缺页时调页，故 B 正确。A、C、D 涉及的是 Cache、流水线和 I/O 控制，与虚拟存储的实现机制不符。', 3, 'AI_GENERATED', 'SD-01;outline=80', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0030 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0030, 'A', 'Cache 与寄存器的配合', 0), (@q0030, 'B', '程序局部性原理与内外存之间的页面调度', 1), (@q0030, 'C', '流水线与并行处理', 2), (@q0030, 'D', '中断与 DMA 的配合', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0030, @kp_61);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0030, @kp_62);

-- SD-01.jsonl:31  SD-01#0031  outline 79 -> 228
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 228, 'SINGLE_CHOICE', '由 $3$ 个部件串联组成的系统，各部件可靠度均为 $0.9$，则系统的可靠度为（　）。', 'MARKDOWN', 'B', '串联系统要求所有部件都正常，系统可靠度等于各部件可靠度之积：$0.9^3=0.729$。选项 A 只取单个部件，C 是并联结构的结果，D 近似值错误，均不正确。', 3, 'AI_GENERATED', 'SD-01;outline=79', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0031 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0031, 'A', '$0.9$', 0), (@q0031, 'B', '$0.729$', 1), (@q0031, 'C', '$0.999$', 2), (@q0031, 'D', '$0.7$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0031, @kp_63);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0031, @kp_64);

-- SD-01.jsonl:32  SD-01#0032  outline 79 -> 228
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 228, 'SINGLE_CHOICE', '由 $3$ 个部件并联（冗余）组成的系统，各部件可靠度均为 $0.9$，则系统的可靠度为（　）。', 'MARKDOWN', 'C', '并联系统只要有一个部件正常即可工作，其失效概率为各部件失效概率之积：$(1-0.9)^3=0.001$，故可靠度为 $1-0.001=0.999$。选项 A 是串联结构的结果，B、D 计算错误，均不正确。', 3, 'AI_GENERATED', 'SD-01;outline=79', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0032 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0032, 'A', '$0.729$', 0), (@q0032, 'B', '$0.9$', 1), (@q0032, 'C', '$0.999$', 2), (@q0032, 'D', '$0.99$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0032, @kp_65);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0032, @kp_66);

-- SD-01.jsonl:33  SD-01#0033  outline 79 -> 228
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 228, 'SINGLE_CHOICE', '衡量系统可用性（Availability）的常用表达式是（　）。', 'MARKDOWN', 'A', '可用性指系统在任意时刻可正常工作的概率，等于平均无故障时间与平均无故障时间和平均修复时间之和的比，即 $A=MTBF/(MTBF+MTTR)$。选项 B 是停机率，C、D 都不是可用性的定义式，均不正确。', 4, 'AI_GENERATED', 'SD-01;outline=79', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0033 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0033, 'A', '$MTBF/(MTBF+MTTR)$', 0), (@q0033, 'B', '$MTTR/(MTBF+MTTR)$', 1), (@q0033, 'C', '$MTBF\\times MTTR$', 2), (@q0033, 'D', '$1/MTBF$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0033, @kp_67);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0033, @kp_68);

-- SD-01.jsonl:34  SD-01#0034  outline 79 -> 228
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 228, 'SINGLE_CHOICE', '在计算机性能评价中，$CPI$ 的含义是（　）。', 'MARKDOWN', 'B', 'CPI（Cycles Per Instruction）表示执行一条指令平均所需的时钟周期数，故 B 正确。选项 A 是 MIPS 的含义，C 是主频，D 是执行时间，均与 CPI 定义不符。', 2, 'AI_GENERATED', 'SD-01;outline=79', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0034 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0034, 'A', '每秒执行的百万条指令数', 0), (@q0034, 'B', '执行一条指令所需的平均时钟周期数', 1), (@q0034, 'C', 'CPU 的时钟频率', 2), (@q0034, 'D', '程序执行的总时间', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0034, @kp_69);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0034, @kp_70);

-- SD-01.jsonl:35  SD-01#0035  outline 79 -> 228
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 228, 'SINGLE_CHOICE', '某 CPU 主频为 $1\\text{GHz}$，某程序共执行 $10^8$ 条指令，平均 $CPI$ 为 $2$，则该程序的执行时间约为（　）。', 'MARKDOWN', 'B', '执行时间 $=$ 指令条数 $\\times CPI\\div$ 主频 $=10^8\\times2\\div10^9=0.2\\text{s}$。选项 A 忘记乘 $CPI$，C、D 计算错误，均不正确。', 4, 'AI_GENERATED', 'SD-01;outline=79', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0035 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0035, 'A', '$0.1\\text{s}$', 0), (@q0035, 'B', '$0.2\\text{s}$', 1), (@q0035, 'C', '$0.5\\text{s}$', 2), (@q0035, 'D', '$2\\text{s}$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0035, @kp_71);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0035, @kp_72);

-- SD-01.jsonl:36  SD-01#0036  outline 85 -> 229
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 229, 'SINGLE_CHOICE', '在含 $n$ 个元素的有序表上进行折半查找，其时间复杂度为（　）。', 'MARKDOWN', 'B', '折半查找每次把查找区间缩小一半，最多比较 $\\lceil\\log_2(n+1)\\rceil$ 次，故时间复杂度为 $O(\\log_2 n)$。选项 A 是顺序查找的复杂度，C、D 明显偏大，均不正确。', 2, 'AI_GENERATED', 'SD-01;outline=85', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0036 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0036, 'A', '$O(n)$', 0), (@q0036, 'B', '$O(\\log_2 n)$', 1), (@q0036, 'C', '$O(n^2)$', 2), (@q0036, 'D', '$O(n\\log_2 n)$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0036, @kp_73);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0036, @kp_74);

-- SD-01.jsonl:37  SD-01#0037  outline 85 -> 229
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 229, 'SINGLE_CHOICE', '下列排序方法中，属于稳定排序的是（　）。', 'MARKDOWN', 'C', '稳定排序指关键字相等的记录在排序后保持原有相对次序。归并排序在合并时对相等元素按原顺序处理，是稳定的；快速排序、堆排序、直接选择排序在交换过程中可能打乱相等元素的相对次序，均不稳定。故 C 正确。', 3, 'AI_GENERATED', 'SD-01;outline=85', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0037 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0037, 'A', '快速排序', 0), (@q0037, 'B', '堆排序', 1), (@q0037, 'C', '归并排序', 2), (@q0037, 'D', '直接选择排序', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0037, @kp_75);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0037, @kp_76);

-- SD-01.jsonl:38  SD-01#0038  outline 85 -> 229
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 229, 'SINGLE_CHOICE', '已知权值集合 $\\{5,7,2,4\\}$，用其构造哈夫曼树，则该树的带权路径长度 $WPL$ 为（　）。', 'MARKDOWN', 'C', '按哈夫曼算法每次取最小的两个权值合并：$2+4=6$；再取 $5$ 与 $6$ 合并得 $11$；最后 $7+11=18$。$WPL$ 等于所有合并结点权值之和：$6+11+18=35$。故 C 正确，其余选项求和方式有误。', 4, 'AI_GENERATED', 'SD-01;outline=85', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0038 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0038, 'A', '$18$', 0), (@q0038, 'B', '$34$', 1), (@q0038, 'C', '$35$', 2), (@q0038, 'D', '$36$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0038, @kp_77);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0038, @kp_78);

-- SD-01.jsonl:39  SD-01#0039  outline 85 -> 229
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 229, 'SINGLE_CHOICE', '在二叉树的遍历序列中，仅凭下列（　）组序列不能唯一确定一棵二叉树。', 'MARKDOWN', 'A', '由“先序 + 中序”或“中序 + 后序”都可以借助中序序列划分左右子树，唯一确定二叉树。而先序和后序都只能确定根，无法区分左右子树，故不能唯一确定，选 A。', 4, 'AI_GENERATED', 'SD-01;outline=85', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0039 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0039, 'A', '先序序列和后序序列', 0), (@q0039, 'B', '先序序列和中序序列', 1), (@q0039, 'C', '中序序列和后序序列', 2), (@q0039, 'D', '中序序列和层序序列', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0039, @kp_79);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0039, @kp_80);

-- SD-01.jsonl:40  SD-01#0040  outline 85 -> 229
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 229, 'SINGLE_CHOICE', '具有 $n$ 个顶点的无向连通图，至少需要（　）条边。', 'MARKDOWN', 'B', '$n$ 个顶点的连通图至少要包含一棵生成树，而 $n$ 个顶点的树恰有 $n-1$ 条边，故最少需要 $n-1$ 条边。选项 A、C 边数虽然也能连通但并非最少，D 是完全图的边数，均不正确。', 2, 'AI_GENERATED', 'SD-01;outline=85', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0040 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0040, 'A', '$n$', 0), (@q0040, 'B', '$n-1$', 1), (@q0040, 'C', '$n+1$', 2), (@q0040, 'D', '$n(n-1)/2$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0040, @kp_81);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0040, @kp_82);

-- SD-01.jsonl:41  SD-01#0041  outline 84 -> 230
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 230, 'SINGLE_CHOICE', '在进程状态转换中，进程由运行态转变为就绪态的原因通常是（　）。', 'MARKDOWN', 'B', '进程在运行中时间片用完或被更高优先级进程抢占时，会从运行态回到就绪态等待再次调度，故 B 正确。等待 I/O 会转为阻塞态，被调度选中是从就绪态转为运行态，执行结束是从运行态转为终止态，均不正确。', 3, 'AI_GENERATED', 'SD-01;outline=84', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0041 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0041, 'A', '等待 I/O 操作完成', 0), (@q0041, 'B', '时间片用完', 1), (@q0041, 'C', '被处理机调度选中', 2), (@q0041, 'D', '进程执行结束', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0041, @kp_83);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0041, @kp_84);

-- SD-01.jsonl:42  SD-01#0042  outline 84 -> 230
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 230, 'SINGLE_CHOICE', '下列选项中，不属于产生死锁必要条件的是（　）。', 'MARKDOWN', 'D', '产生死锁的四个必要条件是互斥、请求并保持、不可剥夺和循环等待。资源数量充足恰恰有利于避免竞争，不是死锁的必要条件，故选 D。A、B、C 均是四个必要条件之一。', 2, 'AI_GENERATED', 'SD-01;outline=84', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0042 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0042, 'A', '互斥条件', 0), (@q0042, 'B', '请求并保持条件', 1), (@q0042, 'C', '不可剥夺条件', 2), (@q0042, 'D', '资源数量充足', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0042, @kp_85);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0042, @kp_86);

-- SD-01.jsonl:43  SD-01#0043  outline 84 -> 230
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 230, 'SINGLE_CHOICE', '银行家算法在操作系统中主要用于（　）。', 'MARKDOWN', 'B', '银行家算法在每次分配资源前先进行安全性检查，只有当分配后系统仍处于安全状态才真正分配，从而避免死锁发生，属于死锁避免策略，故 B 正确。它不属于进程调度、页面置换，也不是在死锁发生后的检测算法，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-01;outline=84', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0043 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0043, 'A', '进程调度', 0), (@q0043, 'B', '死锁避免', 1), (@q0043, 'C', '死锁检测', 2), (@q0043, 'D', '页面置换', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0043, @kp_87);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0043, @kp_88);

-- SD-01.jsonl:44  SD-01#0044  outline 84 -> 230
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 230, 'SINGLE_CHOICE', '在页面置换算法中，可能会出现 Belady 异常的是（　）。', 'MARKDOWN', 'C', 'Belady 异常指分配的物理块增多反而使缺页次数增加的怪异现象。FIFO 算法会出现该异常，故 C 正确。LRU、OPT 属于堆栈类算法，不会出现该异常；LFU 通常也不具备该异常特征。', 4, 'AI_GENERATED', 'SD-01;outline=84', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0044 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0044, 'A', 'LRU', 0), (@q0044, 'B', 'OPT', 1), (@q0044, 'C', 'FIFO', 2), (@q0044, 'D', 'LFU', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0044, @kp_89);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0044, @kp_90);

-- SD-01.jsonl:45  SD-01#0045  outline 84 -> 230
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 230, 'SINGLE_CHOICE', '若信号量 $S$ 的初值为 $3$，经过若干次 $P$、$V$ 操作后当前值为 $-2$，则表示当前有（　）个进程正在等待该资源。', 'MARKDOWN', 'B', '信号量值为负时，其绝对值表示在该信号量队列上等待的进程数目。当前 $S=-2$，故有 $2$ 个进程在等待，选 B。初值 $3$ 只说明最多可有 $3$ 个进程同时使用该资源，与等待数无关，故 A、C、D 不正确。', 4, 'AI_GENERATED', 'SD-01;outline=84', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0045 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0045, 'A', '$0$', 0), (@q0045, 'B', '$2$', 1), (@q0045, 'C', '$3$', 2), (@q0045, 'D', '$5$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0045, @kp_91);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0045, @kp_92);

-- SD-01.jsonl:46  SD-01#0046  outline 83 -> 231
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 231, 'SINGLE_CHOICE', '高级语言源程序在编译过程中，各阶段的正确顺序是（　）。', 'MARKDOWN', 'A', '编译过程一般先由词法分析把字符流分解为单词符号，再由语法分析依据文法检查结构，然后做语义分析并生成中间代码，最后生成目标代码，故顺序为 A。其余选项把分析阶段的先后顺序颠倒，均不正确。', 2, 'AI_GENERATED', 'SD-01;outline=83', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0046 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0046, 'A', '词法分析→语法分析→语义分析→代码生成', 0), (@q0046, 'B', '语法分析→词法分析→语义分析→代码生成', 1), (@q0046, 'C', '语义分析→词法分析→语法分析→代码生成', 2), (@q0046, 'D', '词法分析→语义分析→语法分析→代码生成', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0046, @kp_93);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0046, @kp_94);

-- SD-01.jsonl:47  SD-01#0047  outline 83 -> 231
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 231, 'SINGLE_CHOICE', '与编译方式相比，解释方式的主要特点是（　）。', 'MARKDOWN', 'B', '解释方式对源程序逐句翻译并立即执行，不产生独立的目标代码，因此运行速度通常比编译方式慢，故 B 正确。A 描述的是编译方式，C 错误（解释同样需要语法分析），D 与解释执行可以边报错边处理的事实不符。', 2, 'AI_GENERATED', 'SD-01;outline=83', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0047 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0047, 'A', '先生成目标代码再执行，运行速度快', 0), (@q0047, 'B', '不生成独立的目标代码，边解释边执行，运行速度较慢', 1), (@q0047, 'C', '不需要进行语法分析', 2), (@q0047, 'D', '只能处理不含错误的程序', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0047, @kp_95);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0047, @kp_96);

-- SD-01.jsonl:48  SD-01#0048  outline 83 -> 231
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 231, 'SINGLE_CHOICE', '在程序设计中采用传引用（引用传递）方式调用函数时，形参的改变（　）影响实参。', 'MARKDOWN', 'B', '传引用方式把实参的地址传给形参，形参和实参引用同一存储单元，因此对形参的修改会直接影响实参，故选 B。选项 A 描述的是传值方式，C、D 的说法没有依据，均不正确。', 2, 'AI_GENERATED', 'SD-01;outline=83', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0048 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0048, 'A', '不会', 0), (@q0048, 'B', '会', 1), (@q0048, 'C', '有时会，取决于返回类型', 2), (@q0048, 'D', '仅在形参为数组时才', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0048, @kp_97);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0048, @kp_98);

-- SD-01.jsonl:49  SD-01#0049  outline 83 -> 231
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 231, 'SINGLE_CHOICE', '关于正规式（正则表达式）与有限自动机的关系，下列说法正确的是（　）。', 'MARKDOWN', 'A', '正规式与有限自动机是等价的两种形式化描述工具，任何能被正规式描述的正规语言都存在一个有限自动机可以识别它，故 A 正确。正因为二者等价，B、D 说它们能力不等价或不能互转是错误的，C 也错误。', 4, 'AI_GENERATED', 'SD-01;outline=83', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0049 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0049, 'A', '能用正规式描述的语言都能被有限自动机识别', 0), (@q0049, 'B', '有限自动机的识别能力强于正规式描述的语言', 1), (@q0049, 'C', '正规式与有限自动机描述的是完全不同的语言', 2), (@q0049, 'D', '正规式无法转换为等价的有限自动机', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0049, @kp_99);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0049, @kp_100);

-- SD-01.jsonl:50  SD-01#0050  outline 83 -> 231
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 231, 'SINGLE_CHOICE', '在面向对象程序设计中，函数调用地址在程序运行时才能确定的绑定方式称为（　）。', 'MARKDOWN', 'B', '动态绑定（晚绑定）在程序运行期间根据对象的实际类型确定要调用的函数，是实现多态的重要机制，故选 B。静态绑定和早期绑定都在编译阶段确定调用地址，词法绑定与运行时确定无关，均不正确。', 2, 'AI_GENERATED', 'SD-01;outline=83', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0050 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0050, 'A', '静态绑定', 0), (@q0050, 'B', '动态绑定', 1), (@q0050, 'C', '早期绑定', 2), (@q0050, 'D', '词法绑定', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0050, @kp_101);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0050, @kp_102);

-- SD-02.jsonl:1  SD-02#0001  outline 82 -> 232
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 232, 'SINGLE_CHOICE', '在关系代数中，从关系中选出满足给定条件的元组、只对行进行筛选的运算是（　）。', 'MARKDOWN', 'B', '选择运算按给定条件逐行判断，保留满足条件的元组，只对行进行筛选，故 B 正确。投影是选出部分列，连接是把两个关系按条件组合，笛卡儿积是两关系的全组合，均不是只筛选行，故 A、C、D 不正确。', 2, 'AI_GENERATED', 'SD-02;outline=82', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0051 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0051, 'A', '投影', 0), (@q0051, 'B', '选择', 1), (@q0051, 'C', '连接', 2), (@q0051, 'D', '笛卡儿积', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0051, @kp_103);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0051, @kp_104);

-- SD-02.jsonl:2  SD-02#0002  outline 82 -> 232
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 232, 'SINGLE_CHOICE', '在关系模式 $R$ 中，若每个非主属性都完全函数依赖于码，且不存在对码的传递函数依赖，则 $R$ 至少属于（　）。', 'MARKDOWN', 'C', '第一范式要求属性不可再分；第二范式要求每个非主属性完全函数依赖于码，即消除部分函数依赖；第三范式在第二范式基础上再消除非主属性对码的传递函数依赖；BC 范式要求每个决定因素都包含码。题干条件正是第三范式的定义，故 C 正确。满足题干者未必满足 BC 范式，故 D 不选；A、B 的要求低于题干，也不选。', 3, 'AI_GENERATED', 'SD-02;outline=82', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0052 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0052, 'A', '第一范式', 0), (@q0052, 'B', '第二范式', 1), (@q0052, 'C', '第三范式', 2), (@q0052, 'D', 'BC 范式', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0052, @kp_105);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0052, @kp_106);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0052, @kp_107);

-- SD-02.jsonl:3  SD-02#0003  outline 82 -> 232
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 232, 'SINGLE_CHOICE', '在 SQL 中，要删除某数据表中满足指定条件的行，同时保留该表本身的结构，应使用的语句是（　）。', 'MARKDOWN', 'C', 'DELETE 语句配合 WHERE 子句可删除满足条件的行，且保留表结构，故 C 正确。DROP TABLE 会把表连同结构一起删除，ALTER TABLE 用于修改表结构，CREATE TABLE 用于新建表，均不能达到题干要求，故 A、B、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=82', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0053 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0053, 'A', 'DROP TABLE', 0), (@q0053, 'B', 'ALTER TABLE', 1), (@q0053, 'C', 'DELETE', 2), (@q0053, 'D', 'CREATE TABLE', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0053, @kp_108);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0053, @kp_109);

-- SD-02.jsonl:4  SD-02#0004  outline 82 -> 232
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 232, 'SINGLE_CHOICE', '将 E-R 图转换为关系模式时，对于一个 $1:n$ 的联系，通常采用的做法是（　）。', 'MARKDOWN', 'C', '$1:n$ 联系中，$n$ 端实体的多条记录对应 $1$ 端的同一条记录，因此把 $1$ 端实体的主键放入 $n$ 端关系模式作为外键，即可表达该联系且不产生冗余，故 C 正确。单独建关系模式通常用于 $m:n$ 联系，直接套用会引入冗余，A 不选；B 的方向弄反；D 会丢失联系信息，也不正确。', 4, 'AI_GENERATED', 'SD-02;outline=82', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0054 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0054, 'A', '单独建立一个新的关系模式，其键由两端实体键组合而成', 0), (@q0054, 'B', '与 $1$ 端实体的关系模式合并，并把 $n$ 端实体的主键作为外键', 1), (@q0054, 'C', '与 $n$ 端实体的关系模式合并，并把 $1$ 端实体的主键作为外键', 2), (@q0054, 'D', '不作任何处理，联系信息无需在关系模式中体现', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0054, @kp_110);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0054, @kp_111);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0054, @kp_112);

-- SD-02.jsonl:5  SD-02#0005  outline 82 -> 232
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 232, 'SINGLE_CHOICE', '在数据库并发控制中，事务 $T_1$ 读取了事务 $T_2$ 尚未提交的修改数据，此后 $T_2$ 因故回滚，导致 $T_1$ 读到的数据无效，这种现象称为（　）。', 'MARKDOWN', 'B', '脏读指一个事务读到了另一个事务未提交且随后被回滚的数据，读到的是无效数据，故 B 正确。丢失修改指两个事务修改同一数据导致一方修改被覆盖；不可重复读指同一事务两次读取同一数据结果不一致；幻读指同一查询两次返回的记录数不同，均与题干情形不符，故 A、C、D 不正确。', 4, 'AI_GENERATED', 'SD-02;outline=82', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0055 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0055, 'A', '丢失修改', 0), (@q0055, 'B', '脏读', 1), (@q0055, 'C', '不可重复读', 2), (@q0055, 'D', '幻读', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0055, @kp_113);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0055, @kp_114);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0055, @kp_115);

-- SD-02.jsonl:6  SD-02#0006  outline 22 -> 169
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 169, 'SINGLE_CHOICE', '在 OSI 参考模型中，负责为分组选择路由、实现不同网络之间互连的是（　）。', 'MARKDOWN', 'C', '网络层负责分组的路由选择与转发，实现不同网络之间的互连，故 C 正确。物理层处理比特流的传输，数据链路层负责相邻结点之间的帧传输与差错控制，传输层提供端到端的进程间通信，均不承担路由选择功能，故 A、B、D 不正确。', 2, 'AI_GENERATED', 'SD-02;outline=22', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0056 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0056, 'A', '物理层', 0), (@q0056, 'B', '数据链路层', 1), (@q0056, 'C', '网络层', 2), (@q0056, 'D', '传输层', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0056, @kp_116);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0056, @kp_117);

-- SD-02.jsonl:7  SD-02#0007  outline 22 -> 169
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 169, 'SINGLE_CHOICE', '在 TCP/IP 体系结构中，工作在传输层并提供面向连接的可靠传输服务的协议是（　）。', 'MARKDOWN', 'B', 'TCP 位于传输层，提供面向连接、可靠、按序的字节流传输服务，故 B 正确。UDP 虽在传输层，但无连接且不保证可靠；IP 位于网际层，负责寻址与转发；ICMP 用于传递差错与控制报文，均不符合题干描述，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=22', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0057 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0057, 'A', 'UDP', 0), (@q0057, 'B', 'TCP', 1), (@q0057, 'C', 'IP', 2), (@q0057, 'D', 'ICMP', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0057, @kp_118);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0057, @kp_119);

-- SD-02.jsonl:8  SD-02#0008  outline 22 -> 169
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 169, 'SINGLE_CHOICE', '某主机的 IP 地址为 $192.168.1.130$，子网掩码为 $255.255.255.192$，则该主机所在子网的网络地址是（　）。', 'MARKDOWN', 'C', '将 IP 地址与子网掩码按位求与即得网络地址。末段 $130$ 的二进制为 $10000010$，掩码末段 $192$ 的二进制为 $11000000$，二者相与得 $10000000$，即 $128$，故网络地址为 $192.168.1.128$，选 C。其余选项是掩码对齐错误时的结果，均不正确。', 3, 'AI_GENERATED', 'SD-02;outline=22', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0058 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0058, 'A', '$192.168.1.0$', 0), (@q0058, 'B', '$192.168.1.64$', 1), (@q0058, 'C', '$192.168.1.128$', 2), (@q0058, 'D', '$192.168.1.192$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0058, @kp_120);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0058, @kp_121);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0058, @kp_122);

-- SD-02.jsonl:9  SD-02#0009  outline 22 -> 169
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 169, 'SINGLE_CHOICE', '在 TCP/IP 应用层中，负责把域名解析为对应 IP 地址的协议是（　），其默认使用的端口号为 $53$。', 'MARKDOWN', 'C', 'DNS 域名系统负责把便于记忆的域名解析为对应的 IP 地址，默认使用端口 $53$，故 C 正确。HTTP 用于网页传输，FTP 用于文件传输，SMTP 用于发送电子邮件，均不具备域名解析功能，故 A、B、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=22', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0059 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0059, 'A', 'HTTP', 0), (@q0059, 'B', 'FTP', 1), (@q0059, 'C', 'DNS', 2), (@q0059, 'D', 'SMTP', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0059, @kp_123);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0059, @kp_124);

-- SD-02.jsonl:10  SD-02#0010  outline 22 -> 169
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 169, 'SINGLE_CHOICE', '把一个 C 类网络划分为若干子网，若要求每个子网最多可容纳 $30$ 台主机，则应采用的子网掩码是（　）。', 'MARKDOWN', 'B', '掩码末段 $224$ 对应 $/27$，主机号占 $5$ 位，可分配主机地址为 $2^5-2=30$ 个，恰好满足要求，故 B 正确。$192$ 对应 $/26$，可容纳 $62$ 台主机，超过需求；$240$ 对应 $/28$，只能容纳 $14$ 台；$248$ 对应 $/29$，只能容纳 $6$ 台，均不符合，故 A、C、D 不正确。', 4, 'AI_GENERATED', 'SD-02;outline=22', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0060 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0060, 'A', '$255.255.255.192$', 0), (@q0060, 'B', '$255.255.255.224$', 1), (@q0060, 'C', '$255.255.255.240$', 2), (@q0060, 'D', '$255.255.255.248$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0060, @kp_125);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0060, @kp_121);

-- SD-02.jsonl:11  SD-02#0011  outline 23 -> 170
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 170, 'SINGLE_CHOICE', '在模拟音频数字化的过程中，把时间上连续的信号在时间轴上离散化的操作称为（　）。', 'MARKDOWN', 'A', '采样是按一定时间间隔抽取信号的瞬时值，使连续信号在时间轴上离散化，故 A 正确。量化是把采样值的幅度离散化，编码是把量化后的数值用二进制表示，压缩是减少数据量，均与时间轴离散化不符，故 B、C、D 不正确。', 1, 'AI_GENERATED', 'SD-02;outline=23', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0061 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0061, 'A', '采样', 0), (@q0061, 'B', '量化', 1), (@q0061, 'C', '编码', 2), (@q0061, 'D', '压缩', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0061, @kp_126);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0061, @kp_127);

-- SD-02.jsonl:12  SD-02#0012  outline 23 -> 170
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 170, 'SINGLE_CHOICE', '一幅分辨率为 $1024\\times768$、每个像素用 $24$ 位真彩色表示的图像，其未压缩的数据量约为（　）。', 'MARKDOWN', 'B', '数据量 $=$ 像素总数 $\\times$ 每像素位数 $=1024\\times768\\times24$ 位 $=1024\\times768\\times3$ 字节 $=2359296$ 字节，约合 $2.25\\text{MB}$，故 B 正确。其余选项是把位换算为字节时遗漏了除以 $8$，或位数取值错误所致，均不正确。', 3, 'AI_GENERATED', 'SD-02;outline=23', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0062 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0062, 'A', '$0.75\\text{MB}$', 0), (@q0062, 'B', '$2.25\\text{MB}$', 1), (@q0062, 'C', '$18\\text{MB}$', 2), (@q0062, 'D', '$24\\text{MB}$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0062, @kp_128);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0062, @kp_129);

-- SD-02.jsonl:13  SD-02#0013  outline 23 -> 170
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 170, 'SINGLE_CHOICE', '下列多媒体数据压缩方法中，属于无损压缩的是（　）。', 'MARKDOWN', 'A', '哈夫曼编码依据符号出现频率分配变长码字，压缩后能够完全恢复原始数据，属于无损压缩，故 A 正确。变换编码、预测编码、子带编码在有损压缩中常与量化结合使用，会丢失部分信息，属于有损压缩范畴，故 B、C、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=23', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0063 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0063, 'A', '哈夫曼编码', 0), (@q0063, 'B', '变换编码', 1), (@q0063, 'C', '预测编码', 2), (@q0063, 'D', '子带编码', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0063, @kp_130);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0063, @kp_131);

-- SD-02.jsonl:14  SD-02#0014  outline 23 -> 170
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 170, 'SINGLE_CHOICE', '一段时长为 $10$ 秒、帧率为 $25\\text{fps}$、分辨率为 $640\\times480$、每个像素 $24$ 位的未压缩视频，其数据量约为（　）。', 'MARKDOWN', 'C', '每帧数据量为 $640\\times480\\times24$ 位 $=640\\times480\\times3$ 字节 $=921600$ 字节；每秒 $25$ 帧、共 $10$ 秒，故总量为 $921600\\times25\\times10=230400000$ 字节，约合 $220\\text{MB}$，故 C 正确。其余选项在帧率或时长的换算上存在遗漏或数量级错误，均不正确。', 4, 'AI_GENERATED', 'SD-02;outline=23', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0064 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0064, 'A', '$22\\text{MB}$', 0), (@q0064, 'B', '$55\\text{MB}$', 1), (@q0064, 'C', '$220\\text{MB}$', 2), (@q0064, 'D', '$2200\\text{MB}$', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0064, @kp_132);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0064, @kp_133);

-- SD-02.jsonl:15  SD-02#0015  outline 23 -> 170
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 170, 'SINGLE_CHOICE', '在彩色视频处理中，常把颜色空间从 RGB 转换为 YUV，将亮度（Y）与色度（U、V）分离，这样做的主要目的是（　）。', 'MARKDOWN', 'A', '人眼对亮度细节较敏感、对色度细节相对不敏感，把二者分离后可以对色度分量进行更大比例的缩减采样与压缩，从而显著降低数据量，故 A 正确。YUV 转换并不改变空间分辨率，也不能消除噪声或增加颜色位数，故 B、C、D 不正确。', 4, 'AI_GENERATED', 'SD-02;outline=23', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0065 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0065, 'A', '便于对亮度和色度采用不同的压缩策略，利用人眼对色度不敏感的特性降低数据量', 0), (@q0065, 'B', '使图像的空间分辨率成倍提高', 1), (@q0065, 'C', '彻底消除图像中的噪声', 2), (@q0065, 'D', '把每个像素的颜色位数增加到 $48$ 位', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0065, @kp_134);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0065, @kp_135);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0065, @kp_136);

-- SD-02.jsonl:16  SD-02#0016  outline 24 -> 171
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 171, 'SINGLE_CHOICE', '软件工程的三要素通常指的是（　）。', 'MARKDOWN', 'A', '软件工程通常被认为由方法、工具和过程三个要素构成：方法提供技术手段，工具支撑方法的实施，过程把方法与工具有机组织起来，故 A 正确。B 是软件开发的若干阶段，C 是项目资源，D 属于计算机系统组成，均不是软件工程三要素，故不正确。', 2, 'AI_GENERATED', 'SD-02;outline=24', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0066 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0066, 'A', '方法、工具、过程', 0), (@q0066, 'B', '需求、设计、测试', 1), (@q0066, 'C', '人员、资金、时间', 2), (@q0066, 'D', '硬件、软件、网络', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0066, @kp_137);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0066, @kp_138);

-- SD-02.jsonl:17  SD-02#0017  outline 24 -> 171
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 171, 'SINGLE_CHOICE', '软件生命周期是指（　）。', 'MARKDOWN', 'A', '软件生命周期涵盖从需求提出、开发、使用到最终退役的整个过程，包含可行性与需求分析、设计、编码、测试、运行维护等阶段，故 A 正确。B、C、D 都只描述了其中的一小部分，覆盖范围过窄，故不正确。', 2, 'AI_GENERATED', 'SD-02;outline=24', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0067 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0067, 'A', '从提出软件需求开始，到软件最终停用报废为止所经历的整个时期', 0), (@q0067, 'B', '编写源程序所花费的时间', 1), (@q0067, 'C', '仅指软件测试阶段', 2), (@q0067, 'D', '软件投入使用后的维护时间', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0067, @kp_139);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0067, @kp_138);

-- SD-02.jsonl:18  SD-02#0018  outline 24 -> 171
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 171, 'SINGLE_CHOICE', '下列现象中，不属于软件危机典型表现的是（　）。', 'MARKDOWN', 'C', '软件危机主要表现为开发进度与成本难以控制、质量难以保证、维护困难、生产率提高缓慢等现象，故 A、B、D 都是其典型表现。硬件成本属于硬件领域的问题，不是软件危机的表现，故选 C。', 3, 'AI_GENERATED', 'SD-02;outline=24', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0068 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0068, 'A', '软件开发进度和成本难以准确估计', 0), (@q0068, 'B', '软件质量难以保证，隐藏错误较多', 1), (@q0068, 'C', '硬件成本持续上涨、采购困难', 2), (@q0068, 'D', '软件维护困难，维护成本居高不下', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0068, @kp_140);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0068, @kp_138);

-- SD-02.jsonl:19  SD-02#0019  outline 24 -> 171
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 171, 'SINGLE_CHOICE', '软件工程方法学一般分为结构化方法和面向对象方法两大类，二者的主要区别在于（　）。', 'MARKDOWN', 'A', '结构化方法强调自顶向下、逐步求精的功能分解，以数据流为分析主线；面向对象方法以对象和类为基本单位，通过封装、继承、多态来组织系统，二者思维方式不同，故 A 正确。B、C、D 的说法与事实不符，均不正确。', 3, 'AI_GENERATED', 'SD-02;outline=24', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0069 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0069, 'A', '结构化方法以功能分解和数据流为中心，面向对象方法以对象、类及其关系为中心', 0), (@q0069, 'B', '二者所使用的程序设计语言完全不同，无法互相替代', 1), (@q0069, 'C', '结构化方法不允许划分模块', 2), (@q0069, 'D', '面向对象方法不需要进行需求分析', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0069, @kp_141);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0069, @kp_142);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0069, @kp_143);

-- SD-02.jsonl:20  SD-02#0020  outline 24 -> 171
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 171, 'SINGLE_CHOICE', '为缓解软件危机，软件工程提出的主要思路是（　）。', 'MARKDOWN', 'B', '软件危机的根源之一是缺乏工程化的组织与管理。软件工程主张把需求、设计、编码、测试等纳入规范化、可度量的工程过程，用方法、工具和过程来组织开发，故 B 正确。单纯增加人数可能加剧沟通成本，取消文档和只用汇编语言都违背软件工程思想，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=24', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0070 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0070, 'A', '单纯增加程序员数量以加快开发进度', 0), (@q0070, 'B', '把工程化、规范化的管理方法引入软件开发，按工程原则组织和管理开发过程', 1), (@q0070, 'C', '尽量减少甚至取消开发文档', 2), (@q0070, 'D', '始终使用汇编语言编写程序', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0070, @kp_144);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0070, @kp_141);

-- SD-02.jsonl:21  SD-02#0021  outline 25 -> 172
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 172, 'SINGLE_CHOICE', '在系统分析阶段，获取用户需求最常用的方法是（　）。', 'MARKDOWN', 'A', '需求获取需要与用户充分沟通交流，常用方法包括面谈、问卷调查、现场观察、原型演示等，故 A 正确。编译源程序、阅读目标代码、查看硬件清单都不能获得用户对系统功能的真实诉求，故 B、C、D 不正确。', 2, 'AI_GENERATED', 'SD-02;outline=25', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0071 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0071, 'A', '与用户面谈、发放调查问卷、现场观察业务流程等', 0), (@q0071, 'B', '直接编译源程序', 1), (@q0071, 'C', '单独阅读已有的目标代码', 2), (@q0071, 'D', '只查看硬件配置清单', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0071, @kp_145);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0071, @kp_146);

-- SD-02.jsonl:22  SD-02#0022  outline 25 -> 172
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 172, 'SINGLE_CHOICE', '结构化分析方法（SA）主要使用的描述工具是（　）。', 'MARKDOWN', 'A', '结构化分析方法面向数据流，用数据流图描述系统的数据加工与流动，用数据字典对图中数据元素进行定义，故 A 正确。类图和时序图属于面向对象方法工具，甘特图与网络图用于进度计划，状态转换图主要用于实时系统的行为描述，均不是 SA 的主要工具，故 B、C、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=25', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0072 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0072, 'A', '数据流图和数据字典', 0), (@q0072, 'B', '类图和时序图', 1), (@q0072, 'C', '甘特图和网络图', 2), (@q0072, 'D', '状态转换图和 Petri 网', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0072, @kp_147);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0072, @kp_148);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0072, @kp_149);

-- SD-02.jsonl:23  SD-02#0023  outline 25 -> 172
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 172, 'SINGLE_CHOICE', '在数据流图中，表示数据加工（处理）所使用的图形符号通常是（　）。', 'MARKDOWN', 'A', '数据流图中，加工用圆或圆角矩形表示，数据流用带箭头的线表示，数据存储用开口矩形表示，外部实体用直角矩形表示，故 A 正确。B 表示数据流，C 表示数据存储，D 表示外部实体，均与题干不符，故不正确。', 3, 'AI_GENERATED', 'SD-02;outline=25', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0073 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0073, 'A', '圆或圆角矩形', 0), (@q0073, 'B', '带箭头的线', 1), (@q0073, 'C', '开口矩形', 2), (@q0073, 'D', '直角矩形', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0073, @kp_148);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0073, @kp_150);

-- SD-02.jsonl:24  SD-02#0024  outline 25 -> 172
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 172, 'SINGLE_CHOICE', '在结构化分析中，数据字典的主要作用是（　）。', 'MARKDOWN', 'A', '数据字典与数据流图互为补充，用来精确定义和说明图中出现的每个数据流、数据项、数据存储和加工，使分析结果无二义，故 A 正确。算法实现细节属于详细设计与编码，模块层次结构由结构图描述，测试用例属于测试文档，故 B、C、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=25', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0074 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0074, 'A', '对数据流图中的数据流、数据项、数据存储和加工等进行定义和说明', 0), (@q0074, 'B', '描述程序内部的算法实现细节', 1), (@q0074, 'C', '表示软件的模块层次结构', 2), (@q0074, 'D', '记录测试用例及其执行结果', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0074, @kp_149);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0074, @kp_151);

-- SD-02.jsonl:25  SD-02#0025  outline 25 -> 172
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 172, 'SINGLE_CHOICE', '在绘制分层数据流图时，关于父图与子图的平衡，下列说法正确的是（　）。', 'MARKDOWN', 'B', '父图中某个加工的展开即为其子图，子图的输入输出数据流必须与父图中该加工的输入输出数据流一致，这称为父子图的平衡，故 B 正确。数据流条数并不要求与父图相同，但边界上的输入输出必须对应，A 的说法过于绝对；C、D 会破坏分层结构的一致性，故不正确。', 4, 'AI_GENERATED', 'SD-02;outline=25', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0075 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0075, 'A', '子图中数据流的条数必须与父图完全相同', 0), (@q0075, 'B', '子图的所有输入输出数据流应与父图中对应加工的输入输出数据流保持一致', 1), (@q0075, 'C', '父子图之间不需要任何对应关系，可各自独立绘制', 2), (@q0075, 'D', '子图中可以随意增加父图没有的外部实体', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0075, @kp_148);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0075, @kp_152);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0075, @kp_153);

-- SD-02.jsonl:26  SD-02#0026  outline 26 -> 173
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 173, 'SINGLE_CHOICE', '衡量模块独立性高低所依据的两个定性标准是（　）。', 'MARKDOWN', 'A', '模块独立性由耦合和内聚共同衡量：耦合越低、内聚越高，模块独立性越强，故 A 正确。其余选项与模块独立性评价无关，均不正确。', 2, 'AI_GENERATED', 'SD-02;outline=26', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0076 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0076, 'A', '耦合与内聚', 0), (@q0076, 'B', '时间与空间', 1), (@q0076, 'C', '长度与宽度', 2), (@q0076, 'D', '输入与输出', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0076, @kp_154);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0076, @kp_155);

-- SD-02.jsonl:27  SD-02#0027  outline 26 -> 173
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 173, 'SINGLE_CHOICE', '下列模块间的耦合类型中，耦合程度最低、模块独立性最好的是（　）。', 'MARKDOWN', 'C', '数据耦合中模块之间只通过参数传递简单的数据，互相影响最小，耦合程度最低，故 C 正确。内容耦合是模块直接访问另一模块的内部，耦合最强；公共耦合通过全局数据区通信；控制耦合传递控制信息影响对方逻辑，耦合程度均高于数据耦合，故 A、B、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=26', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0077 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0077, 'A', '内容耦合', 0), (@q0077, 'B', '公共耦合', 1), (@q0077, 'C', '数据耦合', 2), (@q0077, 'D', '控制耦合', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0077, @kp_156);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0077, @kp_154);

-- SD-02.jsonl:28  SD-02#0028  outline 26 -> 173
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 173, 'SINGLE_CHOICE', '下列模块内聚类型中，内聚程度最高、模块独立性最好的是（　）。', 'MARKDOWN', 'D', '功能内聚指模块内各成分共同完成一个明确而单一的功能，联系最紧密，内聚程度最高，故 D 正确。偶然内聚的组成成分之间没有实质关系，内聚最低；逻辑内聚和时间内聚的紧密程度也明显低于功能内聚，故 A、B、C 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=26', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0078 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0078, 'A', '偶然内聚', 0), (@q0078, 'B', '逻辑内聚', 1), (@q0078, 'C', '时间内聚', 2), (@q0078, 'D', '功能内聚', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0078, @kp_157);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0078, @kp_154);

-- SD-02.jsonl:29  SD-02#0029  outline 26 -> 173
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 173, 'SINGLE_CHOICE', '结构化设计方法在划分模块时，通常要求遵循的原则是（　）。', 'MARKDOWN', 'B', '模块划分应使模块之间的联系尽量少、模块内部的联系尽量紧密，即低耦合、高内聚，这样模块独立性好、易于维护，故 B 正确。其余组合都会降低模块独立性，均不正确。', 3, 'AI_GENERATED', 'SD-02;outline=26', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0079 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0079, 'A', '高耦合、低内聚', 0), (@q0079, 'B', '低耦合、高内聚', 1), (@q0079, 'C', '高耦合、高内聚', 2), (@q0079, 'D', '低耦合、低内聚', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0079, @kp_158);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0079, @kp_159);

-- SD-02.jsonl:30  SD-02#0030  outline 26 -> 173
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 173, 'SINGLE_CHOICE', '软件体系结构主要描述的是（　）。', 'MARKDOWN', 'B', '软件体系结构关注系统的高层组织，包括构成系统的构件、构件之间的连接关系以及作用于其上的约束和风格，故 B 正确。变量定义与算法步骤属于实现细节，数据表字段属于数据设计，均不属于体系结构层次，故 A、C、D 不正确。', 4, 'AI_GENERATED', 'SD-02;outline=26', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0080 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0080, 'A', '程序中各变量的定义与类型', 0), (@q0080, 'B', '系统的构件、构件之间的连接以及约束等高层结构', 1), (@q0080, 'C', '各函数的内部算法步骤', 2), (@q0080, 'D', '数据库中数据表的具体字段', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0080, @kp_160);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0080, @kp_161);

-- SD-02.jsonl:31  SD-02#0031  outline 27 -> 174
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 174, 'SINGLE_CHOICE', '系统实施阶段的主要任务通常包括（　）。', 'MARKDOWN', 'A', '系统实施是在设计完成之后把方案变为可运行系统，主要包括程序设计（编码）、系统测试和系统转换（上线）等工作，故 A 正确。可行性研究、需求分析、总体设计都属于实施之前的阶段，故 B、C、D 不正确。', 2, 'AI_GENERATED', 'SD-02;outline=27', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0081 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0081, 'A', '程序设计、系统测试与系统转换等', 0), (@q0081, 'B', '只是进行可行性研究', 1), (@q0081, 'C', '只开展需求分析', 2), (@q0081, 'D', '只完成总体设计', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0081, @kp_162);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0081, @kp_163);

-- SD-02.jsonl:32  SD-02#0032  outline 27 -> 174
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 174, 'SINGLE_CHOICE', '关于程序中的注释，下列说法正确的是（　）。', 'MARKDOWN', 'A', '良好的注释用于说明设计思路、算法要点和接口约定，帮助读者理解代码意图，而不是复述语句本身，故 A 正确。机械地逐句注释会带来冗余且难以维护，注释也不应与代码逐字重复，完全不写注释则降低可读性，故 B、C、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=27', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0082 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0082, 'A', '注释应说明程序的设计思想与意图，而不是简单重复语句本身', 0), (@q0082, 'B', '注释越多越好，应逐句解释每条语句', 1), (@q0082, 'C', '注释内容必须与代码逐字重复', 2), (@q0082, 'D', '程序不需要任何注释', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0082, @kp_164);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0082, @kp_165);

-- SD-02.jsonl:33  SD-02#0033  outline 27 -> 174
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 174, 'SINGLE_CHOICE', '关于软件测试与调试，下列说法正确的是（　）。', 'MARKDOWN', 'A', '软件测试的目的是通过执行程序尽可能多地发现错误；调试则是在发现错误后分析原因、定位并修改错误，二者目的不同，故 A 正确。B 混淆了概念；调试一般在发现错误之后进行，C 的顺序颠倒；测试贯穿多个阶段而不是只在编码后进行，D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=27', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0083 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0083, 'A', '测试的目的是尽可能发现错误，调试的目的是定位错误并加以改正', 0), (@q0083, 'B', '测试与调试是同一件事的不同叫法', 1), (@q0083, 'C', '调试通常应在测试之前完成', 2), (@q0083, 'D', '测试只在编码结束后的一个阶段进行', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0083, @kp_166);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0083, @kp_167);

-- SD-02.jsonl:34  SD-02#0034  outline 27 -> 174
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 174, 'SINGLE_CHOICE', '与自底向上集成相比，自顶向下集成的主要优点是（　）。', 'MARKDOWN', 'A', '自顶向下集成从主控模块开始逐步向下组装，能较早地验证主要控制逻辑和总体结构，及早暴露总体性问题，故 A 正确。该方式需要编写桩模块，故 B 错；它需要桩模块而不需要驱动模块，C 的说法仍不正确；集成测试不能取代单元测试，D 也不正确。', 3, 'AI_GENERATED', 'SD-02;outline=27', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0084 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0084, 'A', '能较早验证系统的主要控制逻辑和总体结构，便于尽早发现总体性问题', 0), (@q0084, 'B', '完全不需要编写桩模块', 1), (@q0084, 'C', '完全不需要编写驱动模块和桩模块', 2), (@q0084, 'D', '可以完全取代单元测试', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0084, @kp_168);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0084, @kp_169);

-- SD-02.jsonl:35  SD-02#0035  outline 27 -> 174
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 174, 'SINGLE_CHOICE', '在系统转换方式中，直接转换的主要特点是（　）。', 'MARKDOWN', 'B', '直接转换在某一时刻把旧系统直接交给新系统，无需并行运行，费用最低，但一旦新系统出问题便无退路，风险最大，故 B 正确。A 描述的是并行转换，C 描述的是逐步转换，D 的说法过于绝对，均不正确。', 4, 'AI_GENERATED', 'SD-02;outline=27', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0085 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0085, 'A', '新旧系统并行运行较长时间，风险最小但费用最高', 0), (@q0085, 'B', '旧系统立即停用、新系统立即启用，费用低但风险大', 1), (@q0085, 'C', '按子系统分批逐步转换，兼顾风险与费用', 2), (@q0085, 'D', '只在夜间进行转换，因而不存在任何风险', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0085, @kp_170);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0085, @kp_171);

-- SD-02.jsonl:36  SD-02#0036  outline 28 -> 175
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 175, 'SINGLE_CHOICE', '为了适应外部运行环境的变化（例如操作系统升级、数据格式改变）而对软件进行的修改，属于（　）。', 'MARKDOWN', 'B', '适应性维护是为使软件适应外部环境（硬件、操作系统、数据格式等）的变化而进行的修改，故 B 正确。改正性维护针对运行中暴露的错误，完善性维护为增加功能或提高性能，预防性维护着眼于提高未来的可维护性，均不符合题意，故 A、C、D 不正确。', 2, 'AI_GENERATED', 'SD-02;outline=28', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0086 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0086, 'A', '改正性维护', 0), (@q0086, 'B', '适应性维护', 1), (@q0086, 'C', '完善性维护', 2), (@q0086, 'D', '预防性维护', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0086, @kp_172);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0086, @kp_173);

-- SD-02.jsonl:37  SD-02#0037  outline 28 -> 175
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 175, 'SINGLE_CHOICE', '软件在运行过程中出现了开发阶段未被发现的错误，为改正这些错误而进行的维护属于（　）。', 'MARKDOWN', 'A', '改正性维护专门用于识别和纠正软件中遗留或潜在的错误，故 A 正确。适应性维护针对环境变化，完善性维护针对功能与性能的扩充，预防性维护旨在提高将来的可维护性，均与改正运行中暴露的错误不符，故 B、C、D 不正确。', 2, 'AI_GENERATED', 'SD-02;outline=28', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0087 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0087, 'A', '改正性维护', 0), (@q0087, 'B', '适应性维护', 1), (@q0087, 'C', '完善性维护', 2), (@q0087, 'D', '预防性维护', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0087, @kp_174);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0087, @kp_173);

-- SD-02.jsonl:38  SD-02#0038  outline 28 -> 175
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 175, 'SINGLE_CHOICE', '用户在使用中提出增加新的功能、提高运行效率等要求，为满足这类要求而进行的维护属于（　）。', 'MARKDOWN', 'C', '完善性维护是在软件已有功能之外扩充功能、改善性能和质量，以满足用户新的需求，故 C 正确。改正性维护、适应性维护分别针对纠错和环境变化，预防性维护偏重提高可维护性，均不符合题干，故 A、B、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=28', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0088 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0088, 'A', '改正性维护', 0), (@q0088, 'B', '适应性维护', 1), (@q0088, 'C', '完善性维护', 2), (@q0088, 'D', '预防性维护', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0088, @kp_175);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0088, @kp_173);

-- SD-02.jsonl:39  SD-02#0039  outline 28 -> 175
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 175, 'SINGLE_CHOICE', '软件的可维护性通常与下列哪一组因素关系最密切（　）。', 'MARKDOWN', 'A', '可维护性反映软件被理解、测试和修改的难易程度，主要取决于可理解性、可测试性、可修改性以及文档质量、可靠性等因素，故 A 正确。运行速度、硬件品牌、编译器版本都不是决定可维护性的核心因素，故 B、C、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=28', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0089 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0089, 'A', '可理解性、可测试性、可修改性', 0), (@q0089, 'B', '仅与程序的运行速度有关', 1), (@q0089, 'C', '仅与所使用硬件的品牌有关', 2), (@q0089, 'D', '仅与编译器的版本有关', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0089, @kp_176);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0089, @kp_177);

-- SD-02.jsonl:40  SD-02#0040  outline 28 -> 175
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 175, 'SINGLE_CHOICE', '关于软件维护成本，下列说法正确的是（　）。', 'MARKDOWN', 'B', '随着软件使用寿命延长，功能扩充、环境适应和纠错等维护工作不断累积，维护成本常占软件总成本的很大比例，并随时间上升，故 B 正确。维护成本并不可忽略，A 错；维护内容远不止纠错，且必然产生费用，C 错；文档质量直接影响可理解性和可维护性，进而影响维护成本，D 错。', 4, 'AI_GENERATED', 'SD-02;outline=28', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0090 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0090, 'A', '维护成本在软件总成本中通常可以忽略不计', 0), (@q0090, 'B', '维护工作往往在软件总成本中占很大比重，并随软件运行年限的延长而增加', 1), (@q0090, 'C', '维护只涉及改正错误，不会产生额外费用', 2), (@q0090, 'D', '维护成本与软件文档质量无关', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0090, @kp_178);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0090, @kp_179);

-- SD-02.jsonl:41  SD-02#0041  outline 29 -> 176
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 176, 'SINGLE_CHOICE', '软件质量保证（SQA）的主要目的是（　）。', 'MARKDOWN', 'A', 'SQA 是一项有计划的系统性活动，通过对过程、方法和规程的检查与评审，为软件满足质量要求提供保证，强调事前预防而非事后补救，故 A 正确。统计代码行数只是度量手段之一，仅做一次测试属于末端检验，硬件采购与 SQA 无关，故 B、C、D 不正确。', 1, 'AI_GENERATED', 'SD-02;outline=29', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0091 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0091, 'A', '通过有计划的质量活动为软件满足规定质量要求提供保证，重在预防缺陷', 0), (@q0091, 'B', '只统计源程序的代码行数', 1), (@q0091, 'C', '仅在交付前进行一次测试', 2), (@q0091, 'D', '只负责硬件设备的采购', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0091, @kp_180);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0091, @kp_181);

-- SD-02.jsonl:42  SD-02#0042  outline 29 -> 176
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 176, 'SINGLE_CHOICE', '下列活动中，不属于软件质量保证（SQA）范畴的是（　）。', 'MARKDOWN', 'D', 'SQA 通常包括技术评审、软件配置管理、质量审核、测试验证等一系列保证软件质量的活动，故 A、B、C 都属于其范畴。硬件电路焊接属于硬件生产工艺控制，不在软件质量保证范围内，故选 D。', 3, 'AI_GENERATED', 'SD-02;outline=29', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0092 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0092, 'A', '技术评审', 0), (@q0092, 'B', '软件配置管理', 1), (@q0092, 'C', '阶段质量审核', 2), (@q0092, 'D', '硬件电路的焊接工艺控制', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0092, @kp_180);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0092, @kp_182);

-- SD-02.jsonl:43  SD-02#0043  outline 29 -> 176
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 176, 'SINGLE_CHOICE', '关于正式技术评审，下列说法正确的是（　）。', 'MARKDOWN', 'A', '缺陷发现得越早，修复代价越低，因此正式技术评审应在早期尽早、分阶段进行，故 A 正确。集中到项目结束才评审会使缺陷积累，B 错；评审对象包括需求、设计、代码等多种工作产品，C 错；评审应形成记录并跟踪，D 错。', 3, 'AI_GENERATED', 'SD-02;outline=29', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0093 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0093, 'A', '应尽早安排评审，以便在开发早期发现缺陷、降低后期返工成本', 0), (@q0093, 'B', '只需在项目结束时集中进行一次', 1), (@q0093, 'C', '评审对象只能是可执行程序', 2), (@q0093, 'D', '评审不需要形成任何记录', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0093, @kp_183);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0093, @kp_180);

-- SD-02.jsonl:44  SD-02#0044  outline 29 -> 176
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 176, 'SINGLE_CHOICE', '软件度量一般分为产品度量、过程度量和项目度量，其中过程度量主要关注（　）。', 'MARKDOWN', 'B', '过程度量面向开发过程本身，用于评价过程的能力与效率，如各阶段缺陷发现率、返工率、评审效率等，故 B 正确。产品度量关注产品属性，项目度量关注进度与资源的整体情况，工资和硬件寿命不属于过程度量，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=29', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0094 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0094, 'A', '软件产品本身的属性，如模块复杂度', 0), (@q0094, 'B', '开发过程的属性，如缺陷发现效率、阶段返工率', 1), (@q0094, 'C', '项目组成员的个人工资水平', 2), (@q0094, 'D', '所使用硬件的使用寿命', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0094, @kp_184);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0094, @kp_185);

-- SD-02.jsonl:45  SD-02#0045  outline 29 -> 176
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 176, 'SINGLE_CHOICE', '在软件质量成本中，为预防缺陷发生而开展的活动（如培训、制定过程规范）所投入的成本属于（　）。', 'MARKDOWN', 'A', '质量成本分为预防成本、评价成本和失效成本。预防成本是为防止缺陷发生而投入的成本，如培训与过程规范建设，故 A 正确。评价成本用于检验与测试，内部与外部失效成本用于处理已发生的缺陷，均与题干所述活动不符，故 B、C、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=29', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0095 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0095, 'A', '预防成本', 0), (@q0095, 'B', '评价成本', 1), (@q0095, 'C', '内部失效成本', 2), (@q0095, 'D', '外部失效成本', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0095, @kp_186);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0095, @kp_181);

-- SD-02.jsonl:46  SD-02#0046  outline 30 -> 177
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 177, 'SINGLE_CHOICE', '软件过程改进的主要目的是（　）。', 'MARKDOWN', 'A', '过程改进着眼于改进组织的开发过程，使其更加规范、可控和高效，从而稳定地交付高质量软件，故 A 正确。追求代码行数、取消文档、取消工具都与过程改进的目标相悖，故 B、C、D 不正确。', 2, 'AI_GENERATED', 'SD-02;outline=30', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0096 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0096, 'A', '持续提高软件开发过程的成熟度与效率，从而稳定地生产出高质量的软件', 0), (@q0096, 'B', '尽可能增加程序中的代码行数', 1), (@q0096, 'C', '尽量减少甚至取消开发文档', 2), (@q0096, 'D', '完全取消各类开发工具的使用', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0096, @kp_187);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0096, @kp_188);

-- SD-02.jsonl:47  SD-02#0047  outline 30 -> 177
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 177, 'SINGLE_CHOICE', '在软件过程改进中，所谓软件过程指的是（　）。', 'MARKDOWN', 'A', '软件过程是把用户需求转化为软件产品所涉及的一系列活动、方法、实践与转换的集合，是过程改进的对象，故 A 正确。可执行程序是产品结果，预算表和硬件配置分别是项目资源与运行环境，均不是软件过程，故 B、C、D 不正确。', 2, 'AI_GENERATED', 'SD-02;outline=30', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0097 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0097, 'A', '软件开发中所使用的一系列活动、方法、实践和转换的集合', 0), (@q0097, 'B', '最终交付给用户的可执行程序', 1), (@q0097, 'C', '项目的经费预算表', 2), (@q0097, 'D', '运行软件的硬件配置', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0097, @kp_189);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0097, @kp_188);

-- SD-02.jsonl:48  SD-02#0048  outline 30 -> 177
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 177, 'SINGLE_CHOICE', '软件过程改进的 IDEAL 模型所包含的五个阶段依次是（　）。', 'MARKDOWN', 'A', 'IDEAL 模型把过程改进描述为初始化、诊断、建立、行动、学习五个阶段，并以学习阶段为起点进入下一轮改进，故 A 正确。B 是软件生命周期阶段，C 是项目管理过程组，D 表述的是 PDCA 循环，均不是 IDEAL 模型的阶段划分，故 B、C、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=30', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0098 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0098, 'A', '初始化、诊断、建立、行动、学习', 0), (@q0098, 'B', '需求、设计、编码、测试、维护', 1), (@q0098, 'C', '启动、计划、执行、监控、收尾', 2), (@q0098, 'D', '计划、执行、检查、处理、总结', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0098, @kp_190);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0098, @kp_191);

-- SD-02.jsonl:49  SD-02#0049  outline 30 -> 177
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 177, 'SINGLE_CHOICE', '在软件过程改进中，通常要求组织建立相应的改进组织，并遵循的改进思想是（　）。', 'MARKDOWN', 'B', '过程改进是一个持续、循环往复的过程，通常借助计划—执行—检查—处理（PDCA）的循环，对改进措施不断评估和调整，故 B 正确。改进不可能一劳永逸，也不应由少数人代替组织共同参与，更不应只局限于某一环节，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-02;outline=30', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0099 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0099, 'A', '一次性完成，此后无需再改变', 0), (@q0099, 'B', '按计划—执行—检查—处理的方式持续循环改进', 1), (@q0099, 'C', '只由少数高层管理者一次性决定即可', 2), (@q0099, 'D', '只对测试环节进行改进，其余环节保持不变', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0099, @kp_192);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0099, @kp_193);

-- SD-02.jsonl:50  SD-02#0050  outline 30 -> 177
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 177, 'SINGLE_CHOICE', '组织在开展软件过程改进时，下列做法正确的是（　）。', 'MARKDOWN', 'A', '过程改进必须立足组织自身的现状与目标，先评估当前过程的强项与弱项，再有针对性地制定并实施改进措施，并在实施后评估效果，故 A 正确。照搬他人做法而不做适配往往水土不服，改进是持续活动而非一次性项目，也需要全员参与而非仅是基层个人之事，故 B、C、D 不正确。', 4, 'AI_GENERATED', 'SD-02;outline=30', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0100 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0100, 'A', '结合自身实际情况，从现状评估出发，制定改进计划并逐步实施与评估', 0), (@q0100, 'B', '直接照搬其他组织的全部过程，无需进行任何评估', 1), (@q0100, 'C', '把过程改进当作一次性项目，完成后不再调整', 2), (@q0100, 'D', '认为过程改进只是基层程序员个人的事情', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0100, @kp_187);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0100, @kp_191);

-- SD-03.jsonl:1  SD-03#0001  outline 53 -> 200
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 200, 'SINGLE_CHOICE', '瀑布模型将软件生存周期划分为若干阶段，各阶段自上而下、顺序衔接。该模型最适用于（　）的项目。', 'MARKDOWN', 'C', '瀑布模型强调阶段间的顺序性与依赖性，前一阶段完成并评审通过后才进入下一阶段，因此最适用于需求明确且基本稳定的项目，故选 C。需求模糊、需反复确认的项目更适合原型模型；规模庞大、风险很高的项目更适合螺旋模型；要求尽早交付部分可运行功能的项目更适合增量模型，故 A、B、D 均不正确。', 2, 'AI_GENERATED', 'SD-03;outline=53', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0101 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0101, 'A', '需求模糊、需要反复确认', 0), (@q0101, 'B', '规模庞大且风险很高', 1), (@q0101, 'C', '需求明确且基本稳定', 2), (@q0101, 'D', '要求尽早交付部分可运行功能', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0101, @kp_194);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0101, @kp_195);

-- SD-03.jsonl:2  SD-03#0002  outline 53 -> 200
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 200, 'SINGLE_CHOICE', '在软件开发过程中，先快速构建一个可运行的版本交给用户试用，据此澄清并细化需求，这种开发模型称为（　）。', 'MARKDOWN', 'B', '原型模型的基本思想是先构造一个简化但可运行的原型，让用户在试用中表达真实需求，从而逐步明确需求，故选 B。瀑布模型要求阶段顺序推进，螺旋模型突出风险分析，增量模型强调分批交付功能，均与题述先做原型再澄清需求不符。', 1, 'AI_GENERATED', 'SD-03;outline=53', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0102 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0102, 'A', '瀑布模型', 0), (@q0102, 'B', '原型模型', 1), (@q0102, 'C', '螺旋模型', 2), (@q0102, 'D', '增量模型', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0102, @kp_196);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0102, @kp_145);

-- SD-03.jsonl:3  SD-03#0003  outline 53 -> 200
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 200, 'SINGLE_CHOICE', '螺旋模型把开发过程组织成多轮螺旋，每轮包含制定计划、风险分析、实施工程和客户评估等活动。与瀑布模型相比，螺旋模型最突出的优点是（　）。', 'MARKDOWN', 'D', '螺旋模型在每一轮迭代中都显式进行风险分析，能在早期发现并控制风险，因而特别适合规模大、风险高的项目，故选 D。螺旋模型强调用户全程参与，采用迭代而非一次完成，质量控制也贯穿各阶段，故 A、B、C 均不正确。', 3, 'AI_GENERATED', 'SD-03;outline=53', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0103 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0103, 'A', '完全不需要用户参与', 0), (@q0103, 'B', '每个阶段只进行一次即可完成开发', 1), (@q0103, 'C', '只在编码阶段开展质量控制', 2), (@q0103, 'D', '把风险分析贯穿始终，特别适合大型复杂项目', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0103, @kp_197);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0103, @kp_198);

-- SD-03.jsonl:4  SD-03#0004  outline 53 -> 200
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 200, 'SINGLE_CHOICE', '采用增量模型开发软件时，把软件分解为若干增量构件，分批设计、编码并交付。其最突出的优点是（　）。', 'MARKDOWN', 'A', '增量模型按增量构件分批交付，先交付的构件可尽早投入试用，用户反馈能及时反映到后续增量中，故选 A。该模型允许需求逐步细化，各增量仍需分别测试，且每个增量只覆盖部分功能，故 B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-03;outline=53', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0104 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0104, 'A', '用户可以较早获得并使用部分功能，及时提出反馈', 0), (@q0104, 'B', '需求必须在项目开始时全部确定且不得变更', 1), (@q0104, 'C', '无须对各增量分别进行测试', 2), (@q0104, 'D', '每个增量都必须包含系统的全部功能', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0104, @kp_199);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0104, @kp_200);

-- SD-03.jsonl:5  SD-03#0005  outline 53 -> 200
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 200, 'SINGLE_CHOICE', '统一过程（RUP）以「用例驱动、以架构为中心、迭代与增量」为核心特征，其生命周期的四个阶段依次是（　）。', 'MARKDOWN', 'C', 'RUP 把生命周期划分为初始、细化、构建、移交四个阶段，每个阶段可包含若干次迭代，故选 C。需求、设计、编码、测试是具体工作流；计划、执行、检查、处置是质量管理中的循环；概念、逻辑、物理、实现是数据库设计的阶段划分，故 A、B、D 均不正确。', 3, 'AI_GENERATED', 'SD-03;outline=53', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0105 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0105, 'A', '需求、设计、编码、测试', 0), (@q0105, 'B', '计划、执行、检查、处置', 1), (@q0105, 'C', '初始、细化、构建、移交', 2), (@q0105, 'D', '概念、逻辑、物理、实现', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0105, @kp_201);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0105, @kp_202);

-- SD-03.jsonl:6  SD-03#0006  outline 54 -> 201
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 201, 'SINGLE_CHOICE', '在软件需求分类中，「系统在并发用户数为 $500$ 时，页面响应时间不超过 $2$ 秒」这一需求属于（　）。', 'MARKDOWN', 'B', '该需求约束的是系统响应时间与并发能力，属于对性能等质量属性的约束，而非描述系统「做什么」的功能，故为非功能需求，选 B。功能需求描述系统应完成的功能，数据需求描述数据对象及其关系，接口需求描述系统与外部交互的约定，均与该描述不符。', 2, 'AI_GENERATED', 'SD-03;outline=54', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0106 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0106, 'A', '功能需求', 0), (@q0106, 'B', '非功能需求', 1), (@q0106, 'C', '数据需求', 2), (@q0106, 'D', '接口需求', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0106, @kp_203);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0106, @kp_204);

-- SD-03.jsonl:7  SD-03#0007  outline 54 -> 201
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 201, 'SINGLE_CHOICE', '下列活动中，不属于需求分析阶段核心任务的是（　）。', 'MARKDOWN', 'D', '需求分析的核心任务是获取并整理需求、导出系统逻辑模型、编写需求规格说明书；程序编码与运行效率优化属于设计、实现阶段的工作，故选 D。A、B、C 都是需求分析阶段的主要活动。', 2, 'AI_GENERATED', 'SD-03;outline=54', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0107 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0107, 'A', '通过与用户交流确定系统应当做什么', 0), (@q0107, 'B', '对获取的需求进行去伪存真、归纳整理', 1), (@q0107, 'C', '导出系统的逻辑模型并用文档加以描述', 2), (@q0107, 'D', '完成程序编码并优化程序的运行效率', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0107, @kp_205);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0107, @kp_145);

-- SD-03.jsonl:8  SD-03#0008  outline 54 -> 201
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 201, 'SINGLE_CHOICE', '软件需求规格说明书（SRS）在项目中最主要的作用是（　）。', 'MARKDOWN', 'A', 'SRS 把经确认的需求以文档形式固定下来，既是开发方与用户对需求达成一致的标志，也是后续设计、实现、测试和验收的依据，故选 A。算法与数据结构细节属于设计文档，测试结果属于测试报告，预算与工资与需求文档无关，故 B、C、D 均不正确。', 2, 'AI_GENERATED', 'SD-03;outline=54', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0108 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0108, 'A', '作为用户与开发方就需求达成一致，并作为后续设计与验收依据的文档', 0), (@q0108, 'B', '描述程序的内部算法与数据结构实现细节', 1), (@q0108, 'C', '记录测试用例的执行过程与结果', 2), (@q0108, 'D', '规定项目各阶段的人员工资与预算分配', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0108, @kp_206);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0108, @kp_207);

-- SD-03.jsonl:9  SD-03#0009  outline 54 -> 201
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 201, 'SINGLE_CHOICE', '在结构化分析方法中，用于从数据传递和加工的角度描述系统逻辑功能、刻画数据流动情况的是（　）。', 'MARKDOWN', 'C', '数据流图以加工、数据流、数据存储和外部实体为基本元素，从数据流动与加工的角度描述系统的逻辑功能，故选 C。实体联系图描述数据对象及其联系，状态转换图描述系统状态随事件的迁移，甘特图是进度管理工具，均与题述角度不符。', 3, 'AI_GENERATED', 'SD-03;outline=54', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0109 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0109, 'A', '实体联系图', 0), (@q0109, 'B', '状态转换图', 1), (@q0109, 'C', '数据流图', 2), (@q0109, 'D', '甘特图', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0109, @kp_148);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0109, @kp_151);

-- SD-03.jsonl:10  SD-03#0010  outline 54 -> 201
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 201, 'SINGLE_CHOICE', '在结构化分析中使用数据字典，其主要作用是（　）。', 'MARKDOWN', 'B', '数据字典与数据流图配套使用，对数据流、数据项、数据存储、加工等元素给出精确定义，弥补数据流图无法表达细节的不足，故选 B。界面布局、版本管理、硬件拓扑分别属于界面设计、配置管理、部署设计的范畴，均与数据字典的作用不符。', 3, 'AI_GENERATED', 'SD-03;outline=54', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0110 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0110, 'A', '描述系统运行时的界面布局与交互样式', 0), (@q0110, 'B', '对数据流图中出现的数据流、数据项、数据存储和加工逐一定义', 1), (@q0110, 'C', '记录源代码的版本演进与分支合并情况', 2), (@q0110, 'D', '给出系统部署所需的硬件拓扑结构', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0110, @kp_149);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0110, @kp_151);

-- SD-03.jsonl:11  SD-03#0011  outline 55 -> 202
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 202, 'SINGLE_CHOICE', '软件设计通常划分为概要设计和详细设计两个阶段。其中详细设计阶段的主要任务是（　）。', 'MARKDOWN', 'D', '概要设计确定软件总体结构、模块划分和模块间接口；详细设计则针对每个模块确定内部算法、数据结构与处理流程，为编码提供直接依据，故选 D。A 属于概要设计，B 属于测试设计，C 属于项目管理，均不正确。', 2, 'AI_GENERATED', 'SD-03;outline=55', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0111 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0111, 'A', '确定软件的总体结构、模块划分与接口关系', 0), (@q0111, 'B', '确定系统的验收测试方案', 1), (@q0111, 'C', '确定项目的人员分工与开发进度', 2), (@q0111, 'D', '确定每个模块的内部算法与数据结构，给出可直接编码的描述', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0111, @kp_208);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0111, @kp_209);

-- SD-03.jsonl:12  SD-03#0012  outline 55 -> 202
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 202, 'SINGLE_CHOICE', '在结构化设计中，要求模块之间尽可能低耦合。下列耦合类型中，耦合程度最低的是（　）。', 'MARKDOWN', 'C', '耦合由低到高大致为：非直接耦合、数据耦合、标记耦合、控制耦合、外部耦合、公共耦合、内容耦合。数据耦合仅通过参数传递简单数据，耦合程度较低，故选 C。内容耦合、公共耦合、控制耦合的程度都高于数据耦合，故 A、B、D 均不正确。', 4, 'AI_GENERATED', 'SD-03;outline=55', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0112 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0112, 'A', '内容耦合', 0), (@q0112, 'B', '公共耦合', 1), (@q0112, 'C', '数据耦合', 2), (@q0112, 'D', '控制耦合', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0112, @kp_210);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0112, @kp_211);

-- SD-03.jsonl:13  SD-03#0013  outline 55 -> 202
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 202, 'SINGLE_CHOICE', '某模块内的所有语句共同完成一个单一、明确的功能，缺少其中任何一条都无法完成该功能。该模块的内聚类型属于（　）。', 'MARKDOWN', 'A', '功能内聚指模块内各元素共同完成单一而明确的功能，内聚程度最高，故选 A。逻辑内聚指模块内若干在逻辑上相似的功能由参数选择执行；时间内聚指若干任务必须在同一时间段内执行；偶然内聚指模块内元素之间没有实质联系，均与题述不符。', 4, 'AI_GENERATED', 'SD-03;outline=55', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0113 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0113, 'A', '功能内聚', 0), (@q0113, 'B', '逻辑内聚', 1), (@q0113, 'C', '时间内聚', 2), (@q0113, 'D', '偶然内聚', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0113, @kp_212);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0113, @kp_213);

-- SD-03.jsonl:14  SD-03#0014  outline 55 -> 202
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 202, 'SINGLE_CHOICE', '在面向对象设计中，用父类类型的引用指向子类对象，并在运行时根据对象的实际类型调用相应的方法，这体现了面向对象的（　）特性。', 'MARKDOWN', 'C', '多态指同一操作作用于不同对象可产生不同的行为，其典型实现是父类引用指向子类对象并在运行时动态绑定到子类的实现，故选 C。封装强调把数据与操作隐藏在对象内部，继承强调子类复用父类的属性与方法，抽象强调提取事物的共性，均与题述的运行时动态调用不符。', 3, 'AI_GENERATED', 'SD-03;outline=55', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0114 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0114, 'A', '封装', 0), (@q0114, 'B', '继承', 1), (@q0114, 'C', '多态', 2), (@q0114, 'D', '抽象', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0114, @kp_102);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0114, @kp_214);

-- SD-03.jsonl:15  SD-03#0015  outline 55 -> 202
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 202, 'SINGLE_CHOICE', '在软件结构设计中，衡量模块独立性优劣的最主要依据是（　）。', 'MARKDOWN', 'B', '模块独立性由内聚和耦合共同决定：内聚越高、耦合越低，模块独立性越好，故选 B。扇入、扇出只反映模块的调用关系，对独立性有一定影响但并非最主要依据；模块规模、效率、复用次数、注释密度都不是衡量独立性的标准，故 A、C、D 均不正确。', 4, 'AI_GENERATED', 'SD-03;outline=55', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0115 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0115, 'A', '模块的规模大小与运行效率', 0), (@q0115, 'B', '模块的内聚程度与耦合程度', 1), (@q0115, 'C', '模块的扇入数与扇出数', 2), (@q0115, 'D', '模块的复用次数与注释密度', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0115, @kp_154);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0115, @kp_215);

-- SD-03.jsonl:16  SD-03#0016  outline 56 -> 203
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 203, 'SINGLE_CHOICE', '软件测试的根本目的是（　）。', 'MARKDOWN', 'B', '测试的目的是以尽可能少的代价发现尽可能多的缺陷，从而降低软件交付后的风险，故选 B。测试只能证明错误的存在，不能证明程序没有错误，也无法据此说明功能全部实现，更与责任免除无关，故 A、C、D 均不正确。', 2, 'AI_GENERATED', 'SD-03;outline=56', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0116 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0116, 'A', '证明程序中不存在任何错误', 0), (@q0116, 'B', '以尽可能少的代价发现尽可能多的缺陷', 1), (@q0116, 'C', '说明软件的全部功能均已正确实现', 2), (@q0116, 'D', '为开发人员免除质量责任', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0116, @kp_216);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0116, @kp_217);

-- SD-03.jsonl:17  SD-03#0017  outline 56 -> 203
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 203, 'SINGLE_CHOICE', '在黑盒测试方法中，把输入数据的取值范围划分为若干等价类，从每个等价类中选取少数代表性数据作为测试用例，这种方法称为（　）。', 'MARKDOWN', 'D', '等价类划分法把输入域划分为若干等价类，认为同一等价类中任取一个数据对揭示缺陷的作用相同，从而用少量用例覆盖全部等价类，故选 D。因果图法分析输入与输出的因果组合；路径覆盖和语句覆盖属于白盒方法，需了解程序内部结构，故 A、B、C 均不正确。', 2, 'AI_GENERATED', 'SD-03;outline=56', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0117 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0117, 'A', '因果图法', 0), (@q0117, 'B', '路径覆盖法', 1), (@q0117, 'C', '语句覆盖法', 2), (@q0117, 'D', '等价类划分法', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0117, @kp_218);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0117, @kp_219);

-- SD-03.jsonl:18  SD-03#0018  outline 56 -> 203
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 203, 'SINGLE_CHOICE', '某程序的输入变量为 $[1,100]$ 范围内的整数，采用边界值分析法设计测试用例时，通常应重点选取的边界值是（　）。', 'MARKDOWN', 'B', '边界值分析重点考察边界及其邻近取值。对区间 $[1,100]$ 应取略小于下界的 $0$、下界 $1$、上界 $100$ 以及略大于上界的 $101$，故选 B。只取两个端点会漏掉越界情况，取中间值或全部整数都不符合边界值分析的重点，故 A、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-03;outline=56', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0118 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0118, 'A', '$1$ 和 $100$ 两个端点值', 0), (@q0118, 'B', '$0$、$1$、$100$、$101$', 1), (@q0118, 'C', '$50$ 这个中间值', 2), (@q0118, 'D', '$1$ 到 $100$ 之间的全部整数', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0118, @kp_220);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0118, @kp_221);

-- SD-03.jsonl:19  SD-03#0019  outline 56 -> 203
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 203, 'SINGLE_CHOICE', '某程序的控制流图中包含若干判定结点，若要求每个判定的所有可能结果组合而成的每条路径至少被执行一次，则对应的覆盖准则是（　）。', 'MARKDOWN', 'A', '路径覆盖要求执行程序中所有可能的路径，覆盖能力最强，故选 A。语句覆盖只要求每条可执行语句被执行一次；判定覆盖只要求每个判定取到真、假两种结果；条件覆盖只要求每个判定中的每个条件取到真、假两种结果，它们都远弱于路径覆盖，故 B、C、D 均不正确。', 4, 'AI_GENERATED', 'SD-03;outline=56', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0119 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0119, 'A', '路径覆盖', 0), (@q0119, 'B', '语句覆盖', 1), (@q0119, 'C', '判定覆盖', 2), (@q0119, 'D', '条件覆盖', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0119, @kp_222);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0119, @kp_223);

-- SD-03.jsonl:20  SD-03#0020  outline 56 -> 203
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 203, 'SINGLE_CHOICE', '按测试的实施阶段划分，软件测试通常依次经历单元测试、集成测试和（　）。', 'MARKDOWN', 'C', '软件测试按实施阶段一般依次为单元测试、集成测试、系统测试和验收测试，故在集成测试之后进行的是系统测试，选 C。语句测试和逻辑测试是白盒覆盖准则的名称，模块测试即单元测试的别称，都不是测试阶段，故 A、B、D 均不正确。', 3, 'AI_GENERATED', 'SD-03;outline=56', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0120 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0120, 'A', '语句测试', 0), (@q0120, 'B', '逻辑测试', 1), (@q0120, 'C', '系统测试', 2), (@q0120, 'D', '模块测试', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0120, @kp_224);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0120, @kp_225);

-- SD-03.jsonl:21  SD-03#0021  outline 57 -> 204
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 204, 'SINGLE_CHOICE', '在软件质量特性中，衡量软件在规定条件下和规定时间内维持其性能水平、完成规定功能的能力的是（　）。', 'MARKDOWN', 'D', '可靠性指软件在规定条件下和规定时间内完成规定功能、维持其性能水平的能力，故选 D。功能性关注满足明确和隐含功能要求的程度，易用性关注用户理解、学习和使用软件的难易程度，可移植性关注软件迁移到其它环境的适应能力，均与题述定义不符。', 3, 'AI_GENERATED', 'SD-03;outline=57', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0121 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0121, 'A', '功能性', 0), (@q0121, 'B', '易用性', 1), (@q0121, 'C', '可移植性', 2), (@q0121, 'D', '可靠性', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0121, @kp_226);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0121, @kp_227);

-- SD-03.jsonl:22  SD-03#0022  outline 57 -> 204
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 204, 'SINGLE_CHOICE', '在软件质量特性中，衡量用户理解、学习和使用软件所需努力程度的是（　）。', 'MARKDOWN', 'A', '易用性衡量用户理解、学习、操作软件并对其产生满意感的难易程度，故选 A。效率关注在给定资源条件下软件性能水平的高低，可维护性关注修改软件的难易程度，功能性关注满足功能需求的能力，均与理解、学习、使用的难易程度不符。', 3, 'AI_GENERATED', 'SD-03;outline=57', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0122 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0122, 'A', '易用性', 0), (@q0122, 'B', '效率', 1), (@q0122, 'C', '可维护性', 2), (@q0122, 'D', '功能性', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0122, @kp_226);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0122, @kp_228);

-- SD-03.jsonl:23  SD-03#0023  outline 57 -> 204
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 204, 'SINGLE_CHOICE', '在软件质量特性中，用于衡量软件被修改（如纠错、改进、适应环境变化）的难易程度的是（　）。', 'MARKDOWN', 'B', '可维护性刻画对软件进行纠错性、适应性、完善性或预防性修改的难易程度，常由可分析性、可改变性、稳定性、可测试性等子特性衡量，故选 B。效率、可靠性、可移植性分别关注性能、稳定运行和环境迁移能力，均不是衡量修改难易程度的特性。', 3, 'AI_GENERATED', 'SD-03;outline=57', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0123 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0123, 'A', '效率', 0), (@q0123, 'B', '可维护性', 1), (@q0123, 'C', '可靠性', 2), (@q0123, 'D', '可移植性', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0123, @kp_226);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0123, @kp_176);

-- SD-03.jsonl:24  SD-03#0024  outline 57 -> 204
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 204, 'SINGLE_CHOICE', '在软件质量控制中，为在开发早期发现并纠正缺陷、降低后期返工成本，通常采用的主要手段是（　）。', 'MARKDOWN', 'A', '质量控制强调尽早发现缺陷，在需求、设计等阶段设置正式的评审与检查，可在缺陷产生的阶段将其暴露，显著降低修复成本，故选 A。把测试集中在编码之后会使缺陷发现过晚；省略设计文档会削弱可维护性；测试仅由开发人员单独完成则独立性与覆盖面不足，故 B、C、D 均不是有效手段。', 4, 'AI_GENERATED', 'SD-03;outline=57', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0124 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0124, 'A', '在需求、设计等各阶段结束后组织正式的评审与检查', 0), (@q0124, 'B', '只在全部编码完成后集中进行一次测试', 1), (@q0124, 'C', '省略设计文档以加快开发进度', 2), (@q0124, 'D', '仅由开发人员编写并执行自己设计的测试', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0124, @kp_229);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0124, @kp_230);

-- SD-03.jsonl:25  SD-03#0025  outline 57 -> 204
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 204, 'SINGLE_CHOICE', '在软件质量特性中，软件从一种运行环境迁移到另一种运行环境所需工作量的多少，对应的质量特性是（　）。', 'MARKDOWN', 'D', '可移植性反映软件适应不同硬件平台或软件环境的能力，常由适应性、可安装性、共存性、可替换性等子特性构成，故选 D。功能性关注功能满足程度，可维护性关注对软件本身的修改，效率关注资源占用与性能，均不直接刻画环境迁移所需的工作量。', 4, 'AI_GENERATED', 'SD-03;outline=57', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0125 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0125, 'A', '功能性', 0), (@q0125, 'B', '可维护性', 1), (@q0125, 'C', '效率', 2), (@q0125, 'D', '可移植性', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0125, @kp_226);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0125, @kp_231);

-- SD-03.jsonl:26  SD-03#0026  outline 58 -> 205
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 205, 'SINGLE_CHOICE', '在能力成熟度模型（CMM）中，已对过程与产品质量进行定量度量与控制、过程表现可预测的成熟度等级是（　）。', 'MARKDOWN', 'D', 'CMM 依次分为初始级、可重复级、已定义级、已管理级和优化级。已管理级对过程和产品质量进行量化度量与控制，使过程可预测，故选 D。初始级过程无序，可重复级主要依靠项目管理，已定义级建立了组织级标准过程，均未达到定量控制的程度。', 3, 'AI_GENERATED', 'SD-03;outline=58', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0126 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0126, 'A', '初始级', 0), (@q0126, 'B', '可重复级', 1), (@q0126, 'C', '已定义级', 2), (@q0126, 'D', '已管理级', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0126, @kp_232);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0126, @kp_233);

-- SD-03.jsonl:27  SD-03#0027  outline 58 -> 205
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 205, 'SINGLE_CHOICE', '在 CMMI 的连续式表示中，用于衡量单个过程域能力提升程度、取值从 $0$ 级到 $5$ 级的是（　）。', 'MARKDOWN', 'B', 'CMMI 连续式表示用能力等级衡量单个过程域的能力，共 $0$ 至 $5$ 级；阶段式表示则用成熟度等级衡量组织整体过程成熟度，共 $1$ 至 $5$ 级，故选 B。成熟度等级用于阶段式表示，组织等级和项目等级并非 CMMI 规定的等级名称，故 A、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-03;outline=58', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0127 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0127, 'A', '成熟度等级', 0), (@q0127, 'B', '能力等级', 1), (@q0127, 'C', '组织等级', 2), (@q0127, 'D', '项目等级', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0127, @kp_234);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0127, @kp_235);

-- SD-03.jsonl:28  SD-03#0028  outline 58 -> 205
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 205, 'SINGLE_CHOICE', '在 CMM 中，过程往往无序、成功与否主要取决于个人经验、缺乏稳定管理的成熟度等级是（　）。', 'MARKDOWN', 'B', '初始级是 CMM 的最低等级，此时过程无序，成功与否主要取决于个人能力与经验，缺乏稳定的管理，故选 B。可重复级已建立基本的项目管理过程，已定义级建立了组织级标准过程，优化级是最高等级、强调持续过程改进，故 A、C、D 均不是最低等级。', 3, 'AI_GENERATED', 'SD-03;outline=58', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0128 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0128, 'A', '优化级', 0), (@q0128, 'B', '初始级', 1), (@q0128, 'C', '已定义级', 2), (@q0128, 'D', '可重复级', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0128, @kp_232);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0128, @kp_236);

-- SD-03.jsonl:29  SD-03#0029  outline 58 -> 205
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 205, 'SINGLE_CHOICE', '在 CMMI 阶段式表示的成熟度等级中，组织已建立覆盖全组织的标准过程并据项目特点加以裁剪，这一等级是（　）。', 'MARKDOWN', 'C', '已定义级（$3$ 级）要求组织建立标准过程集，并结合项目特点裁剪形成项目已定义过程，使管理建立在组织级基础之上，故选 C。可重复级（$2$ 级）侧重于项目级管理；已管理级（$4$ 级）进一步做定量管理；优化级（$5$ 级）强调持续的过程改进，均不符合「覆盖全组织的标准过程」这一描述。', 4, 'AI_GENERATED', 'SD-03;outline=58', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0129 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0129, 'A', '可重复级', 0), (@q0129, 'B', '已管理级', 1), (@q0129, 'C', '已定义级', 2), (@q0129, 'D', '优化级', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0129, @kp_234);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0129, @kp_237);

-- SD-03.jsonl:30  SD-03#0030  outline 58 -> 205
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 205, 'SINGLE_CHOICE', 'SPICE（软件过程改进与能力评定）模型对软件过程能力进行评定，其评定结果是（　）。', 'MARKDOWN', 'B', 'SPICE 通过对过程实施、管理与制度化等过程属性进行评定，给出 $0$ 至 $5$ 级的能力等级，反映被测过程的能力剖面，故选 B。它并非简单的合格与否判定，也不针对个人技术水平，更不以代码行数衡量过程能力，故 A、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-03;outline=58', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0130 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0130, 'A', '只给出「合格」或「不合格」的简单结论', 0), (@q0130, 'B', '在若干过程属性上给出能力等级，从而形成过程能力的剖面', 1), (@q0130, 'C', '只评价软件开发人员的个人技术水平', 2), (@q0130, 'D', '用软件代码行数衡量过程能力的高低', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0130, @kp_238);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0130, @kp_239);

-- SD-03.jsonl:31  SD-03#0031  outline 59 -> 206
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 206, 'SINGLE_CHOICE', '在软件开发工具中，能够根据源文件之间的依赖关系自动完成编译、连接等重复性工作，从而生成目标程序的一类工具是（　）。', 'MARKDOWN', 'A', '构建工具依据源文件之间的依赖关系自动调用编译器和连接器完成编译与构建，减少人工重复操作，故选 A。版本控制工具管理代码变更历史，文档生成工具从代码或模型生成文档，数据库建模工具辅助数据库设计，均不承担编译构建职责，故 B、C、D 均不正确。', 2, 'AI_GENERATED', 'SD-03;outline=59', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0131 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0131, 'A', '构建工具', 0), (@q0131, 'B', '版本控制工具', 1), (@q0131, 'C', '文档生成工具', 2), (@q0131, 'D', '数据库建模工具', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0131, @kp_240);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0131, @kp_241);

-- SD-03.jsonl:32  SD-03#0032  outline 59 -> 206
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 206, 'SINGLE_CHOICE', '在团队协作开发中，用于记录源代码的每次变更、支持多人并行修改、并可在必要时回退到历史版本的工具属于（　）。', 'MARKDOWN', 'C', '版本控制工具保存文件的历史版本与变更记录，支持分支、合并与回退，是多人协同开发的基础设施，故选 C。代码检查工具用于静态分析潜在缺陷，单元测试框架用于编写和运行测试，需求管理工具用于跟踪需求，均不具备版本历史管理能力，故 A、B、D 均不正确。', 2, 'AI_GENERATED', 'SD-03;outline=59', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0132 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0132, 'A', '代码检查工具', 0), (@q0132, 'B', '单元测试框架', 1), (@q0132, 'C', '版本控制工具', 2), (@q0132, 'D', '需求管理工具', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0132, @kp_242);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0132, @kp_243);

-- SD-03.jsonl:33  SD-03#0033  outline 59 -> 206
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 206, 'SINGLE_CHOICE', '按在软件生存周期中的作用划分，CASE 工具可分为上游工具与下游工具。下列属于上游 CASE 工具的是（　）。', 'MARKDOWN', 'A', '上游 CASE 工具支持软件生存周期的前期活动，如需求分析、系统设计与建模，故选 A。编译链接、测试执行和运行维护都属于编码之后的下游活动，对应下游 CASE 工具，故 B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-03;outline=59', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0133 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0133, 'A', '需求分析与设计建模工具', 0), (@q0133, 'B', '编译链接工具', 1), (@q0133, 'C', '测试执行工具', 2), (@q0133, 'D', '软件维护工具', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0133, @kp_244);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0133, @kp_245);

-- SD-03.jsonl:34  SD-03#0034  outline 59 -> 206
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 206, 'SINGLE_CHOICE', '在程序调试过程中，通过在源程序的指定位置设置断点使程序暂停，从而观察变量取值与调用层次，这种手段属于（　）。', 'MARKDOWN', 'D', '断点、单步执行、变量监视等都是对可执行程序在源码层面进行的动态分析手段，属于源代码级调试，故选 D。需求评审属于需求阶段的质量活动，版本合并属于配置管理，硬件焊接属于硬件作业，均与动态调试无关。', 3, 'AI_GENERATED', 'SD-03;outline=59', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0134 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0134, 'A', '需求评审', 0), (@q0134, 'B', '版本合并', 1), (@q0134, 'C', '硬件焊接', 2), (@q0134, 'D', '源代码级调试', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0134, @kp_246);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0134, @kp_247);

-- SD-03.jsonl:35  SD-03#0035  outline 59 -> 206
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 206, 'SINGLE_CHOICE', '在软件开发工具中，能够根据图形或文本描述的模型自动生成程序框架或数据库脚本，从而减少人工编码量的是（　）。', 'MARKDOWN', 'C', '代码生成工具依据模型或形式化描述自动产生源代码框架、接口或数据库脚本，可提高一致性与开发效率，故选 C。汇编器把汇编语言翻译为机器码，反汇编工具把机器码转换回汇编语句，磁盘碎片整理属于系统维护工具，均不承担由模型生成代码的功能，故 A、B、D 均不正确。', 4, 'AI_GENERATED', 'SD-03;outline=59', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0135 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0135, 'A', '汇编器', 0), (@q0135, 'B', '反汇编工具', 1), (@q0135, 'C', '代码生成工具', 2), (@q0135, 'D', '磁盘碎片整理工具', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0135, @kp_248);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0135, @kp_244);

-- SD-03.jsonl:36  SD-03#0036  outline 60 -> 207
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 207, 'SINGLE_CHOICE', '在 UML 中，用于描述对象之间按时间先后顺序进行交互、强调消息次序的图是（　）。', 'MARKDOWN', 'A', '顺序图以对象及其生命线描述消息在时间轴上的交互顺序，强调消息的先后次序，故选 A。类图描述类的属性、方法及类之间的静态关系，用例图从外部视角描述系统的功能，组件图描述软件组件的组织与依赖，均不强调消息的时间顺序。', 2, 'AI_GENERATED', 'SD-03;outline=60', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0136 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0136, 'A', '顺序图', 0), (@q0136, 'B', '类图', 1), (@q0136, 'C', '用例图', 2), (@q0136, 'D', '组件图', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0136, @kp_249);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0136, @kp_250);

-- SD-03.jsonl:37  SD-03#0037  outline 60 -> 207
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 207, 'SINGLE_CHOICE', '在面向对象分析中，从用户视角描述系统对外提供的功能、刻画外部参与者与系统之间交互的 UML 图是（　）。', 'MARKDOWN', 'D', '用例图由参与者、用例及其关系构成，从系统外部视角描述对外的功能，故选 D。状态图描述一个对象的状态随事件的迁移，活动图描述工作流或控制流，部署图描述软硬件节点的物理部署，均不用于从用户视角刻画系统功能。', 3, 'AI_GENERATED', 'SD-03;outline=60', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0137 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0137, 'A', '状态图', 0), (@q0137, 'B', '活动图', 1), (@q0137, 'C', '部署图', 2), (@q0137, 'D', '用例图', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0137, @kp_249);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0137, @kp_251);

-- SD-03.jsonl:38  SD-03#0038  outline 60 -> 207
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 207, 'SINGLE_CHOICE', '在 UML 类图中，「整体与部分具有相同的生命周期、部分不能脱离整体独立存在」所对应的类之间关系是（　）。', 'MARKDOWN', 'B', '组合是强聚合，其整体与部分同生共死，部分不能独立于整体存在，故选 B。聚合是弱的整体—部分关系，部分可以独立存在；泛化表示一般与特殊的继承关系；依赖表示一个元素的变化会影响另一个元素，均不符合题述「同生共死」的约束。', 3, 'AI_GENERATED', 'SD-03;outline=60', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0138 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0138, 'A', '聚合', 0), (@q0138, 'B', '组合', 1), (@q0138, 'C', '泛化', 2), (@q0138, 'D', '依赖', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0138, @kp_252);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0138, @kp_253);

-- SD-03.jsonl:39  SD-03#0039  outline 60 -> 207
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 207, 'SINGLE_CHOICE', '面向对象分析（OOA）通常按一定步骤进行，其中首先要完成的是（　）。', 'MARKDOWN', 'A', 'OOA 一般先识别问题域中的对象与类，再定义其属性、服务及类之间的关系，进而建立对象模型、动态模型和功能模型，故选 A。数据库表结构设计、单元测试和性能优化分别属于设计、测试和实现阶段的工作，不是 OOA 的起始步骤，故 B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-03;outline=60', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0139 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0139, 'A', '识别问题域中的对象与类', 0), (@q0139, 'B', '设计数据库表结构', 1), (@q0139, 'C', '编写单元测试用例', 2), (@q0139, 'D', '对代码进行性能优化', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0139, @kp_254);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0139, @kp_255);

-- SD-03.jsonl:40  SD-03#0040  outline 60 -> 207
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 207, 'SINGLE_CHOICE', '在面向对象设计中，把相互协作的一组类组织为可在扩展点上定制行为的半成品设计，并在此基础上复用，这种复用的主要形式是（　）。', 'MARKDOWN', 'C', '框架是一组相互协作的类构成的、可在扩展点定制行为的半成品设计，属于大粒度的面向对象复用，故选 C。代码片段复制会造成维护困难，数据复用和硬件复用不属于面向对象设计的复用形式，均不符合题述在扩展点定制行为的复用方式。', 4, 'AI_GENERATED', 'SD-03;outline=60', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0140 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0140, 'A', '代码片段复制', 0), (@q0140, 'B', '数据复用', 1), (@q0140, 'C', '框架复用', 2), (@q0140, 'D', '硬件复用', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0140, @kp_256);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0140, @kp_257);

-- SD-03.jsonl:41  SD-03#0041  outline 61 -> 208
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 208, 'SINGLE_CHOICE', '在 B/S（浏览器/服务器）结构的应用系统中，负责界面展示与用户交互的表现逻辑主要运行在（　）。', 'MARKDOWN', 'A', 'B/S 结构中，浏览器负责界面展示与用户交互，即表现逻辑主要位于客户端浏览器，故选 A。数据库服务器负责数据的存储与管理，操作系统内核负责底层资源管理，备份服务器用于数据备份，均不是表现逻辑的主要载体，故 B、C、D 均不正确。', 2, 'AI_GENERATED', 'SD-03;outline=61', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0141 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0141, 'A', '客户端浏览器', 0), (@q0141, 'B', '数据库服务器', 1), (@q0141, 'C', '操作系统内核', 2), (@q0141, 'D', '备份服务器', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0141, @kp_258);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0141, @kp_259);

-- SD-03.jsonl:42  SD-03#0042  outline 61 -> 208
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 208, 'SINGLE_CHOICE', '与 B/S 结构相比，C/S（客户机/服务器）结构的主要缺点是（　）。', 'MARKDOWN', 'D', 'C/S 结构需要在每台客户机上安装专用客户端程序，业务变更或版本升级时要逐台部署，维护成本较高，故选 D。C/S 同样可以利用服务器的处理能力、实现数据共享并支持图形界面，故 A、B、C 的说法都不成立。', 3, 'AI_GENERATED', 'SD-03;outline=61', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0142 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0142, 'A', '无法利用服务器端的处理能力', 0), (@q0142, 'B', '不能实现数据共享', 1), (@q0142, 'C', '不能提供图形化的用户界面', 2), (@q0142, 'D', '需要在每台客户机上安装并维护专用客户端，升级维护成本较高', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0142, @kp_260);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0142, @kp_261);

-- SD-03.jsonl:43  SD-03#0043  outline 61 -> 208
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 208, 'SINGLE_CHOICE', '在分布式软件系统中，位于操作系统与应用软件之间，为应用屏蔽网络通信、事务处理和并发控制等底层细节并提供通用服务的软件称为（　）。', 'MARKDOWN', 'B', '中间件位于操作系统与应用软件之间，向应用屏蔽网络、并发、事务等底层细节，提供通信、消息、事务等通用服务，故选 B。编译器负责将源程序翻译为目标代码，设备驱动程序使操作系统能控制硬件，解释器逐句翻译执行源程序，均不提供上述跨网络的通用服务。', 3, 'AI_GENERATED', 'SD-03;outline=61', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0143 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0143, 'A', '编译器', 0), (@q0143, 'B', '中间件', 1), (@q0143, 'C', '设备驱动程序', 2), (@q0143, 'D', '解释器', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0143, @kp_262);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0143, @kp_263);

-- SD-03.jsonl:44  SD-03#0044  outline 61 -> 208
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 208, 'SINGLE_CHOICE', '采用客户机/服务器方式开发网络应用时，套接字（Socket）为应用程序提供的主要抽象是（　）。', 'MARKDOWN', 'A', '套接字是应用进程进行网络通信的端点抽象，应用程序通过它与远端进程交换数据，从而屏蔽底层协议的细节，故选 A。磁盘物理位置、进程页表、外键约束分别属于存储管理、内存管理和数据库领域，与套接字提供的通信端点抽象无关，故 B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-03;outline=61', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0144 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0144, 'A', '网络通信的端点，应用可通过它发送和接收数据', 0), (@q0144, 'B', '磁盘文件的物理存储位置', 1), (@q0144, 'C', '进程在内存中的页表', 2), (@q0144, 'D', '数据库表之间的外键约束', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0144, @kp_264);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0144, @kp_265);

-- SD-03.jsonl:45  SD-03#0045  outline 61 -> 208
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 208, 'SINGLE_CHOICE', '在网络环境中，构件可经由远程接口被分布在不同机器上的程序调用，且调用方无需了解对方所用的操作系统与编程语言，实现这一目标的关键是（　）。', 'MARKDOWN', 'C', '要使分布在不同机器、采用不同操作系统和语言编写的构件能够互操作，需要中间件提供跨平台的远程调用机制（如基于接口定义与服务注册的远程过程调用），故选 C。本地函数调用和静态库嵌入都局限于同一进程或同一平台，单机多线程调度只在单机内并行，均无法跨越网络与异构平台，故 A、B、D 均不正确。', 4, 'AI_GENERATED', 'SD-03;outline=61', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0145 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0145, 'A', '单一操作系统上的本地函数调用', 0), (@q0145, 'B', '把静态链接库直接嵌入调用方程序', 1), (@q0145, 'C', '中间件提供的跨平台远程调用机制', 2), (@q0145, 'D', '在单台机器上进行多线程调度', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0145, @kp_262);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0145, @kp_266);

-- SD-03.jsonl:46  SD-03#0046  outline 34 -> 181
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 181, 'SINGLE_CHOICE', '在阅读系统需求说明时，为确认对需求的理解准确无误，最重要的做法是（　）。', 'MARKDOWN', 'B', '需求说明可能存在歧义或遗漏，阅读时应与用户及相关干系人逐项确认含义、明确边界并记录待澄清问题，以形成对需求的一致理解，故选 B。未经确认即编码会导致返工，忽略歧义会积累风险，把需求改写为代码属于实现阶段的工作，均不是正确做法。', 2, 'AI_GENERATED', 'SD-03;outline=34', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0146 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0146, 'A', '按自己的理解直接开始编码', 0), (@q0146, 'B', '与用户及相关干系人逐项确认需求含义并记录待澄清问题', 1), (@q0146, 'C', '忽略含义有歧义的描述以提高效率', 2), (@q0146, 'D', '把需求说明原样改写成程序代码', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0146, @kp_267);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0146, @kp_268);

-- SD-03.jsonl:47  SD-03#0047  outline 34 -> 181
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 181, 'SINGLE_CHOICE', '在理解系统需求说明时，若发现两份需求描述相互矛盾，恰当的处理方式是（　）。', 'MARKDOWN', 'B', '需求冲突应由用户与相关干系人共同裁决，分析人员应先记录冲突并在需求评审中提出，促成统一结论，故选 B。自行取舍或凭经验实现会使系统偏离真实需求，测试人员不承担需求裁决职责，故 A、C、D 均不恰当。', 3, 'AI_GENERATED', 'SD-03;outline=34', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0147 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0147, 'A', '自行选择其中较易实现的一条继续开发', 0), (@q0147, 'B', '先记录冲突，再在需求评审中与用户确认，形成一致的结论', 1), (@q0147, 'C', '忽略两份描述，凭经验实现', 2), (@q0147, 'D', '把冲突直接交由测试人员裁决', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0147, @kp_269);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0147, @kp_270);

-- SD-03.jsonl:48  SD-03#0048  outline 34 -> 181
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 181, 'SINGLE_CHOICE', '在对系统需求说明进行可验证性检查时，下列需求描述中最便于据此编写测试用例进行客观验证的是（　）。', 'MARKDOWN', 'C', '可验证的需求应明确、可观测。C 给出了明确的触发条件与可观测的预期结果，便于设计测试用例判定通过与否，故选 C。A、B、D 使用了「美观」「快」「满意」等主观、模糊的表述，缺乏判定标准，难以客观验证。', 3, 'AI_GENERATED', 'SD-03;outline=34', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0148 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0148, 'A', '系统界面应美观大方', 0), (@q0148, 'B', '系统应当运行得足够快', 1), (@q0148, 'C', '输入非法字符时，系统应提示「输入格式不正确」并拒绝提交', 2), (@q0148, 'D', '系统应尽量让用户感到满意', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0148, @kp_271);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0148, @kp_272);

-- SD-03.jsonl:49  SD-03#0049  outline 34 -> 181
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 181, 'SINGLE_CHOICE', '在理解系统需求说明的过程中，绘制数据流图与建立数据字典的主要目的是（　）。', 'MARKDOWN', 'D', '数据流图与数据字典把零散的自然语言需求整理为结构化、可检查的分析模型，便于发现遗漏与不一致，是理解需求的重要手段，故选 D。它们不能直接生成目标程序，也不能取代与用户的沟通，更不是工作量度量工具，故 A、B、C 均不正确。', 3, 'AI_GENERATED', 'SD-03;outline=34', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0149 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0149, 'A', '直接产生可运行的目标程序', 0), (@q0149, 'B', '替代与用户的沟通', 1), (@q0149, 'C', '用于衡量开发人员的工作量', 2), (@q0149, 'D', '把用户口述的需求转化为结构化、可检查的分析模型', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0149, @kp_148);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0149, @kp_273);

-- SD-03.jsonl:50  SD-03#0050  outline 35 -> 182
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 182, 'SINGLE_CHOICE', '在正式启动系统开发之前，为降低项目风险、明确开发范围与资源安排，通常需要先完成的关键工作是（　）。', 'MARKDOWN', 'A', '开发准备阶段应基于已确认的需求制定可行的开发计划，明确工作任务、进度安排、人员分工与资源投入，并识别风险，为后续开发提供依据，故选 A。尚未制定计划即编码会导致返工，未经开发便采购部署上线不符合工程流程，完全冻结需求也不现实（需求可在受控前提下变更），故 B、C、D 均不恰当。', 3, 'AI_GENERATED', 'SD-03;outline=35', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0150 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0150, 'A', '基于确认的需求制定可行的开发计划，明确任务、进度、人员与资源配置', 0), (@q0150, 'B', '立即着手编写全部功能代码', 1), (@q0150, 'C', '先采购最终运行的硬件并直接部署上线', 2), (@q0150, 'D', '一次性冻结所有需求，此后不再允许任何调整', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0150, @kp_274);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0150, @kp_275);

-- SD-04.jsonl:1  SD-04#0001  outline 35 -> 182
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 182, 'SINGLE_CHOICE', '在系统开发前的可行性研究中，通常需要从技术可行性、经济可行性和（　）等方面进行分析。', 'MARKDOWN', 'A', '可行性研究一般从技术、经济、操作（运行）三个方面展开：技术可行性判断现有技术条件能否实现系统，经济可行性分析成本与效益，操作可行性考虑系统在组织中能否被顺利接受和运行，故 A 正确。编译可行性、语法可行性、编码可行性都不是可行性研究的内容，B、C、D 均不正确。', 2, 'AI_GENERATED', 'SD-04;outline=35', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0151 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0151, 'A', '操作（运行）可行性', 0), (@q0151, 'B', '编译可行性', 1), (@q0151, 'C', '语法可行性', 2), (@q0151, 'D', '编码可行性', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0151, @kp_276);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0151, @kp_277);

-- SD-04.jsonl:2  SD-04#0002  outline 35 -> 182
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 182, 'SINGLE_CHOICE', '在制定软件开发计划时，能够清晰表达各项任务之间的依赖关系并据此找出关键路径的工具是（　）。', 'MARKDOWN', 'B', '网络计划图用结点和箭线表示任务及其先后依赖，可以计算各任务的最早与最迟开始时间，从而找出决定总工期的关键路径，故 B 正确。甘特图便于直观显示任务的起止时间，但不易表达任务之间的依赖关系；数据流图描述数据的加工与流动，程序流程图描述程序的执行逻辑，均与进度计划无关，A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-04;outline=35', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0152 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0152, 'A', '甘特图', 0), (@q0152, 'B', '网络计划图', 1), (@q0152, 'C', '数据流图', 2), (@q0152, 'D', '程序流程图', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0152, @kp_274);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0152, @kp_278);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0152, @kp_279);

-- SD-04.jsonl:3  SD-04#0003  outline 35 -> 182
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 182, 'SINGLE_CHOICE', '关于系统开发方式的选择，下列说法正确的是（　）。', 'MARKDOWN', 'A', '自行开发能更好地贴合本单位特殊需求、便于后续维护，但前提是具备相应技术力量，且通常周期较长、投入较大，故 A 正确。委托开发必须提出明确需求，否则难以验收；购买现成软件同样要考虑与现有系统的接口和数据集成；开发方式应根据需求、技术力量、成本与进度综合选择，不能一概而论，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=35', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0153 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0153, 'A', '自行开发便于满足本单位的特殊需求并有利于后期维护，但需要具备相应的技术力量且开发周期可能较长', 0), (@q0153, 'B', '委托开发时可以先不提出明确的需求', 1), (@q0153, 'C', '购买现成软件时无需考虑它与现有系统的集成问题', 2), (@q0153, 'D', '无论具体情况如何都应选择自行开发', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0153, @kp_280);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0153, @kp_281);

-- SD-04.jsonl:4  SD-04#0004  outline 36 -> 183
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 183, 'SINGLE_CHOICE', '在系统功能设计中，把系统的总功能逐层分解为若干相对独立、职责明确的子功能，这种做法称为（　）。', 'MARKDOWN', 'A', '功能分解是把复杂的总功能按层次逐步细化为若干较小、较简单的子功能，直至每个子功能便于实现，故 A 正确。数据抽象强调用数据及其操作刻画问题，结构优化与代码优化属于实现阶段的性能处理，都不是对功能的层次分解，B、C、D 不正确。', 2, 'AI_GENERATED', 'SD-04;outline=36', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0154 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0154, 'A', '功能分解', 0), (@q0154, 'B', '数据抽象', 1), (@q0154, 'C', '结构优化', 2), (@q0154, 'D', '代码优化', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0154, @kp_282);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0154, @kp_283);

-- SD-04.jsonl:5  SD-04#0005  outline 36 -> 183
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 183, 'SINGLE_CHOICE', '系统功能结构图主要用来描述（　）。', 'MARKDOWN', 'A', '功能结构图以树状层次形式表示系统功能自顶向下的分解，体现各功能之间的从属与包含关系，故 A 正确。数据的加工与流动由数据流图描述，对象之间的消息传递由顺序图等描述，语句的执行顺序属于程序逻辑，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=36', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0155 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0155, 'A', '系统各功能的层次分解及其从属关系', 0), (@q0155, 'B', '数据在系统中的加工与流动过程', 1), (@q0155, 'C', '对象之间消息的传递顺序', 2), (@q0155, 'D', '程序语句的先后执行顺序', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0155, @kp_284);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0155, @kp_285);

-- SD-04.jsonl:6  SD-04#0006  outline 36 -> 183
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 183, 'SINGLE_CHOICE', '对系统功能进行合理的模块划分时，应尽量使每个模块（　）。', 'MARKDOWN', 'A', '合理的模块划分追求高内聚、低耦合，即模块内部各成分联系紧密、职责单一，而模块之间联系尽量少，故 A 正确。模块过大不利于理解和维护；共享大量全局数据会提高耦合；对外暴露内部细节会破坏封装，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=36', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0156 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0156, 'A', '职责单一，内部各部分联系紧密而与其他模块的联系尽量少', 0), (@q0156, 'B', '规模尽可能大以减少模块之间的接口数量', 1), (@q0156, 'C', '与其他模块共享尽可能多的全局数据', 2), (@q0156, 'D', '对外只暴露其内部的详细处理过程', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0156, @kp_286);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0156, @kp_287);

-- SD-04.jsonl:7  SD-04#0007  outline 36 -> 183
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 183, 'SINGLE_CHOICE', '系统功能设计的主要依据是（　）。', 'MARKDOWN', 'A', '功能设计是把需求说明中确定的功能需求逐步细化为可实现的子功能，因此其依据主要是功能需求，故 A 正确。语法规则与指令系统属于实现层面的技术细节，测试用例是在设计之后用于验证的产物，均不能作为功能设计的依据，B、C、D 不正确。', 1, 'AI_GENERATED', 'SD-04;outline=36', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0157 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0157, 'A', '需求说明中确定的功能需求', 0), (@q0157, 'B', '程序设计语言的语法规则', 1), (@q0157, 'C', '硬件的指令系统', 2), (@q0157, 'D', '测试用例的设计结果', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0157, @kp_282);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0157, @kp_288);

-- SD-04.jsonl:8  SD-04#0008  outline 37 -> 184
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 184, 'SINGLE_CHOICE', 'E-R 图主要用于建立数据库的（　）。', 'MARKDOWN', 'A', 'E-R 图用实体、属性和联系刻画现实世界的信息结构，与具体的数据库管理系统无关，属于概念数据模型，故 A 正确。逻辑数据模型（如关系模式）由 E-R 图转换而来，物理数据模型涉及存储结构与存取路径，外部数据模型描述用户视图，B、C、D 均不是 E-R 图的直接产物。', 1, 'AI_GENERATED', 'SD-04;outline=37', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0158 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0158, 'A', '概念数据模型', 0), (@q0158, 'B', '逻辑数据模型', 1), (@q0158, 'C', '物理数据模型', 2), (@q0158, 'D', '外部数据模型', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0158, @kp_289);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0158, @kp_290);

-- SD-04.jsonl:9  SD-04#0009  outline 37 -> 184
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 184, 'SINGLE_CHOICE', '将 E-R 图转换为关系模式时，对于多对多（$m:n$）联系，通常的处理方式是（　）。', 'MARKDOWN', 'A', '多对多联系无法用某一端实体的外键唯一确定，因此需要单独建立一个关系模式，其主键由两端实体的主键组合而成，并可包含联系自身的属性，故 A 正确。并入某一端会造成信息重复或丢失，删除联系会丢失语义，不保留主键则无法体现联系，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=37', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0159 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0159, 'A', '转换成一个独立的关系模式，其属性由两端实体的主键加上联系自身的属性组成', 0), (@q0159, 'B', '把它并入任意一端实体对应的关系模式', 1), (@q0159, 'C', '直接删除该联系', 2), (@q0159, 'D', '只保留联系名称，不保留两端实体的主键', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0159, @kp_291);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0159, @kp_292);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0159, @kp_293);

-- SD-04.jsonl:10  SD-04#0010  outline 37 -> 184
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 184, 'SINGLE_CHOICE', '若关系模式 $R$ 已属于 $3\\text{NF}$，则下列判断正确的是（　）。', 'MARKDOWN', 'A', '$3\\text{NF}$ 在满足 $2\\text{NF}$ 的基础上，进一步要求非主属性不存在对候选键的传递依赖，因此非主属性既无部分依赖也无传递依赖，故 A 正确。消除多值依赖是 $4\\text{NF}$ 的要求；$\\text{BCNF}$ 的条件比 $3\\text{NF}$ 更强；达到 $3\\text{NF}$ 也不能完全消除冗余，B、C、D 均不正确。', 4, 'AI_GENERATED', 'SD-04;outline=37', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0160 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0160, 'A', '$R$ 中的非主属性不存在对候选键的部分依赖和传递依赖', 0), (@q0160, 'B', '$R$ 中一定不存在多值依赖', 1), (@q0160, 'C', '$R$ 一定属于 $\\text{BCNF}$', 2), (@q0160, 'D', '$R$ 中一定不存在任何数据冗余', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0160, @kp_294);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0160, @kp_107);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0160, @kp_105);

-- SD-04.jsonl:11  SD-04#0011  outline 37 -> 184
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 184, 'SINGLE_CHOICE', '下列关于数据模型三个层次的说法，正确的是（　）。', 'MARKDOWN', 'A', '概念数据模型面向现实世界、独立于具体系统；逻辑数据模型已经选定了某种数据模型（如关系模型）；物理数据模型进一步与具体数据库管理系统和存储设备相关，故 A 正确。正因如此，B、C 的说法错误；规定字段的数据类型与长度属于逻辑设计和物理设计的内容，不是概念数据模型的任务，D 不正确。', 3, 'AI_GENERATED', 'SD-04;outline=37', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0161 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0161, 'A', '概念数据模型与具体数据库管理系统无关，逻辑数据模型依赖所选用的数据模型类型，物理数据模型依赖具体的数据库管理系统与存储设备', 0), (@q0161, 'B', '三个层次的模型都与具体的数据库管理系统无关', 1), (@q0161, 'C', '物理数据模型只与业务需求有关，与存储设备无关', 2), (@q0161, 'D', '概念数据模型直接规定各字段的数据类型与长度', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0161, @kp_295);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0161, @kp_296);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0161, @kp_297);

-- SD-04.jsonl:12  SD-04#0012  outline 38 -> 185
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 185, 'SINGLE_CHOICE', '外部设计文档主要描述（　）。', 'MARKDOWN', 'A', '外部设计文档站在系统外部观察者的角度，说明系统应具备的功能、涉及的数据、对外接口以及用户可见的行为，故 A 正确。模块内部的算法细节属于内部设计文档的内容，目标代码优化与编译程序构造属于实现与工具层面，B、C、D 均不属于外部设计文档。', 2, 'AI_GENERATED', 'SD-04;outline=38', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0162 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0162, 'A', '系统对外的功能、数据、接口以及用户可见的行为', 0), (@q0162, 'B', '各模块内部的算法实现细节', 1), (@q0162, 'C', '目标代码的优化过程', 2), (@q0162, 'D', '编译程序的构造方法', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0162, @kp_298);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0162, @kp_299);

-- SD-04.jsonl:13  SD-04#0013  outline 38 -> 185
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 185, 'SINGLE_CHOICE', '一份完整规范的外部设计文档通常应当包括（　）。', 'MARKDOWN', 'A', '外部设计文档需要从整体上说明系统的外部特征，通常包含引言、总体设计、功能设计、数据设计、接口设计、运行设计等内容，故 A 正确。源代码清单属于程序实现，测试用例与结果属于测试文档，建表语句只是数据设计的一小部分，B、C、D 都不足以构成完整的外部设计文档。', 3, 'AI_GENERATED', 'SD-04;outline=38', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0163 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0163, 'A', '引言、系统总体设计、功能设计、数据设计、接口设计与运行设计等部分', 0), (@q0163, 'B', '仅包括程序源代码清单', 1), (@q0163, 'C', '仅包括测试用例与测试结果', 2), (@q0163, 'D', '仅包括数据库的建表语句', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0163, @kp_298);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0163, @kp_300);

-- SD-04.jsonl:14  SD-04#0014  outline 38 -> 185
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 185, 'SINGLE_CHOICE', '编写外部设计文档时，下列做法不恰当的是（　）。', 'MARKDOWN', 'C', '外部设计文档应当完整、清晰、一致、术语规范，并便于评审和维护，因此 A、B、D 都是恰当的做法。省略关键的约束与限制条件会使实现者缺少必要信息，导致实现偏离需求，是不恰当的做法，故选 C。', 2, 'AI_GENERATED', 'SD-04;outline=38', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0164 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0164, 'A', '内容完整、描述清晰且前后一致', 0), (@q0164, 'B', '全篇使用统一、规范的术语', 1), (@q0164, 'C', '只写概要，省略关键的约束与限制条件', 2), (@q0164, 'D', '便于评审以及后续的维护与追溯', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0164, @kp_298);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0164, @kp_301);

-- SD-04.jsonl:15  SD-04#0015  outline 38 -> 185
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 185, 'SINGLE_CHOICE', '外部设计文档应与需求说明保持一致，其主要原因是（　）。', 'MARKDOWN', 'A', '外部设计文档把需求中描述的系统外部功能与接口进一步具体化，若与需求不一致，后续实现就会偏离用户真正的要求，故 A 正确。需求说明仍是需求的权威来源，设计文档不能取而代之；外部设计文档通常由设计人员依据需求编写；其作用贯穿设计与实现，并非只在编码阶段使用，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=38', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0165 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0165, 'A', '它是对需求中所要求的外部功能与接口的具体化，不一致会使实现偏离需求', 0), (@q0165, 'B', '它可以完全取代需求说明而作为验收的唯一依据', 1), (@q0165, 'C', '它由用户编写且与需求无关', 2), (@q0165, 'D', '它只在编码阶段才需要使用', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0165, @kp_298);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0165, @kp_302);

-- SD-04.jsonl:16  SD-04#0016  outline 39 -> 186
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 186, 'SINGLE_CHOICE', '对外部设计进行评审的主要目的是（　）。', 'MARKDOWN', 'A', '设计评审属于静态检查手段，目的是在设计阶段就发现缺陷与不一致，避免缺陷流入后期造成高昂的返工成本，故 A 正确。评审不能代替测试，也不能直接提高程序运行速度或生成目标代码，B、C、D 均不正确。', 2, 'AI_GENERATED', 'SD-04;outline=39', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0166 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0166, 'A', '尽早发现设计中的缺陷与不一致，降低后期返工的成本', 0), (@q0166, 'B', '代替系统测试以节省测试工作量', 1), (@q0166, 'C', '提高程序的运行速度', 2), (@q0166, 'D', '自动生成可执行的目标代码', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0166, @kp_303);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0166, @kp_304);

-- SD-04.jsonl:17  SD-04#0017  outline 39 -> 186
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 186, 'SINGLE_CHOICE', '在设计评审中，由设计者向评审人员讲解设计并说明思路，评审人员随之检查并提出问题，这种评审方式称为（　）。', 'MARKDOWN', 'A', '走查通常由设计者主导，向评审人员讲解设计过程，评审人员跟随检查并提问；正式审查则由受过训练的主持人按检查单系统地进行，二者组织方式不同，故 A 正确。单元测试与回归测试属于动态测试手段，不是评审方式，C、D 不正确。', 3, 'AI_GENERATED', 'SD-04;outline=39', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0167 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0167, 'A', '走查', 0), (@q0167, 'B', '正式审查', 1), (@q0167, 'C', '单元测试', 2), (@q0167, 'D', '回归测试', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0167, @kp_303);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0167, @kp_305);

-- SD-04.jsonl:18  SD-04#0018  outline 39 -> 186
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 186, 'SINGLE_CHOICE', '对外部设计进行评审时，下列内容通常不属于外部设计评审重点的是（　）。', 'MARKDOWN', 'D', '外部设计评审关注系统对外可见的功能、数据与接口是否完整、合理、一致，因此 A、B、C 都是评审重点。循环展开属于代码实现层面的优化，外部设计阶段尚无源程序，因此 D 不属于外部设计评审的重点，故选 D。', 3, 'AI_GENERATED', 'SD-04;outline=39', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0168 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0168, 'A', '系统对外的功能是否完整覆盖需求', 0), (@q0168, 'B', '数据模型是否合理、是否符合规范化要求', 1), (@q0168, 'C', '对外接口的定义是否清晰且一致', 2), (@q0168, 'D', '源程序中的循环是否已经手工展开优化', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0168, @kp_306);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0168, @kp_307);

-- SD-04.jsonl:19  SD-04#0019  outline 39 -> 186
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 186, 'SINGLE_CHOICE', '设计评审结束后形成的评审记录，其主要作用是（　）。', 'MARKDOWN', 'A', '评审记录用于如实记录评审中发现的问题、相应的处理意见与责任人，并作为后续跟踪与闭环的依据，故 A 正确。评审记录不能代替设计文档，也与考勤、硬件采购无关，B、C、D 均不正确。', 2, 'AI_GENERATED', 'SD-04;outline=39', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0169 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0169, 'A', '记录发现的问题、处理意见与责任人，便于跟踪直至问题关闭', 0), (@q0169, 'B', '作为评审人员的考勤凭证', 1), (@q0169, 'C', '取代设计文档成为唯一交付物', 2), (@q0169, 'D', '用于指导硬件设备的采购', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0169, @kp_308);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0169, @kp_309);

-- SD-04.jsonl:20  SD-04#0020  outline 40 -> 187
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 187, 'SINGLE_CHOICE', '软件结构图主要描述（　）。', 'MARKDOWN', 'A', '软件结构图以层次形式表示系统中各模块及其调用关系，并标注调用过程中传递的数据或控制信息，故 A 正确。数据的加工与流动由数据流图描述，继承层次属于面向对象设计的内容，内存分布属于运行层面的细节，B、C、D 均不正确。', 2, 'AI_GENERATED', 'SD-04;outline=40', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0170 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0170, 'A', '模块的层次调用关系以及模块之间传递的信息', 0), (@q0170, 'B', '数据在系统中加工与流动的详细过程', 1), (@q0170, 'C', '对象之间的继承层次关系', 2), (@q0170, 'D', '程序运行时的内存分布情况', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0170, @kp_310);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0170, @kp_311);

-- SD-04.jsonl:21  SD-04#0021  outline 40 -> 187
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 187, 'SINGLE_CHOICE', '在软件结构设计中，关于内聚与耦合，下列说法正确的是（　）。', 'MARKDOWN', 'A', '结构设计的目标是高内聚、低耦合。内聚由弱到强排列时，功能内聚最强，故 A 正确。内容耦合中一个模块直接访问另一个模块的内部，是最强的耦合，比数据耦合更差；耦合越高越难维护；逻辑内聚只把逻辑相似的功能放在一起，弱于功能内聚，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=40', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0171 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0171, 'A', '内聚越高越好、耦合越低越好，其中功能内聚是最高的内聚类型', 0), (@q0171, 'B', '内容耦合比数据耦合更好', 1), (@q0171, 'C', '模块之间耦合越高越有利于维护', 2), (@q0171, 'D', '逻辑内聚优于功能内聚', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0171, @kp_157);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0171, @kp_156);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0171, @kp_312);

-- SD-04.jsonl:22  SD-04#0022  outline 40 -> 187
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 187, 'SINGLE_CHOICE', '数据以流的方式依次经过一系列相互独立的加工部件，每个部件完成一次变换后把结果交给下一个部件，这种体系结构风格是（　）。', 'MARKDOWN', 'A', '管道-过滤器风格把每个加工部件视为过滤器，部件之间通过管道传递数据流，各过滤器相互独立、便于复用与重组，故 A 正确。分层风格按抽象层次组织、上层依赖下层；仓库风格以共享数据为中心；隐式调用风格通过事件触发调用，均与数据依次流经独立加工部件的特征不符，B、C、D 不正确。', 4, 'AI_GENERATED', 'SD-04;outline=40', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0172 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0172, 'A', '管道-过滤器风格', 0), (@q0172, 'B', '分层风格', 1), (@q0172, 'C', '仓库风格', 2), (@q0172, 'D', '隐式调用风格', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0172, @kp_313);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0172, @kp_314);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0172, @kp_315);

-- SD-04.jsonl:23  SD-04#0023  outline 40 -> 187
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 187, 'SINGLE_CHOICE', '在软件结构图中，模块之间合理的调用关系应当是（　）。', 'MARKDOWN', 'A', '层次化的软件结构一般遵循上层调用下层、上层了解下层的方向，避免出现循环调用造成结构混乱、难以理解和维护，故 A 正确。任意双向调用、全部平级互相调用或强制下层调用上层都会破坏层次关系、导致强耦合，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=40', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0173 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0173, 'A', '通常由上层模块调用下层模块，尽量避免下层模块反向调用上层而形成环状依赖', 0), (@q0173, 'B', '任意两个模块之间都可以双向互相调用', 1), (@q0173, 'C', '所有模块处于同一层次并可以任意互相调用', 2), (@q0173, 'D', '必须由下层模块主动调用上层模块', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0173, @kp_316);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0173, @kp_317);

-- SD-04.jsonl:24  SD-04#0024  outline 41 -> 188
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 188, 'SINGLE_CHOICE', '在人机界面设计中，下列做法不恰当的是（　）。', 'MARKDOWN', 'D', '良好的人机界面应当风格统一、操作一致、及时反馈并减轻用户记忆负担，因此 A、B、C 都是恰当的做法。单纯增加不必要的操作步骤会降低效率、增加用户负担，并不能真正提高安全性，是不恰当的做法，故选 D。', 2, 'AI_GENERATED', 'SD-04;outline=41', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0174 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0174, 'A', '界面风格统一、操作方式一致', 0), (@q0174, 'B', '及时向用户反馈系统状态与操作结果', 1), (@q0174, 'C', '尽量减少用户需要记忆的信息量', 2), (@q0174, 'D', '为提高安全性而尽可能增加不必要的操作步骤', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0174, @kp_318);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0174, @kp_319);

-- SD-04.jsonl:25  SD-04#0025  outline 41 -> 188
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 188, 'SINGLE_CHOICE', '为减少数据输入错误，在输入设计中通常采取的措施是（　）。', 'MARKDOWN', 'A', '输入设计的核心是保证正确、及时、经济地获得数据：一方面减少人工录入量、尽量由系统自动带出，另一方面通过范围检查、格式检查、校验位等手段对输入进行校验，故 A 正确。盲目重复录入、增加字段或取消校验都会降低效率或增加错误，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=41', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0175 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0175, 'A', '尽量减少需要人工录入的数据量，并对输入数据进行必要的校验', 0), (@q0175, 'B', '要求用户把全部数据不加提示地重复录入两次', 1), (@q0175, 'C', '尽可能增加需要录入的字段数量', 2), (@q0175, 'D', '取消数据校验以加快录入速度', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0175, @kp_320);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0175, @kp_321);

-- SD-04.jsonl:26  SD-04#0026  outline 41 -> 188
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 188, 'SINGLE_CHOICE', '在设计输出报表时，下列做法不恰当的是（　）。', 'MARKDOWN', 'C', '输出设计应面向使用者，根据其需求确定输出的内容、详细程度、版式与份数，并可提供必要的汇总统计，因此 A、B、D 都是恰当的做法。不加整理地输出全部明细会造成信息冗余、淹没重点，是不恰当的做法，故选 C。', 3, 'AI_GENERATED', 'SD-04;outline=41', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0176 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0176, 'A', '根据使用者的实际需要确定输出的内容与详细程度', 0), (@q0176, 'B', '合理设计报表的版式与输出份数', 1), (@q0176, 'C', '不加整理地把全部原始明细输出，完全不考虑使用者的需要', 2), (@q0176, 'D', '对输出数据提供必要的汇总与统计信息', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0176, @kp_322);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0176, @kp_323);

-- SD-04.jsonl:27  SD-04#0027  outline 41 -> 188
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 188, 'SINGLE_CHOICE', '在人机交互设计中采用直接操纵方式（如拖放对象并立即看到结果），其主要优点是（　）。', 'MARKDOWN', 'A', '直接操纵让用户像操作现实对象一样作用于屏幕上可见的对象，操作结果即时可见，符合用户直觉，从而降低学习成本和记忆负担，故 A 正确。它并不减少存储需求，也不改变硬件性能；由于人总可能出错，只能通过撤销等机制减轻误操作的后果，无法完全杜绝，B、C、D 均不正确。', 4, 'AI_GENERATED', 'SD-04;outline=41', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0177 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0177, 'A', '用户可以直观地操作对象并立即获得反馈，降低学习与记忆负担', 0), (@q0177, 'B', '可以显著降低系统对存储容量的需求', 1), (@q0177, 'C', '能够提高处理器的时钟频率', 2), (@q0177, 'D', '可以完全杜绝用户的误操作', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0177, @kp_324);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0177, @kp_325);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0177, @kp_326);

-- SD-04.jsonl:28  SD-04#0028  outline 42 -> 189
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 189, 'SINGLE_CHOICE', '数据库物理设计阶段的主要任务是（　）。', 'MARKDOWN', 'A', '物理设计是在逻辑设计确定关系模式之后，结合具体数据库管理系统与存储设备，确定数据的存储结构、索引等存取路径以及空间分配等，故 A 正确。实体联系类型属于概念设计，功能结构属于应用功能设计，操作手册属于文档编写，B、C、D 均不属于物理设计的主要任务。', 3, 'AI_GENERATED', 'SD-04;outline=42', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0178 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0178, 'A', '确定数据的存储结构、存取路径（如索引）与存储分配', 0), (@q0178, 'B', '确定实体之间的联系类型', 1), (@q0178, 'C', '确定应用系统的功能结构', 2), (@q0178, 'D', '编写面向用户的操作手册', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0178, @kp_327);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0178, @kp_328);

-- SD-04.jsonl:29  SD-04#0029  outline 42 -> 189
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 189, 'SINGLE_CHOICE', '在关系数据库中，为某表的某一列建立索引之后，下列说法正确的是（　）。', 'MARKDOWN', 'A', '索引本质上是额外存储换取的、针对特定列的快速查找结构，能加快以该列为条件的检索，但会占用空间并在插入、修改、删除时带来维护代价，故 A 正确。对不涉及该列的查询并无帮助；索引增加而非减少空间占用；是否允许重复值取决于唯一性约束而不是是否建立索引，B、C、D 均不正确。', 4, 'AI_GENERATED', 'SD-04;outline=42', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0179 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0179, 'A', '以该列为条件的检索通常更快，但索引会占用额外存储空间，并在数据更新时增加维护开销', 0), (@q0179, 'B', '该表上的任何查询都一定变得更快', 1), (@q0179, 'C', '会减少该表中数据所占的存储空间', 2), (@q0179, 'D', '会使该列不允许出现重复值', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0179, @kp_329);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0179, @kp_330);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0179, @kp_331);

-- SD-04.jsonl:30  SD-04#0030  outline 42 -> 189
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 189, 'SINGLE_CHOICE', '数据库中按某一关键字顺序连续存放记录、适合批量顺序处理的文件组织方式是（　）。', 'MARKDOWN', 'A', '顺序文件按关键字顺序连续存放记录，顺序读取时效率高，适合批量处理，故 A 正确。散列文件按散列函数定位、适合按关键字随机查找；倒排文件按属性值建立索引、适合多条件检索；堆文件中记录无序存放，均不具备按关键字连续存放的特征，B、C、D 不正确。', 2, 'AI_GENERATED', 'SD-04;outline=42', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0180 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0180, 'A', '顺序文件', 0), (@q0180, 'B', '散列文件', 1), (@q0180, 'C', '倒排文件', 2), (@q0180, 'D', '堆文件', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0180, @kp_332);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0180, @kp_333);

-- SD-04.jsonl:31  SD-04#0031  outline 42 -> 189
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 189, 'SINGLE_CHOICE', '为提高数据库的存取效率，在物理设计中通常应当（　）。', 'MARKDOWN', 'A', '物理设计要根据查询与更新的模式合理选择文件组织方式和索引，并结合磁盘等介质的特点安排存储，从而在查询速度与维护代价之间取得平衡，故 A 正确。重复建索引浪费空间并增加维护负担；取消全部索引会牺牲大量查询性能；把数据集中到一个块既不现实也不利于扩展，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=42', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0181 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0181, 'A', '结合数据的使用特点合理选择文件组织与索引，并考虑存储介质的特性', 0), (@q0181, 'B', '为表中每一列都建立多个功能重复的索引', 1), (@q0181, 'C', '取消所有索引以简化维护', 2), (@q0181, 'D', '把全部数据集中存放到同一个物理块中', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0181, @kp_330);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0181, @kp_334);

-- SD-04.jsonl:32  SD-04#0032  outline 43 -> 190
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 190, 'SINGLE_CHOICE', '软件构件化的核心思想是（　）。', 'MARKDOWN', 'A', '构件化强调把功能相对独立、接口明确的软件单元封装成可独立部署和替换的构件，再通过组装构件来搭建系统，从而提高复用率与开发效率，故 A 正确。集中到单一源文件、每个功能都重新开发、限定单一语言都会削弱复用性与灵活性，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=43', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0182 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0182, 'A', '把可复用的软件单元封装为具有明确接口、可独立部署的构件，通过组装构件来构建系统', 0), (@q0182, 'B', '把系统的全部代码集中写入同一个源文件', 1), (@q0182, 'C', '系统中的每个功能都必须重新开发', 2), (@q0182, 'D', '整个系统只能使用同一种程序设计语言实现', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0182, @kp_335);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0182, @kp_256);

-- SD-04.jsonl:33  SD-04#0033  outline 43 -> 190
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 190, 'SINGLE_CHOICE', '为使构件能够在不同系统中被方便地复用，设计构件时应当（　）。', 'MARKDOWN', 'A', '可复用构件要在多种环境中工作，必须通过明确而稳定的接口对外提供服务，并尽量降低对外部环境和全局状态的依赖、避免与特定平台强绑定，故 A 正确。依赖全局变量或特定硬件会降低可移植性，硬编码业务数据会使构件无法适应新场景，B、C、D 均不利于复用。', 4, 'AI_GENERATED', 'SD-04;outline=43', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0183 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0183, 'A', '提供清晰稳定的接口，减少对外部环境和全局状态的依赖，尽量避免与特定运行环境强绑定', 0), (@q0183, 'B', '让构件直接读写全局变量以获得最大灵活性', 1), (@q0183, 'C', '使构件依赖某一特定型号的硬件设备', 2), (@q0183, 'D', '在构件内部硬编码具体的业务数据和配置', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0183, @kp_336);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0183, @kp_337);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0183, @kp_338);

-- SD-04.jsonl:34  SD-04#0034  outline 43 -> 190
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 190, 'SINGLE_CHOICE', '关于类库与框架的区别，下列说法正确的是（　）。', 'MARKDOWN', 'A', '类库是被动的功能集合，由开发者主动调用；框架则定义了系统骨架与控制流程，开发者按框架的扩展点填入代码，运行时控制权由框架掌握，即所谓“控制反转”，故 A 正确。正因控制流方向相反，B 错误；框架通常由多个协作的类或模块组成，C 错误；类库本身就是可复用的代码，D 错误。', 4, 'AI_GENERATED', 'SD-04;outline=43', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0184 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0184, 'A', '使用类库时由开发人员的代码调用库中的功能，而框架规定了整体控制流程并反过来调用开发人员编写的代码', 0), (@q0184, 'B', '类库与框架在控制流方向上完全一致', 1), (@q0184, 'C', '一个框架中只能包含一个类', 2), (@q0184, 'D', '类库中的代码不能被复用', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0184, @kp_339);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0184, @kp_257);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0184, @kp_340);

-- SD-04.jsonl:35  SD-04#0035  outline 43 -> 190
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 190, 'SINGLE_CHOICE', '在软件开发中引入软件复用，其主要好处是（　）。', 'MARKDOWN', 'A', '复用经过验证的构件可以减少重复劳动、缩短开发周期、降低成本，并且由于复用的构件往往已经过使用和检验，有利于提高软件质量，故 A 正确。复用并不是为了增加代码行数；系统仍需维护；复用构件在集成后同样需要测试，B、C、D 均不正确。', 2, 'AI_GENERATED', 'SD-04;outline=43', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0185 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0185, 'A', '提高开发效率、降低开发成本并有助于提升软件质量', 0), (@q0185, 'B', '单纯增加系统的代码总行数', 1), (@q0185, 'C', '使系统完全不需要任何维护', 2), (@q0185, 'D', '使系统不再需要进行测试', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0185, @kp_256);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0185, @kp_341);

-- SD-04.jsonl:36  SD-04#0036  outline 44 -> 191
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 191, 'SINGLE_CHOICE', '与外部设计文档相比，内部设计文档在内容侧重点上的主要特点是（　）。', 'MARKDOWN', 'A', '内部设计文档面向开发人员，重点说明各模块内部的处理逻辑、数据结构与接口实现细节，为编码提供直接依据，故 A 正确。用户可见的界面与操作方式属于外部设计的关注点；内部设计必须以外部设计为前提并与其保持一致；它不只是运行环境配置，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=44', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0186 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0186, 'A', '更侧重于描述模块内部的实现细节与数据结构，主要读者是开发人员', 0), (@q0186, 'B', '更侧重于描述最终用户可见的界面与操作方式', 1), (@q0186, 'C', '可以不依据外部设计而独立编写', 2), (@q0186, 'D', '只描述系统的运行环境配置即可', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0186, @kp_342);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0186, @kp_343);

-- SD-04.jsonl:37  SD-04#0037  outline 44 -> 191
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 191, 'SINGLE_CHOICE', '内部设计文档中对每个模块的说明，通常应当包含（　）。', 'MARKDOWN', 'A', '模块说明的目的是让开发人员据此实现该模块，因此需要写明模块功能、对外接口、内部数据结构与主要算法，故 A 正确。采购成本、用户操作手册、硬件型号与机房位置分别属于项目管理、用户文档和运行环境范畴，与模块实现无关，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=44', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0187 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0187, 'A', '模块的功能、对外接口、内部数据结构与主要处理算法', 0), (@q0187, 'B', '模块的采购成本与供应商信息', 1), (@q0187, 'C', '面向最终用户的操作手册', 2), (@q0187, 'D', '模块所涉及的硬件型号与机房位置', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0187, @kp_342);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0187, @kp_344);

-- SD-04.jsonl:38  SD-04#0038  outline 44 -> 191
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 191, 'SINGLE_CHOICE', '内部设计文档中的接口说明，一般应当描述（　）。', 'MARKDOWN', 'A', '接口说明用于约定模块之间如何交互，应写明模块名称、输入输出参数及类型与取值范围、调用约定与返回信息等，故 A 正确。编译器路径、操作系统版本、显示器分辨率属于环境配置信息，不能表达模块之间的调用契约，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=44', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0188 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0188, 'A', '模块名称、输入输出参数及其类型与取值范围、调用约定等', 0), (@q0188, 'B', '编译器的安装路径', 1), (@q0188, 'C', '操作系统的版本号', 2), (@q0188, 'D', '显示器的分辨率设置', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0188, @kp_342);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0188, @kp_345);

-- SD-04.jsonl:39  SD-04#0039  outline 44 -> 191
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 191, 'SINGLE_CHOICE', '编写内部设计文档时，应当注意（　）。', 'MARKDOWN', 'A', '内部设计文档要指导编码并支持后续维护，因此必须与外部设计保持一致、接口描述准确完整，故 A 正确。不描述接口会使模块无法正确集成；个人分工与考勤属于项目管理信息；内部设计文档的主要读者是开发人员而非最终用户，过多业务术语反而不利于实现，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=44', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0189 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0189, 'A', '与外部设计保持一致，接口描述准确完整，便于指导编码和后续维护', 0), (@q0189, 'B', '只描述功能的名称而不描述接口', 1), (@q0189, 'C', '详细记录开发人员的个人分工与考勤', 2), (@q0189, 'D', '以最终用户为主要读者，使用大量业务术语而不作技术说明', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0189, @kp_342);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0189, @kp_301);

-- SD-04.jsonl:40  SD-04#0040  outline 45 -> 192
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 192, 'SINGLE_CHOICE', '对内部设计进行评审时，检查的重点通常是（　）。', 'MARKDOWN', 'A', '内部设计评审关注设计本身的质量，重点是模块划分的合理性、接口定义的一致性与完整性、关键算法的正确性以及数据结构是否恰当，故 A 正确。投资回报属于经济决策，采购价格属于硬件选型，排版颜色属于文档外观，都不是内部设计评审的技术重点，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=45', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0190 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0190, 'A', '模块划分是否合理、接口定义是否一致以及关键算法是否正确', 0), (@q0190, 'B', '项目的投资回报率是否达到预期', 1), (@q0190, 'C', '硬件设备的采购价格是否最低', 2), (@q0190, 'D', '文档的排版颜色是否美观', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0190, @kp_346);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0190, @kp_307);

-- SD-04.jsonl:41  SD-04#0041  outline 45 -> 192
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 192, 'SINGLE_CHOICE', '在软件设计评审中，正式审查与走查的主要区别之一是（　）。', 'MARKDOWN', 'A', '正式审查强调有计划、有主持人、有检查单并形成缺陷记录，走查则以作者讲解、评审人员跟随检查为主，故 A 正确。正式审查既可用于设计也可用于代码检查，B 错误；走查同样需要评审人员参与，C 错误；二者在组织方式和严谨程度上存在明显差别，D 错误。', 4, 'AI_GENERATED', 'SD-04;outline=45', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0191 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0191, 'A', '正式审查通常由受过训练的主持人组织、按检查单系统地检查并记录缺陷，而走查一般由作者主导讲解', 0), (@q0191, 'B', '正式审查只能检查源程序，不能检查设计', 1), (@q0191, 'C', '走查不需要任何评审人员参加', 2), (@q0191, 'D', '两者在组织方式与侧重点上完全相同', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0191, @kp_347);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0191, @kp_305);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0191, @kp_348);

-- SD-04.jsonl:42  SD-04#0042  outline 45 -> 192
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 192, 'SINGLE_CHOICE', '在内部设计评审中使用检查单（Checklist），其主要作用是（　）。', 'MARKDOWN', 'A', '检查单把常见的设计缺陷与检查要点条理化，帮助评审人员按统一标准逐项核查，从而减少遗漏、提高评审一致性，故 A 正确。检查单只是辅助工具，不能替代人的判断；它不修复缺陷也不度量性能，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=45', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0192 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0192, 'A', '使评审人员按照统一的标准系统地核查常见设计缺陷，减少漏检', 0), (@q0192, 'B', '自动替代评审人员完成全部评审工作', 1), (@q0192, 'C', '在评审之后自动修复程序中已发现的缺陷', 2), (@q0192, 'D', '用来度量程序的执行性能', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0192, @kp_303);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0192, @kp_349);

-- SD-04.jsonl:43  SD-04#0043  outline 45 -> 192
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 192, 'SINGLE_CHOICE', '在内部设计评审中发现某模块的接口定义与外部设计要求不一致，且该缺陷若不及时修改将影响多个下游模块。按照缺陷处理的一般原则，最恰当的做法是（　）。', 'MARKDOWN', 'A', '评审缺陷应当被记录、分级、指定责任人并跟踪修复，对于影响面较大的接口不一致，还要在修改后重新评审并同步更新受影响的模块设计，以防止缺陷扩散，故 A 正确。拖到联调再改会使缺陷传播、返工代价更大；不记录或自行决定会失去可追溯性；直接关闭缺陷会使问题被掩盖，B、C、D 均不正确。', 5, 'AI_GENERATED', 'SD-04;outline=45', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0193 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0193, 'A', '记录该缺陷并确定其严重程度与责任人，要求修改后重新评审相关接口，必要时同步更新受影响的模块设计', 0), (@q0193, 'B', '先直接进入编码阶段，等到系统联调时再统一修改', 1), (@q0193, 'C', '由责任人自行决定是否修改，不必留下任何记录', 2), (@q0193, 'D', '为了提高进度直接关闭该缺陷，不再跟踪', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0193, @kp_346);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0193, @kp_309);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0193, @kp_350);

-- SD-04.jsonl:44  SD-04#0044  outline 49 -> 196
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 196, 'SINGLE_CHOICE', '为系统选择合适的计算机硬件配置时，主要依据是（　）。', 'MARKDOWN', 'A', '硬件配置要在满足系统处理能力、存储与数据量规模要求的前提下，兼顾预算约束来确定，故 A 正确。外壳颜色、个人偏好与楼层位置都不影响系统的实际处理能力，不能作为硬件配置的主要依据，B、C、D 均不正确。', 2, 'AI_GENERATED', 'SD-04;outline=49', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0194 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0194, 'A', '系统的处理能力需求、数据量规模与预算约束', 0), (@q0194, 'B', '机箱外壳的颜色是否美观', 1), (@q0194, 'C', '开发人员的个人使用偏好', 2), (@q0194, 'D', '公司办公场所所在的楼层', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0194, @kp_351);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0194, @kp_352);

-- SD-04.jsonl:45  SD-04#0045  outline 49 -> 196
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 196, 'SINGLE_CHOICE', '在搭建系统运行环境时，下列工作中通常不属于环境搭建范畴的是（　）。', 'MARKDOWN', 'D', '环境搭建主要包括安装操作系统、数据库、中间件等基础软件，配置网络与应用运行参数，并准备必要的基础数据，因此 A、B、C 都属于环境搭建的范畴。需求规格说明是需求阶段的产物，运行环境搭建阶段不应去修改它，若需求确需变更应按变更流程处理，故 D 不属于环境搭建范畴，选 D。', 3, 'AI_GENERATED', 'SD-04;outline=49', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0195 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0195, 'A', '安装操作系统与数据库等基础软件', 0), (@q0195, 'B', '配置网络与应用运行所需的环境参数', 1), (@q0195, 'C', '准备并初始化应用所需的基础数据', 2), (@q0195, 'D', '修改系统的需求规格说明', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0195, @kp_353);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0195, @kp_354);

-- SD-04.jsonl:46  SD-04#0046  outline 49 -> 196
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 196, 'SINGLE_CHOICE', '在系统部署上线时，先选择一个较小的范围试用或让新旧系统并行运行一段时间，确认稳定后再全面推广，这种部署策略的特点是（　）。', 'MARKDOWN', 'A', '先小范围试用或新旧系统并行，可以在影响有限的情况下暴露问题、及时处理，降低整体切换风险，代价是过渡期内需要维持两套环境、资源开销较大，故 A 正确。这种策略并不是最快的，也仍然需要用户参与和上线前的测试，B、C、D 均不正确。', 3, 'AI_GENERATED', 'SD-04;outline=49', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0196 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0196, 'A', '风险可控、便于及时发现并解决问题，但过渡期的资源开销较大', 0), (@q0196, 'B', '上线速度最快，且几乎不需要额外资源', 1), (@q0196, 'C', '完全不需要最终用户参与', 2), (@q0196, 'D', '取消了上线前的必要测试', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0196, @kp_355);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0196, @kp_356);

-- SD-04.jsonl:47  SD-04#0047  outline 49 -> 196
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 196, 'SINGLE_CHOICE', '为使系统在开发、测试与生产等不同环境中表现一致，在配置计算机系统及环境时应当（　）。', 'MARKDOWN', 'A', '不同环境配置不一致是导致本地正常而上线出错的常见原因，因此应对关键配置进行统一管理和版本记录，尽量缩小环境差异，使测试结果对生产环境有参考意义，故 A 正确。参数各不相同且不记录、只管理生产环境、随意更换软件版本都会放大环境差异并带来不可控风险，B、C、D 均不正确。', 4, 'AI_GENERATED', 'SD-04;outline=49', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0197 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0197, 'A', '对这些环境的关键配置进行统一管理与版本记录，尽量减少环境之间的差异', 0), (@q0197, 'B', '让各环境使用完全不同的参数且不作任何记录', 1), (@q0197, 'C', '只在生产环境中记录配置，其余环境无需管理', 2), (@q0197, 'D', '随意更换各环境所用的软件版本', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0197, @kp_357);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0197, @kp_358);

-- SD-04.jsonl:48  SD-04#0048  outline 50 -> 197
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 197, 'SINGLE_CHOICE', '在选择程序设计语言时，下列依据中最不重要的通常是（　）。', 'MARKDOWN', 'D', '选择语言应综合考虑应用领域与问题特点、工具与运行环境支持、开发人员熟悉程度、可维护性与效率要求等实际因素，这些都会影响项目的成败，故 A、B、C 都是重要依据。语言的名称是否简洁好记与语言的技术特性和适用场景无关，对选型几乎没有价值，因此 D 最不重要，选 D。', 3, 'AI_GENERATED', 'SD-04;outline=50', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0198 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0198, 'A', '应用领域与问题本身的特点', 0), (@q0198, 'B', '可用的开发工具与运行环境的支持情况', 1), (@q0198, 'C', '开发人员对该语言的熟悉程度', 2), (@q0198, 'D', '语言的名称是否简洁好记', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0198, @kp_359);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0198, @kp_360);

-- SD-04.jsonl:49  SD-04#0049  outline 50 -> 197
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 197, 'SINGLE_CHOICE', '对于需要直接操作硬件、对运行效率和实时性要求很高的系统底层程序，通常优先选择（　）。', 'MARKDOWN', 'A', '底层程序需要精细控制硬件资源并追求运行效率与实时响应，汇编语言可直接操作硬件、C 语言兼具接近硬件的控制能力与较高的执行效率，因此优先选用，故 A 正确。脚本语言通常依赖解释执行、效率较低；网页描述语言用于页面展示；数据库查询语言面向数据查询，均不适合直接操作硬件的底层程序，B、C、D 不正确。', 3, 'AI_GENERATED', 'SD-04;outline=50', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0199 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0199, 'A', '汇编语言或 C 语言', 0), (@q0199, 'B', '面向对象的脚本语言', 1), (@q0199, 'C', '通用网页描述语言', 2), (@q0199, 'D', '数据库查询语言', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0199, @kp_359);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0199, @kp_361);

-- SD-04.jsonl:50  SD-04#0050  outline 50 -> 197
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 197, 'SINGLE_CHOICE', '某项目既要求较高的开发效率与可维护性，又需要借助丰富的类库与成熟的图形用户界面支持，并希望缩短开发周期。综合权衡后，选择程序设计语言时最应考虑的是（　）。', 'MARKDOWN', 'A', '语言选型是面向项目需求的综合决策：既要看语言对目标应用领域（如界面开发）的支持，也要权衡开发效率、可用类库与工具生态、可维护性以及团队的掌握程度，故 A 正确。只追求执行速度排名会忽略开发效率与生态；发明时间的先后与语言适用性并无必然联系；只凭个人偏好而无视项目需求会带来明显风险，B、C、D 均不正确。', 5, 'AI_GENERATED', 'SD-04;outline=50', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0200 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0200, 'A', '语言对目标应用领域的支持程度、开发效率、类库与工具生态以及团队掌握程度等综合因素', 0), (@q0200, 'B', '仅依据语言的执行速度排名来决定', 1), (@q0200, 'C', '仅依据语言发明时间的先后顺序来决定', 2), (@q0200, 'D', '仅依据个人偏好而不考虑项目实际需求', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0200, @kp_359);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0200, @kp_362);

-- SD-05.jsonl:1  SD-05#0001  outline 50 -> 197
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 197, 'SINGLE_CHOICE', '在为某科学计算项目选择程序设计语言时，通常应优先考虑的因素是（　）。', 'MARKDOWN', 'D', '程序设计语言的选择应结合应用领域与项目需求综合判断。科学计算项目的核心是大量数值运算与数组（矩阵）处理，因此应优先考虑语言对数值计算、数组运算及其相应函数库的支持能力，故选 D。语法是否为最新、界面是否美观、版本号高低都不属于语言选型的决定性依据，故 A、B、C 不正确。', 2, 'AI_GENERATED', 'SD-05;outline=50', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0201 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0201, 'A', '语言的语法是否为最新版本', 0), (@q0201, 'B', '开发者界面的美观程度', 1), (@q0201, 'C', '语言的版本号高低', 2), (@q0201, 'D', '语言对数值计算与数组运算的支持能力', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0201, @kp_363);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0201, @kp_364);

-- SD-05.jsonl:2  SD-05#0002  outline 51 -> 198
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 198, 'SINGLE_CHOICE', '在 C 语言中，已知 `int a = 5; int *p = &a;`，则执行语句 `*p = 8;` 之后，变量 `a` 的值是（　）。', 'MARKDOWN', 'C', '指针变量 p 中保存的是 a 的地址，`*p` 就是 a 本身，因此 `*p = 8;` 等价于把 8 赋给 a，执行后 a 的值为 8，故选 C。选项 A 忽略了通过指针修改目标变量的效果，B 把变量值误当成地址，D 与语句语义矛盾，均不正确。', 2, 'AI_GENERATED', 'SD-05;outline=51', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0202 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0202, 'A', '5', 0), (@q0202, 'B', 'a 的地址', 1), (@q0202, 'C', '8', 2), (@q0202, 'D', '未定义的值', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0202, @kp_365);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0202, @kp_366);

-- SD-05.jsonl:3  SD-05#0003  outline 51 -> 198
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 198, 'SINGLE_CHOICE', '在 C++ 中，类的 private（私有）成员可以被（　）直接访问。', 'MARKDOWN', 'D', 'C++ 中 private 成员只对本类的成员函数和声明为友元（friend）的函数或类开放，故 D 正确。外部函数、派生类成员函数和全局变量都不能直接访问私有成员，故 A、B、C 都不正确。派生类即使继承了父类，也必须通过父类的公有或保护接口间接访问其私有成员。', 3, 'AI_GENERATED', 'SD-05;outline=51', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0203 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0203, 'A', '任何外部函数', 0), (@q0203, 'B', '任何派生类的成员函数', 1), (@q0203, 'C', '任意的全局变量', 2), (@q0203, 'D', '该类自身的成员函数以及声明为友元的函数或类', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0203, @kp_367);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0203, @kp_368);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0203, @kp_369);

-- SD-05.jsonl:4  SD-05#0004  outline 51 -> 198
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 198, 'SINGLE_CHOICE', '在 C 语言中，用字符串常量初始化字符数组 `s` 后，`sizeof(s)` 与 `strlen(s)` 返回值之间的关系是（　）。', 'MARKDOWN', 'B', 'sizeof 求的是数组所占的字节数，包含末尾的字符串结束标志；strlen 统计的是有效字符的个数，不计结束标志，因此 sizeof(s) 恰好比 strlen(s) 大 1，故选 B。A 与二者定义不符，C 把大小关系颠倒，D 否认了两者之间存在的固定差值，均不正确。', 4, 'AI_GENERATED', 'SD-05;outline=51', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0204 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0204, 'A', '二者总是相等', 0), (@q0204, 'B', 'sizeof(s) 比 strlen(s) 大 1，因为 sizeof 计入字符串结束标志', 1), (@q0204, 'C', 'strlen(s) 比 sizeof(s) 大 1', 2), (@q0204, 'D', '二者之间没有任何固定关系', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0204, @kp_370);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0204, @kp_371);

-- SD-05.jsonl:5  SD-05#0005  outline 51 -> 198
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 198, 'SINGLE_CHOICE', '在 C++ 中，用于抛出异常和捕获异常的关键字分别是（　）。', 'MARKDOWN', 'D', 'C++ 用 throw 抛出异常，用 try 圈定需要监测的代码块，用 catch 捕获并处理异常，因此抛出与捕获的关键字是 throw 和 catch，故选 D。A 把两者颠倒，B 中的 try 是异常监测块而非抛出语句，C 中的 try 不承担捕获职责，均不正确。', 3, 'AI_GENERATED', 'SD-05;outline=51', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0205 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0205, 'A', 'catch 和 throw', 0), (@q0205, 'B', 'try 和 catch', 1), (@q0205, 'C', 'throw 和 try', 2), (@q0205, 'D', 'throw 和 catch', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0205, @kp_372);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0205, @kp_373);

-- SD-05.jsonl:6  SD-05#0006  outline 52 -> 199
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 199, 'SINGLE_CHOICE', '系统测试的主要目的是（　）。', 'MARKDOWN', 'C', '系统测试把整个软件系统作为测试对象，验证其功能、性能、可靠性等是否满足需求规格说明，故 C 正确。验证单个模块内部逻辑属于单元测试，检查书写格式属于代码规范检查，只测界面则范围过窄，故 A、B、D 都不是系统测试的目的。', 2, 'AI_GENERATED', 'SD-05;outline=52', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0206 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0206, 'A', '验证单个模块内部的逻辑是否正确', 0), (@q0206, 'B', '检查源程序的书写格式是否规范', 1), (@q0206, 'C', '验证整个系统是否满足需求规格说明的要求', 2), (@q0206, 'D', '只检查程序的用户界面是否美观', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0206, @kp_374);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0206, @kp_375);

-- SD-05.jsonl:7  SD-05#0007  outline 52 -> 199
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 199, 'SINGLE_CHOICE', '验收测试通常由（　）主导，其依据主要是（　）。', 'MARKDOWN', 'C', '验收测试的目的是确认系统是否满足用户的实际需求，通常由用户或用户代表主导，以需求规格说明和合同为依据，故 C 正确。开发人员、测试人员、程序员主导的是开发者内部的测试活动，详细设计、源代码、接口说明都是开发者视角的文档，不能作为验收依据，故 A、B、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=52', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0207 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0207, 'A', '开发人员，详细设计文档', 0), (@q0207, 'B', '测试人员，源程序代码', 1), (@q0207, 'C', '用户或用户代表，需求规格说明与合同', 2), (@q0207, 'D', '程序员，模块接口说明', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0207, @kp_376);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0207, @kp_377);

-- SD-05.jsonl:8  SD-05#0008  outline 52 -> 199
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 199, 'SINGLE_CHOICE', '通过逐步增加并发用户数或数据量，以确定系统在极限条件下可承受的最大压力的测试类型是（　）。', 'MARKDOWN', 'B', '压力测试通过不断加大负载（如并发用户数、数据量）观察系统在极限状态下的表现和可承受上限，故 B 正确。单元测试针对模块内部逻辑，回归测试用于验证修改后未引入新的错误，代码走查属于人工评审，三者都不涉及极限负载，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=52', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0208 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0208, 'A', '单元测试', 0), (@q0208, 'B', '压力测试', 1), (@q0208, 'C', '回归测试', 2), (@q0208, 'D', '代码走查', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0208, @kp_378);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0208, @kp_379);

-- SD-05.jsonl:9  SD-05#0009  outline 52 -> 199
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 199, 'SINGLE_CHOICE', '在黑盒测试方法中，需要依据输入条件与输出结果之间的逻辑关系，为各判定条件的取值组合设计测试用例，从而发现条件组合错误的方法是（　）。', 'MARKDOWN', 'C', '因果图法把输入条件视为因、输出结果视为果，通过分析其与、或、非等逻辑关系，设计能够覆盖各种取值组合的测试用例，专门用于发现条件组合引起的错误，故 C 正确。等价类划分与边界值分析只关注取值范围，错误推测法依赖经验，都不能系统地覆盖条件组合，故 A、B、D 不正确。', 4, 'AI_GENERATED', 'SD-05;outline=52', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0209 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0209, 'A', '等价类划分', 0), (@q0209, 'B', '边界值分析', 1), (@q0209, 'C', '因果图法', 2), (@q0209, 'D', '错误推测法', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0209, @kp_380);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0209, @kp_381);

-- SD-05.jsonl:10  SD-05#0010  outline 46 -> 193
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 193, 'SINGLE_CHOICE', '在软件模块划分中，通常追求的目标是（　）。', 'MARKDOWN', 'B', '模块独立性由内聚和耦合两项度量决定。内聚越高说明模块内各部分联系越紧密、职责越单一；耦合越低说明模块之间联系越少、越易于独立修改。因此模块划分应追求高内聚、低耦合，故选 B。其余选项与该原则相反或不当。', 1, 'AI_GENERATED', 'SD-05;outline=46', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0210 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0210, 'A', '高耦合、低内聚', 0), (@q0210, 'B', '高内聚、低耦合', 1), (@q0210, 'C', '高内聚、高耦合', 2), (@q0210, 'D', '低内聚、低耦合', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0210, @kp_287);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0210, @kp_382);

-- SD-05.jsonl:11  SD-05#0011  outline 46 -> 193
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 193, 'SINGLE_CHOICE', '信息隐蔽原则在模块划分中强调（　）。', 'MARKDOWN', 'B', '信息隐蔽（信息隐藏）是指模块内部的数据结构和实现细节对外不可见，把容易变化的部分隐藏在模块内部，只通过稳定的接口与外界交互，从而降低模块之间的相互影响，故 B 正确。公开细节会削弱独立性，减少模块数量与信息隐蔽无关，共享全局变量反而增大耦合，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=46', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0211 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0211, 'A', '把模块的全部实现细节都对外公开', 0), (@q0211, 'B', '将可能发生变化的设计决策封装在模块内部，仅对外暴露必要的接口', 1), (@q0211, 'C', '尽量减少模块的总数量', 2), (@q0211, 'D', '让所有模块共享同一组全局变量', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0211, @kp_383);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0211, @kp_154);

-- SD-05.jsonl:12  SD-05#0012  outline 46 -> 193
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 193, 'SINGLE_CHOICE', '衡量模块独立性的两个定性标准是（　）。', 'MARKDOWN', 'C', '模块独立性用耦合性和内聚性两个标准衡量：耦合性衡量模块之间联系的紧密程度，内聚性衡量模块内部各成分联系的紧密程度，两者共同决定模块的独立性，故选 C。模块个数与大小、运行速度与内存占用、编程语言与文档质量都不是衡量模块独立性的标准，故 A、B、D 不正确。', 2, 'AI_GENERATED', 'SD-05;outline=46', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0212 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0212, 'A', '模块的个数和大小', 0), (@q0212, 'B', '编程语言和文档质量', 1), (@q0212, 'C', '耦合性和内聚性', 2), (@q0212, 'D', '程序的运行速度和内存占用', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0212, @kp_384);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0212, @kp_215);

-- SD-05.jsonl:13  SD-05#0013  outline 46 -> 193
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 193, 'SINGLE_CHOICE', '在模块的内聚类型中，模块内各处理部分按照特定次序执行，且前一处理的输出作为后一处理的输入，这种内聚称为（　）。', 'MARKDOWN', 'C', '顺序内聚指模块内各组成部分按一定顺序执行，且前一部分的输出成为后一部分的输入，数据流贯穿始终，故 C 正确。逻辑内聚是把若干功能相近的模块放入同一模块并由参数选择执行；时间内聚是把需要在同一时间段完成的操作集中在一起；功能内聚是模块内所有成分共同完成单一功能、联系最紧密。三者与题干描述不符，故 A、B、D 不正确。', 4, 'AI_GENERATED', 'SD-05;outline=46', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0213 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0213, 'A', '逻辑内聚', 0), (@q0213, 'B', '时间内聚', 1), (@q0213, 'C', '顺序内聚', 2), (@q0213, 'D', '功能内聚', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0213, @kp_385);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0213, @kp_386);

-- SD-05.jsonl:14  SD-05#0014  outline 47 -> 194
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 194, 'SINGLE_CHOICE', '编写程序设计文档的主要作用是（　）。', 'MARKDOWN', 'D', '程序设计文档用于记录程序的设计思路、模块结构、算法、接口等信息，是开发者之间交流以及对软件进行维护和修改的重要依据，故 D 正确。文档不能代替源代码运行，也不是收费依据或仅归档的摆设，故 A、B、C 不正确。', 2, 'AI_GENERATED', 'SD-05;outline=47', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0214 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0214, 'A', '代替程序源代码运行', 0), (@q0214, 'B', '作为向用户收费的依据', 1), (@q0214, 'C', '仅在开发结束时归档，之后不再使用', 2), (@q0214, 'D', '记录程序的设计思路、结构、算法与接口等，便于交流、维护并支持后续修改', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0214, @kp_387);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0214, @kp_388);

-- SD-05.jsonl:15  SD-05#0015  outline 47 -> 194
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 194, 'SINGLE_CHOICE', '关于程序中的注释，下列说法正确的是（　）。', 'MARKDOWN', 'C', '好的注释应说明代码的意图、为什么这样做以及易产生歧义之处，并与代码保持同步更新，故 C 正确。逐行重复代码表面行为会产生大量冗余注释，随意编写会误导阅读者，注释也应在编写过程中同步维护而非事后补充，故 A、B、D 不正确。', 2, 'AI_GENERATED', 'SD-05;outline=47', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0215 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0215, 'A', '注释越多越好，应当逐行解释每条语句', 0), (@q0215, 'B', '注释可以随意编写，不影响程序质量', 1), (@q0215, 'C', '注释应与代码保持一致并及时更新，着重说明设计意图和原因，而非简单重复代码的表面行为', 2), (@q0215, 'D', '只在程序出错之后才需要补充注释', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0215, @kp_389);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0215, @kp_390);

-- SD-05.jsonl:16  SD-05#0016  outline 47 -> 194
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 194, 'SINGLE_CHOICE', '在程序设计文档中，对函数或模块接口的说明通常不包括（　）。', 'MARKDOWN', 'C', '接口说明关注模块对外表现的契约，包括参数、返回值、可能的副作用和异常等，而完整的源代码实现属于内部实现细节，不属于接口说明的内容，故选 C。A、B、D 都是接口说明的常见组成部分，因此不符合题目要求选的这一项。', 3, 'AI_GENERATED', 'SD-05;outline=47', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0216 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0216, 'A', '参数的个数、类型与含义', 0), (@q0216, 'B', '返回值的类型与含义', 1), (@q0216, 'C', '函数完整的源代码实现', 2), (@q0216, 'D', '函数可能产生的副作用或异常', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0216, @kp_345);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0216, @kp_387);

-- SD-05.jsonl:17  SD-05#0017  outline 47 -> 194
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 194, 'SINGLE_CHOICE', '编写程序设计文档时，下列要求中不恰当的是（　）。', 'MARKDOWN', 'C', '程序设计文档强调准确、完整、清晰、规范并与程序同步，故 A、B、D 都是恰当的要求。文档的价值在于传递准确信息，追求辞藻华丽、盲目堆砌篇幅既浪费资源又可能掩盖关键信息，属于不恰当的要求，故选 C。', 3, 'AI_GENERATED', 'SD-05;outline=47', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0217 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0217, 'A', '内容应准确、完整，并与程序保持一致', 0), (@q0217, 'B', '表述应清晰、规范，便于他人阅读理解', 1), (@q0217, 'C', '应追求辞藻华丽，篇幅越多越好', 2), (@q0217, 'D', '应随程序的修改及时更新，保持同步', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0217, @kp_301);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0217, @kp_391);

-- SD-05.jsonl:18  SD-05#0018  outline 48 -> 195
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 195, 'SINGLE_CHOICE', '在程序设计评审中，代码走查通常是指（　）。', 'MARKDOWN', 'B', '代码走查是一种人工评审方式，由程序作者向评审人员讲解程序的逻辑和设计思路，评审人员提出疑问并展开讨论，从而发现缺陷，故 B 正确。自行运行查看结果属于自测，编译器检查语法和自动生成数据都属于自动化手段，不是走查，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=48', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0218 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0218, 'A', '由程序员自己运行程序查看输出结果', 0), (@q0218, 'B', '由评审小组按预定过程，由作者讲解程序、评审者提问并讨论，以发现缺陷', 1), (@q0218, 'C', '由编译器自动检查语法错误', 2), (@q0218, 'D', '由计算机自动生成测试数据', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0218, @kp_392);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0218, @kp_393);

-- SD-05.jsonl:19  SD-05#0019  outline 48 -> 195
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 195, 'SINGLE_CHOICE', '在程序评审中使用检查表的主要目的是（　）。', 'MARKDOWN', 'D', '检查表把以往常见的缺陷类型和处理要点整理成条目，供评审人员逐项核对，使评审更有针对性并减少遗漏，故 D 正确。检查表只是辅助手段，不能替代人工评审，也不能自动修改错误或用于统计代码行数，故 A、B、C 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=48', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0219 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0219, 'A', '完全替代人工评审', 0), (@q0219, 'B', '自动修改程序中的错误', 1), (@q0219, 'C', '统计程序代码的行数', 2), (@q0219, 'D', '使评审更有针对性，避免遗漏常见的缺陷类型', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0219, @kp_394);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0219, @kp_395);

-- SD-05.jsonl:20  SD-05#0020  outline 48 -> 195
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 195, 'SINGLE_CHOICE', '关于程序评审记录，下列说法正确的是（　）。', 'MARKDOWN', 'B', '评审记录应完整记录发现的问题、责任人、处理意见和后续跟踪结果，以便逐项整改并形成闭环，从而真正改进程序质量，故 B 正确。只记结论、不留存或立即销毁记录都会使评审流于形式，无法追踪问题是否真正解决，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=48', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0220 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0220, 'A', '只需记录评审是否通过，无需记录发现的问题', 0), (@q0220, 'B', '应记录发现的问题、责任人、处理意见及跟踪结果，实现闭环管理', 1), (@q0220, 'C', '评审记录属于个人隐私，不应留存', 2), (@q0220, 'D', '评审结束后应立即销毁记录', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0220, @kp_308);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0220, @kp_396);

-- SD-05.jsonl:21  SD-05#0021  outline 48 -> 195
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 195, 'SINGLE_CHOICE', '开展程序设计评审的主要目的是（　）。', 'MARKDOWN', 'B', '程序评审通过人工检查在早期发现设计和编码中的错误与缺陷，缺陷发现得越早，修复成本越低，软件质量也越有保障，故 B 正确。评审不是工作量统计或人事考核手段，也不能代替系统测试，故 A、C、D 不正确。', 2, 'AI_GENERATED', 'SD-05;outline=48', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0221 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0221, 'A', '统计程序员的工作量', 0), (@q0221, 'B', '尽早发现设计中的错误与缺陷，提高软件质量并降低返工成本', 1), (@q0221, 'C', '代替系统测试', 2), (@q0221, 'D', '作为程序员考核晋升的依据', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0221, @kp_393);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0221, @kp_397);

-- SD-05.jsonl:22  SD-05#0022  outline 31 -> 178
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 178, 'SINGLE_CHOICE', '我国标准按适用范围划分，在全国范围内统一的技术要求属于（　）。', 'MARKDOWN', 'D', '按适用范围，我国标准分为国家标准、行业标准、地方标准和企业标准。国家标准是对全国经济技术发展有重大意义、需要在全国范围内统一的技术要求，故 D 正确。行业标准在某个行业范围内统一，地方标准在某个地区统一，企业标准仅在本企业内适用，故 A、B、C 不正确。', 2, 'AI_GENERATED', 'SD-05;outline=31', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0222 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0222, 'A', '企业标准', 0), (@q0222, 'B', '行业标准', 1), (@q0222, 'C', '地方标准', 2), (@q0222, 'D', '国家标准', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0222, @kp_398);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0222, @kp_399);

-- SD-05.jsonl:23  SD-05#0023  outline 31 -> 178
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 178, 'SINGLE_CHOICE', '我国标准按约束性质可分为（　）。', 'MARKDOWN', 'A', '按约束性质，我国标准分为强制性标准和推荐性标准：强制性标准必须执行，推荐性标准由国家鼓励采用，故 A 正确。国家标准、企业标准等属于按适用范围划分的类型，正式与临时、国内与国际并非我国按约束性质的法定分类，故 B、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=31', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0223 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0223, 'A', '强制性标准和推荐性标准', 0), (@q0223, 'B', '国家标准和企业标准', 1), (@q0223, 'C', '正式标准和临时标准', 2), (@q0223, 'D', '国内标准和国际标准', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0223, @kp_400);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0223, @kp_401);

-- SD-05.jsonl:24  SD-05#0024  outline 31 -> 178
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 178, 'SINGLE_CHOICE', '国际标准化组织 ISO 的英文全称是（　）。', 'MARKDOWN', 'D', 'ISO 是 International Organization for Standardization 的缩写，即国际标准化组织，负责制定各类国际标准，故 D 正确。A、B、C 均为与标准组织实际名称不符的组合，属于常见的记忆混淆干扰项，故不正确。', 2, 'AI_GENERATED', 'SD-05;outline=31', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0224 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0224, 'A', 'International Standard Office', 0), (@q0224, 'B', 'International Society for Organization', 1), (@q0224, 'C', 'Industry Standard Organization', 2), (@q0224, 'D', 'International Organization for Standardization', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0224, @kp_402);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0224, @kp_403);

-- SD-05.jsonl:25  SD-05#0025  outline 31 -> 178
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 178, 'SINGLE_CHOICE', '在我国标准编号中，代号 `GB/T` 表示（　）。', 'MARKDOWN', 'B', '我国国家标准的代号为 GB，加斜杠和字母 T（即 GB/T）表示推荐性国家标准，故 B 正确。强制性国家标准只用 GB 表示；行业标准使用各行业代号（如 SJ、YD 等），企业标准代号为 Q，均与 GB/T 的含义不符，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=31', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0225 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0225, 'A', '强制性国家标准', 0), (@q0225, 'B', '推荐性国家标准', 1), (@q0225, 'C', '行业标准', 2), (@q0225, 'D', '企业标准', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0225, @kp_404);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0225, @kp_405);

-- SD-05.jsonl:26  SD-05#0026  outline 32 -> 179
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 179, 'SINGLE_CHOICE', '关于信息化，下列理解正确的是（　）。', 'MARKDOWN', 'C', '信息化培育和发展以信息技术为依托的新生产力，其核心是充分利用信息技术开发信息资源、促进交流与共享，并推动经济社会转型，是一个持续的过程，故 C 正确。只把信息化等同于设备普及或购置服务器是以偏概全，认为其与业务流程无关也违背事实，故 A、B、D 不正确。', 2, 'AI_GENERATED', 'SD-05;outline=32', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0226 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0226, 'A', '信息化仅指计算机设备的普及', 0), (@q0226, 'B', '信息化就是购买更多的服务器', 1), (@q0226, 'C', '信息化是充分利用信息技术、开发利用信息资源、促进信息交流和知识共享，从而推动经济社会发展转型的过程', 2), (@q0226, 'D', '信息化与业务流程和组织管理无关', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0226, @kp_406);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0226, @kp_407);

-- SD-05.jsonl:27  SD-05#0027  outline 32 -> 179
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 179, 'SINGLE_CHOICE', 'ERP（企业资源计划）的核心思想是（　）。', 'MARKDOWN', 'B', 'ERP 是在 MRP、MRP II 基础上发展起来的管理信息系统，其核心是对企业的人、财、物、产、供、销等各类资源进行统一计划和集成管理，实现信息共享与业务协同，故 B 正确。A、C 把 ERP 的范围局限于财务或客户关系，D 与 ERP 无关，均不正确。', 3, 'AI_GENERATED', 'SD-05;outline=32', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0227 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0227, 'A', '仅管理企业的财务账目', 0), (@q0227, 'B', '对企业的人、财、物、产、供、销等资源进行一体化集成管理', 1), (@q0227, 'C', '仅用于维护客户关系', 2), (@q0227, 'D', '仅用于收发电子邮件', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0227, @kp_408);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0227, @kp_409);

-- SD-05.jsonl:28  SD-05#0028  outline 32 -> 179
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 179, 'SINGLE_CHOICE', 'CRM（客户关系管理）系统主要关注的是（　）。', 'MARKDOWN', 'B', 'CRM 以客户为中心，通过管理销售、营销、服务等与客户的交互过程并分析客户行为，提升客户满意度、忠诚度和企业收益，故 B 正确。库存仓储属于 ERP 或进销存范畴，物流配送属于 SCM，网络搭建属于基础设施，都不是 CRM 的重点，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=32', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0228 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0228, 'A', '企业内部库存与仓储管理', 0), (@q0228, 'B', '以客户为中心，管理企业与客户之间的交互和关系，提升客户满意度与忠诚度', 1), (@q0228, 'C', '供应链上的物流与配送', 2), (@q0228, 'D', '计算机网络的搭建与维护', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0228, @kp_410);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0228, @kp_409);

-- SD-05.jsonl:29  SD-05#0029  outline 32 -> 179
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 179, 'SINGLE_CHOICE', '与传统的操作型数据库相比，数据仓库最主要的特点是（　）。', 'MARKDOWN', 'A', '数据仓库用于分析决策，其四个基本特征是面向主题、集成、相对稳定（非易失）和随时间变化，故 A 正确。高频的联机事务处理是操作型数据库的职责；数据仓库既保存历史数据又支持查询与分析，故 B、C、D 均不正确。', 4, 'AI_GENERATED', 'SD-05;outline=32', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0229 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0229, 'A', '面向主题、集成、相对稳定、随时间不断变化', 0), (@q0229, 'B', '主要承担高频的联机事务处理', 1), (@q0229, 'C', '只保存当前最新的明细数据', 2), (@q0229, 'D', '不支持任何形式的查询与分析', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0229, @kp_411);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0229, @kp_412);

-- SD-05.jsonl:30  SD-05#0030  outline 33 -> 180
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 180, 'SINGLE_CHOICE', '自然人的软件著作权，其保护期截止于（　）。', 'MARKDOWN', 'B', '自然人的软件著作权，其发表权和各项财产权的保护期为作者终生及其死亡后 50 年，截止于作者死亡后第 50 年的 12 月 31 日，故 B 正确。A 适用于法人或其他组织的软件著作权；C 的 20 年不是软件著作权的保护期；财产权有期限限制并非永久有效，故 A、C、D 不正确。', 4, 'AI_GENERATED', 'SD-05;outline=33', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0230 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0230, 'A', '软件首次发表后第 50 年的 12 月 31 日', 0), (@q0230, 'B', '作者死亡后第 50 年的 12 月 31 日', 1), (@q0230, 'C', '作者死亡后第 20 年的 12 月 31 日', 2), (@q0230, 'D', '永久有效', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0230, @kp_413);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0230, @kp_414);

-- SD-05.jsonl:31  SD-05#0031  outline 33 -> 180
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 180, 'SINGLE_CHOICE', '我国发明专利权的保护期限，自申请日起计算为（　）。', 'MARKDOWN', 'D', '我国专利法规定，发明专利权的保护期为 20 年，实用新型专利权和外观设计专利权的保护期为 10 年，均自申请日起计算，故 D 正确。A 是实用新型或外观设计专利权的保护期；B、C 均不符合专利权有期限限制的规定，故 A、B、C 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=33', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0231 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0231, 'A', '10 年', 0), (@q0231, 'B', '50 年', 1), (@q0231, 'C', '长期有效', 2), (@q0231, 'D', '20 年', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0231, @kp_415);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0231, @kp_416);

-- SD-05.jsonl:32  SD-05#0032  outline 33 -> 180
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 180, 'SINGLE_CHOICE', '我国注册商标的有效期为（　），期满可以办理续展。', 'MARKDOWN', 'B', '我国商标法规定注册商标的有效期为 10 年，自核准注册之日起计算，期满需要继续使用的可以在规定期限内办理续展，故 B 正确。5 年、20 年、50 年均不是注册商标的有效期，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=33', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0232 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0232, 'A', '5 年', 0), (@q0232, 'B', '10 年', 1), (@q0232, 'C', '20 年', 2), (@q0232, 'D', '50 年', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0232, @kp_417);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0232, @kp_418);

-- SD-05.jsonl:33  SD-05#0033  outline 33 -> 180
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 180, 'SINGLE_CHOICE', '下列行为中，通常不构成侵犯软件著作权的是（　）。', 'MARKDOWN', 'B', '为学习和研究目的而使用他人已发表的软件属于著作权法允许的合理使用情形，通常不构成侵权，故选 B。未经许可复制并销售、超出许可范围商用、删除权利管理信息后传播都超出了合理使用的界限，属于侵权行为，故 A、C、D 不符合题意。', 4, 'AI_GENERATED', 'SD-05;outline=33', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0233 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0233, 'A', '未经许可复制他人软件用于销售', 0), (@q0233, 'B', '为学习和研究目的而使用他人已发表的软件', 1), (@q0233, 'C', '超出许可范围将授权软件用于商业经营', 2), (@q0233, 'D', '删除软件的权利管理信息后向他人传播', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0233, @kp_419);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0233, @kp_420);

-- SD-05.jsonl:34  SD-05#0034  outline 4 -> 151
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 151, 'SINGLE_CHOICE', '在面向对象方法中，类与对象的关系是（　）。', 'MARKDOWN', 'C', '类是对具有相同属性和操作的对象的抽象描述，对象是类的一个具体实例，二者是抽象与实例的关系，故 C 正确。A 把二者关系颠倒，B 混淆了抽象与实例，D 否认了二者的联系，均不正确。', 1, 'AI_GENERATED', 'SD-05;outline=4', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0234 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0234, 'A', '类是对象的实例，对象是类的抽象', 0), (@q0234, 'B', '类与对象是完全相同的概念', 1), (@q0234, 'C', '类是对象的抽象，对象是类的实例', 2), (@q0234, 'D', '二者之间没有任何关系', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0234, @kp_421);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0234, @kp_422);

-- SD-05.jsonl:35  SD-05#0035  outline 4 -> 151
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 151, 'SINGLE_CHOICE', '面向对象方法中的封装是指（　）。', 'MARKDOWN', 'C', '封装把数据（属性）和对数据的操作（方法）捆绑在一起，对外只暴露必要的接口并隐藏内部实现，从而降低耦合、提高安全性，故 C 正确。合并类与封装无关，全部成员公开违背了封装的初衷，只保留数据则无从体现对象的行为，故 A、B、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=4', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0235 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0235, 'A', '把多个类合并成一个类', 0), (@q0235, 'B', '让对象的全部成员都可被外界随意访问', 1), (@q0235, 'C', '把对象的属性与操作（方法）结合成一个整体，并尽可能隐蔽其内部实现细节', 2), (@q0235, 'D', '只保留数据而舍弃所有方法', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0235, @kp_423);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0235, @kp_424);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0235, @kp_383);

-- SD-05.jsonl:36  SD-05#0036  outline 4 -> 151
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 151, 'SINGLE_CHOICE', '面向对象方法中引入继承机制的主要作用是（　）。', 'MARKDOWN', 'A', '继承表达一般与特殊的关系，子类自动获得父类的属性和方法，并可增加或重定义以满足自身需要，从而实现代码复用和功能扩展，故 A 正确。B、C 与继承的作用无关，D 描述的是封装而不是继承，均不正确。', 3, 'AI_GENERATED', 'SD-05;outline=4', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0236 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0236, 'A', '使子类可以自动拥有父类的属性和方法，在此基础上复用并扩展功能', 0), (@q0236, 'B', '使各个对象之间彼此独立、互不相关', 1), (@q0236, 'C', '把一个类拆分为多个模块', 2), (@q0236, 'D', '隐藏对象内部的状态', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0236, @kp_425);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0236, @kp_426);

-- SD-05.jsonl:37  SD-05#0037  outline 4 -> 151
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 151, 'SINGLE_CHOICE', '面向对象方法中的多态性是指（　）。', 'MARKDOWN', 'A', '多态性指同样的消息或操作被不同类的对象接收时，可以根据各自的实现产生不同的行为结果，通常通过方法重载和方法重写实现，故 A 正确。B 与类可以创建多个实例的事实不符，C 与子类可重写父类方法的事实相反，D 与对象通过消息协作的思想矛盾，均不正确。', 3, 'AI_GENERATED', 'SD-05;outline=4', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0237 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0237, 'A', '同一操作或消息作用于不同的对象时可以产生不同的执行结果', 0), (@q0237, 'B', '一个类只能创建唯一的一个对象', 1), (@q0237, 'C', '子类不允许重定义父类的方法', 2), (@q0237, 'D', '对象之间不能相互发送消息', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0237, @kp_102);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0237, @kp_427);

-- SD-05.jsonl:38  SD-05#0038  outline 4 -> 151
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 151, 'SINGLE_CHOICE', '在面向对象方法中，对象之间的相互联系和协作主要通过（　）来实现。', 'MARKDOWN', 'B', '对象是独立封装的实体，对象之间通过发送和接收消息进行请求与响应，从而完成协作，故 B 正确。全局变量会破坏封装、增大耦合，继承描述的是类之间的层次关系而非对象之间的动态协作，多态是消息得到响应的一种表现形式，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=4', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0238 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0238, 'A', '全局变量传递', 0), (@q0238, 'B', '消息传递', 1), (@q0238, 'C', '继承关系', 2), (@q0238, 'D', '多态性', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0238, @kp_428);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0238, @kp_429);

-- SD-05.jsonl:39  SD-05#0039  outline 4 -> 151
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 151, 'SINGLE_CHOICE', '面向对象方法中的抽象是指（　）。', 'MARKDOWN', 'A', '抽象通过舍弃与当前问题无关的次要细节，抽取并强调与目标相关的本质属性和行为，是认识事物和建立类的基础，故 A 正确。追求复杂、隐藏全部对象或只关注实现细节都与抽象的思想相反，故 B、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=4', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0239 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0239, 'A', '忽略与当前目标无关的细节，强调与目标有关的本质特征', 0), (@q0239, 'B', '把程序写得越复杂越好', 1), (@q0239, 'C', '隐藏全部对象使其不可见', 2), (@q0239, 'D', '只关注具体的实现细节', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0239, @kp_430);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0239, @kp_431);

-- SD-05.jsonl:40  SD-05#0040  outline 5 -> 152
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 152, 'SINGLE_CHOICE', '下列算法中，属于对称加密算法的是（　）。', 'MARKDOWN', 'C', 'DES 的加密与解密使用同一把密钥，属于对称加密算法，故 C 正确。RSA 是典型的非对称（公钥）加密算法；SHA-1 和 MD5 都是单向的散列（摘要）算法，用于完整性校验而非加密解密，故 A、B、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=5', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0240 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0240, 'A', 'RSA', 0), (@q0240, 'B', 'SHA-1', 1), (@q0240, 'C', 'DES', 2), (@q0240, 'D', 'MD5', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0240, @kp_432);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0240, @kp_433);

-- SD-05.jsonl:41  SD-05#0041  outline 5 -> 152
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 152, 'SINGLE_CHOICE', '在非对称（公钥）加密体制中，若甲要向乙发送保密信息，甲应使用（　）加密，乙使用（　）解密。', 'MARKDOWN', 'A', '公钥可公开、私钥由本人保管。为使只有乙能读懂，甲应使用乙的公钥加密，乙再使用自己的私钥解密，故 A 正确。用公钥加密的内容不能用同一公钥解开，用乙的私钥加密会让任何人都能用乙的公钥解开，其余组合都无法保证保密性，故 B、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=5', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0241 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0241, 'A', '乙的公钥、乙的私钥', 0), (@q0241, 'B', '乙的私钥、乙的公钥', 1), (@q0241, 'C', '甲的公钥、甲的私钥', 2), (@q0241, 'D', '甲的私钥、乙的公钥', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0241, @kp_434);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0241, @kp_435);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0241, @kp_436);

-- SD-05.jsonl:42  SD-05#0042  outline 5 -> 152
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 152, 'SINGLE_CHOICE', '甲向乙发送一份电子合同，为使乙能够确认该合同确实由甲发出且内容未被篡改，甲应当（　）。', 'MARKDOWN', 'B', '数字签名用发送方的私钥对消息摘要进行运算，接收方用发送方的公钥验证，从而确认消息来源的真实性（不可否认性）并检验内容是否被篡改，故 B 正确。用乙的公钥加密只能保证保密性，无法证明发送者身份；对称加密和明文发送都不能提供发送者身份与完整性的保证，故 A、C、D 不正确。', 4, 'AI_GENERATED', 'SD-05;outline=5', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0242 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0242, 'A', '用乙的公钥加密合同后发送', 0), (@q0242, 'B', '用甲自己的私钥对合同的数字摘要进行签名后随合同一同发送', 1), (@q0242, 'C', '用对称密钥加密合同后发送', 2), (@q0242, 'D', '直接以明文形式发送合同', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0242, @kp_437);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0242, @kp_438);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0242, @kp_439);

-- SD-05.jsonl:43  SD-05#0043  outline 5 -> 152
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 152, 'SINGLE_CHOICE', '身份认证的主要目的是（　）。', 'MARKDOWN', 'D', '身份认证用于核实访问者所声称的身份是否真实可信，是实施访问控制的前提，故 D 正确。数据加密解决的是保密性，数据压缩用于提高存储与传输效率，数据备份用于提高可靠性，均不是身份认证的目的，故 A、B、C 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=5', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0243 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0243, 'A', '对传输的数据进行加密', 0), (@q0243, 'B', '压缩数据以节省带宽', 1), (@q0243, 'C', '对重要数据进行备份', 2), (@q0243, 'D', '确认用户或系统所声称的身份是否真实', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0243, @kp_440);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0243, @kp_367);

-- SD-05.jsonl:44  SD-05#0044  outline 5 -> 152
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 152, 'SINGLE_CHOICE', '防火墙在网络安全中的主要作用是（　）。', 'MARKDOWN', 'B', '防火墙部署在内部网络与外部网络之间，依据预设的安全策略对进出的数据流进行检查和过滤，实施访问控制，故 B 正确。断电属于供电问题，CPU 速度属于计算机性能，查杀病毒是杀毒软件的功能，防火墙并不能保证清除全部病毒，故 A、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=5', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0244 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0244, 'A', '防止计算机意外断电', 0), (@q0244, 'B', '在内部网络与外部网络之间实施访问控制，检查并过滤进出网络的数据', 1), (@q0244, 'C', '提高 CPU 的运算速度', 2), (@q0244, 'D', '自动查杀系统中的全部病毒', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0244, @kp_441);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0244, @kp_442);

-- SD-05.jsonl:45  SD-05#0045  outline 5 -> 152
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 152, 'SINGLE_CHOICE', '关于计算机病毒，下列说法正确的是（　）。', 'MARKDOWN', 'A', '计算机病毒本质是一段人为编制的、具有传染性、潜伏性和破坏性的程序代码，能够自我复制并借助各种途径传播，故 A 正确。病毒是软件而非硬件损坏所致，它会感染并破坏软件系统，杀毒软件只能防范已知或可识别的大多数病毒、无法保证杜绝全部病毒，故 B、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=5', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0245 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0245, 'A', '计算机病毒是一段具有传染性和破坏性的程序代码，能够自我复制并传播', 0), (@q0245, 'B', '计算机病毒只能由硬件损坏产生', 1), (@q0245, 'C', '计算机病毒不会影响任何软件系统', 2), (@q0245, 'D', '安装了杀毒软件后就可以彻底杜绝所有病毒', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0245, @kp_443);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0245, @kp_444);

-- SD-05.jsonl:46  SD-05#0046  outline 10 -> 157
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 157, 'SINGLE_CHOICE', '数据库应用系统的设计，其一般步骤是（　）。', 'MARKDOWN', 'A', '数据库设计通常先进行需求分析，明确信息与处理需求；再据此进行概念结构设计，得到独立于具体 DBMS 的概念模型；然后将概念模型转换为某种 DBMS 支持的逻辑结构；最后进行物理结构设计，确定存储结构与存取方法，故 A 正确。其余选项颠倒了各阶段的先后关系，均不正确。', 4, 'AI_GENERATED', 'SD-05;outline=10', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0246 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0246, 'A', '需求分析→概念结构设计→逻辑结构设计→物理结构设计', 0), (@q0246, 'B', '概念结构设计→需求分析→逻辑结构设计→物理结构设计', 1), (@q0246, 'C', '逻辑结构设计→物理结构设计→需求分析→概念结构设计', 2), (@q0246, 'D', '物理结构设计→逻辑结构设计→概念结构设计→需求分析', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0246, @kp_445);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0246, @kp_446);

-- SD-05.jsonl:47  SD-05#0047  outline 10 -> 157
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 157, 'SINGLE_CHOICE', '在数据库概念结构设计阶段，最常用的描述工具是（　）。', 'MARKDOWN', 'D', '概念结构设计的目标是得到与具体 DBMS 无关的概念模型，最常用的描述工具是 E-R 图（实体—联系图），故 D 正确。汇编语言是程序设计语言，甘特图用于项目进度管理，数据流图用于结构化需求分析，均不是概念结构设计的描述工具，故 A、B、C 不正确。', 2, 'AI_GENERATED', 'SD-05;outline=10', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0247 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0247, 'A', '汇编语言', 0), (@q0247, 'B', '甘特图', 1), (@q0247, 'C', '数据流图', 2), (@q0247, 'D', 'E-R 图', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0247, @kp_447);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0247, @kp_289);

-- SD-05.jsonl:48  SD-05#0048  outline 10 -> 157
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 157, 'SINGLE_CHOICE', '将概念结构设计得到的 E-R 图转换为关系模式，属于数据库设计中的（　）阶段。', 'MARKDOWN', 'C', '逻辑结构设计的任务是把概念模型转换为特定 DBMS 支持的数据模型，对关系数据库而言就是把 E-R 图转换为关系模式并进行规范化处理，故 C 正确。需求分析阶段收集需求，概念设计阶段得到 E-R 图，物理设计阶段确定存储结构与存取方法，均不是完成这一转换的阶段，故 A、B、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=10', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0248 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0248, 'A', '需求分析', 0), (@q0248, 'B', '概念结构设计', 1), (@q0248, 'C', '逻辑结构设计', 2), (@q0248, 'D', '物理结构设计', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0248, @kp_446);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0248, @kp_291);

-- SD-05.jsonl:49  SD-05#0049  outline 10 -> 157
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 157, 'SINGLE_CHOICE', '数据库事务的四个特性（ACID）中，原子性（Atomicity）是指（　）。', 'MARKDOWN', 'A', '原子性强调事务是不可分割的工作单位，其中的操作要么全部完成，要么全部不做并回滚，故 A 正确。B 描述的是一致性，C 描述的是隔离性，D 描述的是持久性，三者分别是 ACID 中的 C、I、D，与原子性无关，故 B、C、D 不正确。', 3, 'AI_GENERATED', 'SD-05;outline=10', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0249 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0249, 'A', '事务中的诸操作要么全部执行，要么全部不执行', 0), (@q0249, 'B', '事务执行后数据库必须从一个一致状态转到另一个一致状态', 1), (@q0249, 'C', '多个事务并发执行时互不干扰', 2), (@q0249, 'D', '事务一旦提交，其结果便永久保存', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0249, @kp_448);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0249, @kp_449);

-- SD-05.jsonl:50  SD-05#0050  outline 10 -> 157
INSERT INTO rk_question
  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,
   difficulty, source_type, source_ref, source_license, generation_batch_id, status,
   created_by, published_at)
VALUES (6, 157, 'SINGLE_CHOICE', '在关系数据库中，对经常作为查询条件的列建立索引，其作用是（　）。', 'MARKDOWN', 'A', '索引通过建立类似目录的辅助结构，使按该列进行查询和连接时能更快定位记录，从而加快查询；但索引本身需要额外存储，且插入、删除、修改时要同步维护索引，会降低更新速度，故 A 正确。唯一性由约束保证而非普通索引，索引与加密无关，也不能消除表间关联，故 B、C、D 不正确。', 4, 'AI_GENERATED', 'SD-05;outline=10', 'SELF', @batch, 'PUBLISHED', 1, NOW());
SET @q0250 := LAST_INSERT_ID();
INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES (@q0250, 'A', '加快相关查询的速度，但会占用额外存储空间并可能降低数据更新速度', 0), (@q0250, 'B', '保证该列的数据绝对不发生重复', 1), (@q0250, 'C', '自动对该列的数据进行加密', 2), (@q0250, 'D', '彻底消除表与表之间的关联', 3);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0250, @kp_450);
INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q0250, @kp_451);

-- 审计：如实记录方案 B 的偏离
INSERT INTO sys_operation_log (user_id, module, action, target_type, target_id, detail) VALUES (1, 'QUESTION', 'CREATE', 'generation_batch', @batch, '方案 B 最小导入器：250 题由 EX-SD-001 批量导入并直接置为 PUBLISHED，未经逐题审核（reviewed_by 留 NULL）。');

COMMIT;
-- 冗余计数刷新（不进事务：纯派生数据，重算一遍即可）
UPDATE rk_subject SET question_count = (SELECT COUNT(*) FROM rk_question WHERE subject_id = 6 AND status = 'PUBLISHED') WHERE id = 6;
UPDATE rk_outline_node SET question_count = 20 WHERE id = 148;
UPDATE rk_outline_node SET question_count = 45 WHERE id = 149;
UPDATE rk_outline_node SET question_count = 35 WHERE id = 150;
UPDATE rk_outline_node SET question_count = 6 WHERE id = 151;
UPDATE rk_outline_node SET question_count = 6 WHERE id = 152;
UPDATE rk_outline_node SET question_count = 12 WHERE id = 153;
UPDATE rk_outline_node SET question_count = 0 WHERE id = 154;
UPDATE rk_outline_node SET question_count = 24 WHERE id = 155;
UPDATE rk_outline_node SET question_count = 24 WHERE id = 156;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 157;
UPDATE rk_outline_node SET question_count = 12 WHERE id = 158;
UPDATE rk_outline_node SET question_count = 16 WHERE id = 159;
UPDATE rk_outline_node SET question_count = 45 WHERE id = 160;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 163;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 164;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 165;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 166;
UPDATE rk_outline_node SET question_count = 15 WHERE id = 167;
UPDATE rk_outline_node SET question_count = 20 WHERE id = 168;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 169;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 170;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 171;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 172;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 173;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 174;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 175;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 176;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 177;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 178;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 179;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 180;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 181;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 182;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 183;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 184;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 185;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 186;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 187;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 188;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 189;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 190;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 191;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 192;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 193;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 194;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 195;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 196;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 197;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 198;
UPDATE rk_outline_node SET question_count = 4 WHERE id = 199;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 200;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 201;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 202;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 203;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 204;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 205;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 206;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 207;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 208;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 226;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 227;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 228;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 229;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 230;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 231;
UPDATE rk_outline_node SET question_count = 5 WHERE id = 232;