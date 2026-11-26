# Plan de Desarrollo — InventarIA API (Spring Boot)

**Proyecto:** InventarIA · Backend REST  
**Stack:** Java 21 · Spring Boot 4 · SQL Server · Maven  
**Rúbrica:** Temas 8, 9, 10, 11, 12 de la lista de cotejo  

> Las fases siguen el orden recomendado en `Backend_InventarIA_SpringBoot.md`: CRUD primero, IA al final.  
> Marcar cada tarea con `[x]` al completarla.

---

## Fase 1 · Base

> Prerequisito para todo. Sin esta fase no arranca la aplicación ni conecta a la base de datos.

### 1.1 Proyecto y dependencias

- [x] **Corregir `pom.xml`**
  - Renombrar `spring-boot-starter-webmvc` → `spring-boot-starter-web`
  - Consolidar las tres dependencias de test (`-data-jpa-test`, `-validation-test`, `-webmvc-test`) en una sola: `spring-boot-starter-test`
  - Agregar driver SQL Server: `com.microsoft.sqlserver:mssql-jdbc` (scope `runtime`)
  - Agregar documentación: `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0`

- [x] **Renombrar grupo y artefacto** en `pom.xml` a `com.inventaria` / `inventaria-api` para consistencia con el spec

### 1.2 Configuración

- [x] **`src/main/resources/application.properties`** — configuración completa:
  - Datasource con placeholders `${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}`
  - `spring.jpa.hibernate.ddl-auto=validate`
  - `PhysicalNamingStrategyStandardImpl` (Hibernate usa nombres exactos de las anotaciones)
  - `spring.jackson.time-zone=America/Mexico_City` y `default-property-inclusion=non_null`
  - `spring.servlet.multipart.max-file-size=5MB`
  - Variables `ia.*` (api-key, base-url, model-texto, model-vision, timeout-segundos, max-tokens)

- [x] **`.env.example`** — con todas las variables requeridas y valores vacíos:
  ```
  DB_URL=
  DB_USER=
  DB_PASSWORD=
  IA_API_KEY=
  IA_BASE_URL=
  IA_MODEL_TEXTO=
  IA_MODEL_VISION=
  ```

- [x] **`.gitignore`** — agregar `.env`, `application-local.properties` y `*.env`

### 1.3 Base de datos

- [x] **`database/script_creacion.sql`** — script idempotente (con `DROP TABLE IF EXISTS` en orden inverso a FK):
  - Tablas: `Categorias`, `Proveedores`, `Productos`, `Ventas`, `DetalleVenta`, `ResumenesIA`
  - Constraints: `CHECK (Precio > 0)`, `CHECK (Stock >= 0)`, `CHECK (Tipo IN (...))`, FKs con `ON DELETE CASCADE` donde aplica
  - Índice único en `Productos.CodigoBarras`
  - Datos de prueba:
    - 5 categorías, 4 proveedores
    - 30 productos mezclando los 3 tipos (`Perecedero`, `Electronico`, `Ropa`)
    - Varios productos con `Stock <= StockMinimo` (alguno en 0)
    - Perecederos con `FechaCaducidad` relativa: `DATEADD(DAY, n, CAST(GETDATE() AS DATE))` — incluir uno vencido y varios entre 1–7 días
    - 10–15 ventas con detalle, algunas con fecha de hoy

### 1.4 Infraestructura compartida (`shared/`)

- [x] **Excepciones de dominio** (`shared/error/`):
  - `ResourceNotFoundException` → 404
  - `DuplicateBarcodeException` → 409
  - `InsufficientStockException` → 409
  - `RecordInUseException` → 409
  - `InvalidFileException` → 400/413/415
  - `AiUnavailableException` → 503

- [x] **`ApiError`** — record con campos: `timestamp`, `status`, `codigo`, `mensaje`, `detalles[]`, `ruta`

- [x] **`GlobalExceptionHandler`** (`@RestControllerAdvice`) — maneja:
  - `MethodArgumentNotValidException` → 400 `VALIDACION` con lista de campos
  - `ConstraintViolationException` → 400 `VALIDACION`
  - `DataIntegrityViolationException` → 409 `REGISTRO_EN_USO`
  - `MaxUploadSizeExceededException` → 413 `ARCHIVO_INVALIDO`
  - Excepciones de dominio propias → su código y HTTP correspondiente
  - `Exception` genérica → 500 `ERROR_INTERNO` (sin stacktrace en la respuesta)

- [x] **`CorsConfig`** — permite orígenes para Swagger UI y pruebas desde navegador

- [x] **`OpenApiConfig`** — título, descripción y versión para Swagger UI en `/swagger-ui.html`

- [x] **`IaProperties`** — `@ConfigurationProperties(prefix="ia")` resuelve warnings del IDE

- [x] **`RestClientConfig`** — bean `RestClient` con timeout de conexión 5 s y lectura configurable via `ia.timeout-segundos`

- [x] **`HealthController`** — `GET /api/health` → `{ "status": "UP" }`

---

## Fase 2 · CRUD (Temas 11 y 12 de rúbrica — 24 pts)

> Con esta fase completa se pueden demostrar los temas 11 y 12.

### 2.1 Feature `categories`

- [ ] **Entidad** `Categoria` con `@Table(name="Categorias")`, `@Column` exactos
- [ ] **Repositorio** `CategoriaJpaRepository` extendiendo `JpaRepository<Categoria, Integer>`
- [ ] **Servicio** `CategoriaService`:
  - Validar nombre único antes de crear/editar
  - Al borrar: verificar que no tenga productos (`REGISTRO_EN_USO` si tiene)
- [ ] **Controller** `CategoriaController` con:
  - `GET /api/categorias` — lista completa
  - `GET /api/categorias/{id}` — detalle o 404
  - `POST /api/categorias` → 201 + `Location`
  - `PUT /api/categorias/{id}` → 200 / 404
  - `DELETE /api/categorias/{id}` → 204 / 409
- [ ] **DTOs**: `CategoriaRequest` (nombre, descripcion), `CategoriaResponse` (id, nombre, descripcion)

### 2.2 Feature `suppliers`

- [ ] **Entidad** `Proveedor` con `@Table(name="Proveedores")`
- [ ] **Repositorio** `ProveedorJpaRepository`
- [ ] **Servicio** `ProveedorService`:
  - Al borrar: verificar que no tenga productos asociados
- [ ] **Controller** `ProveedorController` — mismo patrón CRUD que categorías (`/api/proveedores`)
- [ ] **DTOs**: `ProveedorRequest` (nombre, telefono, correo), `ProveedorResponse`
- [ ] **Validaciones**: `correo` con `@Email` si viene, `telefono` máx. 20 chars

### 2.3 Feature `products` — Domain

- [ ] **Entidad** `Producto` con:
  - `@Table(name="Productos")`, todos los `@Column(name="...")` exactos al script SQL
  - Campos: `idProducto`, `codigoBarras`, `nombre`, `descripcion`, `precio` (`BigDecimal`), `stock`, `stockMinimo`, `tipo`, `fechaCaducidad` (`LocalDate`), `mesesGarantia`, `talla`
  - `@ManyToOne` hacia `Categoria` y `Proveedor` (fetch LAZY)
- [ ] **Enum** `TipoProducto` — `PERECEDERO`, `ELECTRONICO`, `ROPA` con valor `String` exacto para la BD
- [ ] **Puerto** `ProductoRepository` (interfaz en domain) con métodos: `findAll(filtros)`, `findById`, `findByCodigoBarras`, `save`, `delete`, `descontarStock`

### 2.4 Feature `products` — Infrastructure

- [ ] **`ProductoJpaRepository`** extendiendo `JpaRepository`:
  - `@Query` nativo `descontarStock(@Param id, @Param cantidad)` — UPDATE atómico `WHERE stock >= cantidad`
  - Consultas con filtros opcionales (`q`, `categoriaId`, `proveedorId`, `tipo`)
- [ ] **`ProductoRepositoryAdapter`** implementando `ProductoRepository` del dominio

### 2.5 Feature `products` — Presentation

- [ ] **`ProductoRequest`** con validaciones Jakarta:
  - `nombre`: `@NotBlank`, `@Size(max=150)`
  - `codigoBarras`: `@NotBlank`, `@Pattern(regexp="^\\d{8,14}$")`
  - `precio`: `@NotNull`, `@DecimalMin("0.01")`, tipo `BigDecimal`
  - `stock`, `stockMinimo`: `@Min(0)`
  - `tipo`: `@NotNull`
  - `idCategoria`, `idProveedor`: `@NotNull`
- [ ] **Validaciones cruzadas por tipo** en `ProductoService`:
  - `Perecedero` → `fechaCaducidad` obligatoria, limpiar `mesesGarantia` y `talla`
  - `Electronico` → `mesesGarantia` obligatorio (`>= 0`), limpiar los demás
  - `Ropa` → `talla` obligatoria, limpiar los demás
- [ ] **`ProductoResponse`** — incluye `id`, `categoriaNombre`, `proveedorNombre` (campos de solo lectura)
- [ ] **`ProductoMapper`** — convierte entre `Producto` ↔ `ProductoRequest` / `ProductoResponse`
- [ ] **`ProductoController`** con endpoints:
  - `GET /api/productos` — lista con filtros opcionales `q`, `categoriaId`, `proveedorId`, `tipo`
  - `GET /api/productos/{id}` — 200 / 404
  - `GET /api/productos/codigo/{codigoBarras}` — búsqueda exacta para el escáner
  - `POST /api/productos` → 201 + `Location`
  - `PUT /api/productos/{id}` → 200 / 404 / 409
  - `DELETE /api/productos/{id}` → 204 / 404 / 409 (si tiene ventas)
  - `PATCH /api/productos/{id}/stock` — body `{ "delta": N }`, acepta negativos con validación

---

## Fase 3 · Ventas

### 3.1 Feature `sales`

- [ ] **Entidades** `Venta` y `DetalleVenta` con relaciones y `@Table` exactos
- [ ] **Repositorios** JPA para ambas entidades
- [ ] **`VentaService`** — toda la lógica en **una sola transacción** (`@Transactional`):
  1. Validar `items` no vacío, `cantidad > 0`; si producto repetido, sumar cantidades
  2. Para cada ítem: llamar `descontarStock()` → si retorna 0, lanzar `StockInsuficienteException` y rollback
  3. Usar `Productos.Precio` de BD como precio base; aceptar `precioUnitario` del request solo si viene y es `> 0`
  4. Calcular `Total` = suma de `cantidad × precioUnitario`
  5. Guardar `Venta` y sus `DetalleVenta`
- [ ] **`VentaController`**:
  - `POST /api/ventas` → 201 con venta completa y detalle
  - `GET /api/ventas` — lista, filtro opcional `fecha=YYYY-MM-DD`
  - `GET /api/ventas/{id}` — venta con detalle y nombres de producto
- [ ] **DTOs**: `VentaRequest` (items[]), `ItemVentaRequest` (idProducto, cantidad, precioUnitario?), `VentaResponse` con `detalle[]`

---

## Fase 4 · Reportes (Tema 12 — JOIN y GROUP BY)

### 4.1 Feature `reports`

- [ ] **Proyecciones de interfaz** para cada consulta:
  - `ProductoDetalleRow` — producto + categoría + proveedor
  - `VentaDetalleRow` — venta + detalle + nombre de producto
  - `StockBajoPorProveedorRow` — proveedor + producto con stock bajo
  - `TotalPorCategoriaRow` — `idCategoria`, `categoria`, `unidadesVendidas`, `totalVendido`
- [ ] **`ReportQueryRepository`** — 4 métodos con `@Query(nativeQuery = true)`:
  - Los alias SQL deben coincidir exactamente con los getters de las proyecciones
  - Para `ventas-detalle`: el servicio sustituye `desde`/`hasta` nulos por valores por defecto (`1900-01-01` / mañana)
- [ ] **`ReportController`**:
  - `GET /api/reportes/productos-detalle`
  - `GET /api/reportes/ventas-detalle?desde=&hasta=`
  - `GET /api/reportes/stock-bajo-por-proveedor`
  - `GET /api/reportes/total-por-categoria`

---

## Fase 5 · Importación masiva (Tema 9 — progreso y cancelación)

### 5.1 Endpoint de lote

- [ ] **`POST /api/productos/lote`** — body: `{ "productos": [ProductoRequest, ...] }` (máx. 500):
  - Cada fila se valida de forma independiente
  - Las válidas se insertan; las inválidas se reportan **sin abortar el lote completo**
  - `@Transactional` en el servicio: cada lote es una transacción independiente
  - Si un código de barras ya existe: reportar como error sin interrumpir
- [ ] **`LoteResponse`** — `recibidos`, `insertados`, `errores[]` (fila, codigoBarras, mensaje)
- [ ] Verificar que la app pueda enviar lotes de 50–100 ítems con progreso y cancelación entre lotes

---

## Fase 6 · Resumen del día con IA de texto (Tema 8 — async)

### 6.1 Feature `summary` — Domain

- [ ] **Puerto** `AiTextClient` (interfaz) — método `generarTexto(String prompt, String datos): String`
- [ ] **`AlertaService`** — genera alertas de forma determinista (sin IA):
  - `CRITICO` (rojo): stock = 0, producto vencido, o caduca en 0–2 días
  - `AVISO` (amarillo): `0 < stock <= stockMinimo`, o caduca en 3–7 días
- [ ] **`GeneradorLocal`** — genera texto de resumen con plantillas en caso de falla de IA (nunca debe fallar)
- [ ] **`ResumenService`** — orquesta: consultas SQL → alertas → llamada IA → fallback local → respuesta

### 6.2 Feature `summary` — Infrastructure

- [ ] **`AiTextClientImpl`** con `RestClient`:
  - Lee `ia.api-key`, `ia.base-url`, `ia.model-texto` de `application.properties`
  - Timeouts: conexión 5 s, lectura `ia.timeout-segundos`
  - Sin reintentos automáticos
  - Limitar a máx. 20 productos por lista antes de enviar
  - **Nunca** loguear la clave ni datos en base64
- [ ] **`ResumenQueryRepository`** — 4 consultas nativas del día:
  - Stock bajo, por caducar/vencidos, ventas del día, top 5 más vendidos
- [ ] **`ResumenIaRepository`** — guarda texto generado en `ResumenesIA`

### 6.3 Feature `summary` — Presentation

- [ ] **Prompt del sistema** en constante o `application.properties`:
  > "Eres un asistente de inventario. Redacta un reporte breve en español (máx. 150 palabras) con resumen de ventas, alertas y 2–3 recomendaciones. Usa solo los datos recibidos. Responde en texto plano."
- [ ] **`ResumenController`**:
  - `GET /api/resumen/datos` — datos estructurados del día sin IA (también sirve al generador local de la app)
  - `POST /api/resumen/generar` — datos + alertas + texto (`origen: "IA"` o `"LOCAL"`)
  - `GET /api/resumen/historial` *(opcional)* — últimos resúmenes de `ResumenesIA`
- [ ] **DTOs**: `ResumenResponse` (generadoEn, origen, texto, alertas[], datos), `AlertaDto` (nivel, tipo, idProducto, titulo, detalle), `DatosResumenDto`

---

## Fase 7 · Escáner de fotos con IA de visión (Tema 8)

### 7.1 Feature `scanner`

- [ ] **Puerto** `AiVisionClient` — método `analizarImagen(byte[] imagen, List<String> categorias): SugerenciaProductoResponse`
- [ ] **`VisionService`** — validación de imagen:
  - No vacía, máx. 5 MB
  - Content-type debe ser `image/jpeg`, `image/png` o `image/webp`
  - Verificar que la imagen realmente se pueda decodificar (no solo confiar en el MIME)
  - Consultar categorías existentes para incluirlas en el prompt
- [ ] **`AiVisionClientImpl`**:
  - Enviar imagen en base64 al modelo de visión
  - Parseo tolerante: quitar bloques ` ```json ``` ` si vienen en la respuesta
  - Normalizar `tipoProducto` a uno de los tres valores válidos; usar `null` si no es reconocible
  - Si la IA falla → lanzar `IaNoDisponibleException` (503)
- [ ] **`ScannerController`** — `POST /api/scanner/analizar-foto`:
  - `multipart/form-data`, campo `foto`
  - Delegar validación a `VisionService`
  - `idCategoriaSugerida`: buscar match por nombre ignorando mayúsculas y acentos; `null` si no hay match
- [ ] **`SugerenciaProductoResponse`** — `nombreSugerido`, `categoriaSugerida`, `idCategoriaSugerida`, `tipoProducto`, `descripcion`

---

## Fase 8 · Pruebas y cierre

### 8.1 Pruebas unitarias (JUnit 5 + Mockito)

- [ ] `ProductoServiceTest` — validaciones cruzadas por tipo (Perecedero/Electronico/Ropa), limpieza de campos nulos
- [ ] `AlertaServiceTest` — casos CRITICO y AVISO por stock y por caducidad, incluir borde (caduca en exactamente 2 y 3 días)
- [ ] `GeneradorLocalTest` — texto generado no es nulo ni vacío con distintos escenarios de datos
- [ ] `VentaServiceTest` — venta exitosa descuenta stock; venta con stock insuficiente lanza excepción y hace rollback
- [ ] `ResumenServiceTest` — IA exitosa → `origen: IA`; timeout / error / clave vacía → `origen: LOCAL`

### 8.2 Pruebas de controllers (`@WebMvcTest` + MockMvc)

- [ ] `ProductoControllerTest`:
  - `POST` con body inválido → 400 con `detalles[]`
  - `POST` con código duplicado → 409 `CODIGO_BARRAS_DUPLICADO`
  - `GET` inexistente → 404 `NO_ENCONTRADO`
  - `PATCH /stock` con delta que dejaría stock negativo → 409 `STOCK_INSUFICIENTE`
- [ ] `VentaControllerTest`:
  - `POST` sin items → 400
  - `POST` con stock insuficiente → 409
  - `POST` exitosa → 201 con cuerpo completo
- [ ] `ScannerControllerTest`:
  - Imagen vacía → 400 `ARCHIVO_INVALIDO`
  - Imagen > 5 MB → 413 `ARCHIVO_INVALIDO`
  - IA falla → 503 `IA_NO_DISPONIBLE`

### 8.3 Verificación manual con Swagger UI

- [ ] Ejecutar CRUD completo de productos en Swagger: crear, editar, buscar por código, eliminar
- [ ] Intentar borrar categoría/proveedor con productos → verificar 409
- [ ] Registrar una venta y ver el descuento en stock
- [ ] Intentar vender más de lo disponible → verificar 409
- [ ] Ejecutar las 4 consultas de reportes y confirmar datos correctos
- [ ] `POST /resumen/generar` con clave configurada → `origen: IA`
- [ ] `POST /resumen/generar` sin clave → `origen: LOCAL`
- [ ] `POST /scanner/analizar-foto` con imagen válida

### 8.4 Cierre

- [ ] **`README.md`** — instrucciones completas:
  1. Prerrequisitos (Java 21, SQL Server, Maven)
  2. Crear la base de datos ejecutando `database/script_creacion.sql`
  3. Configurar variables de entorno copiando `.env.example` → `.env`
  4. `./mvnw spring-boot:run`
  5. Swagger UI: `http://localhost:8080/swagger-ui.html`
  6. Configuración para emulador Android (`10.0.2.2:8080`) y celular físico (IP de la PC)

- [ ] **Auditoría de seguridad final**:
  - `git log --all --full-history -- "*.env"` — confirmar que `.env` nunca se subió
  - Buscar `IA_API_KEY` en el historial de commits
  - Revisar logs de la aplicación: sin claves, sin imágenes base64
  - Confirmar que `.env` y `application-local.properties` están en `.gitignore`

---

## Resumen de endpoints por feature

| Feature | Endpoints |
|---|---|
| **products** | `GET /api/productos` · `GET /api/productos/{id}` · `GET /api/productos/codigo/{codigo}` · `POST /api/productos` · `PUT /api/productos/{id}` · `DELETE /api/productos/{id}` · `PATCH /api/productos/{id}/stock` · `POST /api/productos/lote` |
| **categories** | `GET /api/categorias` · `GET /api/categorias/{id}` · `POST /api/categorias` · `PUT /api/categorias/{id}` · `DELETE /api/categorias/{id}` |
| **suppliers** | `GET /api/proveedores` · `GET /api/proveedores/{id}` · `POST /api/proveedores` · `PUT /api/proveedores/{id}` · `DELETE /api/proveedores/{id}` |
| **sales** | `POST /api/ventas` · `GET /api/ventas` · `GET /api/ventas/{id}` |
| **reports** | `GET /api/reportes/productos-detalle` · `GET /api/reportes/ventas-detalle` · `GET /api/reportes/stock-bajo-por-proveedor` · `GET /api/reportes/total-por-categoria` |
| **summary** | `GET /api/resumen/datos` · `POST /api/resumen/generar` · `GET /api/resumen/historial` |
| **scanner** | `POST /api/scanner/analizar-foto` |
| **utils** | `GET /api/health` · `GET /swagger-ui.html` |

---

## Cobertura de la lista de cotejo

| # | Tema | Pts | Evidencia |
|---|---|---|---|
| 11 | Base de datos SQL Server | 10 | `database/script_creacion.sql` + API conectada a SQL Server |
| 12 | CRUD y consultas JOIN | 14 | `ProductoController` (CRUD) + `ReportController` (4 JOINs/GROUP BY) |
| 8 | async/await | 7 | Carga de productos, resumen con IA y análisis de foto desde la app |
| 9 | Progreso y cancelación | 9 | `POST /productos/lote` permite importar por bloques con progreso |
| 10 | Sincronización | 9 | Descuento atómico de stock (`UPDATE ... WHERE stock >= cantidad`) |

**Regla de oro:** marcar *Cumple* solo lo que se pueda demostrar en ejecución.
