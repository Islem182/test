package com.dgi.platform.controller;

import com.dgi.platform.dto.ApiResponse;
import com.dgi.platform.dto.TaxApplicationRequest;
import com.dgi.platform.dto.TaxApplicationResponse;
import com.dgi.platform.entity.ApplicationStatus;
import com.dgi.platform.service.TaxApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Tag(name = "Tax Applications", description = "Submission, tracking, and management of Tax Applications")
public class TaxApplicationController {

    private final TaxApplicationService taxApplicationService;

    @PostMapping
    @PreAuthorize("hasRole('TAXPAYER') or hasRole('ADMIN')")
    @Operation(summary = "Submit new tax application")
    public ResponseEntity<ApiResponse<TaxApplicationResponse>> createApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TaxApplicationRequest request) {

        TaxApplicationResponse response = taxApplicationService.createApplication(userDetails.getUsername(), request);
        return new ResponseEntity<>(ApiResponse.success("Tax application submitted successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tax application details by ID")
    public ResponseEntity<ApiResponse<TaxApplicationResponse>> getApplicationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        TaxApplicationResponse response = taxApplicationService.getApplicationById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Application details retrieved successfully", response));
    }

    @GetMapping("/status")
    @Operation(summary = "Get status of all tax applications for authenticated taxpayer or filter by status")
    public ResponseEntity<ApiResponse<List<TaxApplicationResponse>>> getApplicationsStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) ApplicationStatus filterStatus) {

        List<TaxApplicationResponse> responses;
        if (filterStatus != null) {
            responses = taxApplicationService.getApplicationsByStatus(filterStatus);
        } else {
            responses = taxApplicationService.getApplicationsByTaxpayer(userDetails.getUsername());
        }

        return ResponseEntity.ok(ApiResponse.success("Tax applications status retrieved successfully", responses));
    }
}
