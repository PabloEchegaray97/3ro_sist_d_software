# ARCA en homologación

La aplicación no expone `token` ni `sign`. Son credenciales temporales y no deben aparecer en Postman, logs ni respuestas HTTP.

## Cómo se firma el acceso

1. `ArcaHomologacionService` genera el XML TRA para el servicio `wsfe`.
2. Carga la clave privada y el certificado desde el PKCS#12 configurado por `ARCA_KEYSTORE_PATH` y `ARCA_KEYSTORE_PASSWORD`.
3. Firma el TRA como CMS/PKCS#7 usando SHA256 con RSA.
4. Envía el CMS a WSAA homologación mediante `loginCms`.
5. Conserva en memoria el ticket hasta cinco minutos antes de su vencimiento.

La petición **ARCA / Probar autenticación WSAA** ejecuta ese flujo y devuelve solamente el servicio y el vencimiento.

## Cómo se emite

La petición **Facturas / Emitir con ARCA** llama `POST /api/facturas/{id}/emitir`. El backend:

1. Bloquea la factura y su punto de venta.
2. Consulta `FECompUltimoAutorizado` para Factura C (`CbteTipo=11`).
3. Calcula el número siguiente.
4. Construye y envía `FECAESolicitar`.
5. Si ARCA devuelve `Resultado=A`, guarda número, fecha de emisión, CAE, vencimiento y estado `EMITIDA`.
6. Si ARCA rechaza, conserva la factura en `SIN_EMITIR` y registra los mensajes.

## Orden de prueba

1. Importar el environment y seleccionarlo.
2. Ejecutar **ARCA / Configuración**.
3. Ejecutar **ARCA / Probar autenticación WSAA**.
4. Revisar IDs con **Datos maestros / Opciones de factura** y **Buscar artículos**.
5. Ajustar las variables del environment.
6. Ejecutar **Facturas / Crear borrador**. El test guarda automáticamente `facturaId`.
7. Ejecutar **Facturas / Emitir con ARCA**.
8. Consultar la factura y abrir su PDF.

La operación `POST /api/facturas/{id}/anular` cambia el estado interno para cumplir el ciclo de vida de la aplicación. No anula fiscalmente un comprobante ya autorizado por ARCA: en un sistema productivo esa reversión debe emitirse mediante la nota de crédito correspondiente.

Requisitos del proceso Java: `ARCA_CUIT`, `ARCA_KEYSTORE_PATH`, `ARCA_KEYSTORE_PASSWORD` y `ARCA_CONDICION_IVA_EMISOR=MONOTRIBUTO`.
