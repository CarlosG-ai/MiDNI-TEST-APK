<#
.SYNOPSIS
    Inyecta un payload QR de prueba al emulador Android via TCP (sin COM fisico).

.DESCRIPTION
    Interpreta el archivo fixture qr_dump_01.txt (formato hex dump), convierte
    los bytes y los envía al emulador a través del socket TCP.

    Flujo:
      1. adb reverse tcp:9876 tcp:9876    (una vez)
      2. En la app: "Puerto serie" -> "Simulacion TCP puerto 9876"
      3. Ejecutar ESTE script para inyectar el payload

.PARAMETER FixtureFile
    Ruta al archivo hex-dump de referencia. Por defecto: el qr_dump_01.txt
    incluido en el repositorio.

.PARAMETER TcpPort
    Puerto TCP (debe coincidir con el de la app). Por defecto: 9876.
#>
param(
    [string]$FixtureFile = "$PSScriptRoot\midni-qr-spec\code\fixtures\qr_dump_01.txt",
    [int]   $TcpPort     = 9876
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ── 1. Parsear el hex dump ────────────────────────────────────────────────
if (-not (Test-Path $FixtureFile)) {
    Write-Error "No se encuentra el fixture: $FixtureFile"
    exit 1
}

$byteList = [System.Collections.Generic.List[byte]]::new()

foreach ($line in Get-Content $FixtureFile) {
    # Cada linea tiene formato: "AAAA - XX XX XX ...    texto"
    # Extraemos los bytes en hexadecimal (grupos de 2 dígitos separados por espacio)
    if ($line -match '^\s*[0-9a-fA-F]{4}\s*-\s*(.+?)\s{2,}') {
        $hexPart = $Matches[1]
        foreach ($token in $hexPart -split '\s+') {
            if ($token -match '^[0-9a-fA-F]{2}$') {
                $byteList.Add([Convert]::ToByte($token, 16))
            }
        }
    }
}

$payload = $byteList.ToArray()
Write-Host "Payload leido: $($payload.Length) bytes" -ForegroundColor Cyan
Write-Host ("Primeros 8 bytes: " + (($payload | Select-Object -First 8 | ForEach-Object { $_.ToString("X2") }) -join " "))

# ── 2. Conectar al socket TCP ─────────────────────────────────────────────
Write-Host ""
Write-Host "Asegurese de que la app ya esta esperando (ha pulsado el boton TCP)." -ForegroundColor Yellow
Write-Host "Conectando a localhost:$TcpPort ..."

$client = New-Object System.Net.Sockets.TcpClient
try {
    $client.Connect("localhost", $TcpPort)
} catch {
    Write-Error "No se pudo conectar a localhost:$TcpPort.`n¿Ejecuto 'adb reverse tcp:$TcpPort tcp:$TcpPort'?`n¿Esta la app esperando datos TCP?"
    exit 1
}

Write-Host "Conectado!" -ForegroundColor Green

$stream = $client.GetStream()

# ── 3. Enviar payload ─────────────────────────────────────────────────────
# Pequeña pausa para que la app registre la conexion antes de recibir datos
Start-Sleep -Milliseconds 200

$stream.Write($payload, 0, $payload.Length)
$stream.Flush()

Write-Host "Payload enviado ($($payload.Length) bytes)." -ForegroundColor Green
Write-Host "La app deberia mostrar el preview en 300 ms."

# Mantener abierto 1 segundo para que la app reciba todos los datos
Start-Sleep -Seconds 1

$stream.Close()
$client.Close()
Write-Host "Conexion cerrada." -ForegroundColor Cyan
