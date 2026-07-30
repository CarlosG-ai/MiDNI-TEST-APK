# AGENTS.md — MiDNI QR Verifier

Guía para agentes de IA que trabajen en este repositorio.

---

## Descripción del proyecto

**MiDNI QR Verifier** es una aplicación Android (Kotlin) para leer y validar los
códigos QR del DNI electrónico español (MiDNI). Soporta cuatro modos de entrada:

| Modo | Descripción |
|------|-------------|
| Cámara | ZXing Embedded |
| HID USB | Lector que emula teclado |
| Serie USB | `usb-serial-for-android` vía USB Host |
| TCP (simulación) | Para pruebas en emulador sin hardware físico |

El QR se valida estructuralmente (magic `0xDC`, cabecera VDS, TLVs) y
criptográficamente (firma ECDSA P-256 con certificado X.509 local).

---

## Stack tecnológico

- **Lenguaje**: Kotlin · Java 17 (source/target compatibility)
- **Plataforma**: Android, `minSdk 24` (Android 7.0), `targetSdk 35`
- **Build**: Gradle Wrapper (`gradlew.bat`) · Android Gradle Plugin 9.2.0
- **Criptografía**: BouncyCastle (`bcprov-jdk18on:1.78.1`, `bcpkix-jdk18on:1.78.1`)
- **QR scanner**: `zxing-android-embedded:4.3.0` + `zxing:core:3.5.3`
- **USB serie**: `usb-serial-for-android:3.7.3`
- **UI**: AndroidX (`core-ktx`, `appcompat`, `material`, `constraintlayout`) · ViewBinding
- **Tests**: JUnit 4 · Mockito 5 · Mockito-Kotlin · ZXing JavaSE (lectura de imágenes QR en JVM)

---

## Estructura del repositorio

```
.
├── app/
│   ├── build.gradle                          # Configuración del módulo Android
│   └── src/
│       ├── main/
│       │   ├── java/es/gob/midni/qrdemo/
│       │   │   ├── MainActivity.kt           # UI, permisos, flujos de entrada
│       │   │   ├── MidniQrParser.kt          # Parser: cabecera VDS + TLVs
│       │   │   ├── MidniQrVerifier.kt        # Validación criptográfica y funcional
│       │   │   ├── VerificationCertStore.kt  # Carga certificados desde assets
│       │   │   ├── C40.kt                    # Decodificación C40
│       │   │   └── AnyOrientationCaptureActivity.kt
│       │   └── assets/
│       │       └── verification_certs.json   # Certificados X.509 en base64
│       └── test/java/es/gob/midni/qrdemo/
│           ├── MidniQrParserTest.kt
│           └── MidniQrVerifierTest.kt
├── midni-qr-spec/                            # Especificación del formato QR MiDNI
│   ├── MiDNI-FormatoQR_v107_sc_PN.md
│   ├── docs/
│   └── assets/qr-ejemplos/                  # Imágenes QR de ejemplo (usadas en tests)
├── build.gradle                              # Script raíz (declara plugin AGP)
├── settings.gradle
├── version.properties                        # buildNumber auto-incremental
├── gradle.properties
├── launch-emulator.ps1                       # Lanza emulador AVD
├── bridge-com-tcp.ps1                        # Puente COM → TCP
└── test-inject-payload.ps1                   # Inyecta payload QR al socket TCP
```

---

## Comandos esenciales

> El proyecto está orientado a Windows/PowerShell. En Linux/macOS usa `./gradlew`
> en lugar de `.\gradlew.bat`.

### Compilar (debug)

```powershell
.\gradlew.bat clean assembleDebug
```

APK generada en `app/build/outputs/apk/debug/app-debug.apk`.

### Ejecutar tests unitarios

```powershell
.\gradlew.bat testDebugUnitTest
```

### Compilar release

```powershell
.\gradlew.bat assembleRelease
```

Requiere `gradle.properties.local` con:
```properties
MIDNI_RELEASE_STORE_FILE=<ruta al keystore>
MIDNI_RELEASE_STORE_PASSWORD=<contraseña>
MIDNI_RELEASE_KEY_ALIAS=midni
MIDNI_RELEASE_KEY_PASSWORD=<contraseña>
```

El `buildNumber` en `version.properties` se incrementa automáticamente al
empaquetar la release.

---

## Convenciones de código

- **Idioma del código**: inglés para nombres de clases, funciones y variables;
  español en comentarios y mensajes de usuario (coherente con el README).
- **Codificación del payload QR**: `ISO-8859-1` — imprescindible para preservar
  bytes binarios al parsear el QR.
- **Criptografía**: siempre usar BouncyCastle; no depender de la implementación
  JVM del proveedor del sistema.
- **TLVs**: los tags relevantes están definidos como constantes hexadecimales en
  `MidniQrParser.kt` y `MidniQrVerifier.kt`.
- **Firma ECDSA**: el QR la almacena como `r||s` (formato raw); debe convertirse
  a DER antes de verificar con `SHA256withECDSA`.
- **ViewBinding**: habilitado — no usar `findViewById` directamente.
- **No hay inyección de dependencias** (sin Hilt/Koin); los componentes se
  instancian directamente.

---

## Tests

Los tests unitarios son JVM puros (sin emulador). Los recursos QR de ejemplo se
toman de `midni-qr-spec/assets/qr-ejemplos/` y `app/src/main/assets/`, ambas
rutas configuradas como `resources.srcDirs` del source set `test`.

Una peculiaridad del build: AGP 9.2.0 genera clases Kotlin en una ruta con
carácter especial (`ó`) que el URLClassLoader no puede resolver en algunos
entornos. El `build.gradle` copia las clases a `java.io.tmpdir` antes de
ejecutar los tests — no modificar esta lógica sin verificar que los tests
siguen pasando.

---

## Especificación del formato QR

La especificación completa está en `midni-qr-spec/`. El documento principal es
`MiDNI-FormatoQR_v107_sc_PN.md`. Consultar antes de modificar la lógica de
parseo o los tags TLV.

---

## Certificados de verificación

`app/src/main/assets/verification_certs.json` (y su copia en la raíz del repo,
`verification_certs.json`) contiene los certificados X.509 en base64 indexados
por referencia. Al añadir soporte para nuevos certificados, actualizar este
fichero.

---

## Simulación en emulador (flujo de referencia)

```powershell
# 1. Lanzar emulador con puente COM→TCP
.\launch-emulator.ps1 -ComPort COM8 -BaudRate 115200

# 2. En la app: modo serie → simulación TCP

# 3. Inyectar payload de prueba
.\test-inject-payload.ps1
```

---

## Seguridad

- **No commitear** claves, keystores ni `gradle.properties.local`.
- Los certificados de verificación son públicos (X.509 de la FNMT/DGP) y sí
  pertenecen al repositorio.
- Escanear los ficheros modificados en busca de secretos antes de cada commit.
