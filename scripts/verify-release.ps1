$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)
& .\mvnw.cmd --batch-mode --no-transfer-progress clean verify
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (git status --porcelain) {
    Write-Warning "В рабочем дереве есть незакоммиченные изменения."
    git status --short
}
Write-Host "Релизная проверка завершена успешно."
