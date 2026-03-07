# Juntos Local Run Script
# This script starts the backend (falcon-alpha) and frontend (falcon-web) concurrently.

$ErrorActionPreference = "Stop"

Write-Host "🏳️‍🌈 Starting Juntos locally..." -ForegroundColor Magenta

# 1. Check for Java 21
Write-Host "Checking for Java 21..." -ForegroundColor Cyan

$javaFound = $false

# Function to check if a java path is valid and version 21
function Test-JavaVersion($path) {
    if (Test-Path $path) {
        Write-Host "Testing Java path: $path" -ForegroundColor Gray
        try {
            $oldPreference = $ErrorActionPreference
            $ErrorActionPreference = "Continue"
            # java -version writes to stderr, so we redirect it
            $versionOutput = & $path -version 2>&1 | Out-String
            $ErrorActionPreference = $oldPreference
            
            if ($versionOutput -match "21") {
                return $true
            } else {
                $firstLine = (($versionOutput -split "`n")[0]).Trim()
                Write-Host "Version mismatch: Found $firstLine" -ForegroundColor Gray
            }
        } catch {
            Write-Host "Error executing $path : $($_.Exception.Message)" -ForegroundColor Gray
        }
    }
    return $false
}

# Try default path
if (Test-JavaVersion "java") {
    Write-Host "Java 21 detected in PATH." -ForegroundColor Green
    $javaFound = $true
} else {
    Write-Host "Java 21 not found in PATH. Searching common locations..." -ForegroundColor Yellow
    
    $commonPaths = @(
        "$env:JAVA_HOME\bin\java.exe",
        "C:\Program Files\Java\jdk-21\bin\java.exe"
    )

    # Specific discovery for the user's environment
    $discoveredJdks = Get-ChildItem -Path "C:\Users\Joao\.jdks", "C:\Program Files\Java" -Filter "*21*" -Directory -ErrorAction SilentlyContinue | ForEach-Object { Join-Path $_.FullName "bin\java.exe" }
    $commonPaths += $discoveredJdks

    foreach ($path in ($commonPaths | Select-Object -Unique)) {
        if ($null -eq $path -or $path -eq "") { continue }
        if (Test-JavaVersion $path) {
            Write-Host "Found Java 21 at: $path" -ForegroundColor Green
            $env:JAVA_HOME = [System.IO.Path]::GetDirectoryName([System.IO.Path]::GetDirectoryName($path))
            $env:PATH = "$([System.IO.Path]::GetDirectoryName($path));$env:PATH"
            $javaFound = $true
            break
        }
    }
}

if (-not $javaFound) {
    Write-Error "Java 21 is not installed or not in PATH. Juntos requires Java 21 (LTS)."
    exit 1
}

# 2. Check for Node.js
Write-Host "Checking Node.js..." -ForegroundColor Cyan
try {
    $nodeVersion = & node -v
    Write-Host "Node.js detected: $nodeVersion" -ForegroundColor Green
} catch {
    Write-Error "Node.js is not installed or not in PATH."
}

# 3. Build and Run Backend (juntos-alpha)
Write-Host "Starting Backend (juntos-alpha) on port 8080..." -ForegroundColor Yellow
$backendJob = Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd juntos-alpha; mvn spring-boot:run" -PassThru

# 4. Build and Run Frontend (juntos-web)
Write-Host "Starting Frontend (juntos-web) on port 5173..." -ForegroundColor Yellow
if (!(Test-Path "juntos-web\node_modules")) {
    Write-Host "Installing frontend dependencies..." -ForegroundColor Cyan
    Start-Process npm -ArgumentList "install" -WorkingDirectory "juntos-web" -Wait
}
$frontendJob = Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd juntos-web; npm run dev" -PassThru

Write-Host "`n✨ Juntos is launching!" -ForegroundColor Magenta
Write-Host "Backend: http://localhost:8080"
Write-Host "Frontend: http://localhost:5173"
Write-Host "Close the new terminal windows to stop the services."
