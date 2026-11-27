package com.inventaria.api.summary.infrastructure;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SummaryQueryRepository extends Repository<AiSummaryRecord, Integer> {

    @Query(value = """
            SELECT TOP 20
                   IdProducto  AS idProducto,
                   Nombre      AS nombre,
                   Stock       AS stock,
                   StockMinimo AS stockMinimo,
                   Tipo        AS tipo
              FROM Productos
             WHERE IdUsuario = :userId
               AND Stock <= StockMinimo
             ORDER BY Stock ASC
            """, nativeQuery = true)
    List<LowStockRow> findLowStockProducts(@Param("userId") Integer userId);

    @Query(value = """
            SELECT IdProducto     AS idProducto,
                   Nombre         AS nombre,
                   FechaCaducidad AS fechaCaducidad,
                   Stock          AS stock
              FROM Productos
             WHERE IdUsuario = :userId
               AND Tipo = 'Perecedero'
               AND FechaCaducidad <= DATEADD(DAY, 7, CAST(GETDATE() AS DATE))
             ORDER BY FechaCaducidad ASC
            """, nativeQuery = true)
    List<ExpiryRow> findExpiringProducts(@Param("userId") Integer userId);

    @Query(value = """
            SELECT COUNT(*)                AS totalVentas,
                   COALESCE(SUM(Total), 0) AS montoTotal
              FROM Ventas
             WHERE IdUsuario = :userId
               AND CAST(Fecha AS DATE) = CAST(GETDATE() AS DATE)
            """, nativeQuery = true)
    List<TodaySalesRow> findTodaySales(@Param("userId") Integer userId);

    @Query(value = """
            SELECT TOP 5
                   p.IdProducto     AS idProducto,
                   p.Nombre         AS nombre,
                   SUM(dv.Cantidad) AS unidadesVendidas
              FROM DetalleVenta dv
             INNER JOIN Productos p ON dv.IdProducto = p.IdProducto
             INNER JOIN Ventas v    ON dv.IdVenta    = v.IdVenta
             WHERE v.IdUsuario = :userId
               AND CAST(v.Fecha AS DATE) = CAST(GETDATE() AS DATE)
             GROUP BY p.IdProducto, p.Nombre
             ORDER BY unidadesVendidas DESC
            """, nativeQuery = true)
    List<TopSellerRow> findTopSellersToday(@Param("userId") Integer userId);
}
