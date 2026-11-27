package com.inventaria.api.categories.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "Categorias")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdCategoria")
    private Integer id;

    /** Dueño del registro (Usuarios.IdUsuario). */
    @Column(name = "IdUsuario", nullable = false, updatable = false)
    private Integer userId;

    @Column(name = "Nombre", nullable = false, length = 100)
    private String name;

    @Column(name = "Descripcion", length = 255)
    private String description;

    public Category() {}

    public Integer getId() { return id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
