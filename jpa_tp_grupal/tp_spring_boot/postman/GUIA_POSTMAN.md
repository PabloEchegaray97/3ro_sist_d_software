# Guia de Postman

## 1. Antes de importar

1. Iniciar MariaDB.
2. Confirmar que existe la base `tp_spring_boot`.
3. Ejecutar `DemoApplication` y esperar el mensaje `Started DemoApplication`.
4. Si ya se importo una version anterior, eliminarla de Postman para no confundir
   requests viejos con los nuevos.
5. Importar `TP-Spring-Boot-Facturas.postman_collection.json`.

La coleccion usa `http://localhost:8080` como `baseUrl`.

## 2. Como ejecutar la coleccion

La primera vez, abrir la carpeta **00 - Preparar datos maestros** y enviar los
requests desde el 01 hasta el 10, respetando el orden. Cada respuesta guarda su
ID automáticamente en una variable de coleccion. Por ejemplo, el request 01
guarda `usuarioId` y el request 02 lo utiliza sin que haya que copiarlo.

Despues ejecutar:

1. **11 - Crear factura con cliente**.
2. **12 - Crear factura Consumidor Final**.
3. Los requests de consulta, validacion y reportes que se quieran comprobar.

No volver a ejecutar toda la carpeta de datos maestros sobre la misma base salvo
que se quieran crear registros repetidos. Las facturas 1001 y 1002 tampoco se
pueden volver a crear con el mismo numero.

## 3. Que representa cada grupo

### 00 - Preparar datos maestros

Crea las entidades que una factura necesita por sus relaciones JPA:

- Usuario: completa los campos de auditoria.
- Condicion IVA: se muestra en el reporte.
- Tipo de moneda: moneda usada por la factura.
- Punto de venta: lugar de emision.
- Cliente: incluye contacto y domicilio.
- Rubro y marca: clasifican el articulo.
- Lista de precios y articulo: definen lo que se vende.
- Precio de articulo: relacion que usa cada detalle de factura.

### 01 - Crear facturas

El endpoint `POST /api/facturas` recibe IDs existentes y una lista de detalles.
El servidor calcula:

- `importeSubtotal` de cada detalle: cantidad por precio unitario.
- `importeTotal`: suma de todos los subtotales.
- `importeSaldo`: importe total menos importe cobrado.

Si `precioUnitario` es `null`, se utiliza el precio de `ListaPrecioArticulo`.
Si `clienteId` es `null`, el reporte muestra `Consumidor Final`.

`cantidadItems` representa la cantidad de lineas de detalle, porque la consulta
usa `COUNT(d)`. No representa la suma de unidades vendidas.

### 02 - Consultar facturas y filtros

El endpoint `GET /api/facturas` devuelve solamente los siete campos de
`FacturaReporteDTO`. Los parametros son opcionales:

- `fechaDesde`: fecha minima incluida, con formato `yyyy-MM-dd`.
- `fechaHasta`: fecha maxima incluida, con formato `yyyy-MM-dd`.
- `estado`: por ejemplo `EMITIDA` o `ANULADA`.
- `montoMinimo`: importe total minimo incluido.

Los cuatro filtros pueden enviarse juntos.

### 03 - Validar errores

Contiene solicitudes incorrectas a proposito. Sirven para verificar respuestas
HTTP 400 ante fechas mal formadas, fechas invertidas, montos negativos y numeros
de factura repetidos.

### 04 - Descargar reportes

Los reportes PDF y Excel utilizan la misma consulta y los mismos filtros del
JSON. Para guardar el archivo, abrir la flecha junto al boton **Send** y elegir
**Send and Download**.

## 4. Variables guardadas automaticamente

Postman completa estas variables al ejecutar los requests de alta:

- `usuarioId`
- `condicionIvaId`
- `tipoMonedaId`
- `puntoVentaId`
- `clienteId`
- `rubroId`
- `marcaId`
- `listaPrecioId`
- `articuloId`
- `listaPrecioArticuloId`
- `facturaId`

Se pueden revisar desde la coleccion, pestaña **Variables**, columna **Current
value**.
