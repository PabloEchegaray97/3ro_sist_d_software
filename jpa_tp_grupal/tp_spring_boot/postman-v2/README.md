# Colección Postman v2.1

Importar estos dos archivos en Postman:

- `TP-Facturacion-ARCA.postman_collection.json`
- `TP-Facturacion-ARCA.postman_environment.json`

Seleccionar el environment **TP Facturación - Local Homologación**. La colección cubre facturas, reportes, ARCA y todos los endpoints actuales de datos maestros.

Los cuerpos usan variables de IDs porque dependen de los datos presentes en MariaDB. Antes de crear una factura, ejecutar los exploradores y actualizar `puntoVentaId`, `condicionIvaId`, `tipoMonedaId`, `listaPrecioId`, `articuloId` y `listaPrecioArticuloId`.

La colección no contiene certificados, contraseñas, token WSAA ni `sign`.
