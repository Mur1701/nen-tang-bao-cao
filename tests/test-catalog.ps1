# Run after backend starts on port 8081. No database access necessary for catalog tests.
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8081'
$items = Invoke-RestMethod "$base/api/reports"
if ($items.Count -ne 12) { throw "Expected 12 reports, got $($items.Count)" }
if ($items[0].code -ne 'I.1' -or $items[11].code -ne 'II.5') {
    throw 'Report IDs or ordering do not match Phase 1.'
}
if ((Invoke-RestMethod "$base/api/reports/I.1").status -ne 'PREVIEW') {
    throw 'I.1 metadata incorrect'
}
Write-Host 'PASS: 12 report definitions + I.1 metadata' -ForegroundColor Green
# Manual: POST /api/reports/I.1/run should give 503 while execution-enabled=false.
