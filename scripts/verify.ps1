# Trip Checker — local verify (M6 gate). Run from repo root in Android Studio Terminal.
$ErrorActionPreference = "Stop"
if (-not $env:JAVA_HOME) {
    $candidates = @(
        "$env:USERPROFILE\.jdks\jbr-21.0.6",
        "${env:ProgramFiles}\Android\Android Studio\jbr"
    )
    foreach ($c in $candidates) {
        if (Test-Path "$c\bin\java.exe") {
            $env:JAVA_HOME = $c
            break
        }
    }
}
if ($env:JAVA_HOME) {
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}
Write-Host "Using JAVA_HOME=$env:JAVA_HOME"
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Optional M6.3: enable release optimization in app/build.gradle.kts, then:"
Write-Host "  .\gradlew.bat assembleRelease"
