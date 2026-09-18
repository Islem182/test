package com.dgi.platform.service.impl;

import com.dgi.platform.dto.AgentApplicationActionRequest;
import com.dgi.platform.dto.TaxApplicationRequest;
import com.dgi.platform.dto.TaxApplicationResponse;
import com.dgi.platform.entity.*;
import com.dgi.platform.exception.ResourceNotFoundException;
import com.dgi.platform.exception.UnauthorizedException;
import com.dgi.platform.mapper.TaxApplicationMapper;
import com.dgi.platform.repository.FiscalNumberRepository;
import com.dgi.platform.repository.TaxApplicationRepository;
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
    private final FiscalNumberRepository fiscalNumberRepository;
    private final NotificationService notificationService;
    private final TaxApplicationMapper taxApplicationMapper;

    @Override
    @Transactional
    public TaxApplicationResponse createApplication(String username, TaxApplicationRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        TaxApplication application = TaxApplication.builder()
                .user(user)
                .status(ApplicationStatus.PENDING)
                .activities(new ArrayList<>())
                .documents(new ArrayList<>())
                .build();

        if (request.getActivities() != null) {
            List<Activity> activities = request.getActivities().stream().map(dto ->
                    Activity.builder()
                            .application(application)
                            .businessName(dto.getBusinessName())
                            .businessType(dto.getBusinessType())
                            .sector(dto.getSector())
                            .address(dto.getAddress())
                            .startDate(dto.getStartDate())
                            .build()
            ).collect(Collectors.toList());
            application.setActivities(activities);
        }

        if (request.getDocuments() != null) {
            List<Document> documents = request.getDocuments().stream().map(dto ->
                    Document.builder()
                            .application(application)
                            .type(dto.getType())
                            .filePath(dto.getFilePath())
                            .verificationStatus(VerificationStatus.PENDING)
                            .build()
            ).collect(Collectors.toList());
            application.setDocuments(documents);
        }

        TaxApplication saved = taxApplicationRepository.save(application);
        notificationService.sendNotification(user, "Dossier reçu",
                "Votre dossier d'immatriculation fiscale #" + saved.getId() + " a bien été reçu.");
        return taxApplicationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxApplicationResponse getApplicationById(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        TaxApplication application = taxApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaxApplication", "id", id));

        boolean isOwner   = application.getUser().getId().equals(user.getId());
        boolean isAdmin   = user.isAdmin();
        if (!isOwner && !isAdmin)
            throw new UnauthorizedException("You are not authorized to view this application");

        return taxApplicationMapper.toResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxApplicationResponse> getApplicationsByTaxpayer(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return taxApplicationRepository.findByUser(user).stream()
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
        User appUser = application.getUser();

        if (actionRequest.getStatus() == ApplicationStatus.APPROVED) {
            if (fiscalNumberRepository.findByUserId(appUser.getId()).isEmpty()) {
                FiscalNumber fn = FiscalNumber.builder()
                        .user(appUser)
                        .fiscalNumber(generateUniqueFiscalNumber())
                        .build();
                fiscalNumberRepository.save(fn);
            }
            notificationService.sendNotification(appUser, "Dossier approuvé !",
                    "Félicitations ! Votre dossier #" + id + " a été approuvé. Votre matricule fiscal a été émis.");
        } else if (actionRequest.getStatus() == ApplicationStatus.REJECTED) {
            notificationService.sendNotification(appUser, "Dossier rejeté",
                    "Votre dossier #" + id + " a été rejeté. Commentaires : " +
                    (actionRequest.getComments() != null ? actionRequest.getComments() : "Vérifiez vos documents."));
        }

        return taxApplicationMapper.toResponse(updated);
    }

    private String generateUniqueFiscalNumber() {
        return (1_000_000 + new Random().nextInt(9_000_000)) + "/A/M/000";
    }
}
