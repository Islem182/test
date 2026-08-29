package com.dgi.platform.repository;

import com.dgi.platform.entity.Taxpayer;
import com.dgi.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaxpayerRepository extends JpaRepository<Taxpayer, Long> {
    Optional<Taxpayer> findByUser(User user);
    Optional<Taxpayer> findByUserId(Long userId);
    Optional<Taxpayer> findByCin(String cin);
    Optional<Taxpayer> findByPassport(String passport);
    Boolean existsByCin(String cin);
    Boolean existsByPassport(String passport);
}
