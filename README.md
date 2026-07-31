# MiDNI QR Verifier - MiDNI TES APK

Aplicacion Android para lectura y validacion de codigos QR generados por la aplicación de la policia nacional española MiDNI.

MiDNI TES APK es el nombre del proyecto
MiDNI QR Verifier es el nombre de la aplicación publicada en UPTODOPWN: https://www.uptodown.dev/apps

La presentación está realizada con GAMMA: https://gamma.app/docs/MiDNI-QR-Verifier-8ckayg78j3r6zdi

Tengo dos videos:
1.- Hecho 100% con IA: https://app.heygen.com/videos/midni-qr-verifier-demostraci-n-t-cnica-a918d60318bb4a73b9ca57617e68b1eb
2.- Hecho manualmente con OBS, intente subirlo a GIHUB pero pesaba mucho: 

se encuentra en la carpeta del proyecto "Documentación". VIDEO_PRESENTACION.MP4

En la carpetade "documentación" existen copias de la presentación, videos y otras cosas interesantes.
En la carpeta "midni-qr-spec\assets\images" se encuentran ejemplos de QR para poder imprimir

---

## 1) Descripcion general del proyecto

MiDNI QR Verifier es una herramienta técnica, no orientada al publico general, que busca resolver el problema al que se enfrentan los desarrolladores de productos electrónicos cuando requieren probar diferentes módulos de hardware, sobre dispositivos existentes.

El código QR generado por la aplicación MiDNI, es muy grande y muy denso. No todos los lectores QR son capaces de leerlo con fluided para conseguir una experiencia de usuario aceptable.

Esta aplicación permite probar diferentes lectores QR, de fabricantes distintos, leyendo el contenido de un código QR desde:
- La cámara del propio dipositivo físico. (ZXing Embedded).
- Un "lector de QR" cuya salida de datos emula un teclado HID, conectado al puerto USB del dispositivo físico o al PC donde se ejeucta el emulador.
- Un "lector de QR" cuya salida de datos es serie, conectado al puerto USB del PC donde se ejecuta el emulador android. Este puerto se comparte con el emulador mediante una pasarela COM-->TCP. (Por defecto usamos el com3 pero este valor depende del PC, hay que cambiarlo y poner el valor que asigna el PC cuando se conecta el lector)

Mantiene la mayor compatibilidad entre versiones de Android, desde la 7 hasta la más moderna. Los dispositivos físicos disponen de Android 7.

La app parsea el payload binario del QR, extrae cabecera y TLVs, resuelve el certificado de verificacion desde un almacén local, valida firma ECDSA y muestra un resumen para usuario junto con datos personales cuando la validacion es correcta.

El proyecto implementa validacion estructural y criptografica del contenido QR, siguiendo la especificacion incluida en el repositorio: `midni-qr-spec/MiDNI-FormatoQR_v107_sc_PN.md`

El proyecto implementa test unitarios, revisión de código con CodeQL y CI para compilar en  ebug usando las autmatizaciones en GitHub Actions

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
- Android SDK con plataforma `API 35`
- JDK 17 configurado para Gradle
- Visual Studio Code
- (Opcional) Android Studio (version reciente compatible con AGP 9.2.0)
- (Opcional) Emulador Android + `adb`

### Desarrollo con Visual Studio Code

Este repositorio esta orientado a trabajar desde Visual Studio Code.

- Workspace recomendado: `MiDNI TEST APK.code-workspace`
- Terminales y scripts PowerShell incluidos para emulador y pruebas (`launch-emulator.ps1`, `bridge-com-tcp.ps1`, `test-inject-payload.ps1`)
- Flujo sugerido en VS Code: compilar con `gradlew.bat`, lanzar emulador con script y validar logs desde terminal integrada

### Ejecutar desde Android Studio (opcional)

1. Seleccionar dispositivo fisico o emulador.
2. Ejecutar la configuracion `app` (Run).
3. Probar alguno de los flujos de entrada: camara, HID o serie.

### Clonar y abrir el proyecto

1. Clonar el repositorio.
2. Abrir la carpeta raiz en Visual Studio Code o abrir `MiDNI TEST APK.code-workspace`.
3. Esperar restauracion/sincronizacion de Gradle y descarga de dependencias.

### Compilar por linea de comandos

En la raiz del proyecto:

```powershell
.\gradlew.bat clean assembleDebug
```

APK debug generada en:

- `app/build/outputs/apk/debug/MiDNI TEST APK-debug.apk`

### Ejecutar tests unitarios

```powershell
.\gradlew.bat testDebugUnitTest
```
### Build release

Existe configuracion release en `app/build.gradle` con firma definida en
`signingConfigs.release`.

La salida de APK release se genera con nombre:

- `app/build/outputs/apk/release/MiDNI TEST APK-release.apk`

La firma release usa keystore en formato PKCS12 (`.p12`) configurado en
`gradle.properties.local`.

Ejemplo de configuracion local:

```properties
MIDNI_RELEASE_STORE_FILE=../midni-release.p12
MIDNI_RELEASE_STORE_PASSWORD=<password>
MIDNI_RELEASE_KEY_ALIAS=midni
MIDNI_RELEASE_KEY_PASSWORD=<password>
```

> Importante: `gradle.properties.local` no debe subirse al repositorio.

Para generar release:

```powershell
.\gradlew.bat assembleRelease
```

> Nota: el proyecto incrementa automaticamente `buildNumber` en
> `version.properties` tras empaquetar `packageRelease`.

### Scripts de soporte (emulador y simulacion)

- `launch-emulator.ps1`
   - Lanza emulador, centra ventana y configura `adb reverse` lanzando el puente COM->TCP desde una ventana separada de powershell
- `bridge-com-tcp.ps1`
   - Abre un puerto COM en host y reenvia bytes a `localhost:9876`.
- `test-inject-payload.ps1`
   - Inyecta payload de fixture QR al socket TCP para pruebas automatizadas/manuales.

Ejemplo de flujo de simulacion en emulador:

1. Iniciar emulador y puente:

```powershell
.\launch-emulator.ps1 -ComPort COM3 -BaudRate 115200
```

2. En la app: abrir modo serie y seleccionar simulacion TCP.
3. Mostrar QR al lector o Inyectar payload de prueba:

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
- Lector serie USB mediante puente TCP para emulador/debug

---

## 6) Automatizacion en GitHub Actions

El proyecto incluye automatizacion en GitHub Actions para integracion continua y seguridad:

- CI de compilacion debug: ejecuta build y tests unitarios en cada push/pull request sobre `master`.
- Analisis de seguridad con CodeQL: analiza Kotlin/Java para detectar vulnerabilidades y patrones inseguros.

Workflows incluidos en el repositorio:

- `.github/workflows/android-debug-ci.yml`
- `.github/workflows/codeql.yml`

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
