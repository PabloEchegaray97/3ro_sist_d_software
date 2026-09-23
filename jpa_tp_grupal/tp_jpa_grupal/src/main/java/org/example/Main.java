package org.example;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import modelo.Articulo;
import modelo.Cliente;
import modelo.FacturaVenta;
import modelo.FacturaVentaDetalle;
import modelo.Marca;
import modelo.PuntoVenta;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("FacturacionPU");
        EntityManager em = emf.createEntityManager();

        try {
            ejecutarConsultas(em);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }
    }

    private static void ejecutarConsultas(EntityManager em) {
        Date fechaDesde = crearFecha(2020, Calendar.JANUARY, 1);
        Date fechaHasta = crearFecha(2030, Calendar.DECEMBER, 31);

        consulta01FacturasCompletas(em);
        consulta02ProyeccionFacturas(em);
        consulta03ArticulosPorRubro(em, "Electronica");
        consulta04FacturasPorRangoFechas(em, fechaDesde, fechaHasta);
        consulta05FacturasEmitidasNoAnuladas(em);
        consulta06ClientesPorTextoOCuit(em, "cliente", "20-");
        consulta07EstadosDistintos(em);
        consulta08AgregacionesFacturas(em);
        consulta09PuntosVentaPorNumeros(em, Arrays.asList(1, 2, 5, 10));
        consulta10FacturasPorUsuarioCarga(em, "admin");
        consulta11DetallesPorPuntoVenta(em, 10);
        consulta12ArticulosConMarca(em);
        consulta13FacturasPorMarca(em, "Logitech");
        consulta14FacturasMayoresAlPromedio(em);
        consulta15AgrupamientoPorPuntoVenta(em);
        consulta16UsuariosConMasDeCincoFacturas(em);
        consulta17VentasAgrupadasPorMarca(em);
        consulta18MarcasConArticulosFacturados(em);
        consulta19ArticulosNuncaFacturados(em);
        consulta20CategoriaFacturaPorImporte(em);
    }

    private static Date crearFecha(int anio, int mes, int dia) {
        Calendar calendario = Calendar.getInstance();
        calendario.set(anio, mes, dia, 0, 0, 0);
        calendario.set(Calendar.MILLISECOND, 0);
        return calendario.getTime();
    }

    private static void consulta01FacturasCompletas(EntityManager em) {
        String jpql = "SELECT fv FROM FacturaVenta fv";
        imprimirConsulta("1. Consulta de entidades completas", jpql);

        List<FacturaVenta> facturas = em.createQuery(jpql, FacturaVenta.class).getResultList();
        imprimirFacturas(facturas);
    }

    private static void consulta02ProyeccionFacturas(EntityManager em) {
        String jpql = "SELECT fv.numero, fv.fechaEmision, fv.importeTotal FROM FacturaVenta fv";
        imprimirConsulta("2. Proyeccion de atributos especificos", jpql);

        List<Object[]> filas = em.createQuery(jpql, Object[].class).getResultList();
        for (Object[] fila : filas) {
            System.out.println("numero=" + fila[0] + ", fechaEmision=" + fila[1] + ", importeTotal=" + fila[2]);
        }
        imprimirSinResultados(filas);
    }

    private static void consulta03ArticulosPorRubro(EntityManager em, String denominacionRubro) {
        String jpql = "SELECT a FROM Articulo a WHERE a.rubro.denominacion = :denominacionRubro";
        imprimirConsulta("3. Filtrado por igualdad", jpql);

        List<Articulo> articulos = em.createQuery(jpql, Articulo.class)
                .setParameter("denominacionRubro", denominacionRubro)
                .getResultList();
        imprimirArticulos(articulos);
    }

    private static void consulta04FacturasPorRangoFechas(EntityManager em, Date fechaDesde, Date fechaHasta) {
        String jpql = "SELECT fv FROM FacturaVenta fv WHERE fv.fechaEmision BETWEEN :fechaDesde AND :fechaHasta";
        imprimirConsulta("4. Filtrado por rango de fechas", jpql);

        List<FacturaVenta> facturas = em.createQuery(jpql, FacturaVenta.class)
                .setParameter("fechaDesde", fechaDesde)
                .setParameter("fechaHasta", fechaHasta)
                .getResultList();
        imprimirFacturas(facturas);
    }

    private static void consulta05FacturasEmitidasNoAnuladas(EntityManager em) {
        String jpql = """
                SELECT fv
                FROM FacturaVenta fv
                WHERE fv.estado = :estado
                  AND fv.importeTotal > :importeMinimo
                  AND fv.fechaAnulacion IS NULL
                """;
        imprimirConsulta("5. Condicionales complejos e IS NULL", jpql);

        List<FacturaVenta> facturas = em.createQuery(jpql, FacturaVenta.class)
                .setParameter("estado", "EMITIDA")
                .setParameter("importeMinimo", 10000.0)
                .getResultList();
        imprimirFacturas(facturas);
    }

    private static void consulta06ClientesPorTextoOCuit(EntityManager em, String textoParcial, String prefijoCuit) {
        String jpql = """
                SELECT c
                FROM Cliente c
                WHERE LOWER(c.denominacion) LIKE LOWER(:textoParcial)
                   OR c.cuitCuil LIKE :prefijoCuit
                """;
        imprimirConsulta("6. Busqueda por patron de texto", jpql);

        List<Cliente> clientes = em.createQuery(jpql, Cliente.class)
                .setParameter("textoParcial", "%" + textoParcial + "%")
                .setParameter("prefijoCuit", prefijoCuit + "%")
                .getResultList();
        imprimirClientes(clientes);
    }

    private static void consulta07EstadosDistintos(EntityManager em) {
        String jpql = "SELECT DISTINCT fv.estado FROM FacturaVenta fv ORDER BY fv.estado ASC";
        imprimirConsulta("7. Valores distintos y ordenamiento", jpql);

        List<String> estados = em.createQuery(jpql, String.class).getResultList();
        for (String estado : estados) {
            System.out.println("estado=" + estado);
        }
        imprimirSinResultados(estados);
    }

    private static void consulta08AgregacionesFacturas(EntityManager em) {
        String jpql = "SELECT COUNT(fv), SUM(fv.importeTotal), AVG(fv.importeTotal) FROM FacturaVenta fv";
        imprimirConsulta("8. Funciones de agregacion simples", jpql);

        Object[] fila = em.createQuery(jpql, Object[].class).getSingleResult();
        System.out.println("cantidad=" + fila[0] + ", sumaImportes=" + fila[1] + ", promedioImportes=" + fila[2]);
    }

    private static void consulta09PuntosVentaPorNumeros(EntityManager em, List<Integer> numeros) {
        String jpql = "SELECT pv FROM PuntoVenta pv WHERE pv.numero IN :numeros";
        imprimirConsulta("9. Operador de inclusion IN", jpql);

        List<PuntoVenta> puntosVenta = em.createQuery(jpql, PuntoVenta.class)
                .setParameter("numeros", numeros)
                .getResultList();
        imprimirPuntosVenta(puntosVenta);
    }

    private static void consulta10FacturasPorUsuarioCarga(EntityManager em, String usuario) {
        String jpql = "SELECT fv FROM FacturaVenta fv WHERE fv.usuarioCarga.usuario = :usuario";
        imprimirConsulta("10. Navegacion implicita por relaciones", jpql);

        List<FacturaVenta> facturas = em.createQuery(jpql, FacturaVenta.class)
                .setParameter("usuario", usuario)
                .getResultList();
        imprimirFacturas(facturas);
    }

    private static void consulta11DetallesPorPuntoVenta(EntityManager em, int numeroPuntoVenta) {
        String jpql = """
                SELECT d
                FROM FacturaVentaDetalle d
                JOIN d.factura fv
                WHERE fv.puntoVenta.numero = :numeroPuntoVenta
                """;
        imprimirConsulta("11. INNER JOIN explicito", jpql);

        List<FacturaVentaDetalle> detalles = em.createQuery(jpql, FacturaVentaDetalle.class)
                .setParameter("numeroPuntoVenta", numeroPuntoVenta)
                .getResultList();
        imprimirDetalles(detalles);
    }

    private static void consulta12ArticulosConMarca(EntityManager em) {
        String jpql = """
                SELECT a.denominacion, m.denominacion
                FROM Articulo a
                LEFT JOIN a.marca m
                ORDER BY a.denominacion
                """;
        imprimirConsulta("12. LEFT JOIN", jpql);

        List<Object[]> filas = em.createQuery(jpql, Object[].class).getResultList();
        for (Object[] fila : filas) {
            System.out.println("articulo=" + fila[0] + ", marca=" + fila[1]);
        }
        imprimirSinResultados(filas);
    }

    private static void consulta13FacturasPorMarca(EntityManager em, String denominacionMarca) {
        String jpql = """
                SELECT DISTINCT fv
                FROM FacturaVenta fv
                JOIN fv.detalles d
                JOIN d.listaPrecioArticulo lpa
                JOIN lpa.articulo a
                JOIN a.marca m
                WHERE m.denominacion = :denominacionMarca
                """;
        imprimirConsulta("13. Navegacion multinivel con JOINs combinados", jpql);

        List<FacturaVenta> facturas = em.createQuery(jpql, FacturaVenta.class)
                .setParameter("denominacionMarca", denominacionMarca)
                .getResultList();
        imprimirFacturas(facturas);
    }

    private static void consulta14FacturasMayoresAlPromedio(EntityManager em) {
        String jpql = """
                SELECT fv
                FROM FacturaVenta fv
                WHERE fv.importeTotal > (
                    SELECT AVG(fv2.importeTotal)
                    FROM FacturaVenta fv2
                )
                """;
        imprimirConsulta("14. Subconsulta en clausula WHERE", jpql);

        List<FacturaVenta> facturas = em.createQuery(jpql, FacturaVenta.class).getResultList();
        imprimirFacturas(facturas);
    }

    private static void consulta15AgrupamientoPorPuntoVenta(EntityManager em) {
        String jpql = """
                SELECT pv.id, pv.descripcion, COUNT(fv), SUM(fv.importeTotal)
                FROM FacturaVenta fv
                JOIN fv.puntoVenta pv
                GROUP BY pv.id, pv.descripcion
                ORDER BY pv.descripcion
                """;
        imprimirConsulta("15. Agrupamiento basico GROUP BY", jpql);

        List<Object[]> filas = em.createQuery(jpql, Object[].class).getResultList();
        for (Object[] fila : filas) {
            System.out.println("puntoVentaId=" + fila[0] + ", puntoVenta=" + fila[1]
                    + ", cantidadFacturas=" + fila[2] + ", totalFacturado=" + fila[3]);
        }
        imprimirSinResultados(filas);
    }

    private static void consulta16UsuariosConMasDeCincoFacturas(EntityManager em) {
        String jpql = """
                SELECT u.id, u.usuario, COUNT(fv)
                FROM FacturaVenta fv
                JOIN fv.usuarioCarga u
                GROUP BY u.id, u.usuario
                HAVING COUNT(fv) > :cantidadMinima
                """;
        imprimirConsulta("16. Agrupamiento con HAVING", jpql);

        List<Object[]> filas = em.createQuery(jpql, Object[].class)
                .setParameter("cantidadMinima", 5L)
                .getResultList();
        for (Object[] fila : filas) {
            System.out.println("usuarioId=" + fila[0] + ", usuario=" + fila[1] + ", cantidadFacturas=" + fila[2]);
        }
        imprimirSinResultados(filas);
    }

    private static void consulta17VentasAgrupadasPorMarca(EntityManager em) {
        String jpql = """
                SELECT m.denominacion, SUM(d.cantidad), SUM(d.importeSubtotal)
                FROM FacturaVentaDetalle d
                JOIN d.listaPrecioArticulo lpa
                JOIN lpa.articulo a
                JOIN a.marca m
                GROUP BY m.denominacion
                ORDER BY m.denominacion
                """;
        imprimirConsulta("17. Agrupamiento sobre entidades relacionadas", jpql);

        List<Object[]> filas = em.createQuery(jpql, Object[].class).getResultList();
        for (Object[] fila : filas) {
            System.out.println("marca=" + fila[0] + ", unidadesVendidas=" + fila[1] + ", subtotalAcumulado=" + fila[2]);
        }
        imprimirSinResultados(filas);
    }

    private static void consulta18MarcasConArticulosFacturados(EntityManager em) {
        String jpql = """
                SELECT m
                FROM Marca m
                WHERE EXISTS (
                    SELECT d.id
                    FROM FacturaVentaDetalle d
                    JOIN d.listaPrecioArticulo lpa
                    JOIN lpa.articulo a
                    WHERE a.marca = m
                )
                """;
        imprimirConsulta("18. Subconsulta correlacionada con EXISTS", jpql);

        List<Marca> marcas = em.createQuery(jpql, Marca.class).getResultList();
        imprimirMarcas(marcas);
    }

    private static void consulta19ArticulosNuncaFacturados(EntityManager em) {
        String jpql = """
                SELECT a
                FROM Articulo a
                WHERE NOT EXISTS (
                    SELECT d.id
                    FROM FacturaVentaDetalle d
                    JOIN d.listaPrecioArticulo lpa
                    WHERE lpa.articulo = a
                )
                """;
        imprimirConsulta("19. Subconsulta correlacionada con NOT EXISTS", jpql);

        List<Articulo> articulos = em.createQuery(jpql, Articulo.class).getResultList();
        imprimirArticulos(articulos);
    }

    private static void consulta20CategoriaFacturaPorImporte(EntityManager em) {
        String jpql = """
                SELECT fv.numero,
                       fv.importeTotal,
                       CASE
                           WHEN fv.importeTotal > :importeAlto THEN 'ALTO VALOR'
                           WHEN fv.importeTotal BETWEEN :importeMedioDesde AND :importeMedioHasta THEN 'MEDIO VALOR'
                           ELSE 'BAJO VALOR'
                       END
                FROM FacturaVenta fv
                ORDER BY fv.importeTotal DESC
                """;
        imprimirConsulta("20. Proyeccion condicional CASE WHEN", jpql);

        List<Object[]> filas = em.createQuery(jpql, Object[].class)
                .setParameter("importeAlto", 50000.0)
                .setParameter("importeMedioDesde", 10000.0)
                .setParameter("importeMedioHasta", 50000.0)
                .getResultList();
        for (Object[] fila : filas) {
            System.out.println("numero=" + fila[0] + ", importeTotal=" + fila[1] + ", categoria=" + fila[2]);
        }
        imprimirSinResultados(filas);
    }

    private static void imprimirConsulta(String titulo, String jpql) {
        System.out.println();
        System.out.println("==================================================");
        System.out.println(titulo);
        System.out.println("JPQL:");
        System.out.println(jpql.trim());
        System.out.println("Resultados:");
    }

    private static void imprimirFacturas(List<FacturaVenta> facturas) {
        for (FacturaVenta factura : facturas) {
            System.out.println("id=" + factura.getId()
                    + ", numero=" + factura.getNumero()
                    + ", fechaEmision=" + factura.getFechaEmision()
                    + ", estado=" + factura.getEstado()
                    + ", importeTotal=" + factura.getImporteTotal());
        }
        imprimirSinResultados(facturas);
    }

    private static void imprimirArticulos(List<Articulo> articulos) {
        for (Articulo articulo : articulos) {
            String rubro = articulo.getRubro() != null ? articulo.getRubro().getDenominacion() : null;
            String marca = articulo.getMarca() != null ? articulo.getMarca().getDenominacion() : null;
            System.out.println("id=" + articulo.getId()
                    + ", codigo=" + articulo.getCodigo()
                    + ", denominacion=" + articulo.getDenominacion()
                    + ", rubro=" + rubro
                    + ", marca=" + marca);
        }
        imprimirSinResultados(articulos);
    }

    private static void imprimirClientes(List<Cliente> clientes) {
        for (Cliente cliente : clientes) {
            System.out.println("id=" + cliente.getId()
                    + ", denominacion=" + cliente.getDenominacion()
                    + ", cuitCuil=" + cliente.getCuitCuil());
        }
        imprimirSinResultados(clientes);
    }

    private static void imprimirPuntosVenta(List<PuntoVenta> puntosVenta) {
        for (PuntoVenta puntoVenta : puntosVenta) {
            System.out.println("id=" + puntoVenta.getId()
                    + ", numero=" + puntoVenta.getNumero()
                    + ", descripcion=" + puntoVenta.getDescripcion());
        }
        imprimirSinResultados(puntosVenta);
    }

    private static void imprimirDetalles(List<FacturaVentaDetalle> detalles) {
        for (FacturaVentaDetalle detalle : detalles) {
            System.out.println("id=" + detalle.getId()
                    + ", facturaNumero=" + detalle.getFactura().getNumero()
                    + ", cantidad=" + detalle.getCantidad()
                    + ", precioUnitario=" + detalle.getPrecioUnitario()
                    + ", importeSubtotal=" + detalle.getImporteSubtotal());
        }
        imprimirSinResultados(detalles);
    }

    private static void imprimirMarcas(List<Marca> marcas) {
        for (Marca marca : marcas) {
            System.out.println("id=" + marca.getId()
                    + ", codigo=" + marca.getCodigo()
                    + ", denominacion=" + marca.getDenominacion());
        }
        imprimirSinResultados(marcas);
    }

    private static void imprimirSinResultados(List<?> resultados) {
        if (resultados.isEmpty()) {
            System.out.println("Sin resultados.");
        }
    }
}
