# JavaWeb 电商系统 - 一键停止 PowerShell 脚本
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "          JavaWeb 电商系统 - 一键停止服务             " -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host ""

function Stop-PortProcess([int]$port, [string]$name) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($connections) {
        foreach ($conn in $connections) {
            $pidToKill = $conn.OwningProcess
            if ($pidToKill -gt 0) {
                Write-Host "正在终止 $name (端口 $port, PID: $pidToKill)..." -ForegroundColor Yellow
                Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
            }
        }
        Write-Host "[成功] $name (端口 $port) 已停止" -ForegroundColor Green
    } else {
        Write-Host "[信息] 端口 $port 无运行中的 $name 进程" -ForegroundColor DarkGray
    }
}

Stop-PortProcess 8080 "后端 Spring Boot 服务"
Stop-PortProcess 5173 "前端 Vite 工程"

Write-Host ""
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "[成功] 所有前后台开发服务已完全停止！" -ForegroundColor Green
Write-Host "=======================================================" -ForegroundColor Cyan
Start-Sleep -Seconds 2
