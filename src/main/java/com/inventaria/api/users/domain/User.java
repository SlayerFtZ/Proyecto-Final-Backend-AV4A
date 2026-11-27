package com.inventaria.api.users.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Usuarios")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdUsuario")
    private Integer id;

    /** Identificador único e inmutable del usuario en Google (claim "sub"). */
    @Column(name = "GoogleSub", nullable = false, length = 255, unique = true)
    private String googleSub;

    @Column(name = "Email", nullable = false, length = 255)
    private String email;

    @Column(name = "Nombre", length = 150)
    private String name;

    @Column(name = "FotoUrl", length = 500)
    private String photoUrl;

    @Column(name = "FechaRegistro", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Column(name = "UltimoAcceso", nullable = false)
    private LocalDateTime lastAccessAt;

    public User() {}

    public Integer getId() { return id; }

    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public LocalDateTime getLastAccessAt() { return lastAccessAt; }
    public void setLastAccessAt(LocalDateTime lastAccessAt) { this.lastAccessAt = lastAccessAt; }
}
