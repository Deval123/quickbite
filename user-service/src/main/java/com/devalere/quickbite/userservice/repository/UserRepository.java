package com.devalere.quickbite.userservice.repository;

import java.util.Optional;
import java.util.UUID;

import com.devalere.quickbite.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>
{
    Optional<User> findByEmail(String email);

    Optional<User> findByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
