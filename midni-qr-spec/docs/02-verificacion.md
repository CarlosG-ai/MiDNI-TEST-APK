## Verificación

## Tipos de códigos

La aplicación miDNI permite generar tres tipos de QR, en los que varía la información que se compartirá con la aplicación de lectura:

Verificación de edad

Solo se compartirá la foto en miniatura, el número de DNI, y si el portador es mayor de edad.

DNI simple

Se compartirá la foto en miniatura, el número de DNI, nombre y apellidos, fecha de nacimiento, sexo, y fecha de caducidad del documento.

DNI completo

Se compartirá la foto en miniatura, el número de DNI, nombre y apellidos, fecha de nacimiento, sexo, fecha de caducidad del documento, lugar de nacimiento, nacionalidad, domicilio, nombre de los padres y número de soporte

Además de estos datos, todos los QR incluyen un campo adicional con la fecha de caducidad de los datos, establecida unos minutos después de su generación.

La función de esta fecha, es que la aplicación lectora pueda saber si el QR acaba de ser generado, o si se está presentando un QR antiguo, que deberá ser descartado.

## Procedimiento de Verificación

En los siguientes apartados se describe el formato y contenido de los códigos bidimensionales. Los datos que contiene cada QR están estructurados tal y como se describe en los siguientes apartados.

Independientemente del tipo de QR que se haya generado (de edad, simple o completo), el procedimiento de verificación debería ser el siguiente:

Decodificar los datos, comprobando que la estructura es la especificada

Obtener la referencia del certificado firmante

Obtener el certificado de firma, comprobar su autenticidad y validez

Verificar la firma de los datos

Verificar la validez temporal de los datos (comparando el campo caducidad de los datos contra la fecha/hora actual)

Extraer los datos cuya autenticidad se acaba de comprobar

### Verificación de la Autenticidad y Validez del Certificado de Firma

En la cabecera del QR se incluye una referencia que identificará al certificado firmante. Este certificado, utilizado para la firma de datos, se podrá obtener de la siguiente dirección:

http://pki.policia.es/cnp/MiDNI

A partir de la referencia al certificado firmante se obtendrá el certificado correspondiente, que estará publicado en la dirección indicada arriba.

Este certificado está a disposición de los interesados en verificar la autenticidad de los datos obtenidos a través de los códigos QR generados por la app miDNI. En caso de que cambiara el certificado firmante, la referencia sería otra y el nuevo certificado se publicaría en la misma dirección.

El estado en el que se encuentra este certificado firmante puede, asimismo, ser verificado mediante OCSP en la siguiente dirección:

http://ocsp.policia.es
