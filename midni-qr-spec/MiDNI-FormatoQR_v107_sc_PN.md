# Verificación y Formato de Códigos QR – DNI en el Móvil

**Proyecto:** DNI en el Móvil  **Documento:** Verificación y formato de QR  **Versión:** 1.0.7  **Fecha:** 20/06/2025  **Categoría:** Documentación Confidencial

---

## Control de Cambios

| Fecha | Versión | Autor | Descripción |
|---|---|---|---|
| 15/04/2024 | 1.0.0 | PN | Versión inicial |
| 04/06/2024 | 1.0.1 | PN | Cambios redacción |
| 05/06/2024 | 1.0.2 | PN | Ajustado formato código ejemplo |
| 07/06/2024 | 1.0.3 | PN | Verificación autenticidad certificado |
| 17/06/2024 | 1.0.4 | PN | Cambios redacción |
| 13/10/2024 | 1.0.5 | PN | Inclusión ejemplos QR |
| 14/10/2024 | 1.0.6 | PN | Explicaciones adicionales |
| 20/06/2025 | 1.0.7 | PN | Inclusión número de soporte |

---

## 1. Objetivo

La aplicación **miDNI** permite visualizar los datos del DNI de un ciudadano, recuperándolos de los servidores centrales de la Policía Nacional. Parte de esta información podrá ser compartida mediante códigos QR firmados digitalmente.

La codificación se basa en la especificación **ICAO 9303 Parte 13 – Visible Digital Seals**.

## 2. Verificación

### 2.1 Tipos de códigos

- **Verificación de edad**: foto miniatura, DNI y mayoría de edad.
- **DNI simple**: datos básicos del DNI.
- **DNI completo**: datos completos incluyendo domicilio, nacionalidad y número de soporte.

Todos los QR incluyen fecha/hora de caducidad de los datos.

### 2.2 Procedimiento de verificación

1. Decodificar estructura.
2. Obtener certificado firmante.
3. Verificar autenticidad y validez.
4. Verificar firma.
5. Verificar caducidad.
6. Extraer datos.

### 2.2.1 Certificado de firma

- Certificados publicados en: https://pki.policia.es/cnp/MiDNI
- OCSP: http://ocsp.policia.es

---

## 3. Formato de los datos

Estructura basada en TLV:

- **Cabecera**
- **Mensaje**
- **Firma**

### 3.1 Encabezamiento

Magic Constant: `0xDC`  Versión: `0x03`  País: `ES`  Firmante + referencia certificado (C40)

### 3.2 Mensaje

Datos TLV según tipo de QR (edad, simple o completo).

### 3.2.1 Datos incluidos

| Tag | Descripción | QR Edad | QR Simple | QR Completo |
|---|---|---|---|---|
| 0x40 | Número de documento | X | X | X |
| 0x42 | Fecha nacimiento |  | X | X |
| 0x44 | Nombre |  | X | X |
| 0x46 | Apellidos |  | X | X |
| 0x48 | Sexo |  | X | X |
| 0x4C | Fecha caducidad |  | X | X |
| 0x50 | Imagen miniatura | X | X | X |
| 0x60 | Dirección |  |  | X |
| 0x68 | Nº soporte |  |  | X |
| 0x70 | Mayor de edad | X |  |  |
| 0x80 | Caducidad datos | X | X | X |

---

## 4. Firma

- Algoritmo: **ECDSA / SHA-256**
- El TLV de firma (`0xFF`) incluye solo los valores **r** y **s**.
- Es necesario reconstruir ASN.1 para validación estándar.

---

## 5. Ejemplos de QR

- Válidos con caducidad extendida
- Caducados
- Datos modificados
- Firmados por certificado distinto

Los ejemplos están firmados con certificados de prueba publicados en el repositorio oficial.

---

## Referencias

- ICAO 9303 Parte 13 – Visible Digital Seals  
  - https://www.icao.int/publications/Documents/9303_p13_cons_es.pdf  
  - https://www.icao.int/publications/Documents/9303_p13_cons_en.pdf
