package com.dgi.platform.repository;

import com.dgi.platform.entity.FiscalNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FiscalNumberRepository extends JpaRepository<FiscalNumber, Long> {
    Optional<FiscalNumber> findByTaxpayerId(Long taxpayerId);
    Optional<FiscalNumber> findByFiscalNumber(String fiscalNumber);
}
