package com.dgi.platform.service.impl;

import com.dgi.platform.dto.AgentApplicationActionRequest;
import com.dgi.platform.dto.TaxApplicationRequest;
import com.dgi.platform.dto.TaxApplicationResponse;
import com.dgi.platform.entity.*;
import com.dgi.platform.exception.BadRequestException;
import com.dgi.platform.exception.ResourceNotFoundException;
import com.dgi.platform.exception.UnauthorizedException;
import com.dgi.platform.mapper.TaxApplicationMapper;
import com.dgi.platform.repository.FiscalNumberRepository;
import com.dgi.platform.repository.TaxApplicationRepository;
import com.dgi.platform.repository.TaxpayerRepository;
import com.dgi.platform.repository.UserRepository;
import com.dgi.platform.service.NotificationService;
import com.dgi.platform.service.TaxApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaxApplicationServiceImpl implements TaxApplicationService {

    private final TaxApplicationRepository taxApplicationRepository;
    private final UserRepository userRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final FiscalNumberRepository fiscalNumberRepository;
    private final NotificationService notificationService;
    private final TaxApplicationMapper taxApplicationMapper;

    @Override
    @Transactional
    public TaxApplicationResponse createApplication(String username, TaxApplicationRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Taxpayer taxpayer = taxpayerRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Only registered taxpayers can submit a tax application"));

        TaxApplication application = TaxApplication.builder()
                .taxpayer(taxpayer)
                .status(ApplicationStatus.PENDING)
                .activities(new ArrayList<>())
                .documents(new ArrayList<>())
                .build();

        if (request.getActivities() != null) {
            List<Activity> activities = request.getActivities().stream().map(dto -> Activity.builder()
                    .application(application)
                    .businessName(dto.getBusinessName())
                    .businessType(dto.getBusinessType())
                    .sector(dto.getSector())
                    .address(dto.getAddress())
                    .startDate(dto.getStartDate())
                    .build()).collect(Collectors.toList());
            application.setActivities(activities);
        }

        if (request.getDocuments() != null) {
            List<Document> documents = request.getDocuments().stream().map(dto -> Document.builder()
                    .application(application)
                    .type(dto.getType())
                    .filePath(dto.getFilePath())
                    .verificationStatus(VerificationStatus.PENDING)
                    .build()).collect(Collectors.toList());
            application.setDocuments(documents);
        }

        TaxApplication savedApplication = taxApplicationRepository.save(application);

        // Trigger Notification
        notificationService.sendNotification(user, "Application Submitted", 
                "Your tax registration application #" + savedApplication.getId() + " has been successfully submitted for review.");

        return taxApplicationMapper.toResponse(savedApplication);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxApplicationResponse getApplicationById(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        TaxApplication application = taxApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaxApplication", "id", id));

        boolean isOwner = application.getTaxpayer().getUser().getId().equals(user.getId());
        boolean isAgentOrAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_AGENT || r.getName() == RoleName.ROLE_ADMIN);

        if (!isOwner && !isAgentOrAdmin) {
            throw new UnauthorizedException("You are not authorized to view this application");
        }

        return taxApplicationMapper.toResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxApplicationResponse> getApplicationsByTaxpayer(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Taxpayer taxpayer = taxpayerRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("User is not registered as a taxpayer"));

        return taxApplicationRepository.findByTaxpayer(taxpayer).stream()
                .map(taxApplicationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxApplicationResponse> getApplicationsByStatus(ApplicationStatus status) {
        return taxApplicationRepository.findByStatus(status).stream()
                .map(taxApplicationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxApplicationResponse> getAllApplicationsForAgent() {
        return taxApplicationRepository.findAll().stream()
                .map(taxApplicationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaxApplicationResponse reviewApplicationByAgent(Long id, AgentApplicationActionRequest actionRequest) {
        TaxApplication application = taxApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaxApplication", "id", id));

        application.setStatus(actionRequest.getStatus());
        TaxApplication updated = taxApplicationRepository.save(application);

        User taxpayerUser = application.getTaxpayer().getUser();

        if (actionRequest.getStatus() == ApplicationStatus.APPROVED) {
            // Auto-generate Fiscal Number if not already present
            Taxpayer taxpayer = application.getTaxpayer();
            if (fiscalNumberRepository.findByTaxpayerId(taxpayer.getId()).isEmpty()) {
                String generatedNum = generateUniqueFiscalNumber();
                FiscalNumber fiscalNumber = FiscalNumber.builder()
                        .taxpayer(taxpayer)
                        .fiscalNumber(generatedNum)
                        .build();
                fiscalNumberRepository.save(fiscalNumber);
            }

            notificationService.sendNotification(taxpayerUser, "Application Approved!", 
                    "Congratulations! Your tax registration application #" + id + " has been approved. Your official Fiscal Number has been issued.");
        } else if (actionRequest.getStatus() == ApplicationStatus.REJECTED) {
            notificationService.sendNotification(taxpayerUser, "Application Rejected", 
                    "Your tax registration application #" + id + " was rejected. Agent comments: " + 
                    (actionRequest.getComments() != null ? actionRequest.getComments() : "Please verify your document details."));
        }

        return taxApplicationMapper.toResponse(updated);
    }

    private String generateUniqueFiscalNumber() {
        Random random = new Random();
        int part1 = 1000000 + random.nextInt(9000000);
        return part1 + "/A/M/000";
    }
}
