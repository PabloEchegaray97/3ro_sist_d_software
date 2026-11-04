# ARCA en homologación

Esta guía documenta cómo la aplicación se autentica ante ARCA, cómo solicita autorización para una Factura C y cómo se prueba el circuito con la colección de Postman incluida en este directorio. Las siglas y los conceptos de facturación electrónica, servicios web y criptografía se definen en el flujo y en el glosario.

> **Importante:** homologación es un ambiente de pruebas. Permite ensayar la integración sin emitir comprobantes fiscales de producción. Sus certificados, puntos de venta y autorizaciones deben estar habilitados específicamente para ese ambiente.

## 1. Arquitectura y propósito

Emitir una factura electrónica requiere dos comunicaciones distintas:

1. **Autenticación:** la aplicación demuestra su identidad y obtiene un permiso temporal.
2. **Autorización del comprobante:** con ese permiso envía los datos fiscales y solicita un CAE.

El recorrido completo es:

```text
Usuario o Postman
       |
       | POST /api/facturas/{id}/emitir
       v
Backend Spring Boot
       |
       | 1. Firma una solicitud de acceso
       v
WSAA de ARCA -----------------> devuelve token, sign y vencimiento
       |
       | 2. Consulta el último número fiscal autorizado
       v
WSFE de ARCA -----------------> devuelve el último número
       |
       | 3. Solicita autorización para el número siguiente
       v
WSFE de ARCA -----------------> aprueba con CAE o rechaza con mensajes
       |
       v
MariaDB guarda el resultado y Postman recibe una respuesta segura
```

Postman no se comunica directamente con ARCA. Llama a la API local; el backend firma, autentica, se comunica con ARCA e interpreta sus respuestas.

### Qué es y qué función cumple cada componente

| Componente | Definición | Responsabilidad en este proyecto |
|---|---|---|
| **ARCA** | Agencia de Recaudación y Control Aduanero, organismo fiscal nacional. | Administra los servicios que autentican a la aplicación y autorizan el comprobante. |
| **Backend Spring Boot** | Aplicación Java que implementa la lógica del sistema y expone una API HTTP. | Valida la factura, firma el pedido de acceso, llama a ARCA y persiste el resultado. |
| **WSAA** | Web Service de Autenticación y Autorización. | Verifica la firma digital y entrega credenciales temporales para acceder a WSFE. No autoriza facturas. |
| **WSFEv1** | Web Service de Factura Electrónica, versión 1. | Consulta la numeración fiscal y procesa la solicitud del CAE. |
| **MariaDB** | Motor de base de datos relacional usado por la aplicación. | Guarda el borrador, sus importes, el estado y el resultado de ARCA. No reemplaza el registro fiscal de ARCA. |
| **PKCS#12** | Contenedor criptográfico protegido por contraseña. | Aporta el certificado X.509 y la clave privada necesarios para firmar el TRA. |
| **Postman** | Cliente para ejecutar peticiones HTTP contra APIs. | Prueba los endpoints del backend; no firma ni llama directamente a ARCA. |

ARCA interviene mediante dos servicios con responsabilidades distintas: **WSAA acredita la identidad y concede acceso temporal**, mientras que **WSFE analiza y autoriza —o rechaza— el comprobante fiscal**.

## 2. Qué necesita la aplicación

El proceso Java debe iniciarse con estas variables de entorno:

| Variable | Para qué sirve |
|---|---|
| `ARCA_CUIT` | CUIT de 11 dígitos del emisor usado en homologación. |
| `ARCA_KEYSTORE_PATH` | Ruta al archivo PKCS#12 que contiene certificado y clave privada. |
| `ARCA_KEYSTORE_PASSWORD` | Contraseña del PKCS#12. Es un secreto y no debe guardarse en Git ni en Postman. |
| `ARCA_CONDICION_IVA_EMISOR` | Debe ser `MONOTRIBUTO` o una condición que contenga `EXENTO`. |

Ejemplo conceptual en PowerShell, reemplazando los valores:

```powershell
$env:ARCA_CUIT = "CUIT_DE_11_DIGITOS"
$env:ARCA_KEYSTORE_PATH = "C:\ruta\certificado-homologacion.p12"
$env:ARCA_KEYSTORE_PASSWORD = "CONTRASEÑA_DEL_ARCHIVO"
$env:ARCA_CONDICION_IVA_EMISOR = "MONOTRIBUTO"
```

Después se debe iniciar Spring Boot desde esa misma terminal. Cargar estos valores como variables de Postman **no configura el backend**.

La aplicación usa exclusivamente:

```text
WSAA: https://wsaahomo.afip.gov.ar/ws/services/LoginCms
WSFE: https://wswhomo.afip.gov.ar/wsfev1/service.asmx
```

Además, ARCA debe tener:

- el certificado de homologación vigente;
- el servicio `wsfe` autorizado para ese certificado;
- un punto de venta electrónico habilitado en homologación;
- el CUIT del emisor asociado a esas autorizaciones.

El endpoint de configuración verifica valores y archivos locales, pero no crea autorizaciones dentro de ARCA.

## 3. Cómo se autentica la aplicación

La autenticación ocurre en `ArcaHomologacionService` y se prueba con **ARCA / Probar autenticación WSAA**.

### Paso 1: cargar la identidad digital

El backend abre el archivo indicado por `ARCA_KEYSTORE_PATH` usando `ARCA_KEYSTORE_PASSWORD`. Obtiene:

- un certificado X.509, que identifica al titular;
- una clave privada RSA, que permite firmar.

También comprueba la vigencia del certificado. La clave privada nunca se envía a Postman ni a ARCA.

### Paso 2: construir el TRA

El backend genera un XML llamado TRA con:

- un identificador único;
- hora de inicio de validez;
- hora de vencimiento;
- servicio solicitado: `wsfe`.

El TRA formaliza la solicitud de acceso temporal al servicio de factura electrónica identificado como `wsfe`.

### Paso 3: firmar

La aplicación firma el TRA con la clave privada usando SHA-256 con RSA. El resultado es un CMS/PKCS#7 codificado en Base64.

La firma permite comprobar que la solicitud proviene del poseedor de la clave privada y que no fue modificada.

### Paso 4: pedir el ticket a WSAA

El CMS se envía a `loginCms`. Si WSAA lo acepta, devuelve un Ticket de Acceso con:

- `token`;
- `sign`;
- fecha y hora de vencimiento.

`token` y `sign` son credenciales temporales. El backend las conserva solamente en memoria y reutiliza el ticket mientras le queden más de cinco minutos de vigencia.

La aplicación **no expone `token` ni `sign`** en Postman, logs o respuestas HTTP. La prueba devuelve solo el ambiente, servicio, vencimiento, estado y un mensaje legible.

> Autenticarse no emite una factura. Solo confirma que la aplicación obtuvo permiso temporal para llamar a WSFE.

## 4. Cómo se crea un borrador

**Facturas / Crear borrador** llama a:

```http
POST /api/facturas
```

El backend guarda la factura con estado `SIN_EMITIR`. En ese momento:

- no tiene número fiscal;
- no tiene CAE;
- todavía no fue informada a ARCA;
- puede editarse o eliminarse según las reglas de la aplicación.

Postman toma el `id` devuelto y lo guarda automáticamente en `facturaId`. Las peticiones siguientes usan `{{facturaId}}`.

Los valores `puntoVentaId`, `listaPrecioId`, `articuloId` y similares son IDs internos de MariaDB, no códigos de ARCA. Deben corresponder a registros existentes.

## 5. Cómo se emite y obtiene el CAE

**Facturas / Emitir con ARCA** llama a:

```http
POST /api/facturas/{id}/emitir
```

### Paso 1: proteger la numeración

El backend bloquea temporalmente la factura y su punto de venta. Así evita que dos solicitudes simultáneas intenten usar el mismo número fiscal.

La secuencia pertenece a cada combinación de punto de venta y tipo de comprobante.

### Paso 2: validar el estado

Solo una factura `SIN_EMITIR` puede enviarse. Una factura ya emitida no puede autorizarse otra vez.

### Paso 3: elegir el comprobante

La integración actual admite emisores monotributistas o exentos y utiliza:

```text
CbteTipo = 11 = Factura C
```

La condición de IVA del receptor no cambia la letra C, pero se informa en `CondicionIVAReceptorId`.

### Paso 4: obtener el número siguiente

El backend llama a `FECompUltimoAutorizado` con CUIT, punto de venta y tipo 11. Si ARCA informa que el último fue el 25, solicita el 26.

No se toma simplemente el último número de MariaDB porque ARCA es la autoridad de la secuencia fiscal.

### Paso 5: solicitar el CAE

El backend llama a `FECAESolicitar` e informa:

- punto de venta, tipo y número;
- fecha del comprobante;
- documento y condición de IVA del receptor;
- total, moneda y cotización.

En la implementación actual:

- Consumidor Final sin cliente: documento tipo `99`, número `0`;
- cliente registrado: documento tipo `80`, con CUIT/CUIL sin guiones;
- concepto `1`: productos;
- cotización `1.000000`;
- importes con dos decimales;
- en Factura C, el total se informa como neto y el IVA discriminado es cero.

### Paso 6: interpretar la respuesta

Una respuesta HTTP correcta no alcanza. La aplicación exige:

```text
Resultado = A
y
CAE no vacío
```

Si ARCA aprueba:

- guarda número y fecha de emisión;
- guarda CAE y vencimiento;
- registra resultado `A`;
- cambia el estado a `EMITIDA`;
- persiste todo en MariaDB.

Si ARCA rechaza:

- la factura continúa `SIN_EMITIR`;
- no se guarda como autorizada;
- se registran resultado y motivo;
- la respuesta incluye mensajes para diagnosticar.

| Tipo de mensaje | Significado práctico |
|---|---|
| `ERROR` | Problema que impide completar la operación. |
| `OBSERVACION` | Advertencia o condición que debe leerse junto con el resultado. |
| `EVENTO` | Aviso general del servicio. |

## 6. Qué guarda la aplicación

| Campo | Significado |
|---|---|
| `numero` | Número fiscal autorizado. |
| `tipoComprobante` | Código fiscal; actualmente `11`. |
| `fechaEmision` | Fecha del comprobante enviada a ARCA. |
| `cae` | Código de autorización devuelto por ARCA. |
| `caeFechaVencimiento` | Vencimiento informado junto con el CAE. |
| `resultadoAfip` | Resultado `A`, `R` o `ERROR`. El nombre histórico se conserva por compatibilidad. |
| `motivoRechazo` | Explicación guardada ante rechazo o fallo de comunicación. |
| `estado` | Pasa a `EMITIDA` únicamente con aprobación y CAE. |

No existe una “fecha de creación del CAE” separada:

- `fechaEmision`: fecha fiscal del comprobante;
- `caeFechaVencimiento`: límite informado por ARCA;
- `fechaModificacion`: auditoría interna de la aplicación.

## 7. Orden de prueba en Postman

Importar:

- `TP-Facturacion-ARCA.postman_collection.json`;
- `TP-Facturacion-ARCA.postman_environment.json`.

Luego:

1. Seleccionar **TP Facturación - Local Homologación**.
2. Comprobar que el backend esté activo y que `baseUrl` apunte a él.
3. Ejecutar **ARCA / Configuración**.
4. Confirmar `listaParaProbar=true` y endpoints de homologación.
5. Ejecutar **ARCA / Probar autenticación WSAA**.
6. Confirmar `autenticada=true`.
7. Consultar **Opciones de factura** y **Buscar artículos**.
8. Cargar en el environment IDs reales: `usuarioId`, `puntoVentaId`, `condicionIvaId`, `tipoMonedaId`, `listaPrecioId`, `articuloId` y `listaPrecioArticuloId`.
9. Ejecutar **Facturas / Crear borrador**. Se guardará `facturaId`.
10. Ejecutar **Obtener por ID** y comprobar `SIN_EMITIR`, sin número ni CAE.
11. Ejecutar **Emitir con ARCA**.
12. Revisar `emitida`, `estado`, `numero`, `cae`, `caeFechaVencimiento`, `mensaje` y `mensajes`.
13. Consultar nuevamente por ID para verificar lo persistido.
14. Abrir el PDF solamente después de una aprobación.

No se debe repetir la emisión sobre el mismo `facturaId`. Para otra prueba hay que crear otro borrador.

## 8. Cómo leer las respuestas

### Configuración

`GET /api/facturas/arca/configuracion` informa la preparación local:

```json
{
  "ambiente": "HOMOLOGACION",
  "endpointsHomologacion": true,
  "listaParaProbar": true
}
```

`listaParaProbar=true` significa que el CUIT tiene formato válido, el PKCS#12 existe, hay contraseña y las URLs son de homologación. No demuestra todavía que ARCA aceptará el certificado.

### Autenticación correcta

```json
{
  "autenticada": true,
  "ambiente": "HOMOLOGACION",
  "servicio": "wsfe",
  "vencimiento": "2026-11-03T15:30:00Z",
  "mensaje": "TRA firmado y ticket de acceso obtenido correctamente"
}
```

La fecha es ilustrativa. La `Z` representa horario UTC.

### Emisión aprobada

```json
{
  "emitida": true,
  "facturaId": 10,
  "numero": 26,
  "estado": "EMITIDA",
  "cae": "...",
  "caeFechaVencimiento": "2026-11-13",
  "mensaje": "ARCA autorizó el comprobante",
  "mensajes": []
}
```

Los valores son ejemplos; los válidos son los devueltos por ARCA.

### Emisión rechazada

Un rechazo fiscal puede llegar con HTTP 200 porque la comunicación funcionó. Hay que mirar `emitida`, no solo el estado HTTP:

```json
{
  "emitida": false,
  "estado": "SIN_EMITIR",
  "cae": null,
  "mensaje": "Motivo informado por ARCA",
  "mensajes": [
    {
      "tipo": "ERROR",
      "codigo": "...",
      "descripcion": "Detalle informado por ARCA"
    }
  ]
}
```

## 9. Errores frecuentes

| Situación | Qué revisar |
|---|---|
| `listaParaProbar=false` | Revisar en la respuesta CUIT, archivo, contraseña y endpoints. |
| Falta `ARCA_CUIT` | La variable no llegó al proceso Java o no tiene 11 dígitos. |
| No existe el certificado | `ARCA_KEYSTORE_PATH` no apunta a un archivo accesible. |
| El PKCS#12 no contiene clave privada | Puede haberse exportado solo el certificado público. |
| Certificado vencido | Se necesita uno vigente de homologación. |
| HTTP `502 Bad Gateway` | Falló configuración, autenticación o comunicación con ARCA. Leer `detail`. |
| HTTP `409 Conflict` | La factura ya no está sin emitir o hubo un conflicto de negocio/concurrencia. |
| ARCA devuelve `R` | La comunicación funcionó, pero rechazó el comprobante. Revisar mensajes. |
| IDs inexistentes | Actualizar Postman con IDs que existan en MariaDB. |
| Punto de venta incorrecto | Su número debe estar habilitado también en ARCA homologación. |
| No se genera el PDF | Confirmar primero que existan número y CAE. |

## 10. Seguridad

- No subir el PKCS#12 ni su contraseña al repositorio.
- No copiar `token`, `sign` o la clave privada a Postman.
- No registrar secretos completos en logs o capturas.
- El backend necesita certificado **y** clave privada dentro del PKCS#12.
- La contraseña protege el archivo; la clave privada produce la firma.
- Reiniciar la aplicación después de cambiar variables de entorno.

## 11. Alcance de la anulación

```http
POST /api/facturas/{id}/anular
```

Esta operación cambia el estado interno, pero **no anula fiscalmente** un comprobante que ARCA ya autorizó.

Una vez otorgado el CAE, la factura pertenece a la secuencia fiscal. En producción, una reversión se realiza mediante el comprobante correspondiente, normalmente una nota de crédito vinculada. Cambiar MariaDB no modifica los registros de ARCA.

## 12. Glosario de términos y siglas

| Término o sigla | Denominación completa y definición |
|---|---|
| **ARCA** | **Agencia de Recaudación y Control Aduanero**. Organismo fiscal nacional que administra, entre otros, los servicios de autenticación y facturación electrónica. |
| **AFIP** | **Administración Federal de Ingresos Públicos**. Denominación anterior del organismo. Permanece en nombres históricos del código, como `resultadoAfip`. |
| **Homologación** | Ambiente de pruebas de ARCA, separado de producción. Sus certificados, autorizaciones, puntos de venta y comprobantes son propios de ese ambiente. |
| **Producción** | Ambiente real de operación fiscal. No comparte automáticamente la configuración de homologación. |
| **Backend** | Aplicación que ejecuta la lógica, valida, persiste, firma y se comunica con ARCA. En este proyecto está desarrollado con Java y Spring Boot. |
| **API** | **Application Programming Interface** o interfaz de programación de aplicaciones. Contrato de operaciones que otro programa puede invocar. |
| **Endpoint** | Dirección y operación concreta de una API, por ejemplo `POST /api/facturas/{id}/emitir`. |
| **Postman** | Cliente que envía peticiones HTTP y permite inspeccionar respuestas de una API. |
| **Environment de Postman** | Conjunto de variables reutilizables, como `baseUrl` y `facturaId`. No configura las variables del proceso Java. |
| **HTTP** | **Hypertext Transfer Protocol**. Protocolo de petición y respuesta usado entre Postman y el backend, y como transporte de SOAP. |
| **URL** | **Uniform Resource Locator**. Dirección completa de un recurso o servicio en la red. |
| **JSON** | **JavaScript Object Notation**. Formato de datos empleado por la API del proyecto en peticiones y respuestas. |
| **SOAP** | **Simple Object Access Protocol**. Protocolo de mensajería basado en XML utilizado por los servicios de ARCA. |
| **XML** | **Extensible Markup Language**. Lenguaje de marcado que estructura el TRA y los mensajes SOAP. |
| **WS** | **Web Service** o servicio web. Sistema accesible por red que expone operaciones para otras aplicaciones. |
| **WSAA** | **Web Service de Autenticación y Autorización**. Valida el CMS firmado y entrega el Ticket de Acceso. No emite ni autoriza la factura. |
| **WSFE / WSFEv1** | **Web Service de Factura Electrónica, versión 1**. Consulta numeración fiscal y procesa solicitudes de CAE. |
| **TRA** | **Ticket de Requerimiento de Acceso**. XML con identificador, vigencia y servicio solicitado; se firma antes de enviarlo a WSAA. |
| **Ticket de Acceso** | Credencial temporal que WSAA devuelve después de validar el TRA firmado. Contiene token, sign y vencimiento. |
| **Token** | Valor temporal emitido por WSAA que integra las credenciales usadas para invocar WSFE. No es una sigla. |
| **Sign** | Valor criptográfico temporal emitido por WSAA que se envía junto con el token. No es una sigla. |
| **X.509** | Estándar para certificados digitales. El certificado identifica al titular, contiene su clave pública e informa emisor y vigencia. |
| **Clave privada** | Componente secreto del par criptográfico. Firma el TRA y nunca debe compartirse. |
| **Clave pública** | Componente público del par criptográfico. Permite verificar la firma sin revelar la clave privada. |
| **Firma digital** | Resultado criptográfico que acredita integridad del contenido y posesión de la clave privada. No es una imagen de una firma manuscrita. |
| **PKCS** | **Public-Key Cryptography Standards**. Familia de estándares de criptografía de clave pública. |
| **PKCS#12 / P12 / PFX** | Estándar número 12 de PKCS y extensiones habituales del contenedor. Puede almacenar certificado y clave privada protegidos por contraseña. |
| **PKCS#7** | Estándar histórico número 7 de PKCS para mensajes criptográficos; antecedente del formato CMS usado para la firma enviada a WSAA. |
| **CMS** | **Cryptographic Message Syntax**. Formato que encapsula el TRA, su firma y el certificado. |
| **RSA** | Algoritmo de clave pública nombrado por **Rivest, Shamir y Adleman**. Se utiliza para producir la firma. |
| **SHA-256** | **Secure Hash Algorithm de 256 bits**. Función que calcula una huella del TRA antes de aplicar la firma RSA. |
| **Base64** | Codificación que representa bytes mediante caracteres de texto. Permite incluir el CMS en XML; no cifra el contenido. |
| **CUIT** | **Clave Única de Identificación Tributaria**. Identifica fiscalmente al emisor o receptor; para el emisor se esperan 11 dígitos. |
| **CUIL** | **Código Único de Identificación Laboral**. Identifica a una persona; la aplicación lo normaliza sin guiones. |
| **Punto de venta** | Número habilitado en ARCA que identifica el origen de emisión. Mantiene una secuencia independiente por tipo de comprobante. |
| **Factura C** | Tipo de comprobante empleado por esta integración para emisores monotributistas o exentos. Su código es `11`. |
| **Cbte** | Abreviatura de “comprobante” usada en campos y operaciones de WSFE. |
| **CbteTipo** | Campo que contiene el código numérico del tipo de comprobante; aquí vale `11`. |
| **FECompUltimoAutorizado** | Operación de WSFE que devuelve el último número autorizado para un punto de venta y un tipo de comprobante. |
| **FECAESolicitar** | Operación de WSFE que envía los datos fiscales de uno o más comprobantes para solicitar su autorización. Este proyecto envía uno por vez. |
| **CAE** | **Código de Autorización Electrónico**. Código devuelto por ARCA que acredita la autorización fiscal del comprobante. |
| **Vencimiento del CAE** | Fecha informada por ARCA junto con el CAE. No implica que la factura deje de existir después de esa fecha. |
| **Resultado `A`** | Resultado **Aprobado**. La aplicación exige además que el CAE no esté vacío. |
| **Resultado `R`** | Resultado **Rechazado**. Deben analizarse los códigos y descripciones devueltos. |
| **IVA** | **Impuesto al Valor Agregado**. Tributo cuya condición fiscal del emisor y receptor forma parte de los datos enviados. |
| **Condición de IVA** | Categoría tributaria del emisor o receptor frente al Impuesto al Valor Agregado. |
| **Consumidor Final** | Condición del receptor sin identificación fiscal individual en este flujo; se envía documento tipo `99` y número `0`. |
| **`SIN_EMITIR`** | Estado local del borrador que todavía no fue autorizado por ARCA. |
| **`EMITIDA`** | Estado local asignado únicamente después de recibir aprobación y CAE. |
| **MariaDB** | Sistema de gestión de base de datos relacional donde se persiste la factura y el resultado fiscal. |
| **Transacción** | Unidad de cambios de base que se confirma o revierte en conjunto para evitar estados parciales. |
| **Bloqueo pesimista** | Reserva temporal de un registro que impide actualizaciones simultáneas incompatibles durante la emisión. |
| **HTTP 200** | Código HTTP de procesamiento técnico correcto. No prueba por sí solo que ARCA haya aprobado: debe revisarse `emitida`. |
| **HTTP 409** | Código HTTP de conflicto con el estado del recurso o una regla de negocio. |
| **HTTP 502** | Código HTTP que indica que el backend falló al configurar, autenticar o comunicarse con ARCA. |
| **UTC / `Z`** | **Coordinated Universal Time** o Tiempo Universal Coordinado. La letra `Z` al final de una fecha técnica indica UTC. |

## 13. Secuencia consolidada de emisión

1. La factura se guarda como `SIN_EMITIR`.
2. El backend firma un TRA con certificado y clave privada.
3. WSAA devuelve credenciales temporales que permanecen en el backend.
4. El backend consulta a WSFE el último número autorizado.
5. Solicita el siguiente mediante `FECAESolicitar`.
6. Solo con `Resultado=A` y CAE pasa a `EMITIDA`.
7. Un rechazo conserva el borrador y deja mensajes de diagnóstico.
8. La anulación local no revierte fiscalmente una factura autorizada.
