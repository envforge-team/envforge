$ErrorActionPreference = "Continue"

$checks = @(
  @{ Name = "Git";       Command = "git";       Args = @("--version") },
  @{ Name = "Java";      Command = "java";      Args = @("-version") },
  @{ Name = "Maven";     Command = "mvn";       Args = @("-version") },
  @{ Name = "Node.js";   Command = "node";      Args = @("--version") },
  @{ Name = "npm";       Command = "npm";       Args = @("--version") },
  @{ Name = "Docker";    Command = "docker";    Args = @("--version") },
  @{ Name = "kubectl";   Command = "kubectl";   Args = @("version", "--client") },
  @{ Name = "Helm";      Command = "helm";      Args = @("version", "--short") },
  @{ Name = "Terraform"; Command = "terraform"; Args = @("version") },
  @{ Name = "Azure CLI"; Command = "az";        Args = @("version") }
)

$failed = $false
foreach ($check in $checks) {
  if (-not (Get-Command $check.Command -ErrorAction SilentlyContinue)) {
    Write-Host "[MISSING] $($check.Name)" -ForegroundColor Red
    $failed = $true
    continue
  }

  Write-Host "[OK] $($check.Name)" -ForegroundColor Green
  & $check.Command @($check.Args) 2>&1 | Select-Object -First 3
  Write-Host ""
}

if (Get-Command docker -ErrorAction SilentlyContinue) {
  docker info *> $null
  if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Docker engine is running" -ForegroundColor Green
  } else {
    Write-Host "[WARNING] Docker is installed but its engine is unavailable" -ForegroundColor Yellow
  }
}

if ($failed) { exit 1 }
