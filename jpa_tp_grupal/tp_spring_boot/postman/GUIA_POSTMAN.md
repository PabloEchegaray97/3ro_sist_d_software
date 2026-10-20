# Guía de Postman: facturación REST

## Importar la versión correcta

1. Iniciar MariaDB y `DemoApplication`.
2. Eliminar de Postman la colección anterior para evitar solicitudes duplicadas.
3. Importar `TP-Spring-Boot-Facturas.postman_collection.json`.
4. No hace falta crear un Environment: se utilizan variables de colección.

La variable `baseUrl` ya contiene `http://localhost:8080`.

## Orden de ejecución

1. Ejecutar del 01 al 10 en `00 - Preparar datos maestros`.
2. Ejecutar 11 y 12 en `01 - Crear facturas`.
3. Probar consultas, errores y reportes.
4. Ejecutar al final del 28 al 31 en `05 - Actualizar, anular y eliminar`.

Las altas de datos maestros guardan automáticamente los IDs en las variables de
la colección. Por ejemplo, crear el artículo guarda `articuloId`, y crear su
precio guarda `listaPrecioArticuloId`.

## POST: creación en cascada

Los requests 11 y 12 envían `FacturaRequestDTO`. Cada detalle incluye:

- `articuloId`: artículo que se factura.
- `listaPrecioArticuloId`: precio asociado al mismo artículo.
- `cantidad` y `precioUnitario`; si el precio es `null`, usa el de la lista.
- `porcentajeBonificacion`.
- `porcentajeIva`; si es `null`, el backend utiliza 21 %.

`fechaEmision` se envía como fecha y hora en formato
`yyyy-MM-dd'T'HH:mm:ss`, por ejemplo `2026-03-15T10:00:00`.

El backend calcula neto, IVA, subtotal, total y saldo. La respuesta correcta es
`201 Created` con un `FacturaReporteDTO`, incluidos su `id` y `estado`.

## GET y filtros

Los filtros de fecha usan `yyyy-MM-dd`. Las variables editables son:

- `fechaDesde`
- `fechaHasta`
- `estado`
- `montoMinimo`

Se modifican desde los tres puntos de la colección, opción **Edit**, pestaña
**Variables**. Los cuatro filtros son opcionales y pueden combinarse.

## PUT completo y anulación

El request 28 utiliza `PUT /api/facturas/{{facturaId}}` y reemplaza cabecera e
ítems. Debe responder `200 OK` y devolver el DTO recalculado.

El request 29 envía solamente:

```json
{
  "estado": "ANULADA",
  "observaciones": "Anulada desde Postman"
}
```

También responde `200 OK`. El servicio asigna automáticamente
`fechaAnulacion`.

## DELETE y 404

El request 30 elimina físicamente la factura y sus detalles. La respuesta
correcta es `204 No Content`, por lo que Postman no mostrará un body.

El request 31 intenta eliminar un ID inexistente y comprueba `404 Not Found`.

## PDF y Excel

Los reportes usan la misma consulta y filtros del JSON. Para guardarlos, abrir la
flecha junto a **Send** y elegir **Send and Download**. Un XLSX visto directamente
en el Body de Postman aparece como datos binarios que empiezan con `PK`; eso es
normal y no indica un archivo dañado.

## Importante al repetir pruebas

Los números 1001 y 1002 son únicos. Si ya existen, el POST responde 400. Se puede
cambiar `numero` en el JSON o eliminar las facturas existentes antes de repetir.
