package com.inventaria.api.users.presentation;

import com.inventaria.api.shared.error.ResourceNotFoundException;
import com.inventaria.api.shared.security.CurrentUser;
import com.inventaria.api.users.infrastructure.UserJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Perfil del usuario autenticado")
public class UserController {

    private final UserJpaRepository users;
    private final CurrentUser currentUser;

    public UserController(UserJpaRepository users, CurrentUser currentUser) {
        this.users = users;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario dueño del token")
    public UserResponse me() {
        Integer id = currentUser.getId();
        return users.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }
}
