package com.dgi.platform.service;

import com.dgi.platform.dto.AgentApplicationActionRequest;
import com.dgi.platform.dto.TaxApplicationRequest;
import com.dgi.platform.dto.TaxApplicationResponse;
import com.dgi.platform.entity.ApplicationStatus;

import java.util.List;

public interface TaxApplicationService {
    TaxApplicationResponse createApplication(String username, TaxApplicationRequest request);
    TaxApplicationResponse getApplicationById(Long id, String username);
    List<TaxApplicationResponse> getApplicationsByTaxpayer(String username);
    List<TaxApplicationResponse> getApplicationsByStatus(ApplicationStatus status);
    List<TaxApplicationResponse> getAllApplicationsForAgent();
    TaxApplicationResponse reviewApplicationByAgent(Long id, AgentApplicationActionRequest actionRequest);
}
