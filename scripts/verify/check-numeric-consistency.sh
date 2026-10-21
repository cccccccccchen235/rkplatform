#!/usr/bin/env bash
# 数值一致性断言 —— 对应 10 §10.2 的「数值一致性」验收项与 15 L7 的「表数量 CI 断言」。
#
# 口径（关键）：**期望值从产物反推，不在本脚本里另写一份常量**。真源只有两处：
#     表数量 ← deploy/sql/schema.sql 的 `CREATE TABLE` 条数
#     科目数 ← deploy/sql/init-data.sql 的 rk_subject INSERT 值元组数
# 文档与 SQL 里"复写不可避免"的数字（验收命令、概述句、断言别名）逐处断言与反推值相等。
# 任一处漂移 → 本脚本非零退出 → CI 失败。
#
# 这正是 10 §10.2 要求的做法：不是"再核对一遍数字"，而是让复写点**不可能**单独演进。
#
# 本地运行：bash scripts/verify/check-numeric-consistency.sh
# 局限：只覆盖无需数据库即可断言的复写点。需要连库的断言（场次 25 / 分节 45 / 满分不变式）
#       在 deploy/sql/verify.sql 里，由人工或后续的带 MySQL service 的 job 执行。

set -uo pipefail

cd "$(dirname "$0")/../.." || exit 2

pass=0
fail=0

# 断言某文件里**所有**匹配 `pat` 的数字都等于 `want`
assert_all() {
  local want="$1" file="$2" pat="$3" label="$4"
  local nums bad=0 n
  nums=$(grep -oE "$pat" "$file" 2>/dev/null | grep -oE '[0-9]+')
  if [ -z "$nums" ]; then
    printf '✗ %-34s %s 里未命中 /%s/\n' "$label" "$file" "$pat"
    fail=$((fail + 1))
    return
  fi
  for n in $nums; do [ "$n" = "$want" ] || bad=1; done
  if [ "$bad" -eq 0 ]; then
    printf '✓ %-34s %s → %s\n' "$label" "$file" "$want"
    pass=$((pass + 1))
  else
    printf '✗ %-34s %s 命中 [%s]，期望 %s\n' "$label" "$file" "$nums" "$want"
    fail=$((fail + 1))
  fi
}

# ---- 反推真源 ----
table_count=$(grep -cE '^CREATE TABLE' deploy/sql/schema.sql)
subject_count=$(awk '
  /^INSERT INTO rk_subject /{ inblk = 1; next }
  inblk && /^\(/            { n++ }
  inblk && /;[[:space:]]*$/ { inblk = 0 }
  END                       { print n + 0 }
' deploy/sql/init-data.sql)

printf '反推真源：表数量 = %s（schema.sql）· 科目数 = %s（init-data.sql）\n\n' \
  "$table_count" "$subject_count"

printf -- '-- 表数量复写点（均应等于 %s）--\n' "$table_count"
assert_all "$table_count" docs/04-database.md       '[0-9]+ 张表'            '04 §4.1 分域总览 / §4.9 交付物表'
assert_all "$table_count" docs/09-deployment.md     '[0-9]+ 张表'            '09 §9.9 校验注释'
assert_all "$table_count" docs/10-roadmap.md        '[0-9]+ 张表'            '10 §10.3 交付物 + 验收'
assert_all "$table_count" deploy/sql/verify.sql     'table_count_期望[0-9]+' 'verify.sql 表数量断言别名'

printf -- '\n-- 科目数复写点（均应等于 %s）--\n' "$subject_count"
# 注意：模式必须钉在"说的是总数"的那句措辞上。docs/10 里另有"17 个科目的样例题"
# （P2 样例范围）与"3 个科目"（FULL 科目数），那是**别的事实**——用宽松的
# `[0-9]+ 个?科目` 会把它们一起匹配进来，得到假阳性失败。
assert_all "$subject_count" docs/01-requirements.md  '[0-9]+ 个软考科目'          '01 §1 G1'
assert_all "$subject_count" docs/04-database.md      '[0-9]+ 科目、卷面模板'      '04 §4.9 交付物表'
assert_all "$subject_count" docs/04-database.md      '全部 [0-9]+ 个科目'         '04 §4.8.2 时长约束'
assert_all "$subject_count" docs/09-deployment.md    '[0-9]+ 科目 \+'             '09 §9.9 校验注释'
assert_all "$subject_count" docs/10-roadmap.md       '[0-9]+ 科目 \+'             '10 §10.3 交付物'
assert_all "$subject_count" docs/10-roadmap.md       '[0-9]+ 个科目在考生端'      '10 §10.7 验收'
assert_all "$subject_count" docs/10-roadmap.md       'rk_subject` 有 [0-9]+ 行'    '10 §10.3 验收'
assert_all "$subject_count" deploy/sql/verify.sql    '科目数_期望[0-9]+'           'verify.sql 科目数断言别名'
assert_all "$subject_count" deploy/sql/init-data.sql '科目（[0-9]+ 条）'          'init-data.sql §4.8.1 标题'

printf '\n合计：%d 通过 / %d 失败\n' "$pass" "$fail"
[ "$fail" -eq 0 ] || exit 1
