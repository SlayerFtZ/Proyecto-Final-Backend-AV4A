package com.inventaria.api.suppliers.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "Proveedores")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdProveedor")
    private Integer id;

    /** Dueño del registro (Usuarios.IdUsuario). */
    @Column(name = "IdUsuario", nullable = false, updatable = false)
    private Integer userId;

    @Column(name = "Nombre", nullable = false, length = 150)
    private String name;

    @Column(name = "Telefono", length = 20)
    private String phone;

    @Column(name = "Correo", length = 150)
    private String email;

    public Supplier() {}

    public Integer getId() { return id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
