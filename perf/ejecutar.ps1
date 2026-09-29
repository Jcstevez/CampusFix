# Corre baseline, carga y estres. La app se reinicia entre escenarios (base H2 limpia).
Set-Location (Join-Path $PSScriptRoot "..")
New-Item -ItemType Directory -Force perf/reports | Out-Null
foreach ($esc in "baseline", "carga", "estres") {
    Write-Host "=== $esc ==="
    $app = Start-Process -FilePath "mvn.cmd" -ArgumentList "-q", "compile", "exec:java" `
        -RedirectStandardOutput "perf/reports/app-$esc.log" -RedirectStandardError "perf/reports/app-$esc.err.log" `
        -PassThru -WindowStyle Hidden
    for ($i = 0; $i -lt 60; $i++) {
        try { Invoke-RestMethod http://localhost:8080/salud | Out-Null; break } catch { Start-Sleep 1 }
    }
    k6 run --summary-export="perf/reports/$esc.json" "perf/scripts/$esc.js"
    taskkill /PID $app.Id /T /F | Out-Null
    Start-Sleep 2
}
Write-Host "Listo. Resumenes en perf/reports/*.md"
