package com.dgi.platform.controller;

import com.dgi.platform.dto.AgentApplicationActionRequest;
import com.dgi.platform.dto.ApiResponse;
import com.dgi.platform.dto.TaxApplicationResponse;
import com.dgi.platform.service.TaxApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
@Tag(name = "Agent Portal & Review Workflow", description = "Tax Agent application review, approval/rejection, and verification handling")
public class AgentController {

    private final TaxApplicationService taxApplicationService;

    @GetMapping("/applications")
    @Operation(summary = "Get all submitted taxpayer applications for Agent review queue")
    public ResponseEntity<ApiResponse<List<TaxApplicationResponse>>> getAllApplications() {
        List<TaxApplicationResponse> applications = taxApplicationService.getAllApplicationsForAgent();
        return ResponseEntity.ok(ApiResponse.success("Agent application queue retrieved successfully", applications));
    }

    @PutMapping("/applications/{id}/review")
    @Operation(summary = "Review, Approve, or Reject taxpayer registration application (auto-generates Fiscal Number on approval)")
    public ResponseEntity<ApiResponse<TaxApplicationResponse>> reviewApplication(
            @PathVariable Long id,
            @Valid @RequestBody AgentApplicationActionRequest actionRequest) {

        TaxApplicationResponse response = taxApplicationService.reviewApplicationByAgent(id, actionRequest);
        return ResponseEntity.ok(ApiResponse.success("Application status updated to " + actionRequest.getStatus(), response));
    }
}
