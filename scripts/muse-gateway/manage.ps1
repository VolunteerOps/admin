param(
  [ValidateSet('Start','Stop','Status','Connect','Disconnect')][string]$Action='Status',
  [switch]$LocalOnly,
  [switch]$StageOnly
)
$ErrorActionPreference='Stop'
$privateDir=Join-Path $env:LOCALAPPDATA 'ResQOps\MuseGateway'
$stateFile=Join-Path $privateDir 'state.json'
$repo=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$tunnelBinary=Join-Path $repo '.work\tools\cloudflared.exe'
$serverScript=Join-Path $PSScriptRoot 'server.mjs'
$awakeScript=Join-Path $PSScriptRoot 'awake.ps1'
$controlScript=Join-Path $PSScriptRoot 'control.mjs'
function Read-State { if(Test-Path -LiteralPath $stateFile){Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json} }
function Is-Ours($processId,$marker) {
  if(!$processId){return $false}
  $process=Get-CimInstance Win32_Process -Filter "ProcessId = $processId"
  return ($process -and $process.CommandLine -and $process.CommandLine.Contains($marker))
}
function Stop-Ours($state) {
  foreach($item in @(@($state.awakePid,$awakeScript),@($state.tunnelPid,$tunnelBinary),@($state.gatewayPid,$serverScript))) {
    if(Is-Ours $item[0] $item[1]){Stop-Process -Id $item[0] -ErrorAction SilentlyContinue}
  }
}
function Connect-Gateway($state) {
  if(!(Is-Ours $state.gatewayPid $serverScript) -or !(Is-Ours $state.tunnelPid $tunnelBinary)){throw 'Start the local gateway first.'}
  $mode=if($StageOnly){'stage'}else{'connect'}
  & node $controlScript $mode $privateDir (Get-Command railway.exe).Source $state.url
  if($LASTEXITCODE -ne 0){throw 'Gateway connection failed; the local processes remain available for retry.'}
}
$state=Read-State
if($Action -eq 'Status') {
  [PSCustomObject]@{GatewayRunning=(Is-Ours $state.gatewayPid $serverScript);TunnelRunning=(Is-Ours $state.tunnelPid $tunnelBinary);KeepAwakeRunning=(Is-Ours $state.awakePid $awakeScript);Url=$state.url;Logs=$privateDir}
  exit
}
if($Action -eq 'Disconnect') {
  & node $controlScript disconnect $privateDir (Get-Command railway.exe).Source
  if($LASTEXITCODE -ne 0){throw 'Railway disconnect failed.'}
  exit
}
if($Action -eq 'Stop'){Stop-Ours $state;Write-Output 'Local gateway stopped. Assistant calls will be unavailable until restarted or disconnected in Railway.';exit}
if($Action -eq 'Connect'){Connect-Gateway $state;exit}
if((Is-Ours $state.gatewayPid $serverScript) -and (Is-Ours $state.tunnelPid $tunnelBinary)) {
  if(!$LocalOnly){Connect-Gateway $state}else{Write-Output 'Local gateway is already running.'}
  exit
}
Stop-Ours $state
New-Item -ItemType Directory -Force -Path $privateDir | Out-Null
# Restrict credentials and logs to this Windows account and SYSTEM before writing secrets.
$acl=[System.IO.Directory]::GetAccessControl($privateDir,[System.Security.AccessControl.AccessControlSections]::Access)
$acl.SetAccessRuleProtection($true,$false)
$account=[System.Security.Principal.WindowsIdentity]::GetCurrent().User
foreach($existing in @($acl.Access)){$acl.RemoveAccessRuleSpecific($existing)}
foreach($identity in @($account,(New-Object System.Security.Principal.SecurityIdentifier('S-1-5-18')))) {
  $rule=New-Object System.Security.AccessControl.FileSystemAccessRule($identity,'FullControl','ContainerInherit,ObjectInherit','None','Allow')
  $acl.AddAccessRule($rule)
}
[System.IO.Directory]::SetAccessControl($privateDir,$acl)
if(!(Test-Path -LiteralPath $tunnelBinary)){throw 'Install the verified cloudflared binary under .work\tools first; see docs/MUSE_LOCAL_GATEWAY.md.'}
if((Get-FileHash -LiteralPath $tunnelBinary -Algorithm SHA256).Hash -ne '2493dbc168923bb3e59e5e7716f71deced92992eea9acd6e9d74f84ac7751343'){throw 'The cloudflared checksum does not match the pinned release.'}
& node $controlScript prepare $privateDir (Get-Command railway.exe).Source
if($LASTEXITCODE -ne 0){throw 'Could not prepare gateway credentials.'}
$state=[PSCustomObject]@{gatewayPid=0;tunnelPid=0;awakePid=0;url='';started=(Get-Date).ToUniversalTime().ToString('o')}
try {
  $gateway=Start-Process -FilePath (Get-Command node.exe).Source -ArgumentList @(('"'+$serverScript+'"'),('"'+(Join-Path $privateDir 'credentials.json')+'"')) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $privateDir 'gateway.log') -RedirectStandardError (Join-Path $privateDir 'gateway-error.log')
  $state.gatewayPid=$gateway.Id
  Start-Sleep -Seconds 2
  if($gateway.HasExited){throw 'The local gateway did not start; check gateway-error.log.'}
  $ready=Invoke-RestMethod 'http://127.0.0.1:4187/health' -TimeoutSec 5
  if($ready.status -ne 'ready'){throw 'Local health check failed.'}
  $tunnel=Start-Process -FilePath $tunnelBinary -ArgumentList @('tunnel','--no-autoupdate','--url','http://127.0.0.1:4187','--protocol','http2','--metrics','127.0.0.1:4188','--loglevel','info') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $privateDir 'tunnel.log') -RedirectStandardError (Join-Path $privateDir 'tunnel-error.log')
  $state.tunnelPid=$tunnel.Id
  for($attempt=0;$attempt -lt 30;$attempt++) {
    Start-Sleep -Seconds 2
    $logs=(Get-Content -LiteralPath (Join-Path $privateDir 'tunnel-error.log') -Raw -ErrorAction SilentlyContinue)
    if($logs -match 'https://[a-z0-9-]+\.trycloudflare\.com'){$state.url=$Matches[0];break}
    if($tunnel.HasExited){throw 'The tunnel did not start; check tunnel-error.log.'}
  }
  if(!$state.url){throw 'The tunnel did not provide a URL within one minute.'}
  $awake=Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',('"'+$awakeScript+'"'),'-GatewayProcess',$gateway.Id) -WindowStyle Hidden -PassThru -RedirectStandardError (Join-Path $privateDir 'awake-error.log')
  $state.awakePid=$awake.Id
  $state | ConvertTo-Json | Set-Content -LiteralPath $stateFile -Encoding UTF8
  Write-Output ('Gateway running: '+$state.url)
}catch{Stop-Ours $state;throw}
if(!$LocalOnly){Connect-Gateway $state}
