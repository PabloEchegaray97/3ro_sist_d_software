# TP Spring Boot - Facturacion

Adaptacion del modelo JPA de `tp_jpa_grupal` a Spring Boot con Lombok, DTOs,
consulta JPQL dinamica, API REST y exportacion de reportes en PDF y Excel.

## Trazabilidad de la consigna

En el codigo, cada implementacion obligatoria esta marcada con comentarios que
comienzan con `CONSIGNA - PARTE`. Desde IntelliJ se pueden encontrar todos con
`Ctrl + Shift + F` y buscando ese texto.

| Punto de la consigna | Archivo principal donde se demuestra |
| --- | --- |
| 1.1 Dependencia Lombok | `build.gradle` |
| 1.2 Refactor de superclases y entidades | `model/EntityId.java`, `model/AuditoriaApp.java` y entidades de `model/` |
| 1.3 Seguridad Lombok/JPA y `callSuper` | `model/AuditoriaApp.java`, `model/FacturaVenta.java` y relaciones de `model/` |
| 2.1 DTO con siete campos | `dto/FacturaReporteDTO.java` |
| 2.2 Lombok en el DTO | `dto/FacturaReporteDTO.java` |
| 2.3 `SELECT new`, joins, `COALESCE`, `COUNT` y `GROUP BY` | `repository/FacturaReporteRepository.java` |
| 2 PDF y Excel desde la consulta | `controller/FacturaRestController.java` y `service/ReporteExportService.java` |
| 3.1 Spring Boot, Web, JPA, MariaDB y Lombok | `build.gradle` y `application.properties` |
| 3.2 Servicio/repositorio dinamico | `service/FacturaService.java` y `repository/FacturaReporteRepository.java` |
| 3.3 Controlador `/api/facturas` | `controller/FacturaRestController.java` |
| 3.4 GET con respuesta JSON | `controller/FacturaRestController.java` |
| 3.5 Cuatro filtros opcionales | `controller/FacturaRestController.java` y `repository/FacturaReporteRepository.java` |

## Requisitos

- JDK 17 o superior.
- MariaDB en `localhost:3306`.
- Una base vacia llamada `tp_spring_boot`.

```sql
CREATE DATABASE tp_spring_boot
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

El proyecto usa estas variables de entorno para no guardar credenciales:

```powershell
$env:DB_URL="jdbc:mariadb://localhost:3306/tp_spring_boot"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="tu_clave"
```

## Ejecutar

Desde la carpeta que contiene `build.gradle`:

```powershell
.\gradlew.bat bootRun
```

La API queda disponible en `http://localhost:8080/api/facturas`.

## Endpoints

- `POST /api/datos-maestros/...`: crea los datos relacionados necesarios.
- `POST /api/facturas`: crea una factura y calcula sus importes.
- `GET /api/facturas`: devuelve JSON.
- `GET /api/facturas/reporte/pdf`: descarga un PDF.
- `GET /api/facturas/reporte/excel`: descarga un archivo XLSX.

Los tres aceptan los mismos filtros opcionales:

- `fechaDesde=yyyy-MM-dd`
- `fechaHasta=yyyy-MM-dd`
- `estado=EMITIDA`
- `montoMinimo=1500`

## Postman

Antes de probar, la base MariaDB debe contener facturas con `condicionIva`,
`puntoVenta` y al menos un detalle, porque la consulta solicitada usa `JOIN`
sobre esas relaciones.

Eliminar de Postman la version anterior de la coleccion e importar nuevamente
`postman/TP-Spring-Boot-Facturas.postman_collection.json`, para evitar tener dos
colecciones con el mismo nombre. La nueva version contiene 27 requests agrupados
y descriptos: datos maestros, creacion de facturas, consultas, errores y
reportes. Su variable `baseUrl` ya vale `http://localhost:8080`.

La guia detallada esta en `postman/GUIA_POSTMAN.md`.

Para guardar un reporte desde Postman se puede usar **Send and Download**.

