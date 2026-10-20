# TP Spring Boot - API REST CRUD de facturación

Adaptación del modelo JPA de `tp_jpa_grupal` a una API Spring Boot con MariaDB,
DTOs, JPQL, reportes PDF/XLSX y operaciones de escritura POST, PUT y DELETE.
Incluye un frontend HTML5 + TypeScript en la carpeta `frontend`.

## Cómo encontrar cada consigna en el código

Las implementaciones anteriores conservan comentarios `CONSIGNA - PARTE`.
Las incorporadas en esta etapa están identificadas como:

```text
CONSIGNA ESCRITURA
```

En IntelliJ se pueden localizar con `Ctrl + Shift + F` buscando ese texto.

## Trazabilidad de la nueva consigna

| Punto | Implementación | Archivo principal |
| --- | --- | --- |
| Objetivo a | Request DTOs separados de JPA | `dto/FacturaRequestDTO.java`, `dto/FacturaDetalleRequestDTO.java` |
| Objetivo b | Creación, actualización y eliminación transaccionales | `service/FacturaService.java` |
| Objetivo c | POST 201, PUT 200, DELETE 204 y errores 404 | `controller/FacturaRestController.java`, `controller/ApiExceptionHandler.java` |
| 1.a | `FacturaRequestDTO` y `FacturaDetalleRequestDTO` para POST/PUT | `dto/` |
| 1.b | Relaciones recibidas como IDs, sin entidades anidadas | ambos Request DTOs y `FacturaService.aplicarCabecera` |
| 2.a | Atomicidad con `@Transactional` | métodos `crear`, `actualizar` y `eliminar` |
| 2.b | Búsqueda por ID, mapeo, cálculos y persistencia en cascada | `FacturaService.crear` y `reemplazarDetalles` |
| 2.c | Búsqueda por ID y actualización de cabecera/ítems | `FacturaService.actualizar` |
| 2.d | DELETE físico y anulación lógica con fecha | `FacturaService.eliminar` y `actualizarFechaAnulacion` |
| 3.1 POST | `POST /api/facturas`, respuesta 201 + `FacturaReporteDTO` | `FacturaRestController` |
| 3.1 PUT | `PUT /api/facturas/{id}`, respuesta 200 | `FacturaRestController` |
| 3.1 DELETE | `DELETE /api/facturas/{id}`, respuesta 204 | `FacturaRestController` |
| 4.a y 5 | Interfaces TypeScript espejo de los DTOs Java | `frontend/src/models/factura.dto.ts` |
| 4.b y 6 | Cliente HTTP con `fetch`, `async/await`, GET/POST/PUT/DELETE | `frontend/src/services/FacturaService.ts` |
| 4.c y 7.a | Tres secciones HTML navegables por pestañas: filtros, alta dinámica y listado CRUD | `frontend/index.html` |
| 7.b | DOM, estado de ítems y eventos de interfaz | `frontend/src/app.ts` |

## Decisiones de negocio

- Cada cliente nuevo se vincula con una `CondicionIva` mediante
  `condicionIvaId`. Al facturarlo, el backend verifica que la condición enviada
  coincida con la registrada en el cliente.
- `POST` exige una factura completa con al menos un detalle.
- `PUT` acepta una actualización completa o parcial. El frontend lo utiliza para
  enviar solamente `estado` y `observaciones` al anular.
- Al cambiar el estado a `ANULADA`, el servicio asigna `fechaAnulacion`.
- `DELETE` realiza eliminación física. Debido a `cascade = ALL` y
  `orphanRemoval = true`, también elimina sus detalles.
- `precioUnitario: null` utiliza el precio guardado en `ListaPrecioArticulo`.
- `porcentajeIva: null` utiliza 21 % como valor predeterminado.
- El servicio no acepta neto, IVA, subtotal ni total calculados por el cliente.

Por cada detalle calcula:

```text
bruto = cantidad * precioUnitario
importeNeto = bruto - bonificación
importeIva = importeNeto * porcentajeIva / 100
importeSubtotal = importeNeto + importeIva
importeTotal = suma de los subtotales
importeSaldo = importeTotal - importeCobrado
```

`FacturaReporteDTO` incluye `id` y `estado`. El primero permite invocar
`PUT /{id}` y `DELETE /{id}` desde cada fila; el segundo permite mostrar la
situación actual y ocultar la acción de anular cuando ya está anulada.

## DTO de entrada para POST

Las relaciones se vinculan exclusivamente mediante identificadores:

```json
{
  "numero": 1001,
  "fechaEmision": "2026-03-15T10:00:00",
  "puntoVentaId": 1,
  "clienteId": 1,
  "condicionIvaId": 1,
  "tipoMonedaId": 1,
  "usuarioId": 1,
  "estado": "EMITIDA",
  "importeCobrado": 0,
  "observaciones": "Factura de prueba",
  "detalles": [
    {
      "articuloId": 1,
      "listaPrecioArticuloId": 1,
      "descripcion": "Artículo de prueba",
      "cantidad": 2,
      "precioUnitario": null,
      "porcentajeBonificacion": 0,
      "porcentajeIva": 21
    }
  ]
}
```

Para anular con PUT alcanza con:

```json
{
  "estado": "ANULADA",
  "observaciones": "Anulada por el usuario"
}
```

## Códigos HTTP

| Operación | Respuesta correcta | Error común |
| --- | --- | --- |
| POST | `201 Created` + `FacturaReporteDTO` | `400` datos inválidos, `404` ID relacionado inexistente |
| PUT | `200 OK` + `FacturaReporteDTO` | `404` factura inexistente |
| DELETE | `204 No Content` | `404` factura inexistente |
| GET | `200 OK` + arreglo JSON | `400` filtros inválidos |

## Requisitos del backend

- JDK 17 o superior.
- MariaDB en `localhost:3306`.
- Base `tp_spring_boot` con `utf8mb4`.

```sql
CREATE DATABASE tp_spring_boot
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

Variables de entorno de la configuración de IntelliJ:

```text
DB_URL=jdbc:mariadb://localhost:3306/tp_spring_boot
DB_USERNAME=root
DB_PASSWORD=tu_clave
```

Desde PowerShell también pueden configurarse así:

```powershell
$env:DB_URL="jdbc:mariadb://localhost:3306/tp_spring_boot"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="tu_clave"
.\gradlew.bat bootRun
```

La API queda disponible en `http://localhost:8080/api/facturas`.

## Endpoints

- `GET /api/facturas`: consulta DTOs y acepta `fechaDesde`, `fechaHasta`,
  `estado` y `montoMinimo`.
- `POST /api/facturas`: alta en cascada.
- `PUT /api/facturas/{id}`: actualización completa o parcial/anulación.
- `DELETE /api/facturas/{id}`: eliminación física.
- `GET /api/facturas/reporte/pdf`: descarga PDF con los mismos filtros.
- `GET /api/facturas/reporte/excel`: descarga XLSX con los mismos filtros.
- `POST /api/datos-maestros/...`: prepara las relaciones necesarias.

## Ejecutar el frontend

El frontend necesita Node.js. Si se usa NVM para Windows:

```powershell
nvm use 20.20.2
cd frontend
npm install
npm run dev
```

Abrir `http://localhost:5173`. El backend permite CORS para ese origen.
El backend debe continuar ejecutándose en el puerto 8080.

La interfaz usa una barra superior con pestañas y muestra un solo panel por vez.
No utiliza componentes tipo card ni frameworks visuales; el HTML se estiliza con
selectores semánticos, IDs y atributos `data-*` para evitar clases innecesarias.

Los IDs de usuario, punto de venta, cliente, condición de IVA, moneda, artículo
y precio se obtienen cargando primero los datos maestros. La colección Postman
automatiza ese proceso.

## Postman

Eliminar la colección anterior de Postman e importar nuevamente:

```text
postman/TP-Spring-Boot-Facturas.postman_collection.json
```

La colección contiene 31 solicitudes. Ejecutar primero los datos maestros, luego
las altas y finalmente la carpeta `05 - Actualizar, anular y eliminar`.
La explicación completa está en `postman/GUIA_POSTMAN.md`.

## Consigna anterior conservada

También continúan implementados Lombok en las entidades, `SELECT new` hacia
`FacturaReporteDTO`, filtros JPQL opcionales, JSON, PDF y Excel. Los comentarios
`CONSIGNA - PARTE` muestran esas implementaciones en el código.
