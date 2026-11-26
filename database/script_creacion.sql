-- ============================================================
-- InventarIA — Script de creación de base de datos
-- Base de datos: SQL Server 2019+
-- Idempotente: elimina y recrea objetos en orden seguro.
-- Ejecutar completo cada vez que se necesite reiniciar la demo.
-- ============================================================

USE master;
GO

-- Crear la base de datos si no existe
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'InventarIA')
BEGIN
    CREATE DATABASE InventarIA;
END
GO

USE InventarIA;
GO

-- ============================================================
-- DROP en orden inverso de dependencias (evita errores de FK)
-- ============================================================
IF OBJECT_ID('dbo.ResumenesIA',  'U') IS NOT NULL DROP TABLE dbo.ResumenesIA;
IF OBJECT_ID('dbo.DetalleVenta', 'U') IS NOT NULL DROP TABLE dbo.DetalleVenta;
IF OBJECT_ID('dbo.Ventas',       'U') IS NOT NULL DROP TABLE dbo.Ventas;
IF OBJECT_ID('dbo.Productos',    'U') IS NOT NULL DROP TABLE dbo.Productos;
IF OBJECT_ID('dbo.Proveedores',  'U') IS NOT NULL DROP TABLE dbo.Proveedores;
IF OBJECT_ID('dbo.Categorias',   'U') IS NOT NULL DROP TABLE dbo.Categorias;
GO

-- ============================================================
-- TABLAS
-- ============================================================

CREATE TABLE Categorias (
    IdCategoria  INT           IDENTITY(1,1) PRIMARY KEY,
    Nombre       NVARCHAR(100) NOT NULL,
    Descripcion  NVARCHAR(255) NULL,
    CONSTRAINT UQ_Categorias_Nombre UNIQUE (Nombre)
);
GO

CREATE TABLE Proveedores (
    IdProveedor  INT           IDENTITY(1,1) PRIMARY KEY,
    Nombre       NVARCHAR(150) NOT NULL,
    Telefono     NVARCHAR(20)  NULL,
    Correo       NVARCHAR(150) NULL
);
GO

CREATE TABLE Productos (
    IdProducto     INT            IDENTITY(1,1) PRIMARY KEY,
    CodigoBarras   NVARCHAR(20)   NOT NULL,
    Nombre         NVARCHAR(150)  NOT NULL,
    Descripcion    NVARCHAR(500)  NULL,
    Precio         DECIMAL(10,2)  NOT NULL,
    Stock          INT            NOT NULL,
    StockMinimo    INT            NOT NULL DEFAULT 5,
    Tipo           NVARCHAR(20)   NOT NULL,
    FechaCaducidad DATE           NULL,       -- Solo Perecedero
    MesesGarantia  INT            NULL,       -- Solo Electronico
    Talla          NVARCHAR(10)   NULL,       -- Solo Ropa
    IdCategoria    INT            NOT NULL REFERENCES Categorias(IdCategoria),
    IdProveedor    INT            NOT NULL REFERENCES Proveedores(IdProveedor),
    CONSTRAINT UQ_Productos_CodigoBarras  UNIQUE (CodigoBarras),
    CONSTRAINT CK_Productos_Precio        CHECK (Precio > 0),
    CONSTRAINT CK_Productos_Stock         CHECK (Stock >= 0),
    CONSTRAINT CK_Productos_StockMinimo   CHECK (StockMinimo >= 0),
    CONSTRAINT CK_Productos_Tipo          CHECK (Tipo IN ('Perecedero', 'Electronico', 'Ropa'))
);
GO

CREATE TABLE Ventas (
    IdVenta  INT            IDENTITY(1,1) PRIMARY KEY,
    Fecha    DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    Total    DECIMAL(12,2)  NOT NULL,
    CONSTRAINT CK_Ventas_Total CHECK (Total >= 0)
);
GO

CREATE TABLE DetalleVenta (
    IdDetalle      INT           IDENTITY(1,1) PRIMARY KEY,
    IdVenta        INT           NOT NULL REFERENCES Ventas(IdVenta) ON DELETE CASCADE,
    IdProducto     INT           NOT NULL REFERENCES Productos(IdProducto),
    Cantidad       INT           NOT NULL,
    PrecioUnitario DECIMAL(10,2) NOT NULL,
    CONSTRAINT CK_DetalleVenta_Cantidad       CHECK (Cantidad > 0),
    CONSTRAINT CK_DetalleVenta_PrecioUnitario CHECK (PrecioUnitario > 0)
);
GO

-- Historial opcional de resúmenes generados por IA
CREATE TABLE ResumenesIA (
    IdResumen  INT           IDENTITY(1,1) PRIMARY KEY,
    Fecha      DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    Texto      NVARCHAR(MAX) NOT NULL
);
GO

-- ============================================================
-- DATOS DE PRUEBA
-- ============================================================

-- ------------------------------------------------------------
-- Categorías (5)
-- ------------------------------------------------------------
INSERT INTO Categorias (Nombre, Descripcion) VALUES
    (N'Lácteos',        N'Leche, quesos, yogurt y derivados'),
    (N'Bebidas',        N'Refrescos, aguas y jugos embotellados'),
    (N'Panadería',      N'Pan, cereales y harinas'),
    (N'Electrónica',    N'Electrónicos, accesorios y consumibles eléctricos'),
    (N'Ropa y Calzado', N'Ropa, calzado y accesorios de vestir');
-- IdCategoria: 1=Lácteos, 2=Bebidas, 3=Panadería, 4=Electrónica, 5=Ropa y Calzado
GO

-- ------------------------------------------------------------
-- Proveedores (4)
-- ------------------------------------------------------------
INSERT INTO Proveedores (Nombre, Telefono, Correo) VALUES
    (N'Distribuidora Norte S.A.', N'2311001000', N'contacto@disnorte.mx'),
    (N'Electro Importaciones MX', N'2312002000', N'ventas@electroimport.mx'),
    (N'Moda Express',             N'2313003000', N'pedidos@modaexpress.mx'),
    (N'Alimentos del Valle',      N'2314004000', N'ventas@alimentosvalle.mx');
-- IdProveedor: 1=DistrNorte, 2=ElectroImport, 3=ModaExpress, 4=AlimValle
GO

-- ------------------------------------------------------------
-- Productos (30)
-- Tipos: Perecedero (12), Electronico (10), Ropa (8)
-- Stock crítico y alertas de caducidad diseñados con fechas
-- relativas para que la demo funcione en cualquier día.
--
-- Resumen de alertas esperadas:
--   CRITICO (rojo): stock=0 en productos 3,8,10,17,25
--                   caduca en 0-2 días: productos 9 (2 días)
--                   ya vencido: producto 8 (-1 día)
--   AVISO (amarillo): stock bajo (0 < stock <= min): productos 2,4,11,15,20,22,26,30
--                     caduca en 3-7 días: productos 3 (3d), 4 (5d), 11 (6d)
-- ------------------------------------------------------------
INSERT INTO Productos
    (CodigoBarras, Nombre, Descripcion, Precio, Stock, StockMinimo,
     Tipo, FechaCaducidad, MesesGarantia, Talla, IdCategoria, IdProveedor)
-- ---- PERECEDEROS ----
SELECT
    N'7501055300075', N'Leche entera 1L',
    N'Leche pasteurizada entera, presentación 1 litro',
    28.50, 40, 10, N'Perecedero',
    DATEADD(DAY, 10, CAST(GETDATE() AS DATE)), NULL, NULL, 1, 1
UNION ALL SELECT
    -- AVISO: Stock(5) < StockMinimo(10)
    N'7501055300082', N'Leche semidescremada 1L',
    N'Leche pasteurizada semidescremada, 1 litro',
    30.00, 5, 10, N'Perecedero',
    DATEADD(DAY, 8, CAST(GETDATE() AS DATE)), NULL, NULL, 1, 1
UNION ALL SELECT
    -- CRITICO: Stock=0  |  AVISO: caduca en 3 días
    N'7503001350018', N'Queso fresco 400g',
    N'Queso fresco artesanal, presentación 400 gramos',
    65.00, 0, 5, N'Perecedero',
    DATEADD(DAY, 3, CAST(GETDATE() AS DATE)), NULL, NULL, 1, 1
UNION ALL SELECT
    -- AVISO: Stock(3) < StockMinimo(8)  |  AVISO: caduca en 5 días
    N'7501003140025', N'Yogurt natural 1kg',
    N'Yogurt natural sin azúcar, presentación 1 kilogramo',
    45.00, 3, 8, N'Perecedero',
    DATEADD(DAY, 5, CAST(GETDATE() AS DATE)), NULL, NULL, 1, 4
UNION ALL SELECT
    N'7501003140032', N'Crema de leche 250ml',
    N'Crema de leche para cocinar, 250 mililitros',
    22.00, 15, 5, N'Perecedero',
    DATEADD(DAY, 15, CAST(GETDATE() AS DATE)), NULL, NULL, 1, 1
UNION ALL SELECT
    N'7503010050038', N'Refresco cola 600ml',
    N'Refresco carbonatado sabor cola, botella 600 ml',
    18.00, 80, 20, N'Perecedero',
    DATEADD(DAY, 90, CAST(GETDATE() AS DATE)), NULL, NULL, 2, 4
UNION ALL SELECT
    N'7503010050045', N'Agua natural 1.5L',
    N'Agua purificada sin gas, presentación 1.5 litros',
    14.00, 100, 30, N'Perecedero',
    DATEADD(DAY, 180, CAST(GETDATE() AS DATE)), NULL, NULL, 2, 4
UNION ALL SELECT
    -- CRITICO: Stock=0  |  CRITICO: ya vencido (-1 día)
    N'7501003200051', N'Jugo naranja 1L',
    N'Jugo de naranja natural, sin conservadores, 1 litro',
    35.00, 0, 10, N'Perecedero',
    DATEADD(DAY, -1, CAST(GETDATE() AS DATE)), NULL, NULL, 2, 4
UNION ALL SELECT
    -- AVISO: Stock(12) < StockMinimo(15)  |  CRITICO: caduca en 2 días
    N'7501003200068', N'Jugo manzana 500ml',
    N'Jugo de manzana 100%, sin azúcar añadida, 500 ml',
    22.00, 12, 15, N'Perecedero',
    DATEADD(DAY, 2, CAST(GETDATE() AS DATE)), NULL, NULL, 2, 4
UNION ALL SELECT
    -- CRITICO: Stock=0
    N'7500435010075', N'Pan de caja 680g',
    N'Pan de caja blanco rebanado, 680 gramos',
    42.00, 0, 5, N'Perecedero',
    DATEADD(DAY, 7, CAST(GETDATE() AS DATE)), NULL, NULL, 3, 4
UNION ALL SELECT
    -- AVISO: Stock(8) < StockMinimo(10)  |  AVISO: caduca en 6 días
    N'7500435010082', N'Pan tostado integral 300g',
    N'Pan tostado de trigo integral, 300 gramos',
    38.00, 8, 10, N'Perecedero',
    DATEADD(DAY, 6, CAST(GETDATE() AS DATE)), NULL, NULL, 3, 4
UNION ALL SELECT
    N'7501003300088', N'Cereal avena 500g',
    N'Hojuelas de avena integral, presentación 500 gramos',
    55.00, 25, 8, N'Perecedero',
    DATEADD(DAY, 120, CAST(GETDATE() AS DATE)), NULL, NULL, 3, 4
-- ---- ELECTRÓNICOS ----
UNION ALL SELECT
    N'6901010100012', N'Audífonos básicos USB',
    N'Audífonos con micrófono integrado, conexión USB-A',
    85.00, 20, 5, N'Electronico',
    NULL, 6, NULL, 4, 2
UNION ALL SELECT
    N'6901010100029', N'Cable USB-C 1m',
    N'Cable de carga y datos USB-A a USB-C, 1 metro',
    45.00, 50, 10, N'Electronico',
    NULL, 3, NULL, 4, 2
UNION ALL SELECT
    -- AVISO: Stock(3) < StockMinimo(5)
    N'6901010100036', N'Cargador celular 20W',
    N'Cargador rápido 20W con puerto USB-C, compatible universalmente',
    220.00, 3, 5, N'Electronico',
    NULL, 12, NULL, 4, 2
UNION ALL SELECT
    N'6901010100043', N'Foco LED 12W',
    N'Foco de luz blanca LED 12W, equivalente a 100W incandescente',
    38.00, 30, 10, N'Electronico',
    NULL, 12, NULL, 4, 2
UNION ALL SELECT
    -- CRITICO: Stock=0
    N'6901010100050', N'Batería AA x4',
    N'Paquete de 4 pilas alcalinas tamaño AA de larga duración',
    65.00, 0, 10, N'Electronico',
    NULL, 0, NULL, 4, 2
UNION ALL SELECT
    N'6901010100067', N'Extensión eléctrica 3m',
    N'Extensión con 3 contactos y protección contra sobrecarga, 3 metros',
    95.00, 12, 5, N'Electronico',
    NULL, 6, NULL, 4, 2
UNION ALL SELECT
    N'6901010100074', N'Mouse inalámbrico',
    N'Mouse óptico inalámbrico con receptor USB nano, 1600 DPI',
    285.00, 8, 3, N'Electronico',
    NULL, 12, NULL, 4, 2
UNION ALL SELECT
    -- AVISO: Stock(4) < StockMinimo(5)
    N'6901010100081', N'Teclado USB básico',
    N'Teclado de membrana en español, conexión USB',
    195.00, 4, 5, N'Electronico',
    NULL, 12, NULL, 4, 2
UNION ALL SELECT
    N'6901010100098', N'Hub USB 4 puertos',
    N'Concentrador USB 3.0 de 4 puertos con cable integrado',
    150.00, 7, 3, N'Electronico',
    NULL, 6, NULL, 4, 2
UNION ALL SELECT
    -- AVISO: Stock(2) < StockMinimo(5)
    N'6901010100104', N'Lámpara USB escritorio',
    N'Lámpara LED flexible con conector USB, luz cálida y fría',
    120.00, 2, 5, N'Electronico',
    NULL, 6, NULL, 4, 2
-- ---- ROPA ----
UNION ALL SELECT
    N'7503030100012', N'Playera básica talla S',
    N'Playera de algodón 100%, corte regular, color blanco',
    95.00, 15, 5, N'Ropa',
    NULL, NULL, N'S', 5, 3
UNION ALL SELECT
    N'7503030100029', N'Playera básica talla M',
    N'Playera de algodón 100%, corte regular, color blanco',
    95.00, 20, 5, N'Ropa',
    NULL, NULL, N'M', 5, 3
UNION ALL SELECT
    -- CRITICO: Stock=0
    N'7503030100036', N'Playera básica talla L',
    N'Playera de algodón 100%, corte regular, color blanco',
    95.00, 0, 5, N'Ropa',
    NULL, NULL, N'L', 5, 3
UNION ALL SELECT
    -- AVISO: Stock(3) < StockMinimo(5)
    N'7503030100043', N'Playera básica talla XL',
    N'Playera de algodón 100%, corte regular, color blanco',
    100.00, 3, 5, N'Ropa',
    NULL, NULL, N'XL', 5, 3
UNION ALL SELECT
    N'7503030200019', N'Pantalón jeans talla 28',
    N'Pantalón de mezclilla recto, corte clásico, color azul',
    380.00, 5, 3, N'Ropa',
    NULL, NULL, N'28', 5, 3
UNION ALL SELECT
    N'7503030200026', N'Pantalón jeans talla 30',
    N'Pantalón de mezclilla recto, corte clásico, color azul',
    380.00, 8, 3, N'Ropa',
    NULL, NULL, N'30', 5, 3
UNION ALL SELECT
    N'7503030300015', N'Calcetines blancos',
    N'Calcetines de algodón, talla única, paquete de 3 pares',
    35.00, 30, 10, N'Ropa',
    NULL, NULL, N'Única', 5, 3
UNION ALL SELECT
    -- AVISO: Stock(2) < StockMinimo(5)
    N'7503030400011', N'Gorra de béisbol',
    N'Gorra con visera curva y cierre ajustable, talla única',
    120.00, 2, 5, N'Ropa',
    NULL, NULL, N'Única', 5, 3;
GO

-- ------------------------------------------------------------
-- Ventas y DetalleVenta (12 ventas en total)
-- 5 ventas de hoy  → "ventas del día" y "más vendidos" funcionan
-- 7 ventas pasadas → reportes de rango de fechas con datos
--
-- IDENTITY de Ventas: 1–5 = hoy, 6–12 = días anteriores
-- Las fechas usan DATEADD sobre CAST(GETDATE() AS DATE) para
-- mantenerse relativas al día de ejecución del script.
-- ------------------------------------------------------------

-- Ventas de HOY (IdVenta 1–5)
INSERT INTO Ventas (Fecha, Total)
SELECT DATEADD(MINUTE,  600, CONVERT(DATETIME2, CONVERT(DATE, GETDATE()))), 75.00   -- 10:00
UNION ALL
SELECT DATEADD(MINUTE,  660, CONVERT(DATETIME2, CONVERT(DATE, GETDATE()))), 130.00  -- 11:00
UNION ALL
SELECT DATEADD(MINUTE,  750, CONVERT(DATETIME2, CONVERT(DATE, GETDATE()))), 140.50  -- 12:30
UNION ALL
SELECT DATEADD(MINUTE,  870, CONVERT(DATETIME2, CONVERT(DATE, GETDATE()))), 130.00  -- 14:30
UNION ALL
SELECT DATEADD(MINUTE,  960, CONVERT(DATETIME2, CONVERT(DATE, GETDATE()))), 76.00;  -- 16:00

-- Ventas de DÍAS ANTERIORES (IdVenta 6–12)
INSERT INTO Ventas (Fecha, Total)
SELECT DATEADD(MINUTE, 780, DATEADD(DAY, -1, CONVERT(DATETIME2, CONVERT(DATE, GETDATE())))), 480.00  -- ayer 13:00
UNION ALL
SELECT DATEADD(MINUTE, 540, DATEADD(DAY, -1, CONVERT(DATETIME2, CONVERT(DATE, GETDATE())))), 150.50  -- ayer 09:00
UNION ALL
SELECT DATEADD(MINUTE, 660, DATEADD(DAY, -2, CONVERT(DATETIME2, CONVERT(DATE, GETDATE())))), 82.00   -- hace 2 días
UNION ALL
SELECT DATEADD(MINUTE, 840, DATEADD(DAY, -2, CONVERT(DATETIME2, CONVERT(DATE, GETDATE())))), 370.00  -- hace 2 días
UNION ALL
SELECT DATEADD(MINUTE, 600, DATEADD(DAY, -3, CONVERT(DATETIME2, CONVERT(DATE, GETDATE())))), 171.00  -- hace 3 días
UNION ALL
SELECT DATEADD(MINUTE, 720, DATEADD(DAY, -3, CONVERT(DATETIME2, CONVERT(DATE, GETDATE())))), 570.00  -- hace 3 días
UNION ALL
SELECT DATEADD(MINUTE, 480, DATEADD(DAY, -4, CONVERT(DATETIME2, CONVERT(DATE, GETDATE())))), 95.00;  -- hace 4 días
GO

-- DetalleVenta
-- Producto IdProducto:
--   1=Leche entera, 2=Leche semidescremada, 3=Queso fresco, 4=Yogurt,
--   5=Crema de leche, 6=Refresco cola, 7=Agua natural, 8=Jugo naranja,
--   9=Jugo manzana, 10=Pan de caja, 11=Pan tostado, 12=Cereal avena,
--   13=Audífonos, 14=Cable USB-C, 15=Cargador, 16=Foco LED,
--   17=Batería AA, 18=Extensión eléctrica, 19=Mouse, 20=Teclado,
--   21=Hub USB, 22=Lámpara USB, 23=Playera S, 24=Playera M,
--   25=Playera L, 26=Playera XL, 27=Pantalón 28, 28=Pantalón 30,
--   29=Calcetines, 30=Gorra

INSERT INTO DetalleVenta (IdVenta, IdProducto, Cantidad, PrecioUnitario)
VALUES
    -- Venta 1 (hoy): Leche entera x2 + Refresco cola x1 = 75.00
    (1,  1,  2, 28.50),
    (1,  6,  1, 18.00),

    -- Venta 2 (hoy): Audífonos x1 + Cable USB-C x1 = 130.00
    (2, 13,  1, 85.00),
    (2, 14,  1, 45.00),

    -- Venta 3 (hoy): Leche entera x1 + Yogurt x2 + Crema x1 = 140.50
    (3,  1,  1, 28.50),
    (3,  4,  2, 45.00),
    (3,  5,  1, 22.00),

    -- Venta 4 (hoy): Playera M x1 + Calcetines x1 = 130.00
    (4, 24,  1, 95.00),
    (4, 29,  1, 35.00),

    -- Venta 5 (hoy): Pan tostado x1 + Agua natural x1 + Crema x1 = 76.00
    (5, 11,  1, 38.00),
    (5,  7,  1, 14.00),
    (5,  5,  1, 24.00),   -- precio especial aceptado

    -- Venta 6 (ayer): Mouse x1 + Teclado x1 = 480.00
    (6, 19,  1, 285.00),
    (6, 20,  1, 195.00),

    -- Venta 7 (ayer): Leche entera x3 + Queso fresco x1 = 150.50
    (7,  1,  3, 28.50),
    (7,  3,  1, 65.00),

    -- Venta 8 (hace 2 días): Agua natural x2 + Refresco cola x3 = 82.00
    (8,  7,  2, 14.00),
    (8,  6,  3, 18.00),

    -- Venta 9 (hace 2 días): Cargador x1 + Hub USB x1 = 370.00
    (9, 15,  1, 220.00),
    (9, 21,  1, 150.00),

    -- Venta 10 (hace 3 días): Foco LED x2 + Extensión x1 = 171.00
    (10, 16,  2, 38.00),
    (10, 18,  1, 95.00),

    -- Venta 11 (hace 3 días): Pantalón talla 30 x1 + Playera M x2 = 570.00
    (11, 28,  1, 380.00),
    (11, 24,  2, 95.00),

    -- Venta 12 (hace 4 días): Jugo naranja x1 + Leche semidescremada x2 = 95.00
    (12,  8,  1, 35.00),
    (12,  2,  2, 30.00);
GO

-- ============================================================
-- VERIFICACIÓN RÁPIDA
-- Ejecutar estas consultas para confirmar que el script
-- se ejecutó correctamente.
-- ============================================================

-- Resumen de registros insertados
SELECT 'Categorias'  AS Tabla, COUNT(*) AS Registros FROM Categorias
UNION ALL SELECT 'Proveedores',  COUNT(*) FROM Proveedores
UNION ALL SELECT 'Productos',    COUNT(*) FROM Productos
UNION ALL SELECT 'Ventas',       COUNT(*) FROM Ventas
UNION ALL SELECT 'DetalleVenta', COUNT(*) FROM DetalleVenta;

-- Alertas esperadas en el resumen del día
SELECT
    p.Nombre,
    p.Stock,
    p.StockMinimo,
    p.Tipo,
    p.FechaCaducidad,
    DATEDIFF(DAY, CAST(GETDATE() AS DATE), p.FechaCaducidad) AS DiasRestantes,
    CASE
        WHEN p.Stock = 0                                                                           THEN 'CRITICO - Sin stock'
        WHEN p.Tipo = 'Perecedero' AND p.FechaCaducidad <= CAST(GETDATE() AS DATE)                THEN 'CRITICO - Vencido'
        WHEN p.Tipo = 'Perecedero' AND DATEDIFF(DAY, CAST(GETDATE() AS DATE), p.FechaCaducidad)
             BETWEEN 0 AND 2                                                                       THEN 'CRITICO - Caduca en 0-2 días'
        WHEN p.Stock > 0 AND p.Stock <= p.StockMinimo                                             THEN 'AVISO   - Stock bajo'
        WHEN p.Tipo = 'Perecedero' AND DATEDIFF(DAY, CAST(GETDATE() AS DATE), p.FechaCaducidad)
             BETWEEN 3 AND 7                                                                       THEN 'AVISO   - Caduca en 3-7 días'
    END AS NivelAlerta
FROM Productos p
WHERE
    p.Stock <= p.StockMinimo
    OR (p.Tipo = 'Perecedero' AND p.FechaCaducidad <= DATEADD(DAY, 7, CAST(GETDATE() AS DATE)))
ORDER BY NivelAlerta, p.Nombre;

-- Ventas del día
SELECT COUNT(*) AS NumVentas, COALESCE(SUM(Total), 0) AS TotalDia
FROM Ventas
WHERE CAST(Fecha AS DATE) = CAST(GETDATE() AS DATE);

-- Más vendidos del día (top 5)
SELECT TOP 5 p.Nombre, SUM(d.Cantidad) AS Unidades
FROM DetalleVenta d
INNER JOIN Ventas    v ON v.IdVenta    = d.IdVenta
INNER JOIN Productos p ON p.IdProducto = d.IdProducto
WHERE CAST(v.Fecha AS DATE) = CAST(GETDATE() AS DATE)
GROUP BY p.Nombre
ORDER BY SUM(d.Cantidad) DESC;
GO
