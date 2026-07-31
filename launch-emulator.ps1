# Lanza el emulador y lo centra en pantalla automáticamente.
# Por defecto TAMBIEN lanza el bridge COM -> TCP (puerto 9876).
# Uso: .\launch-emulator.ps1 [-ComPort COM3] [-BaudRate 115200] [-NoBridge] [-WindowPreset medium]
param(
    [string]$ComPort  = "COM3",
    [int]   $BaudRate = 115200,
    [switch]$NoBridge,
    [ValidateSet("compact", "medium", "large")]
    [string]$WindowPreset = "medium"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$emulator  = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
$avd       = "Medium_Phone_API_36"
$cameraArg = "-camera-back webcam0"   # Cambia a 'virtualscene' si no tienes webcam

function Resolve-AvdConfigPath {
    param([string]$AvdName)

    $avdRoot = Join-Path $env:USERPROFILE ".android\avd"
    $iniPath = Join-Path $avdRoot "$AvdName.ini"
    if (-not (Test-Path $iniPath)) {
        return $null
    }

    $pathLine = Get-Content $iniPath | Where-Object { $_ -match '^path=' } | Select-Object -First 1
    if (-not $pathLine) {
        return $null
    }

    $avdDir = ($pathLine -replace '^path=', '').Trim()
    $configPath = Join-Path $avdDir "config.ini"
    if (Test-Path $configPath) {
        return $configPath
    }

    return $null
}

function Get-AvdDisplaySpec {
    param([string]$AvdName)

    # Fallback razonable para no bloquear el lanzamiento.
    $spec = @{
        Width = 360
        Height = 640
        Density = 240
    }

    $configPath = Resolve-AvdConfigPath -AvdName $AvdName
    if (-not $configPath) {
        return $spec
    }

    $config = Get-Content $configPath
    $wLine = $config | Where-Object { $_ -match '^hw\.lcd\.width\s*=' } | Select-Object -First 1
    $hLine = $config | Where-Object { $_ -match '^hw\.lcd\.height\s*=' } | Select-Object -First 1
    $dLine = $config | Where-Object { $_ -match '^hw\.lcd\.density\s*=' } | Select-Object -First 1

    if ($wLine) {
        $parsed = 0
        if ([int]::TryParse(($wLine -replace '^hw\.lcd\.width\s*=\s*', '').Trim(), [ref]$parsed) -and $parsed -gt 0) {
            $spec.Width = $parsed
        }
    }
    if ($hLine) {
        $parsed = 0
        if ([int]::TryParse(($hLine -replace '^hw\.lcd\.height\s*=\s*', '').Trim(), [ref]$parsed) -and $parsed -gt 0) {
            $spec.Height = $parsed
        }
    }
    if ($dLine) {
        $parsed = 0
        if ([int]::TryParse(($dLine -replace '^hw\.lcd\.density\s*=\s*', '').Trim(), [ref]$parsed) -and $parsed -gt 0) {
            $spec.Density = $parsed
        }
    }

    return $spec
}

function Get-WindowSize {
    param(
        [hashtable]$Spec,
        [string]$Preset,
        [System.Drawing.Rectangle]$WorkingArea
    )

    # Factores pensados para emulador en escritorio (incluyendo decoraciones de ventana).
    $scaleByPreset = @{
        compact = 0.70
        medium  = 0.82
        large   = 0.92
    }

    $scale = $scaleByPreset[$Preset]
    $targetH = [int]([Math]::Round($WorkingArea.Height * $scale))

    # Reservar ancho según aspecto del display Android + margen por marco/controles.
    $aspect = [double]$Spec.Width / [double]$Spec.Height
    $targetW = [int]([Math]::Round(($targetH * $aspect) + 56))

    # Limitar para no desbordar la pantalla.
    $maxW = [Math]::Max(320, $WorkingArea.Width)
    $maxH = [Math]::Max(480, $WorkingArea.Height)
    $winW = [Math]::Min($targetW, $maxW)
    $winH = [Math]::Min($targetH, $maxH)

    return @{
        Width = $winW
        Height = $winH
    }
}

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

$screen = [System.Windows.Forms.Screen]::PrimaryScreen.WorkingArea
$displaySpec = Get-AvdDisplaySpec -AvdName $avd
$windowSize = Get-WindowSize -Spec $displaySpec -Preset $WindowPreset -WorkingArea $screen
$winW = [int]$windowSize.Width
$winH = [int]$windowSize.Height

$maxX = $screen.Right - $winW
$maxY = $screen.Bottom - $winH
$x = [int][Math]::Max($screen.Left, [Math]::Min(($screen.Left + [int](($screen.Width - $winW) / 2)), $maxX))
$y = [int][Math]::Max($screen.Top, [Math]::Min(($screen.Top + [int](($screen.Height - $winH) / 2)), $maxY))

Write-Host "Perfil AVD: $($displaySpec.Width)x$($displaySpec.Height) @ $($displaySpec.Density) dpi"
Write-Host "Preset ventana: $WindowPreset -> ${winW}x${winH} en area util $($screen.Width)x$($screen.Height)"

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
    Write-Host "Centrando ventana..."
    [Win32]::MoveWindow($handle, $x, $y, $winW, $winH, $true) | Out-Null
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
