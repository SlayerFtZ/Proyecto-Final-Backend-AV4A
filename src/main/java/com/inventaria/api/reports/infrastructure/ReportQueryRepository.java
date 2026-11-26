package com.inventaria.api.reports.infrastructure;

import com.inventaria.api.reports.domain.LowStockBySupplierRow;
import com.inventaria.api.reports.domain.ProductDetailRow;
import com.inventaria.api.reports.domain.SaleDetailRow;
import com.inventaria.api.reports.domain.TotalByCategoryRow;
import com.inventaria.api.sales.domain.Sale;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportQueryRepository extends Repository<Sale, Integer> {

    @Query(value = """
            SELECT p.IdProducto       AS idProducto,
                   p.CodigoBarras     AS codigoBarras,
                   p.Nombre           AS nombre,
                   p.Descripcion      AS descripcion,
                   p.Precio           AS precio,
                   p.Stock            AS stock,
                   p.StockMinimo      AS stockMinimo,
                   p.Tipo             AS tipo,
                   p.FechaCaducidad   AS fechaCaducidad,
                   p.MesesGarantia    AS mesesGarantia,
                   p.Talla            AS talla,
                   c.IdCategoria      AS idCategoria,
                   c.Nombre           AS categoriaNombre,
                   pr.IdProveedor     AS idProveedor,
                   pr.Nombre          AS proveedorNombre
              FROM Productos p
              INNER JOIN Categorias  c  ON p.IdCategoria = c.IdCategoria
              INNER JOIN Proveedores pr ON p.IdProveedor = pr.IdProveedor
             ORDER BY p.Nombre
            """, nativeQuery = true)
    List<ProductDetailRow> findProductDetails();

    @Query(value = """
            SELECT v.IdVenta         AS idVenta,
                   v.Fecha           AS fecha,
                   v.Total           AS total,
                   dv.IdDetalle      AS idDetalle,
                   dv.Cantidad       AS cantidad,
                   dv.PrecioUnitario AS precioUnitario,
                   p.IdProducto      AS idProducto,
                   p.Nombre          AS productoNombre,
                   p.CodigoBarras    AS codigoBarras
              FROM Ventas v
              INNER JOIN DetalleVenta dv ON v.IdVenta    = dv.IdVenta
              INNER JOIN Productos    p  ON dv.IdProducto = p.IdProducto
             WHERE v.Fecha >= :desde AND v.Fecha < :hasta
             ORDER BY v.Fecha DESC, v.IdVenta, dv.IdDetalle
            """, nativeQuery = true)
    List<SaleDetailRow> findSaleDetails(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query(value = """
            SELECT pr.IdProveedor AS idProveedor,
                   pr.Nombre      AS proveedorNombre,
                   pr.Telefono    AS telefono,
                   pr.Correo      AS correo,
                   p.IdProducto   AS idProducto,
                   p.CodigoBarras AS codigoBarras,
                   p.Nombre       AS productoNombre,
                   p.Stock        AS stock,
                   p.StockMinimo  AS stockMinimo,
                   p.Tipo         AS tipo
              FROM Productos p
              INNER JOIN Proveedores pr ON p.IdProveedor = pr.IdProveedor
             WHERE p.Stock <= p.StockMinimo
             ORDER BY pr.Nombre, p.Stock ASC
            """, nativeQuery = true)
    List<LowStockBySupplierRow> findLowStockBySupplier();

    @Query(value = """
            SELECT c.IdCategoria                                        AS idCategoria,
                   c.Nombre                                             AS categoria,
                   COALESCE(SUM(dv.Cantidad), 0)                        AS unidadesVendidas,
                   COALESCE(SUM(CAST(dv.Cantidad AS DECIMAL(12,2)) * dv.PrecioUnitario), 0) AS totalVendido
              FROM Categorias c
              LEFT JOIN Productos    p  ON c.IdCategoria  = p.IdCategoria
              LEFT JOIN DetalleVenta dv ON p.IdProducto   = dv.IdProducto
             GROUP BY c.IdCategoria, c.Nombre
             ORDER BY totalVendido DESC
            """, nativeQuery = true)
    List<TotalByCategoryRow> findTotalByCategory();
}
