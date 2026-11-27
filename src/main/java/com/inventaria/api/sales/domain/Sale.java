package com.inventaria.api.sales.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Ventas")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdVenta")
    private Integer id;

    /** Dueño del registro (Usuarios.IdUsuario). */
    @Column(name = "IdUsuario", nullable = false, updatable = false)
    private Integer userId;

    @Column(name = "Fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "Total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SaleItem> items = new ArrayList<>();

    public Sale() {}

    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
    }

    public Integer getId() { return id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public List<SaleItem> getItems() { return items; }
}
