package com.inventaria.api.users.infrastructure;

import com.inventaria.api.users.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Integer> {

    Optional<User> findByGoogleSub(String googleSub);
}
