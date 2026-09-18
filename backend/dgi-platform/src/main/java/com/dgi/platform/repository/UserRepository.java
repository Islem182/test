package com.dgi.platform.repository;

import com.dgi.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    Optional<User> findByCin(String cin);
    Optional<User> findByPassport(String passport);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Boolean existsByCin(String cin);
    Boolean existsByPassport(String passport);
    List<User> findAllByIsAdminFalse();
}
