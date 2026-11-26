package com.inventaria.api.sales.domain;

import com.inventaria.api.products.domain.Product;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "DetalleVenta")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdDetalle")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdVenta", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdProducto", nullable = false)
    private Product product;

    @Column(name = "Cantidad", nullable = false)
    private Integer quantity;

    @Column(name = "PrecioUnitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    public SaleItem() {}

    public Integer getId() { return id; }

    public Sale getSale() { return sale; }
    public void setSale(Sale sale) { this.sale = sale; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
