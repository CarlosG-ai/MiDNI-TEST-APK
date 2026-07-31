# **Presentación del Proyecto: MiDNI QR Verifier**

## **Diapositiva 1: Portada**

### **MiDNI QR Verifier (Android 7+)**

* **Subtítulo:** Aplicación Android para lectura, validación estructural y comprobación criptográfica de códigos QR MiDNI.  
* **Autor/Equipo:![][image1]**  
* **Sugerencia visual:** Imagen de un teléfono Android escaneando un código QR con iconos de seguridad (candado/certificado) y USB.

## **Diapositiva 2: El Problema Técnico**

### **Desafío en Pruebas de Hardware Electrónico**

* **Necesidad de prueba:** Los desarrolladores de hardware necesitan validar la lectura de QR desde múltiples fuentes en dispositivos Android.  
* **Restricción crítica:** Requisito obligatorio de funcionar sobre **Android 7.0 (API 24\)** hasta las versiones más recientes (API 35).  
* **Múltiples interfaces:**  
  1. Cámara integrada del dispositivo.  
  2. Lectores de código de barras USB (Emulación Teclado HID).  
  3. Lectores USB por puerto serie (Virtual COM).  
  4. Entorno de simulación TCP para emuladores.  
* **Sugerencia visual:** Diagrama de bloques que muestra los 4 métodos de entrada conectándose a un teléfono Android.

## **Diapositiva 3: La Solución Desarrollada**

### **MiDNI QR Verifier**

* **Propósito:** App Android robusta orientada a pruebas funcionales y validación completa del estándar MiDNI.  
* **Procesamiento de Payload:**  
  * Parsea el contenido binario del QR preservando la codificación original.  
  * Extrae cabeceras, formato C40 y etiquetas TLV (Type-Length-Value).  
* **Verificación Criptográfica Real:**  
  * Resuelve certificados X.509 locales.  
  * Valida firmas digitales ECDSA de forma totalmente desconectada.  
* **Sugerencia visual:** Captura de pantalla de la aplicación mostrando el resultado exitoso de la validación con datos personales estructurados.

## **Diapositiva 4: Arquitectura y Stack Tecnológico**

### **Tecnologías y Librerías Utilizadas**

* **Plataforma & Lenguaje:** Android, Kotlin, Java 17\.  
* **Build System:** Gradle Wrapper, Android Gradle Plugin (AGP 9.2.0).  
* **Escaneo y USB:**  
  * ZXing Embedded: Procesamiento visual de códigos QR.  
  * usb-serial-for-android: Comunicación serie directa por puerto USB Host.  
* **Seguridad y Criptografía:**  
  * BouncyCastle: Verificación de certificados X.509 y firmas ECDSA.  
* **Sugerencia visual:** Logotipos de Kotlin, Android, Gradle y un escudo de seguridad criptográfica.

## **Diapositiva 5: Proceso de Validación Criptográfica**

### **¿Cómo asegura la app la validez del QR?**

1. **Comprobación Estructural:** Validación del *magic byte* (0xDC) y versión del formato VDS.  
2. **Decodificación C40:** Lectura del emisor y referencia del certificado.  
3. **Gestión de Certificados:** Búsqueda del certificado en el almacén local (verification\_certs.json).  
4. **Validación Criptográfica:**  
   * Verificación de la fecha de vigencia del certificado X.509.  
   * Conversión de firma ECDSA (r||s) a formato ASN.1 DER.  
   * Validación mediante algoritmo SHA256withECDSA.  
5. **Comprobación de Expiración:** Control de caducidad de los datos del QR en tiempo real.  
* **Sugerencia visual:** Diagrama de flujo simplificado del proceso de validación en 5 pasos.

## **Diapositiva 6: Flujos de Trabajo y Simulaciones**

### **Pruebas de Integración y Herramientas de Soporte**

* **Soporte PowerShell Integrado:**  
  * Script launch-emulator.ps1: Inicializa el emulador y gestiona el puente COM.  
  * Script bridge-com-tcp.ps1: Redirige tráfico del puerto COM del PC hacia la app vía TCP.  
  * Script test-inject-payload.ps1: Inyecta datos QR binarios de prueba de forma automatizada.  
* **Resultado:** Capacidad de probar código sin necesidad de hardware físico conectado.  
* **Sugerencia visual:** Captura de la terminal con los scripts PowerShell ejecutándose junto al emulador de Android.

## **Diapositiva 7: Conclusiones y Logros**

### **Impacto del Proyecto**

* **Compatibilidad Extrema:** Funcionamiento verificado en Android 7 (dispositivos industriales antiguos) y Android 15 (dispositivos modernos).  
* **Versatilidad de Entrada:** Integración exitosa de 4 canales de entrada independientes en una sola interfaz.  
* **Seguridad Rigurosa:** Implementación completa de la especificación oficial de validación del QR MiDNI.  
* **Sugerencia visual:** Icono de checklist de éxito o mockup del dispositivo en funcionamiento.

[image1]: <data:image/png;base64,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>