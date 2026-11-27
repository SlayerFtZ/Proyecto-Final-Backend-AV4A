package com.inventaria.api.summary.infrastructure;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ResumenesIA")
public class AiSummaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdResumen")
    private Integer id;

    /** Dueño del registro (Usuarios.IdUsuario). */
    @Column(name = "IdUsuario", nullable = false, updatable = false)
    private Integer userId;

    @Column(name = "Fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "Texto", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String text;

    public Integer getId() { return id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
