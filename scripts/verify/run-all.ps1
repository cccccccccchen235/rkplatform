# ===============================================================
#  本地一键质量门禁（23 §4.2）
#
#  为什么需要它：本项目按 23 §1 的前提"在本地完成、不上传仓库"，
#  .github/workflows/consistency.yml 在本地一次都不会执行。没有 CI 就只剩
#  "人记得跑"，而本项目已两次吃过"自查按印象勾选"的亏（12 的两项假 ✅、
#  13 的批评）。这条命令让"跑完全部门禁"可复述、可交接。
#
#  ⚠️ 入口**自己处理 bash / mvn / java 的可达性**，不要求使用者先配 PATH——
#     否则它和 22 的文档犯同一个错（23 §2.1 实测：bash.exe 在 D:\Git\bin，
#     而 PATH 里当时只有 D:\Git\cmd）。
#
#  ⚠️ 门禁 1 与门禁 5 在本机**互斥**（实测，见 docs/24）：
#     门禁 1 跑 `mvn clean package`，要删/改 server/rk-boot/target/rk-boot.jar；
#     而运行中的后端 JVM 以"缺 FILE_SHARE_DELETE"的方式持有该 jar
#     （实测：改名 → 失败；追加写 → 成功）。于是后端在线时 clean 与 package
#     都会失败，而门禁 5 又要求后端在线。本入口**不静默降级**，而是把互斥
#     关系直接说清并给出两种跑法。
#
#  用法：
#     run-all.cmd            门禁 1~4（+ 门禁 5，若后端恰在线）
#     run-all.cmd -Gate5     只跑门禁 5（后端须在线）
#
#  想一次跑成全绿：让后端从 target/ 之外的 jar 副本启动（见 docs/24）。
#
#  实现注记：各门禁以**脚本块**而非命令字符串下发。曾经用
#  `Invoke-Expression "`"$exe`" args"`，PowerShell 会在"带引号的路径 + 参数"
#  上报 ParserError（实测三处全挂）——脚本块没有这个引号层级。
# ===============================================================
#requires -version 5.1
[CmdletBinding()]
param([switch]$Gate5)

$ErrorActionPreference = 'Continue'
$ProgressPreference    = 'SilentlyContinue'

# ---- 编码统一（重要）----
# 启动器已 `chcp 65001`。这里再让 .NET 侧按 UTF-8 解码子进程输出 / 写日志，
# 否则 PowerShell 5.1 会用控制台代码页（本机 GBK）去解 bash 的 UTF-8 输出，
# 日志里会出现 `鍙嶆帹鐪熸簮` 这类双重乱码（实测踩到）。
$utf8 = New-Object Text.UTF8Encoding($false)
[Console]::OutputEncoding = $utf8
$OutputEncoding           = $utf8

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location -LiteralPath $root

$logDir = Join-Path $env:TEMP ("rk-verify-" + (Get-Random) + (Get-Random))
New-Item -ItemType Directory -Path $logDir -Force | Out-Null

$script:Passed  = 0
$script:Failed  = 0
$script:Skipped = 0

# ---- 工具解析：不依赖调用者的 PATH ----
function Resolve-Tool([object[]]$Candidates) {
    foreach ($c in $Candidates) {
        if ($c -and (Test-Path -LiteralPath $c)) { return $c }
    }
    return $null
}

$bash = Resolve-Tool @(
    'D:\Git\bin\bash.exe',
    'D:\Git\usr\bin\bash.exe',
    (Join-Path $env:ProgramFiles 'Git\bin\bash.exe'),
    "${env:ProgramFiles(x86)}\Git\bin\bash.exe",
    (Get-Command bash.exe -ErrorAction SilentlyContinue).Source
)
$mvn = Resolve-Tool @(
    $(if ($env:MAVEN_HOME) { Join-Path $env:MAVEN_HOME 'bin\mvn.cmd' }),
    'D:\apache-maven\bin\mvn.cmd',
    (Get-Command mvn.cmd -ErrorAction SilentlyContinue).Source
)
if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'D:\jdk21')) { $env:JAVA_HOME = 'D:\jdk21' }

# ---- 环境探测 ----
$jar = Join-Path $root 'server\rk-boot\target\rk-boot.jar'
$jarLocked = $false
if (Test-Path -LiteralPath $jar) {
    try { [IO.File]::Open($jar, 'Open', 'ReadWrite', 'None').Close() }
    catch { $jarLocked = $true }
}
$backendUp = $false
foreach ($p in 8080, 8081) {
    if (-not $backendUp) {
        try { $c = New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1', $p); $c.Close(); $backendUp = $true } catch { }
    }
}

$mode = if ($Gate5) { '仅门禁 5' } else { '门禁 1~4(+5)' }
Write-Host '==============================================================='
Write-Host (" 软考平台 · 本地质量门禁（23 §4.2）        模式：" + $mode)
Write-Host (" 项目根：" + $root)
Write-Host (" 日志：" + $logDir)
Write-Host '---------------------------------------------------------------'
Write-Host (" bash      : " + $(if ($bash) { $bash } else { '**未找到**' }))
Write-Host (" mvn       : " + $(if ($mvn)  { $mvn }  else { '**未找到**' }))
Write-Host (" JAVA_HOME : " + $(if ($env:JAVA_HOME) { $env:JAVA_HOME } else { '**未设置**' }))
Write-Host (" jar 被占用 : " + $jarLocked)
Write-Host (" 后端在线   : " + $backendUp)
Write-Host '==============================================================='

if (-not $bash) {
    Write-Host ''
    Write-Host '[失败] 找不到 bash.exe（试过 D:\Git\bin、D:\Git\usr\bin、Program Files、PATH）。'
    Write-Host '       门禁 3/4 无法执行。安装 Git for Windows 后重试。'
    $script:Failed++
}
if (-not $mvn) {
    Write-Host ''
    Write-Host '[失败] 找不到 mvn.cmd（试过 $env:MAVEN_HOME\bin、D:\apache-maven\bin、PATH）。'
    Write-Host '       门禁 1 无法执行。'
    $script:Failed++
}

function Invoke-Gate {
    param(
        [string]    $Label,
        [string]    $Dir,
        [scriptblock] $Action,
        [string]    $Log
    )
    Write-Host ''
    Write-Host ("=== " + $Label + " ===")
    Remove-Item -LiteralPath $Log -Force -ErrorAction SilentlyContinue
    Push-Location -LiteralPath $Dir
    $code = 1
    $out  = ''
    try {
        $out  = & $Action 2>&1 | Out-String
        $code = if ($null -eq $LASTEXITCODE) { 0 } else { $LASTEXITCODE }
    }
    catch {
        $out  = ($_ | Out-String)
        $code = 1
    }
    finally { Pop-Location }
    # 明确按 UTF-8 落盘（`>>` 在 PS 5.1 用的是 Out-File 默认编码，不可依赖）
    [IO.File]::WriteAllText($Log, $out, $utf8)

    if ($code -eq 0) {
        Write-Host ("  [通过] " + $Label)
        $script:Passed++
    }
    else {
        Write-Host ("  [失败] " + $Label + "  (exit " + $code + ")")
        Write-Host ("  ---- 日志末尾 40 行（全文见 " + $Log + "）----")
        Get-Content -LiteralPath $Log -Encoding UTF8 -Tail 40 -ErrorAction SilentlyContinue |
            ForEach-Object { Write-Host ("  " + $_) }
        $script:Failed++
    }
}

if (-not $Gate5) {

    # ---- 1. 后端：编译 + 单测 + enforcer 分层约束 ----
    if ($mvn) {
        if ($jarLocked) {
            Write-Host ''
            Write-Host '=== 1/5 后端 mvn -q clean package（编译+单测+enforcer）==='
            Write-Host '  [未验证] 后端正在运行并锁定 target\rk-boot.jar，'
            Write-Host '           `mvn clean` 删不掉它（Windows 文件锁，缺 FILE_SHARE_DELETE）。'
            Write-Host '           两种跑法：'
            Write-Host '             a) 停掉后端 → 重跑本入口（门禁 1~4 会绿）→ 启动后端 → run-all.cmd -Gate5'
            Write-Host '             b) 让后端从 target/ 之外的 jar 副本启动（见 docs/24），本入口即可一次全绿'
            $script:Skipped++
        }
        else {
            Invoke-Gate '1/5 后端 mvn -q clean package（编译+单测+enforcer）' `
                        (Join-Path $root 'server') `
                        { & $mvn '-q' 'clean' 'package' } `
                        (Join-Path $logDir '1-backend.log')
        }
    }

    # ---- 2. 前端：lint + type-check + build（失败即停）----
    Invoke-Gate '2/5 前端 pnpm lint && type-check && build' `
                (Join-Path $root 'web') `
                {
                    pnpm lint
                    if ($LASTEXITCODE -ne 0) { return }
                    pnpm type-check
                    if ($LASTEXITCODE -ne 0) { return }
                    pnpm build
                } `
                (Join-Path $logDir '2-web.log')

    # ---- 3. 数值一致性（13 处复写点）----
    if ($bash) {
        Invoke-Gate '3/5 数值一致性 check-numeric-consistency.sh' `
                    $root `
                    { & $bash 'scripts/verify/check-numeric-consistency.sh' } `
                    (Join-Path $logDir '3-numeric.log')
    }

    # ---- 4. GR-1 语料目录纯净 ----
    if ($bash) {
        Invoke-Gate '4/5 GR-1 check-no-corpus-files.sh' `
                    $root `
                    { & $bash 'scripts/verify/check-no-corpus-files.sh' } `
                    (Join-Path $logDir '4-corpus.log')
    }
}

# ---- 5. 真 HTTP 鉴权回路（需后端在 8080/8081 + Redis 带口令）----
if ($backendUp) {
    Invoke-Gate '5/5 鉴权回路 auth-chain.js' `
                $root `
                { node 'scripts/verify/auth-chain.js' } `
                (Join-Path $logDir '5-auth.log')
}
else {
    Write-Host ''
    Write-Host '=== 5/5 鉴权回路 auth-chain.js ==='
    Write-Host '  [未验证] 后端未在 8080/8081 监听。'
    Write-Host '           门禁 5 需要 MySQL + Redis + 后端进程同时在线（23 §4.2）。'
    Write-Host '           启动后端后单独跑：scripts\verify\run-all.cmd -Gate5'
    $script:Skipped++
}

# ---- 汇总 ----
Write-Host ''
Write-Host '==============================================================='
Write-Host (" 汇总：" + $script:Passed + " 通过 / " + $script:Failed + " 失败 / " + $script:Skipped + " 未验证")
Write-Host (" 日志目录：" + $logDir)
Write-Host '==============================================================='

if ($script:Failed -ne 0) {
    Write-Host ' 结果：**未通过**（失败项见上）'
    exit 1
}
if ($script:Skipped -ne 0) {
    Write-Host (" 结果：已跑项全绿，但有 " + $script:Skipped + " 项**未验证**（见上）——不要当成全绿。")
    exit 1
}
Write-Host ' 结果：全绿（五类门禁均已实跑）'
exit 0
