# Lanza el emulador y lo centra en pantalla automáticamente
# Uso: .\launch-emulator.ps1 [-ComPort COM8] [-BaudRate 115200] [-NoBridge]
param(
    [string]$ComPort  = "COM8",
    [int]   $BaudRate = 115200,
    [switch]$NoBridge
)

$emulator  = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
$avd       = "Medium_Phone_API_36"
$cameraArg = "-camera-back webcam0"   # Cambia a 'virtualscene' si no tienes webcam

# Tamaño deseado de la ventana del emulador (ajusta a tu gusto)
$winW = 420
$winH = 860

# ── Cargar Win32 API ──────────────────────────────────────────────────────────
Add-Type -AssemblyName System.Windows.Forms

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win32 {
    [DllImport("user32.dll")]
    public static extern bool MoveWindow(IntPtr hWnd, int X, int Y, int nWidth, int nHeight, bool bRepaint);
}
"@

# ── Calcular posición centrada ────────────────────────────────────────────────
$screen = [System.Windows.Forms.Screen]::PrimaryScreen.WorkingArea
$x = [int](($screen.Width  - $winW) / 2)
$y = [int](($screen.Height - $winH) / 2)

# ── Lanzar emulador ───────────────────────────────────────────────────────────
Write-Host "Iniciando emulador '$avd'..."
Start-Process $emulator -ArgumentList "-avd $avd $cameraArg -no-snapshot-load"

# ── Esperar a que aparezca la ventana (máx 60 s) ─────────────────────────────
$handle = [IntPtr]::Zero
$waited = 0
while ($handle -eq [IntPtr]::Zero -and $waited -lt 60) {
    Start-Sleep -Seconds 1
    $waited++
    $p = Get-Process -Name "qemu-system-x86_64","emulator","emulator-arm" -ErrorAction SilentlyContinue |
         Where-Object { $_.MainWindowHandle -ne [IntPtr]::Zero } |
         Select-Object -First 1
    if ($p) {
        $handle = $p.MainWindowHandle
    }
}

if ($handle -ne [IntPtr]::Zero) {
    # Reposicionar en bucle durante 15 s para anular la posición guardada del emulador
    Write-Host "Centrando ventana..."
    $end = (Get-Date).AddSeconds(15)
    while ((Get-Date) -lt $end) {
        [Win32]::MoveWindow($handle, $x, $y, $winW, $winH, $true) | Out-Null
        Start-Sleep -Milliseconds 400
    }
    Write-Host "Ventana fija en ($x, $y) con tamaño ${winW}x${winH}"
} else {
    Write-Host "No se pudo localizar la ventana del emulador."
}

# ── Lanzar puente COM → TCP ───────────────────────────────────────────────────
if (-not $NoBridge) {
    $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    Write-Host "Esperando arranque de Android para configurar adb reverse..."
    $booted = ""; $t = 0
    while ($booted -ne "1" -and $t -lt 120) {
        Start-Sleep -Seconds 4; $t += 4
        $booted = (& $adb shell getprop sys.boot_completed 2>&1).Trim()
    }
    & $adb reverse tcp:9876 tcp:9876 | Out-Null
    Write-Host "adb reverse tcp:9876 configurado."

    $bridgeScript = Join-Path $PSScriptRoot "bridge-com-tcp.ps1"
    Start-Process powershell -ArgumentList "-NoProfile -ExecutionPolicy Bypass -NoExit -Command `"& '$bridgeScript' -ComPort $ComPort -BaudRate $BaudRate`""
    Write-Host "Puente $ComPort @ $BaudRate bps lanzado en ventana separada."
}
