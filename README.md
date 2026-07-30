# MiDNI QR Verifier (Android 7+)

Aplicacion Android para lectura y validacion de codigos QR de MiDNI.

El proyecto implementa validacion estructural y criptografica del contenido QR,
siguiendo la especificacion incluida en el repositorio:

- `midni-qr-spec/MiDNI-FormatoQR_v107_sc_PN.md`

---

## 1) Descripcion general del proyecto

MiDNI QR Verifier es una app Android orientada a pruebas funcionales de lectura
y verificacion de QR MiDNI en distintos escenarios:

- Escaneo por camara (ZXing Embedded).
- Lectura por dispositivo HID USB (lector que emula teclado).
- Lectura por puerto serie USB (USB Host con `usb-serial-for-android`).
- Simulacion por TCP en emulador Android para pruebas sin hardware fisico.

La app parsea el payload binario del QR, extrae cabecera y TLVs, resuelve el
certificado de verificacion desde un almacén local, valida firma ECDSA y muestra
un resumen para usuario junto con datos personales cuando la validacion es correcta.

---

## 2) Stack tecnologico utilizado

### Plataforma y lenguaje

- Android (App module Gradle)
- Kotlin
- Java 17 (source/target compatibility)

### Build system

- Gradle Wrapper (`gradlew.bat`)
- Android Gradle Plugin `9.2.0`

### Librerias principales

- `com.journeyapps:zxing-android-embedded:4.3.0`
- `com.google.zxing:core:3.5.3`
- `com.github.mik3y:usb-serial-for-android:3.7.3`
- `org.bouncycastle:bcprov-jdk18on:1.78.1`
- `org.bouncycastle:bcpkix-jdk18on:1.78.1`
- AndroidX (`core-ktx`, `appcompat`, `material`, `constraintlayout`)

### Testing

- JUnit 4
- Mockito + Mockito Kotlin
- ZXing JavaSE para lectura de imagenes QR en tests

---

## 3) Instalacion y ejecucion

### Requisitos previos

- Windows con PowerShell (scripts `.ps1` incluidos)
- Android Studio (version reciente compatible con AGP 9.2.0)
- Android SDK con plataforma `API 35`
- JDK 17 configurado en Android Studio/Gradle
- (Opcional) Emulador Android + `adb`

### Clonar y abrir el proyecto

1. Clonar el repositorio.
2. Abrir la carpeta raiz en Android Studio.
3. Esperar sincronizacion de Gradle y descarga de dependencias.

### Compilar por linea de comandos

En la raiz del proyecto:

```powershell
.\gradlew.bat clean assembleDebug
```

APK debug generada en:

- `app/build/outputs/apk/debug/app-debug.apk`

### Ejecutar tests unitarios

```powershell
.\gradlew.bat testDebugUnitTest
```

### Ejecutar desde Android Studio

1. Seleccionar dispositivo fisico o emulador.
2. Ejecutar la configuracion `app` (Run).
3. Probar alguno de los flujos de entrada: camara, HID o serie.

### Build release

Existe configuracion release en `app/build.gradle` con firma definida en
`signingConfigs.release`.

Para generar release:

```powershell
.\gradlew.bat assembleRelease
```

> Nota: el proyecto incrementa automaticamente `buildNumber` en
> `version.properties` tras empaquetar `packageRelease`.

### Scripts de soporte (emulador y simulacion)

- `launch-emulator.ps1`
   - Lanza emulador, centra ventana y opcionalmente configura `adb reverse` y
      abre puente COM->TCP.
- `bridge-com-tcp.ps1`
   - Abre un puerto COM en host y reenvia bytes a `localhost:9876`.
- `test-inject-payload.ps1`
   - Inyecta payload de fixture QR al socket TCP para pruebas automatizadas/manuales.

Ejemplo de flujo de simulacion en emulador:

1. Iniciar emulador y puente:

```powershell
.\launch-emulator.ps1 -ComPort COM8 -BaudRate 115200
```

2. En la app: abrir modo serie y seleccionar simulacion TCP.
3. Inyectar payload de prueba:

```powershell
.\test-inject-payload.ps1
```

---

## 4) Estructura del proyecto

```text
.
|-- app/
|   |-- src/main/
|   |   |-- java/es/gob/midni/qrdemo/
|   |   |   |-- MainActivity.kt
|   |   |   |-- MidniQrParser.kt
|   |   |   |-- MidniQrVerifier.kt
|   |   |   |-- VerificationCertStore.kt
|   |   |   |-- C40.kt
|   |   |   `-- AnyOrientationCaptureActivity.kt
|   |   |-- assets/
|   |   |   `-- verification_certs.json
|   |   `-- res/
|   `-- src/test/java/es/gob/midni/qrdemo/
|       |-- MidniQrParserTest.kt
|       `-- MidniQrVerifierTest.kt
|-- midni-qr-spec/
|   |-- MiDNI-FormatoQR_v107_sc_PN.md
|   `-- assets/qr-ejemplos/
|-- launch-emulator.ps1
|-- bridge-com-tcp.ps1
|-- test-inject-payload.ps1
|-- build.gradle
|-- settings.gradle
`-- version.properties
```

### Componentes clave

- `MainActivity.kt`: flujo UI, permisos, escaneo QR, lectura HID y serie/TCP.
- `MidniQrParser.kt`: parser de estructura VDS, cabecera C40 y TLVs.
- `MidniQrVerifier.kt`: validaciones criptograficas y reglas funcionales.
- `VerificationCertStore.kt`: carga de certificados base64 desde assets.
- `C40.kt`: decodificacion C40 usada en cabecera y referencia de certificado.

---

## 5) Funcionalidades principales

### Validaciones del QR MiDNI

1. Verifica estructura minima y cabecera VDS (`magic 0xDC`, version soportada).
2. Decodifica campos C40 de emisor y referencia de certificado.
3. Parsea TLVs y localiza la firma (`tag 0xFF`).
4. Resuelve certificado de firma por referencia (`verification_certs.json`).
5. Valida vigencia temporal del certificado X.509.
6. Comprueba correspondencia entre referencia del QR y serial del certificado.
7. Convierte firma ECDSA `r||s` a ASN.1 DER y valida `SHA256withECDSA`.
8. Verifica caducidad de datos QR (`tag 0x80`, fecha UTC).

### Extraccion de datos para presentacion

- Numero de documento (`tag 0x40`)
- Fecha de nacimiento (`tag 0x42`)
- Nombre (`tag 0x44`)
- Apellidos (`tag 0x46`)
- Sexo (`tag 0x48`)
- Caducidad del documento (`tag 0x4C`)
- Fotografia (`tag 0x50`)
- Direccion (`tag 0x60`)
- Numero de soporte (`tag 0x68`)
- Indicador de mayoria de edad (`tag 0x70`)

### Flujos de entrada soportados

- Camara (QR scanner)
- Lector HID USB
- Lector serie USB
- Simulacion TCP para emulador/debug

---

## Compatibilidad Android

- `minSdk = 24` (Android 7.0)
- `targetSdk = 35`
- `compileSdk = 35`

---

## Notas operativas

- Si el dispositivo no tiene camara, el boton de escaneo se deshabilita.
- Si falta permiso de camara, la app lo solicita en tiempo de ejecucion.
- El parseo usa `ISO-8859-1` para preservar payload binario del QR.
