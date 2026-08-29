package com.dgi.platform.controller;

import com.dgi.platform.dto.ApiResponse;
import com.dgi.platform.dto.ComplaintRequest;
import com.dgi.platform.dto.ComplaintResponse;
import com.dgi.platform.service.ComplaintService;
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
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@Tag(name = "Complaints", description = "Taxpayer complaints submission and agent handling")
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    @PreAuthorize("hasRole('TAXPAYER') or hasRole('ADMIN')")
    @Operation(summary = "Submit new taxpayer complaint")
    public ResponseEntity<ApiResponse<ComplaintResponse>> createComplaint(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ComplaintRequest request) {

        ComplaintResponse response = complaintService.createComplaint(userDetails.getUsername(), request);
        return new ResponseEntity<>(ApiResponse.success("Complaint submitted successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get list of complaints for authenticated taxpayer or all complaints for AGENT/ADMIN")
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> getComplaints(
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAgentOrAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT") || a.getAuthority().equals("ROLE_ADMIN"));

        List<ComplaintResponse> complaints;
        if (isAgentOrAdmin) {
            complaints = complaintService.getAllComplaints();
        } else {
            complaints = complaintService.getComplaintsByTaxpayer(userDetails.getUsername());
        }

        return ResponseEntity.ok(ApiResponse.success("Complaints retrieved successfully", complaints));
    }
}
