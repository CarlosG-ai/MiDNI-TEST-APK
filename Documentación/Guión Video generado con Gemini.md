# **Guión de Video: Demostración de MiDNI QR Verifier**

**Duración estimada:** 3 minutos

**Tono:** Profesional, técnico y conciso

### **\[00:00 \- 00:30\] Introducción y Contexto del Problema**

* **Escena visual:** El presentador en cámara o pantalla completa mostrando el título del proyecto, pasando rápidamente a una imagen de dispositivos Android antiguos y lectores de código USB.  
* **Voz en off / Locución:**"Hola a todos. Mi nombre es Carlos Garcia, aunque tengo formación técnica, hace más de 30 años que no uso un IDE, ni me he dedicado nunca a programar aplicaciones. Hoy les presento mi primera aplicación Android desarrollada con ayuda del Master en desarrollo con IA de BIG SCHOOL. La aplicación se llama **MiDNI QR Verifier,**  pensada para resolver la necesidad de validar códigos QR de alta seguridad en múltiples dispositivos y desde diferentes fuentes de entrada durante el desarrollo de hardware y sistemas electrónicos.   
  Un requisito clave es la compatibilidad desde **Android 7.0** hasta las versiones más recientes, ya que muchos terminales físicos en la industria funcionan con esta versiones antiguas del sistema operativo."

### **\[00:30 \- 01:15\] Fuentes de Entrada y Stack Tecnológico**

* **Escena visual:** Muestra de la arquitectura de la app o captura de la pantalla principal (MainActivity), destacando los 3 botones o modos de entrada.  
* **Voz en off / Locución:**"Para lograr máxima flexibilidad, la app permite capturar el código QR a través de tres canales distintos:  
  Primero, desde la cámara integrada en el propio dispositivo mediante la librería ZXing.  
  Segundo, desde un lector externo conectado por USB emulando un teclado.  
  Tercero, desde una comunicación por puerto serie USB usando la librería usb-serial-for-android, con opción a seleccionar un puente TCP.  
  La aplicación está desarrollada en **Kotlin** con Java 17 y utiliza la librería **BouncyCastle** para la capa de seguridad criptográfica."

### **\[01:15 \- 02:15\] Demostración de Funcionamiento y Criptografía**

* **Escena visual:** Grabación de pantalla del emulador o dispositivo escaneando o inyectando un QR. Se observa la pantalla mostrando la lectura y la verificación exitosa de los datos (Nombre, Documento, Firma válida).  
* **Voz en off / Locución:**"Veamos cómo funciona la validación. Cuando se recibe el código QR, la app no solo lee el texto: analiza el contenido binario sin alterar su codificación.  
  Extrae la cabecera VDS, decodifica los datos en formato C40 y busca el certificado correspondiente en nuestro almacén local.  
  A continuación, la app verifica la vigencia del certificado X.509 y valida la firma digital **ECDSA con SHA-256**.  
  Si la firma es correcta y los datos no están caducados, la app muestra en pantalla los datos del documento: nombre, apellidos, número de soporte y fotografía."

### **\[02:15 \- 02:45\] Herramientas de Pruebas y Simulaciones**

* **Escena visual:** Pantalla dividida mostrando la terminal ejecutando los scripts PowerShell (launch-emulator.ps1 e inject-payload.ps1) y el emulador respondiendo al instante.  
* **Voz en off / Locución:**"Además, el proyecto incluye un conjunto de scripts en PowerShell que permiten simular la entrada de puerto serie redireccionando datos por TCP al emulador. Esto facilita enormemente la realización de pruebas unitarias y de integración sin depender de cables o lectores físicos durante la fase de desarrollo."

### **\[02:45 \- 03:00\] Cierre y Conclusiones**

* **Escena visual:** Pantalla final con el enlace al repositorio de GitHub https://github.com/CarlosG-ai/MiDNI-TEST-APK.git  
* **Voz en off / Locución:**"En conclusión, **MiDNI QR Verifie**r ofrece una solución completa, segura y retrocompatible para la validación de QR de identidad. Muchas gracias por su atención."