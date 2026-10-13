$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw "jpackage не найден. Запустите сценарий с JDK 24, а не с JRE."
}

& .\mvnw.cmd --batch-mode --no-transfer-progress clean verify package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$inputDir = Join-Path $PWD "target\release-input"
$outputDir = Join-Path $PWD "target\distribution"
Remove-Item $inputDir, $outputDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $inputDir, $outputDir | Out-Null

Copy-Item "target\dde-solver-1.0.0.jar" $inputDir
& .\mvnw.cmd --batch-mode --no-transfer-progress dependency:copy-dependencies `
    "-DincludeScope=runtime" "-DoutputDirectory=$inputDir"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& jpackage `
    --type app-image `
    --name "DDE Solver" `
    --app-version "1.0.0" `
    --vendor "Ylhh17" `
    --description "Численное решение дифференциальных уравнений с запаздыванием" `
    --input $inputDir `
    --dest $outputDir `
    --main-jar "dde-solver-1.0.0.jar" `
    --main-class "dde.AppLauncher" `
    --java-options "--enable-native-access=javafx.graphics"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$archive = Join-Path $outputDir "DDE-Solver-1.0.0-windows-x64.zip"
Compress-Archive -Path (Join-Path $outputDir "DDE Solver") -DestinationPath $archive -Force
Write-Host "Готово: $archive"
