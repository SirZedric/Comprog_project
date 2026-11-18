$ErrorActionPreference = 'Stop'
$quizRoot = Split-Path -Parent $PSScriptRoot
$quizWrapperPath = Join-Path $quizRoot 'gradle\wrapper\gradle-wrapper.jar'
$quizExpectedHash = '2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046'
$quizWrapperUrl = 'https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar'
$quizTempPath = $null

try {
    if (-not (Test-Path -LiteralPath $quizWrapperPath -PathType Leaf)) {
        Write-Host 'Downloading the official Gradle wrapper (first run only)...'
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $quizTempPath = "$quizWrapperPath.$([Guid]::NewGuid().ToString('N')).tmp"
        Invoke-WebRequest -Uri $quizWrapperUrl -OutFile $quizTempPath -UseBasicParsing
        $quizDownloadedHash = (Get-FileHash -LiteralPath $quizTempPath -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($quizDownloadedHash -ne $quizExpectedHash) {
            throw 'Gradle wrapper checksum failed. The downloaded file will not run.'
        }
        Move-Item -LiteralPath $quizTempPath -Destination $quizWrapperPath
    }

    $quizExistingHash = (Get-FileHash -LiteralPath $quizWrapperPath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($quizExistingHash -ne $quizExpectedHash) {
        throw 'Existing Gradle wrapper checksum failed. Remove the JAR and retry.'
    }
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
} finally {
    if ($quizTempPath -and (Test-Path -LiteralPath $quizTempPath)) {
        Remove-Item -LiteralPath $quizTempPath -Force
    }
}
