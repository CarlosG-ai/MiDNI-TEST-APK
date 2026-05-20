# MiDNI QR Verifier (Android 7+)

Aplicacion Android para escanear QR de miDNI y validar su autenticidad siguiendo el documento:
- MiDNI-FormatoQR_v107_sc_PN.pdf

## Que valida

1. Estructura del QR (cabecera VDS + TLVs + firma 0xFF).
2. Referencia de certificado del firmante desde cabecera C40.
3. Certificado de firma desde `verification_certs.json`.
4. Validez temporal del certificado X509.
5. Correspondencia entre referencia y serial del certificado.
6. Firma ECDSA SHA-256 (con conversion de `r|s` a ASN.1 DER).
7. Caducidad de datos del QR en tag `0x80` (UTC).

## Compatibilidad Android

- `minSdk = 24` (Android 7.0)
- `targetSdk = 35`

## Estructura principal

- `app/src/main/java/es/gob/midni/qrdemo/MainActivity.kt`: UI y escaneo QR.
- `app/src/main/java/es/gob/midni/qrdemo/MidniQrParser.kt`: parseo cabecera + TLV.
- `app/src/main/java/es/gob/midni/qrdemo/MidniQrVerifier.kt`: validacion criptografica.
- `app/src/main/java/es/gob/midni/qrdemo/C40.kt`: decodificador C40.
- `app/src/main/java/es/gob/midni/qrdemo/VerificationCertStore.kt`: carga de certificados.
- `app/src/main/assets/verification_certs.json`: certificados de confianza.

## Generar APK (Android Studio)

1. Abrir la carpeta del proyecto en Android Studio.
2. Esperar a que sincronice Gradle y descargue dependencias.
3. Ir a `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. APK debug esperada en:
   - `app/build/outputs/apk/debug/app-debug.apk`

Para release firmada:
1. `Build > Generate Signed Bundle / APK`.
2. Elegir `APK` y completar keystore.

## Notas

- Si no existe camara, el boton de escaneo queda deshabilitado.
- Si falta permiso de camara, la app lo solicita en tiempo de ejecucion.
- Si el QR no contiene bytes crudos, se intenta lectura en ISO-8859-1.
