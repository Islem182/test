package com.dgi.platform.repository;

import com.dgi.platform.entity.ApplicationStatus;
import com.dgi.platform.entity.TaxApplication;
import com.dgi.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxApplicationRepository extends JpaRepository<TaxApplication, Long> {
    List<TaxApplication> findByUser(User user);
    List<TaxApplication> findByUserId(Long userId);
    List<TaxApplication> findByStatus(ApplicationStatus status);
}
