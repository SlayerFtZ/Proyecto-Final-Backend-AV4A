# InventarIA · Especificación del Backend (Spring Boot)

**Proyecto:** InventarIA, inventario inteligente de tienda con resumen diario por IA y escaneo de productos  
**Componente:** `inventaria-api`, API REST con Spring Boot + SQL Server  
**Cliente:** app .NET MAUI (Clean Architecture por features, MVVM)  
**Materia:** Tecnologías de Programación, Maestría en Sistemas Computacionales, ITS de Teziutlán

> Este documento lista todo lo que debe hacer el backend, derivado de la *Propuesta de Proyecto Final*. Está pensado para usarse como guía de desarrollo: cada sección termina en tareas concretas y al final hay un checklist por fases.

---

## 1. Rol del backend

El backend es una **capa delgada de acceso a datos y de integración con IA**. La app MAUI nunca habla directo con SQL Server ni con el proveedor de IA; todo pasa por esta API.

Responsabilidades:

1. Exponer **CRUD REST** de productos, categorías y proveedores.
2. Registrar **ventas** con detalle y **descuento atómico de stock**.
3. Exponer **4 consultas JOIN / GROUP BY** (tema 12 de la lista de cotejo).
4. Generar el **resumen y las alertas del día**: consultas SQL, armado de JSON, llamada a la IA de texto y generador local de respaldo.
5. Recibir una **foto** y devolver, vía IA de visión, una sugerencia de producto en JSON.
6. Buscar productos por **código de barras** (usado por el escáner).
7. Soportar la **importación masiva** de productos (la app lee el CSV y envía lotes).
8. **Proteger la clave de la IA** (variables de entorno, nunca en GitHub).
9. Devolver errores en un formato uniforme y validar toda entrada.

Qué **no** hace el backend: la jerarquía de clases `Producto`, genéricos y polimorfismo viven en la app C# (temas 1 a 5). El backend usa entidades Java sencillas y entrega cada producto con un campo `tipo`.

Puntos de la lista de cotejo que dependen del backend: **tema 11** (Base de datos SQL Server, 10 pts) y **tema 12** (CRUD y consultas JOIN, 14 pts). Más la base de los temas 8 (async/await con la API) y 9/10 (importación y simulador).

---

## 2. Stack tecnológico

| Componente | Elección |
|---|---|
| Lenguaje | Java 17 o 21 |
| Framework | Spring Boot 3.3 o superior |
| Build | Maven |
| Web | `spring-boot-starter-web` |
| Persistencia | `spring-boot-starter-data-jpa` (CRUD) + consultas nativas con `@Query` (JOIN) |
| Base de datos | SQL Server con driver `mssql-jdbc` |
| Validación | `spring-boot-starter-validation` (Jakarta Validation) |
| Documentación | `springdoc-openapi-starter-webmvc-ui` (Swagger UI en `/swagger-ui.html`) |
| Cliente HTTP a la IA | `RestClient` de Spring (incluido en Boot 3.2+) |
| Pruebas | `spring-boot-starter-test` (JUnit 5, Mockito, MockMvc) |
| Opcionales | Lombok, `spring-boot-starter-actuator`, Spring Security + JWT (solo si se hace `auth`) |

Dependencias clave del `pom.xml`:

```xml
<dependencies>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
  <dependency><groupId>com.microsoft.sqlserver</groupId><artifactId>mssql-jdbc</artifactId><scope>runtime</scope></dependency>
  <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>2.6.0</version></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
</dependencies>
```

---

## 3. Arquitectura por features

El backend se organiza **por features**, cada una con capas `domain`, `infrastructure` y `presentation`, igual que la app (regla de dependencia hacia el dominio).

| Capa | Contenido | Depende de |
|---|---|---|
| `domain` | Entidades, interfaces de repositorio (puertos), servicios con reglas de negocio, excepciones de dominio | Nada del framework web |
| `infrastructure` | Implementaciones de repositorio (Spring Data / JPA), consultas nativas, clientes de IA | `domain` |
| `presentation` | Controllers REST, DTOs de entrada/salida, mappers DTO ↔ entidad | `domain` |

**Decisión pragmática (para no perder tiempo, ver riesgo "demasiadas capas"):** completar la estructura en `products` y usarla como plantilla. En features pequeñas (`categories`, `suppliers`) se permite que la interfaz de repositorio extienda `JpaRepository` directamente y omitir carpetas innecesarias.

### 3.1 Estructura del proyecto

```
inventaria-api/
├── pom.xml
├── .gitignore                       # incluye application-local.properties y .env
├── .env.example                     # variables de entorno de ejemplo (sin secretos)
├── README.md
├── database/
│   └── script_creacion.sql          # tablas + datos de prueba (se copia a /Database del repo)
└── src/main/java/com/inventaria/api/
    ├── InventariaApplication.java
    ├── shared/
    │   ├── config/                  # CorsConfig, OpenApiConfig, JacksonConfig, RestClientConfig
    │   ├── error/                   # ApiError, GlobalExceptionHandler, excepciones base
    │   └── util/                    # utilidades comunes
    ├── products/
    │   ├── domain/           entity/Producto, TipoProducto
    │   │                     repository/ProductoRepository
    │   │                     service/ProductoService
    │   ├── infrastructure/   ProductoJpaRepository, ProductoRepositoryAdapter
    │   └── presentation/     ProductoController, ProductoMapper
    │                         dto/ProductoRequest, ProductoResponse,
    │                         StockRequest, LoteResponse
    ├── categories/           domain, infrastructure, presentation
    ├── suppliers/            domain, infrastructure, presentation
    ├── sales/
    │   ├── domain/           entity/Venta, DetalleVenta, VentaService
    │   ├── infrastructure/
    │   └── presentation/     VentaController, dto/VentaRequest, VentaResponse
    ├── reports/
    │   ├── domain/           ReportService
    │   ├── infrastructure/   ReportQueryRepository (consultas nativas)
    │   └── presentation/     ReportController, dto/*Row
    ├── summary/
    │   ├── domain/           ResumenService, AlertaService, GeneradorLocal,
    │   │                     puerto AiTextClient
    │   ├── infrastructure/   AiTextClientImpl (RestClient),
    │   │                     ResumenQueryRepository, ResumenIaRepository
    │   └── presentation/     ResumenController,
    │                         dto/ResumenResponse, AlertaDto, DatosResumenDto
    ├── scanner/
    │   ├── domain/           VisionService, puerto AiVisionClient
    │   ├── infrastructure/   AiVisionClientImpl
    │   └── presentation/     ScannerController, dto/SugerenciaProductoResponse
    └── auth/        (OPCIONAL)
src/main/resources/
    ├── application.properties
    └── application-local.properties   # NO se sube a GitHub
```

---

## 4. Configuración y secretos

### 4.1 `application.properties` (se sube a GitHub, sin secretos)

```properties
spring.application.name=inventaria-api
server.port=8080
server.address=0.0.0.0

# SQL Server (valores desde variables de entorno)
spring.datasource.url=${DB_URL:jdbc:sqlserver://localhost:1433;databaseName=InventarIA;encrypt=true;trustServerCertificate=true}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

# JPA: el esquema lo crea el script SQL, Hibernate solo valida
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
spring.jpa.open-in-view=false
spring.jpa.show-sql=false

# JSON
spring.jackson.time-zone=America/Mexico_City
spring.jackson.default-property-inclusion=non_null

# Subida de fotos
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=6MB

# IA (la clave NUNCA va escrita aquí)
ia.api-key=${IA_API_KEY:}
ia.base-url=${IA_BASE_URL:}
ia.model-texto=${IA_MODEL_TEXTO:}
ia.model-vision=${IA_MODEL_VISION:}
ia.timeout-segundos=20
ia.max-tokens=500
```

`PhysicalNamingStrategyStandardImpl` hace que Hibernate use **exactamente** los nombres de tabla y columna de las anotaciones (`Productos`, `IdProducto`), sin convertirlos a snake_case.

### 4.2 Variables de entorno

```
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=InventarIA;encrypt=true;trustServerCertificate=true
DB_USER=sa
DB_PASSWORD=********
IA_API_KEY=********
IA_BASE_URL=https://...
IA_MODEL_TEXTO=...
IA_MODEL_VISION=...
```

Reglas de seguridad de secretos:

- `.env` y `application-local.properties` van en `.gitignore`.
- Solo se sube `.env.example` con valores vacíos.
- Si la clave se llega a subir por error: **revocarla** en el proveedor, no basta con borrar el commit.
- La clave solo se lee en `AiTextClientImpl` y `AiVisionClientImpl`. Ningún endpoint la devuelve ni la escribe en logs.

### 4.3 Conexión con la app

- Emulador Android: la app usa `http://10.0.2.2:8080`. Celular físico: IP de la PC en la misma red (`server.address=0.0.0.0` ya lo permite).
- En desarrollo se permite HTTP; la app debe habilitar tráfico en claro en Android, o bien usar HTTPS con certificado de desarrollo.
- Abrir el puerto 8080 en el firewall de Windows.
- SQL Server: habilitar TCP/IP (puerto 1433) en el Configuration Manager y usar `trustServerCertificate=true` en desarrollo local.
- CORS: la app nativa no lo requiere, pero se habilita para Swagger UI y pruebas desde navegador.

---

## 5. Modelo de datos (SQL Server)

Cinco tablas más una opcional. El script va en `Database/script_creacion.sql` y **debe poder ejecutarse completo y repetidamente** (con `DROP` condicional al inicio).

```sql
CREATE DATABASE InventarIA;
GO
USE InventarIA;
GO

CREATE TABLE Categorias (
    IdCategoria  INT IDENTITY(1,1) PRIMARY KEY,
    Nombre       NVARCHAR(100) NOT NULL UNIQUE,
    Descripcion  NVARCHAR(255) NULL
);

CREATE TABLE Proveedores (
    IdProveedor  INT IDENTITY(1,1) PRIMARY KEY,
    Nombre       NVARCHAR(150) NOT NULL,
    Telefono     NVARCHAR(20)  NULL,
    Correo       NVARCHAR(150) NULL
);

CREATE TABLE Productos (
    IdProducto      INT IDENTITY(1,1) PRIMARY KEY,
    CodigoBarras    NVARCHAR(20)  NOT NULL UNIQUE,
    Nombre          NVARCHAR(150) NOT NULL,
    Descripcion     NVARCHAR(500) NULL,
    Precio          DECIMAL(10,2) NOT NULL CHECK (Precio > 0),
    Stock           INT NOT NULL CHECK (Stock >= 0),
    StockMinimo     INT NOT NULL DEFAULT 5 CHECK (StockMinimo >= 0),
    Tipo            NVARCHAR(20)  NOT NULL CHECK (Tipo IN ('Perecedero','Electronico','Ropa')),
    FechaCaducidad  DATE NULL,           -- solo Perecedero
    MesesGarantia   INT  NULL,           -- solo Electronico
    Talla           NVARCHAR(10) NULL,   -- solo Ropa
    IdCategoria     INT NOT NULL REFERENCES Categorias(IdCategoria),
    IdProveedor     INT NOT NULL REFERENCES Proveedores(IdProveedor)
);

CREATE TABLE Ventas (
    IdVenta  INT IDENTITY(1,1) PRIMARY KEY,
    Fecha    DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    Total    DECIMAL(12,2) NOT NULL CHECK (Total >= 0)
);

CREATE TABLE DetalleVenta (
    IdDetalle       INT IDENTITY(1,1) PRIMARY KEY,
    IdVenta         INT NOT NULL REFERENCES Ventas(IdVenta) ON DELETE CASCADE,
    IdProducto      INT NOT NULL REFERENCES Productos(IdProducto),
    Cantidad        INT NOT NULL CHECK (Cantidad > 0),
    PrecioUnitario  DECIMAL(10,2) NOT NULL CHECK (PrecioUnitario > 0)
);

-- Opcional: historial de resumenes de IA
CREATE TABLE ResumenesIA (
    IdResumen  INT IDENTITY(1,1) PRIMARY KEY,
    Fecha      DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    Texto      NVARCHAR(MAX) NOT NULL
);
```

**Datos de prueba** (en el mismo script):

- Al menos 5 categorías, 4 proveedores y unos 30 productos mezclando los tres tipos.
- Varios productos con `Stock <= StockMinimo` (alguno en 0) para que el resumen siempre tenga alertas.
- Varios perecederos con `FechaCaducidad` **relativa a hoy** (`DATEADD(DAY, n, CAST(GETDATE() AS DATE))`), incluyendo uno ya vencido y otros entre 1 y 7 días, para que la demo funcione en cualquier fecha.
- Entre 10 y 15 ventas con detalle, algunas con fecha de **hoy**, para que "ventas del día" y "más vendidos" no salgan vacíos.

---

## 6. Convenciones de la API

- Prefijo `/api`, JSON en `camelCase`, fechas en ISO 8601 (`2026-12-01`, `2026-12-01T10:30:00`).
- `tipo` de producto con valores exactos: `Perecedero`, `Electronico`, `Ropa`. El mapper de la app crea la clase derivada a partir de este campo.
- Códigos HTTP: `200` OK, `201` creado (con cabecera `Location`), `204` sin contenido, `400` validación, `404` no encontrado, `409` conflicto, `413/415` archivo inválido, `503` IA no disponible.
- Los endpoints de lista devuelven **arreglos JSON** simples (fáciles de consumir con `RepositorioBase<T>`).
- Los identificadores en la URL son enteros (`/api/productos/12`).

### 6.1 Formato uniforme de error

```json
{
  "timestamp": "2026-11-26T14:05:12",
  "status": 400,
  "codigo": "VALIDACION",
  "mensaje": "Hay campos inválidos",
  "detalles": [ { "campo": "precio", "mensaje": "debe ser mayor a 0" } ],
  "ruta": "/api/productos"
}
```

| `codigo` | HTTP | Cuándo |
|---|---|---|
| `VALIDACION` | 400 | Falla de validación en el cuerpo o parámetros |
| `NO_ENCONTRADO` | 404 | Id o código de barras inexistente |
| `CODIGO_BARRAS_DUPLICADO` | 409 | Al crear/editar con un código ya usado |
| `STOCK_INSUFICIENTE` | 409 | Venta que dejaría el stock negativo |
| `REGISTRO_EN_USO` | 409 | Borrar categoría, proveedor o producto con referencias |
| `ARCHIVO_INVALIDO` | 400/413/415 | Foto vacía, muy grande o de tipo no permitido |
| `IA_NO_DISPONIBLE` | 503 | Falla, timeout o respuesta inválida de la IA |
| `ERROR_INTERNO` | 500 | Cualquier excepción no prevista (sin exponer el stacktrace) |

Se implementa con un `@RestControllerAdvice` (`GlobalExceptionHandler`) que maneja `MethodArgumentNotValidException`, `ConstraintViolationException`, `DataIntegrityViolationException`, `MaxUploadSizeExceededException` y las excepciones de dominio.

---

## 7. Endpoints por feature

### 7.1 `products` (CRUD completo, la feature principal)

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/productos` | Lista. Filtros opcionales: `q` (nombre o código), `categoriaId`, `proveedorId`, `tipo` | 200 `[ProductoResponse]` |
| GET | `/api/productos/{id}` | Detalle | 200 / 404 |
| GET | `/api/productos/codigo/{codigoBarras}` | Búsqueda exacta por código (escáner) | 200 / 404 |
| POST | `/api/productos` | Crear | 201 + `Location` |
| PUT | `/api/productos/{id}` | Actualizar completo | 200 / 404 / 409 |
| DELETE | `/api/productos/{id}` | Eliminar | 204 / 404 / 409 si tiene ventas |
| PATCH | `/api/productos/{id}/stock` | Ajuste de stock: `{ "delta": 10 }` (suma o resta) | 200 / 409 si queda negativo |
| POST | `/api/productos/lote` | Alta masiva (importación CSV), máximo 500 por llamada | 200 `LoteResponse` |

**`ProductoRequest` / `ProductoResponse`:**

```json
{
  "id": 12,
  "codigoBarras": "7501055300075",
  "nombre": "Leche entera 1 L",
  "descripcion": "Leche pasteurizada",
  "precio": 28.50,
  "stock": 40,
  "stockMinimo": 10,
  "tipo": "Perecedero",
  "fechaCaducidad": "2026-12-03",
  "mesesGarantia": null,
  "talla": null,
  "idCategoria": 2,
  "idProveedor": 1,
  "categoriaNombre": "Lácteos",
  "proveedorNombre": "Distribuidora Norte"
}
```

(`id`, `categoriaNombre` y `proveedorNombre` solo se devuelven, no se aceptan en el request.)

**Reglas de validación** (reflejan las validaciones del dominio de la app: precio > 0, nombre no vacío, stock no negativo, código con formato válido):

- `nombre`: obligatorio, no vacío, máximo 150.
- `codigoBarras`: obligatorio, `^\d{8,14}$` (EAN-8, UPC-A, EAN-13), único.
- `precio`: obligatorio, mayor a 0, máximo 2 decimales.
- `stock` y `stockMinimo`: enteros mayores o iguales a 0.
- `tipo`: uno de los tres valores permitidos.
- Reglas cruzadas por tipo:
  - `Perecedero` → `fechaCaducidad` obligatoria; `mesesGarantia` y `talla` se ignoran (se guardan en `NULL`).
  - `Electronico` → `mesesGarantia` obligatorio (≥ 0); los demás en `NULL`.
  - `Ropa` → `talla` obligatoria; los demás en `NULL`.
- `idCategoria` e `idProveedor` deben existir (si no, `400 VALIDACION` o `404`, definirlo y mantenerlo igual en todo el API).

**`POST /api/productos/lote`** (para la importación con progreso):

- Entrada: `{ "productos": [ProductoRequest, ...] }`.
- Cada fila se valida de forma independiente. Las válidas se insertan; las inválidas (o con código repetido) se reportan sin abortar todo el lote.
- Salida:

```json
{
  "recibidos": 100,
  "insertados": 97,
  "errores": [
    { "fila": 14, "codigoBarras": "123", "mensaje": "codigoBarras: formato inválido" },
    { "fila": 52, "codigoBarras": "7501055300075", "mensaje": "El código de barras ya existe" }
  ]
}
```

- La app parte el CSV en lotes de 50 a 100 filas, así puede mostrar progreso por lote y **cancelar entre lotes** con su `CancellationToken`. Cada lote es una transacción independiente (`@Transactional` en el servicio), así que cancelar a la mitad deja los lotes anteriores guardados.

### 7.2 `categories` y `suppliers`

CRUD simétrico:

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/categorias` · `/api/proveedores` | 200 lista |
| GET | `/api/categorias/{id}` · `/api/proveedores/{id}` | 200 / 404 |
| POST | `/api/categorias` · `/api/proveedores` | 201 |
| PUT | `/api/categorias/{id}` · `/api/proveedores/{id}` | 200 / 404 |
| DELETE | `/api/categorias/{id}` · `/api/proveedores/{id}` | 204 / 409 `REGISTRO_EN_USO` si tiene productos |

Validaciones: `nombre` obligatorio (máx. 100 categorías, 150 proveedores), nombre de categoría único, `correo` con formato válido si viene, `telefono` máximo 20.

### 7.3 `sales` (ventas con detalle y descuento de stock)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/ventas` | Registrar venta |
| GET | `/api/ventas` | Lista (filtro opcional `fecha=2026-11-26`) |
| GET | `/api/ventas/{id}` | Venta con su detalle y nombres de producto |

**Request:**

```json
{
  "items": [
    { "idProducto": 12, "cantidad": 2 },
    { "idProducto": 7,  "cantidad": 1, "precioUnitario": 45.00 }
  ]
}
```

**Reglas del registro** (todo en **una sola transacción**):

1. Validar que `items` no esté vacío y que cada `cantidad` sea mayor a 0. Si un producto se repite, sumar sus cantidades.
2. Para cada ítem, **descontar el stock con un UPDATE atómico** (ver 7.3.1). Si afecta 0 filas, lanzar `STOCK_INSUFICIENTE` y hacer **rollback de toda la venta**.
3. Precio por ítem: usar `Productos.Precio` de la base. El campo `precioUnitario` es opcional y existe porque la app calcula el precio final con polimorfismo (`CalcularPrecioFinal()`, descuentos por caducidad, etc.); si viene, se acepta solo si es mayor a 0. *Decisión a confirmar con el equipo y dejar documentada en el README.*
4. Guardar `Ventas` (con `Total` = suma de `cantidad × precioUnitario`) y sus `DetalleVenta`.
5. Responder `201` con la venta completa.

**Respuesta:**

```json
{
  "idVenta": 31,
  "fecha": "2026-11-26T14:20:05",
  "total": 101.00,
  "detalle": [
    { "idProducto": 12, "producto": "Leche entera 1 L", "cantidad": 2, "precioUnitario": 28.50, "subtotal": 57.00 },
    { "idProducto": 7, "producto": "Audífonos básicos", "cantidad": 1, "precioUnitario": 45.00, "subtotal": 45.00 }
  ]
}
```

#### 7.3.1 Descuento atómico de stock (importante)

Nunca hacer "leer stock, restar en Java, guardar", porque dos ventas simultáneas pisarían el valor. Se hace en una sola sentencia:

```java
public interface ProductoJpaRepository extends JpaRepository<Producto, Integer> {

    @Modifying
    @Query("UPDATE Producto p SET p.stock = p.stock - :cantidad " +
           "WHERE p.id = :id AND p.stock >= :cantidad")
    int descontarStock(@Param("id") int id, @Param("cantidad") int cantidad);
}
```

Si devuelve `0`, el producto no existe o no alcanza el stock, y se lanza la excepción. Además, la restricción `CHECK (Stock >= 0)` de la base es la última barrera. Esto complementa la demo del simulador (la parte "sin sincronización vs. con lock/Interlocked" ocurre en la app C#; aquí se demuestra la forma correcta en el servidor).

### 7.4 `reports` (consultas JOIN y GROUP BY, tema 12)

Cuatro endpoints de solo lectura. **Las consultas se escriben explícitamente en SQL** (consultas nativas) para poder mostrarlas y explicarlas en la demo.

| Ruta | Tablas | Descripción |
|---|---|---|
| `GET /api/reportes/productos-detalle` | Productos + Categorias + Proveedores | Productos con su categoría y proveedor |
| `GET /api/reportes/ventas-detalle?desde=&hasta=` | Ventas + DetalleVenta + Productos | Ventas con detalle y nombre de producto |
| `GET /api/reportes/stock-bajo-por-proveedor` | Productos + Proveedores | Productos con stock ≤ mínimo, ordenados por proveedor |
| `GET /api/reportes/total-por-categoria` | DetalleVenta + Productos + Categorias | Total vendido por categoría (`GROUP BY`) |

```sql
-- 1. Productos con categoría y proveedor
SELECT p.IdProducto, p.CodigoBarras, p.Nombre, p.Precio, p.Stock, p.StockMinimo, p.Tipo,
       c.Nombre AS Categoria, pr.Nombre AS Proveedor
FROM Productos p
INNER JOIN Categorias  c  ON c.IdCategoria  = p.IdCategoria
INNER JOIN Proveedores pr ON pr.IdProveedor = p.IdProveedor
ORDER BY p.Nombre;

-- 2. Ventas con detalle
SELECT v.IdVenta, v.Fecha, v.Total, d.IdDetalle, p.Nombre AS Producto,
       d.Cantidad, d.PrecioUnitario, (d.Cantidad * d.PrecioUnitario) AS Subtotal
FROM Ventas v
INNER JOIN DetalleVenta d ON d.IdVenta = v.IdVenta
INNER JOIN Productos    p ON p.IdProducto = d.IdProducto
WHERE v.Fecha >= :desde AND v.Fecha < :hasta
ORDER BY v.Fecha DESC, v.IdVenta, d.IdDetalle;

-- 3. Stock bajo por proveedor
SELECT pr.IdProveedor, pr.Nombre AS Proveedor, pr.Telefono, pr.Correo,
       p.IdProducto, p.Nombre AS Producto, p.Stock, p.StockMinimo
FROM Proveedores pr
INNER JOIN Productos p ON p.IdProveedor = pr.IdProveedor
WHERE p.Stock <= p.StockMinimo
ORDER BY pr.Nombre, p.Nombre;

-- 4. Total vendido por categoría (GROUP BY)
SELECT c.IdCategoria, c.Nombre AS Categoria,
       SUM(d.Cantidad) AS UnidadesVendidas,
       SUM(d.Cantidad * d.PrecioUnitario) AS TotalVendido
FROM DetalleVenta d
INNER JOIN Productos  p ON p.IdProducto = d.IdProducto
INNER JOIN Categorias c ON c.IdCategoria = p.IdCategoria
GROUP BY c.IdCategoria, c.Nombre
ORDER BY TotalVendido DESC;
```

Implementación en Spring Data con **proyecciones de interfaz**:

```java
public interface ReportQueryRepository extends Repository<Producto, Integer> {

    @Query(value = """
        SELECT c.IdCategoria AS idCategoria, c.Nombre AS categoria,
               SUM(d.Cantidad) AS unidadesVendidas,
               SUM(d.Cantidad * d.PrecioUnitario) AS totalVendido
        FROM DetalleVenta d
        INNER JOIN Productos p ON p.IdProducto = d.IdProducto
        INNER JOIN Categorias c ON c.IdCategoria = p.IdCategoria
        GROUP BY c.IdCategoria, c.Nombre
        ORDER BY totalVendido DESC
        """, nativeQuery = true)
    List<TotalPorCategoriaRow> totalPorCategoria();
}

public interface TotalPorCategoriaRow {
    Integer getIdCategoria();
    String getCategoria();
    Long getUnidadesVendidas();
    java.math.BigDecimal getTotalVendido();
}
```

Notas prácticas:

- Los alias de las columnas deben coincidir con los getters de la proyección (`AS totalVendido` → `getTotalVendido()`).
- En SQL Server, un parámetro `datetime` nulo en una consulta nativa (`:desde IS NULL OR ...`) suele fallar por inferencia de tipo. Por eso el servicio **sustituye los nulos por valores por defecto** (`desde` = `1900-01-01`, `hasta` = mañana) antes de llamar al repositorio.
- Alternativa equivalente: `JdbcTemplate` con `RowMapper`.

### 7.5 `summary` (resumen y alertas del día con IA de texto)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/resumen/datos` | Solo los **datos estructurados** del día, sin IA (sirve también al generador local de la app) |
| POST | `/api/resumen/generar` | Datos + alertas + texto del reporte (IA o respaldo) |
| GET | `/api/resumen/historial` | *(opcional)* últimos resúmenes guardados en `ResumenesIA` |

**Flujo de `POST /api/resumen/generar`:**

1. Ejecutar las consultas SQL del día:
   - **Stock bajo:** productos con `Stock <= StockMinimo`.
   - **Por caducar:** perecederos con `FechaCaducidad` hoy o dentro de los próximos 7 días, **más los ya vencidos** (marcados como tal).
   - **Ventas del día:** número de ventas y total.
   - **Más vendidos del día:** top 5 por unidades.
2. Construir las **alertas de forma determinista en el servidor** (no las inventa la IA):

   | Nivel | Regla |
   |---|---|
   | `CRITICO` (rojo) | Stock = 0, producto vencido o que caduca en 0 a 2 días |
   | `AVISO` (amarillo) | `0 < Stock <= StockMinimo`, o caduca en 3 a 7 días |

3. Armar el objeto JSON de datos y enviarlo a la IA junto con el prompt (ver más abajo).
4. Si la IA responde, devolver `origen: "IA"`. Si falla (sin internet, timeout, error HTTP, respuesta vacía o sin clave configurada), generar el texto con **plantillas locales** y devolver `origen: "LOCAL"`. **Nunca debe devolverse un error si hay datos**: la demo no puede quedarse sin resultado.
5. Guardar el texto en `ResumenesIA` (opcional) y responder.

```sql
-- Stock bajo
SELECT IdProducto, Nombre, Stock, StockMinimo FROM Productos
WHERE Stock <= StockMinimo ORDER BY Stock - StockMinimo, Nombre;

-- Por caducar o vencidos
SELECT IdProducto, Nombre, FechaCaducidad,
       DATEDIFF(DAY, CAST(GETDATE() AS DATE), FechaCaducidad) AS DiasRestantes
FROM Productos
WHERE Tipo = 'Perecedero' AND FechaCaducidad IS NOT NULL
  AND FechaCaducidad <= DATEADD(DAY, 7, CAST(GETDATE() AS DATE))
ORDER BY FechaCaducidad;

-- Ventas del día
SELECT COUNT(*) AS NumVentas, COALESCE(SUM(Total), 0) AS TotalDia
FROM Ventas WHERE CAST(Fecha AS DATE) = CAST(GETDATE() AS DATE);

-- Más vendidos del día
SELECT TOP 5 p.Nombre, SUM(d.Cantidad) AS Unidades
FROM DetalleVenta d
INNER JOIN Ventas v ON v.IdVenta = d.IdVenta
INNER JOIN Productos p ON p.IdProducto = d.IdProducto
WHERE CAST(v.Fecha AS DATE) = CAST(GETDATE() AS DATE)
GROUP BY p.Nombre ORDER BY SUM(d.Cantidad) DESC;
```

**Respuesta:**

```json
{
  "generadoEn": "2026-11-26T14:30:00",
  "origen": "IA",
  "texto": "Hoy se registraron 8 ventas por $1,240.50. Hay 3 productos en stock crítico...",
  "alertas": [
    { "nivel": "CRITICO", "tipo": "STOCK_BAJO", "idProducto": 4, "titulo": "Sin stock: Pan de caja", "detalle": "Stock 0, mínimo 5" },
    { "nivel": "AVISO", "tipo": "CADUCIDAD", "idProducto": 12, "titulo": "Caduca en 5 días: Leche entera", "detalle": "Vence 2026-12-01" }
  ],
  "datos": {
    "ventasDia": { "numVentas": 8, "total": 1240.50 },
    "masVendidos": [ { "nombre": "Leche entera 1 L", "unidades": 14 } ]
  }
}
```

**Prompt a la IA de texto** (en `application` o clase de constantes, no en el controller):

- *Sistema:* "Eres un asistente de inventario para una tienda. Con los datos JSON que recibes redacta un reporte breve en español (máximo 150 palabras) con: resumen de ventas, alertas importantes y 2 o 3 recomendaciones de reabastecimiento. Usa solo los datos recibidos; no inventes productos ni cifras. Responde en texto plano."
- *Usuario:* el JSON de datos.

**Cliente de IA (`AiTextClient`):**

- Interfaz en `summary/domain`; implementación con `RestClient` en `infrastructure`. Así el proveedor es intercambiable y los tests usan un *mock*.
- Timeouts: conexión 5 s, lectura `ia.timeout-segundos` (20 s). Sin reintentos automáticos.
- Limitar el tamaño de la solicitud (máx. 20 productos por lista, `max-tokens` configurable) para controlar costo.
- El payload exacto depende del proveedor elegido (se recomienda uno con capa gratuita o crédito de prueba). Toda la clave, URL y modelo vienen de `ia.*`.

**Cancelación:** el botón Cancelar de la app aborta la petición con su `CancellationToken`. El servidor solo debe garantizar que una petición abortada no deje datos inconsistentes (el guardado en `ResumenesIA` ocurre al final, es opcional y no afecta ventas ni stock). La llamada a la IA está acotada por el timeout.

### 7.6 `scanner` (código de barras y foto del producto)

**Código de barras:** lo cubre `GET /api/productos/codigo/{codigoBarras}`. Existe → `200` con la ficha (la app muestra sumar stock o editar, usando `PATCH .../stock`). No existe → `404 NO_ENCONTRADO` (la app navega con Shell al formulario de alta pasando el código).

**Foto del producto:**

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/scanner/analizar-foto` | `multipart/form-data`, campo `foto` (JPEG, PNG o WebP) |

Pasos:

1. Validar la imagen: no vacía, máximo 5 MB, tipo permitido (verificar el *content type* **y** que realmente se pueda decodificar como imagen). Si no, `ARCHIVO_INVALIDO`.
2. Recomendado: reducir la imagen a un lado máximo de ~1024 px antes de enviarla (menos costo y latencia).
3. Consultar las categorías existentes para incluirlas en el prompt y que la IA elija una de ellas.
4. Enviar la imagen en base64 al modelo con visión y exigir **solo JSON**.
5. Parsear con tolerancia (quitar posibles bloques ```` ```json ````), validar campos y normalizar `tipoProducto` a uno de los tres valores. Si no es válido, usar `null`.
6. Responder la **sugerencia**; el backend **no guarda nada**: la app precarga el formulario y la persona confirma o corrige.

**Respuesta:**

```json
{
  "nombreSugerido": "Refresco de cola 600 ml",
  "categoriaSugerida": "Bebidas",
  "idCategoriaSugerida": 3,
  "tipoProducto": "Perecedero",
  "descripcion": "Refresco carbonatado sabor cola en botella de 600 ml."
}
```

- `idCategoriaSugerida` se llena solo si la categoría sugerida coincide (sin importar mayúsculas ni acentos) con una existente; si no, va `null`.
- Si la IA falla o no responde: `503 IA_NO_DISPONIBLE`. La app ofrece captura manual del código y selector de archivos como respaldo.

### 7.7 `auth` (opcional, no es requisito de la rúbrica)

Solo si sobra tiempo: `POST /api/auth/login` con usuario y contraseña → JWT; Spring Security protege todo `/api/**` excepto login, Swagger y `/api/health`. Contraseñas con BCrypt. Agregar una tabla `Usuarios`. Si se implementa, documentar cómo usarlo en la demo para que no estorbe.

### 7.8 Utilitarios

- `GET /api/health` → `{ "status": "UP" }` para que la app (y la demo) verifiquen que el servidor responde.
- Swagger UI en `/swagger-ui.html` y OpenAPI en `/v3/api-docs`, con descripción de cada endpoint.

---

## 8. Reglas de negocio transversales

1. **Integridad:** el código de barras es único; no se puede borrar una categoría, proveedor o producto con referencias (`REGISTRO_EN_USO`, 409). Conviene validarlo antes con una consulta de conteo y, además, capturar `DataIntegrityViolationException` por si acaso.
2. **Stock nunca negativo:** UPDATE atómico + `CHECK` en la base.
3. **Transacciones:** `@Transactional` en los servicios de escritura (ventas, lotes). Los controllers no abren transacciones.
4. **Entidades fuera de la API:** los controllers nunca devuelven entidades JPA; siempre DTOs (evita exponer relaciones y problemas de serialización lazy).
5. **Dinero:** `BigDecimal` en Java y `DECIMAL` en SQL; nunca `double`.
6. **Fechas:** `LocalDate` para caducidad, `LocalDateTime` para ventas. Una sola zona horaria (`America/Mexico_City`) y la misma lógica de "hoy" en el servidor y en las consultas SQL.
7. **Logs:** nivel INFO para operaciones relevantes, sin datos sensibles ni la clave de la IA, y sin imprimir imágenes en base64.

---

## 9. Pruebas

| Tipo | Qué cubrir | Herramienta |
|---|---|---|
| Unitarias de servicio | Validaciones cruzadas por tipo, cálculo de alertas (CRITICO/AVISO), generador local, venta con stock insuficiente | JUnit 5 + Mockito |
| Controllers | Códigos HTTP y formato de error | `@WebMvcTest` + MockMvc |
| Repositorios / SQL | Las 4 consultas de reportes devuelven filas con los datos de prueba | Pruebas manuales contra SQL Server (o Testcontainers) |
| Integración de IA | `AiTextClient` simulado: éxito, timeout y respuesta inválida → origen `LOCAL` | Mockito |
| Pruebas manuales | Colección en Swagger UI o archivo `requests.http` / Postman con todos los endpoints | Swagger / Postman |

Prueba de concurrencia en servidor (recomendada): lanzar N ventas simultáneas del mismo producto con stock limitado y verificar que el stock final nunca es negativo y que las que no alcanzan reciben `409`.

---

## 10. Plan de desarrollo del backend (orden recomendado)

Coincide con las fases 1, 3, 7 y 8 del plan de trabajo de la propuesta. La prioridad si el tiempo es limitado: **CRUD con JOIN primero**, IA al final.

### Fase 1 · Base
- [ ] Crear repositorio en GitHub con `.gitignore` (incluye `.env` y `application-local.properties`).
- [ ] Escribir y probar `script_creacion.sql` con datos de prueba relativos a la fecha.
- [ ] Generar el proyecto Spring Boot con las dependencias de la sección 2.
- [ ] Configurar `application.properties` y variables de entorno; verificar la conexión a SQL Server.
- [ ] Implementar `shared`: `ApiError`, `GlobalExceptionHandler`, CORS, OpenAPI, `/api/health`.

### Fase 3 · API y CRUD (temas 11 y 12)
- [ ] Feature `categories` y `suppliers`: entidad, repositorio, servicio, controller, DTOs.
- [ ] Feature `products`: entidad con `tipo`, validaciones cruzadas, CRUD, búsqueda por código y filtros.
- [ ] `PATCH /productos/{id}/stock` con ajuste atómico.
- [ ] Feature `sales`: registro transaccional con descuento atómico y consulta con detalle.
- [ ] Feature `reports`: las 4 consultas con `@Query(nativeQuery = true)` y proyecciones.
- [ ] Verificar todo en Swagger UI. **Ya con esto se pueden demostrar los temas 11 y 12.**

### Fase 6 · Soporte a los procesos concurrentes
- [ ] `POST /api/productos/lote` con reporte de errores por fila.
- [ ] Verificar que la app puede importar cientos de productos en lotes y cancelar entre lotes.

### Fase 7 · IA
- [ ] Elegir proveedor de IA (capa gratuita), guardar clave en variable de entorno.
- [ ] `AiTextClient` + `ResumenService` + alertas deterministas + **generador local de respaldo** (hacer primero el respaldo; así la demo ya funciona sin IA).
- [ ] `GET /api/resumen/datos` y `POST /api/resumen/generar`.
- [ ] `AiVisionClient` + `POST /api/scanner/analizar-foto` con validación de imagen.
- [ ] Probar: sin clave, sin internet, timeout y respuesta mal formada → siempre sale un resultado o un `503` claro.

### Fase 8 · Cierre
- [ ] Pruebas unitarias y de controllers principales.
- [ ] README del backend: cómo crear la base, variables de entorno, cómo correr y URL de Swagger.
- [ ] Revisar que **ningún secreto** esté en el historial de Git.
- [ ] Ensayar la demo con la API y la base reiniciadas desde cero (ejecutar el script completo).

---

## 11. Riesgos específicos del backend

| Riesgo | Mitigación |
|---|---|
| Spring Boot no conecta a SQL Server | Habilitar TCP/IP y el puerto 1433, usar `mssql-jdbc` y `trustServerCertificate=true` en desarrollo. |
| Hibernate usa nombres distintos a los de las tablas | `PhysicalNamingStrategyStandardImpl` y `ddl-auto=validate`. |
| La app no alcanza la API desde emulador o celular | `10.0.2.2` en el emulador, IP de la PC en celular, `server.address=0.0.0.0`, regla de firewall para 8080. |
| Falla la IA durante la demo | Respaldo local en el servidor (`origen: LOCAL`) y generador local en la app; probar la demo con anticipación. |
| Clave de IA expuesta | Variables de entorno, `.gitignore`, llamada a la IA solo desde la API; revocar si se filtra. |
| Costo de la IA | Proveedor con capa gratuita, límite de tamaño de solicitud y de imagen. |
| Parámetros de fecha nulos en consultas nativas | Valores por defecto en el servicio antes de llamar al repositorio. |
| Demasiadas capas retrasan el desarrollo | Estructura completa solo en `products`; modo simplificado en las demás features. |
| Datos de prueba vencidos el día de la demo | Fechas relativas con `DATEADD(DAY, n, GETDATE())` en el script. |

---

## 12. Cumplimiento de la lista de cotejo (parte backend)

| # | Tema | Pts | Evidencia del backend |
|---|---|---|---|
| 11 | Base de datos SQL Server | 10 | `Database/script_creacion.sql` con 5 tablas con PK y FK; API Spring Boot conectada a SQL Server (`inventaria-api/`). |
| 12 | CRUD y consultas JOIN | 14 | Controllers de productos (CRUD completo) y `ReportController` con 4 consultas JOIN/GROUP BY, visibles en `ReportesPage` de la app. |
| 8 | async/await | 7 | La API responde sin bloquear a la app: carga de productos, resumen con IA y análisis de foto. |
| 9 | Progreso y cancelación | 9 | `POST /productos/lote` permite importar por bloques con progreso y cancelación en la app. |
| 10 | Sincronización | 9 | Descuento atómico de stock en el servidor como contraparte del simulador de la app. |

Regla de oro de la propuesta: marcar *Cumple* solo lo que se pueda demostrar en ejecución.

---

## 13. Guion de demostración (parte backend)

1. Mostrar el repositorio, el script SQL y las 5 tablas con sus relaciones.
2. Abrir Swagger UI y ejecutar un CRUD de productos; mostrar un error de validación y un `409`.
3. Ejecutar las 4 consultas de reportes y señalar el SQL (JOIN y GROUP BY) en el código.
4. Registrar una venta y comprobar el descuento de stock; intentar vender más de lo disponible (`STOCK_INSUFICIENTE`).
5. Importar un CSV desde la app y mostrar el reporte de errores por fila.
6. Generar el resumen del día (origen `IA`) y luego quitar la clave o la red para mostrar el respaldo (`LOCAL`).
7. Escanear un código existente (200) y uno nuevo (404); analizar una foto y mostrar la sugerencia en JSON.
8. Mostrar que no hay secretos en GitHub (`.gitignore`, `.env.example`).

---

## 14. Resumen de endpoints

| Feature | Endpoints |
|---|---|
| products | `GET /api/productos` · `GET /api/productos/{id}` · `GET /api/productos/codigo/{codigo}` · `POST /api/productos` · `PUT /api/productos/{id}` · `DELETE /api/productos/{id}` · `PATCH /api/productos/{id}/stock` · `POST /api/productos/lote` |
| categories | `GET/POST /api/categorias` · `GET/PUT/DELETE /api/categorias/{id}` |
| suppliers | `GET/POST /api/proveedores` · `GET/PUT/DELETE /api/proveedores/{id}` |
| sales | `POST /api/ventas` · `GET /api/ventas` · `GET /api/ventas/{id}` |
| reports | `GET /api/reportes/productos-detalle` · `ventas-detalle` · `stock-bajo-por-proveedor` · `total-por-categoria` |
| summary | `GET /api/resumen/datos` · `POST /api/resumen/generar` · `GET /api/resumen/historial` (opc.) |
| scanner | `POST /api/scanner/analizar-foto` |
| utilitarios | `GET /api/health` · `/swagger-ui.html` |
| auth (opc.) | `POST /api/auth/login` |
