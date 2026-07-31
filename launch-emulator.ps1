# Lanza el emulador y lo centra en pantalla automáticamente.
# Por defecto TAMBIEN lanza el bridge COM -> TCP (puerto 9876).
# Uso: .\launch-emulator.ps1 [-ComPort COM3] [-BaudRate 115200] [-NoBridge]
param(
    [string]$ComPort  = "COM3",
    [int]   $BaudRate = 115200,
    [switch]$NoBridge
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

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

# ── Esperar a que aparezca la ventana (máx 30 s) ─────────────────────────────
$handle = [IntPtr]::Zero
$waited = 0
while ($handle -eq [IntPtr]::Zero -and $waited -lt 30) {
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
    # Reposicionar en bucle durante 10 s para anular la posición guardada del emulador
    Write-Host "Centrando ventana..."
    $end = (Get-Date).AddSeconds(10)
    while ((Get-Date) -lt $end) {
        [Win32]::MoveWindow($handle, $x, $y, $winW, $winH, $true) | Out-Null
        Start-Sleep -Milliseconds 400
    }
    Write-Host "Ventana fija en ($x, $y) con tamaño ${winW}x${winH}"
} else {
    Write-Host "No se pudo localizar la ventana del emulador."
}

function Resolve-AdbPath {
    $candidates = @()

    if ($env:ANDROID_HOME) {
        $candidates += (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe")
    }
    if ($env:ANDROID_SDK_ROOT) {
        $candidates += (Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe")
    }
    if ($env:LOCALAPPDATA) {
        $candidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe")
    }

    $localProps = Join-Path $PSScriptRoot "local.properties"
    if (Test-Path $localProps) {
        $sdkLine = Get-Content $localProps | Where-Object { $_ -match '^sdk.dir=' } | Select-Object -First 1
        if ($sdkLine) {
            $raw = ($sdkLine -replace '^sdk.dir=', '').Trim()
            # local.properties guarda rutas escapadas, p.ej. C\:\\Users\\...
            $sdkDir = $raw -replace '\\\\', '\\'
            $sdkDir = $sdkDir -replace '^([A-Za-z])\\:', '$1:'
            $candidates += (Join-Path $sdkDir "platform-tools\adb.exe")
        }
    }

    $uniqueCandidates = $candidates | Where-Object { $_ } | Select-Object -Unique
    foreach ($candidate in $uniqueCandidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    return $null
}

# ── Lanzar puente COM → TCP ───────────────────────────────────────────────────
if (-not $NoBridge) {
    Write-Host "Bridge TCP: ACTIVO (modo por defecto)." -ForegroundColor Green

    $adb = Resolve-AdbPath
    if (-not $adb) {
        Write-Host "No se pudo localizar adb.exe automaticamente." -ForegroundColor Red
        Write-Host "Define ANDROID_HOME/ANDROID_SDK_ROOT o revisa local.properties." -ForegroundColor Yellow
    } else {
        Write-Host "adb encontrado en: $adb"
        Write-Host "Esperando arranque de Android para configurar adb reverse..."

        & $adb start-server | Out-Null
        & $adb wait-for-device | Out-Null

        $booted = ""
        $elapsed = 0
        while ($booted -ne "1" -and $elapsed -lt 120) {
            Start-Sleep -Seconds 2
            $elapsed += 2
            $bootedRaw = & $adb shell getprop sys.boot_completed 2>&1
            $booted = (($bootedRaw | Out-String).Trim())
        }

        & $adb reverse tcp:9876 tcp:9876 | Out-Null
        Write-Host "adb reverse tcp:9876 configurado." -ForegroundColor Green
    }

    $bridgeScript = Join-Path $PSScriptRoot "bridge-com-tcp.ps1"
    if (Test-Path $bridgeScript) {
        Start-Process powershell -ArgumentList "-NoProfile -ExecutionPolicy Bypass -NoExit -File `"$bridgeScript`" -ComPort `"$ComPort`" -BaudRate $BaudRate"
        Write-Host "Puente $ComPort @ $BaudRate bps lanzado en ventana separada." -ForegroundColor Green
    } else {
        Write-Host "No se encontro el script del puente: $bridgeScript" -ForegroundColor Red
    }
} else {
    Write-Host "Bridge TCP: DESACTIVADO por parametro -NoBridge." -ForegroundColor Yellow
    Write-Host "Modo -NoBridge activo: se inicia solo el emulador (sin puente TCP)." -ForegroundColor Yellow
}
