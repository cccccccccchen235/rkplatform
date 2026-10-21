#!/usr/bin/env bash
# GR-1 目录纯净断言（08 §8.4.4 / §8.10）：scripts/generate/ 下不得出现真题语料文件。
# 与 .gitignore「题库语料」组同一口径——那边防止提交，这边防止构建通过。
#
# 本地运行：bash scripts/verify/check-no-corpus-files.sh

set -uo pipefail

cd "$(dirname "$0")/../.." || exit 2

if [ ! -d scripts/generate ]; then
  echo '✓ scripts/generate/ 尚未交付——无语料文件'
  exit 0
fi

hits=$(find scripts/generate -type f \
  \( -name '*.pdf' -o -name '*.docx' -o -name '*.doc' -o -name '*.txt' -o -name '*.csv' \) -print)

if [ -n "$hits" ]; then
  echo '✗ GR-1 违规：scripts/generate/ 下出现语料文件（命中即构建失败，08 §8.4.4）：'
  printf '%s\n' "$hits"
  exit 1
fi

echo '✓ GR-1 目录纯净'
