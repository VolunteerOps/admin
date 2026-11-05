param([Parameter(Mandatory=$true)][int]$GatewayProcess)
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class ResQAwake {
  [DllImport("kernel32.dll")] public static extern uint SetThreadExecutionState(uint flags);
}
'@
try {
  # Prevent automatic system sleep while the gateway exists; allow the screen to turn off.
  [ResQAwake]::SetThreadExecutionState(2147483649) | Out-Null
  while(Get-Process -Id $GatewayProcess -ErrorAction SilentlyContinue) { Start-Sleep -Seconds 15 }
} finally { [ResQAwake]::SetThreadExecutionState(2147483648) | Out-Null }
