package com.inventaria.api.products.domain;

import com.inventaria.api.categories.domain.Category;
import com.inventaria.api.suppliers.domain.Supplier;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Productos")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdProducto")
    private Integer id;

    /** Dueño del registro (Usuarios.IdUsuario). */
    @Column(name = "IdUsuario", nullable = false, updatable = false)
    private Integer userId;

    // Único por usuario: UQ_Productos_Usuario_CodigoBarras (IdUsuario, CodigoBarras)
    @Column(name = "CodigoBarras", nullable = false, length = 20)
    private String barcode;

    @Column(name = "Nombre", nullable = false, length = 150)
    private String name;

    @Column(name = "Descripcion", length = 500)
    private String description;

    @Column(name = "Precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "Stock", nullable = false)
    private Integer stock;

    @Column(name = "StockMinimo", nullable = false)
    private Integer minimumStock;

    @Column(name = "Tipo", nullable = false, length = 20)
    @Convert(converter = ProductTypeConverter.class)
    private ProductType type;

    @Column(name = "FechaCaducidad")
    private LocalDate expirationDate;

    @Column(name = "MesesGarantia")
    private Integer warrantyMonths;

    @Column(name = "Talla", length = 10)
    private String size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdCategoria", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdProveedor", nullable = false)
    private Supplier supplier;

    public Product() {}

    public Integer getId() { return id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getMinimumStock() { return minimumStock; }
    public void setMinimumStock(Integer minimumStock) { this.minimumStock = minimumStock; }

    public ProductType getType() { return type; }
    public void setType(ProductType type) { this.type = type; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public Integer getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(Integer warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
}
