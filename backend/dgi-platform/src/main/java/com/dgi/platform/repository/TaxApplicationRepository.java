package com.dgi.platform.repository;

import com.dgi.platform.entity.ApplicationStatus;
import com.dgi.platform.entity.TaxApplication;
import com.dgi.platform.entity.Taxpayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxApplicationRepository extends JpaRepository<TaxApplication, Long> {
    List<TaxApplication> findByTaxpayer(Taxpayer taxpayer);
    List<TaxApplication> findByTaxpayerId(Long taxpayerId);
    List<TaxApplication> findByStatus(ApplicationStatus status);
}
