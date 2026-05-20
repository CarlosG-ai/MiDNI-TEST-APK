## Formato de los datos

Al leer los datos contenidos en cada uno de los QR generados por la aplicación miDNI, se obtiene una estructura de datos conforme a la especificación de ‘Sellos Digitales Visibles’, definida en el documento ICAO 9303 parte 13 para “Sellos Digitales Visibles” [ICAO_930313].

En esta estructura, se diferencian tres partes:

Una cabecera en la que incluyen datos generales de la estructura, e información del firmante.

El mensaje o conjunto de los datos que se quieren incluir, en estructuras del tipo ‘etiqueta → longitud → valor’ (TLV). Se podrán incluir tantas estructuras como la aplicación los requiera.

Un último TLV con la firma de todos los datos anteriores, incluyendo la cabecera.

La siguiente imagen lo muestra de forma gráfica:

![](../assets/images/image_020.jpg)

## Encabezamiento

La cabecera tiene la estructura definida en el documento [ICAO_9303-13]:

| Posición | Tamaño | Descripción |
| --- | --- | --- |
| 0x00 | 1 | ‘Magic Constant’. Siempre es el valor 0xDC |
| 0x01 | 1 | Versión del formato utilizado. Siempre será el valor 0x03, que indica que es la versión 4. Se utiliza esta versión por ser la más actual, y que permite datos de tamaño superior a 254 bytes. |
| 0x02 | 2 | País expedidor. Siempre tendrá el valor ‘ES’. |
| 0x04 | v | Identificador del firmante, y referencia del certificado. <br>Está formado: <br>Dos letras que identifican el país. <br>Dos caracteres que identifican la entidad firmante en el país. <br>Dos dígitos que indican el tamaño de la referencia del certificado. <br>Cadena hexadecimal que referencia el certificado de firma. <br>El Identificador del firmante (cuatro primeros caracteres) debe coincidir con el DN (Distinguished Name) del sujeto del certificado, y la referencia del certificado con el número de serie del certificado. |
| 0x04+v | 3 | Fecha de emisión del documento |
| 0x07+v | 3 | Fecha de firma de los datos |
| 0x0A+v | 1 | Referencia a la definición de los elementos del documento: <br>7: Verificación simple <br>8: Verificación completa <br>9: Verificación de edad |
| 0x0B+v | 1 | Categoría de tipo de documento:  	9: DNI en el móvil de España |


Los campos de texto incluidos es esta cabecera, utilizan la codificación C40 descrita en el documento [ICAO_9303-13].

Los dos campos de fecha incluidos en la cabecera utilizan la codificación definida en el apartado 2.3.1 del documento [ICAO_9303-13].

## Mensaje

A continuación, se definen los datos que compartiría la app móvil para su verificación por parte de otros dispositivos, de acuerdo a los tres perfiles de datos previstos:

Verificación Simple, incluyendo los datos básicos del DNI.

Verificación Completa, incluyendo datos adicionales.

Verificación de mayoría de edad, únicamente si el ciudadano es mayor de edad.

### Datos incluidos según el tipo de QR

En esta sección, se muestran todos los datos que pueden encontrarse en un QR generado por la aplicación miDNI, y se indica en qué tipo de QR está presente:

| Etiqueta | Descripción | Formato |  |  |  |
| --- | --- | --- | --- | --- | --- |
| 0x40 | Número de documento (nueve caracteres más significativos + letra de verificación) |  | X | X | X |
| 0x42 | Fecha de nacimiento | ‘DD-MM-YYYY’ |  | X | X |
| 0x44 | Nombre |  |  | X | X |
| 0x46 | Apellidos |  |  | X | X |
| 0x48 | Sexo | F / M |  | X | X |
| 0x4c | Fecha de caducidad del documento | ‘DD-MM-YYYY’ |  | X | X |
| 0x50 | Imagen en miniatura | Jpeg2000 | X | X | X |
| 0x60 | Dirección completa |  |  |  | X |
| 0x72 | Lugar de domicilio, línea 1 |  |  |  | X |
| 0x74 | Lugar de domicilio, línea 2 |  |  |  | X |
| 0x76 | Lugar de domicilio, línea 3 |  |  |  | X |
| 0x62 | Lugar de nacimiento, línea 1 |  |  |  | X |
| 0x78 | Lugar de nacimiento, línea 2 |  |  |  | X |
| 0x7a | Lugar de nacimiento, línea 3 |  |  |  | X |
| 0x64 | Nacionalidad |  |  |  | X |
| 0x66 | Nombre de padre y madre |  |  |  | X |
| 0x68 | Número de soporte del DNI físico |  |  |  | X |
| 0x70 | Si el ciudadano es mayor de Edad | Un byte 0x00/0x01 | X |  |  |
| 0x80 | Fecha/hora de caducidad de los datos | ‘DD-MM-YYYY hh:mm:ss’ | X | X | X |
