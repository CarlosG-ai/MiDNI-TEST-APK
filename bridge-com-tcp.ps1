<#
.SYNOPSIS
    Puente COM → TCP para testear la lectura serie en el emulador Android.

.DESCRIPTION
    1. Inicia un servidor TCP en el puerto indicado (por defecto 9876).
    2. Espera a que la app Android se conecte (via 'adb reverse').
    3. Abre el puerto COM y reenvía todos los bytes recibidos al socket TCP.

.EJEMPLO
    # Paso 1: configurar adb reverse
    adb reverse tcp:9876 tcp:9876

    # Paso 2: ejecutar este script (en otra ventana PowerShell)
    .\bridge-com-tcp.ps1 -ComPort COM7 -BaudRate 9600

    # Paso 3: en la app Android, pulsar "Leer QR MIDNI con puerto serie virtual USB"
    #         y seleccionar "[Simulacion TCP puerto 9876 — emulador/debug]"

.PARAMETER ComPort
    Puerto serie del host (por defecto "COM3").

.PARAMETER BaudRate
    Velocidad del puerto serie (por defecto 9600).

.PARAMETER TcpPort
    Puerto TCP local en el que escucha el puente (por defecto 9876).
#>
param(
    [string]$ComPort  = "COM3",
    [int]   $BaudRate = 9600,
    [int]   $TcpPort  = 9876
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "=== Bridge COM -> TCP ===" -ForegroundColor Cyan
Write-Host "Puerto serie : $ComPort @ $BaudRate bps"
Write-Host "Puerto TCP   : $TcpPort"
Write-Host ""
Write-Host "Asegurese de haber ejecutado antes:" -ForegroundColor Yellow
Write-Host "  adb reverse tcp:$TcpPort tcp:$TcpPort" -ForegroundColor Yellow
Write-Host ""

# ── Iniciar servidor TCP ───────────────────────────────────────────────────
$listener = [System.Net.Sockets.TcpListener]::new(
    [System.Net.IPAddress]::Loopback, $TcpPort)
$listener.Start()
Write-Host "Servidor TCP escuchando en localhost:$TcpPort ..." -ForegroundColor Green
Write-Host "Ahora pulse el boton de la app para conectarse."

try {
    # Bloquea hasta que el Android se conecte
    $client = $listener.AcceptTcpClient()
    Write-Host "Android conectado!" -ForegroundColor Green

    $stream = $client.GetStream()

    # ── Abrir puerto serie ─────────────────────────────────────────────────
    Write-Host "Abriendo $ComPort @ $BaudRate bps ..."
    $com = New-Object System.IO.Ports.SerialPort(
        $ComPort, $BaudRate,
        [System.IO.Ports.Parity]::None,
        8,
        [System.IO.Ports.StopBits]::One)
    $com.ReadTimeout  = 500   # ms; permite comprobar Ctrl+C periodicamente
    $com.WriteTimeout = 1000
    $com.Open()
    Write-Host "Puerto serie abierto. Redirigiendo datos a Android..." -ForegroundColor Green
    Write-Host "(Ctrl+C para detener)" -ForegroundColor Gray

    $buf = New-Object byte[] 4096
    $totalBytes = 0

    try {
        while ($true) {
            try {
                $n = $com.BaseStream.Read($buf, 0, $buf.Length)
                if ($n -gt 0) {
                    $stream.Write($buf, 0, $n)
                    $stream.Flush()
                    $totalBytes += $n
                    $hex = ($buf[0..([Math]::Min($n,16)-1)] | ForEach-Object { $_.ToString("X2") }) -join " "
                    Write-Host "$n bytes  ->  $hex ..." -ForegroundColor DarkCyan
                }
            }
            catch [System.TimeoutException] {
                # Sin datos en este ciclo; sigue esperando
            }

            # Comprobar si el socket Android sigue abierto
            if (-not $client.Connected) {
                Write-Host "Android desconectado." -ForegroundColor Yellow
                break
            }
        }
    }
    finally {
        $com.Close()
        Write-Host "Puerto $ComPort cerrado. Total bytes enviados: $totalBytes"
    }
}
finally {
    try { $client.Close()   } catch {}
    try { $listener.Stop()  } catch {}
    Write-Host "Servidor TCP cerrado." -ForegroundColor Cyan
}
