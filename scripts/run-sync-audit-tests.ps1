param([string]$TestClass = '', [switch]$EndToEnd)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$root = Split-Path $PSScriptRoot -Parent
$logs = Join-Path $root 'app\build\reports\sync-audit'
New-Item -ItemType Directory -Path $logs -Force | Out-Null
& $adb -s emulator-5554 install -r (Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk')
if ($LASTEXITCODE -ne 0) { throw 'App installation failed' }
& $adb -s emulator-5554 install -r (Join-Path $root 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk')
if ($LASTEXITCODE -ne 0) { throw 'Test installation failed' }
$arguments = @('-s', 'emulator-5554', 'shell', 'am', 'instrument', '-w', '-r')
if ($TestClass) { $arguments += @('-e', 'class', $TestClass) }
if ($EndToEnd) { $arguments += @('-e', 'syncTestUrl', 'http://10.0.2.2:3101/', '-e', 'syncTestIsolated', 'true') }
$arguments += 'com.example.basketballtracker.test/androidx.test.runner.AndroidJUnitRunner'
$result = & $adb @arguments
$filename = if ($TestClass) { ($TestClass.Split('.')[-1] + '.txt') } elseif ($EndToEnd) { 'all-end-to-end.txt' } else { 'instrumentation.txt' }
$result | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $logs $filename)
$result | Select-Object -Last 35 | Write-Output
if ($LASTEXITCODE -ne 0 -or ($result -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed')) {
    throw 'Instrumentation reported a failure; see app/build/reports/sync-audit/instrumentation.txt'
}
