package com.dgi.platform.mapper;

import com.dgi.platform.dto.ActivityDto;
import com.dgi.platform.dto.DocumentDto;
import com.dgi.platform.dto.TaxApplicationResponse;
import com.dgi.platform.entity.Activity;
import com.dgi.platform.entity.Document;
import com.dgi.platform.entity.TaxApplication;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class TaxApplicationMapper {

    public TaxApplicationResponse toResponse(TaxApplication application) {
        if (application == null) return null;

        String taxpayerName = application.getTaxpayer() != null ?
                application.getTaxpayer().getFirstName() + " " + application.getTaxpayer().getLastName() : null;

        return TaxApplicationResponse.builder()
                .id(application.getId())
                .taxpayerId(application.getTaxpayer() != null ? application.getTaxpayer().getId() : null)
                .taxpayerName(taxpayerName)
                .status(application.getStatus())
                .createdDate(application.getCreatedDate())
                .updatedDate(application.getUpdatedDate())
                .activities(application.getActivities() != null ?
                        application.getActivities().stream().map(this::toActivityDto).collect(Collectors.toList()) : Collections.emptyList())
                .documents(application.getDocuments() != null ?
                        application.getDocuments().stream().map(this::toDocumentDto).collect(Collectors.toList()) : Collections.emptyList())
                .build();
    }

    public ActivityDto toActivityDto(Activity activity) {
        if (activity == null) return null;

        return ActivityDto.builder()
                .id(activity.getId())
                .businessName(activity.getBusinessName())
                .businessType(activity.getBusinessType())
                .sector(activity.getSector())
                .address(activity.getAddress())
                .startDate(activity.getStartDate())
                .build();
    }

    public DocumentDto toDocumentDto(Document document) {
        if (document == null) return null;

        return DocumentDto.builder()
                .id(document.getId())
                .type(document.getType())
                .filePath(document.getFilePath())
                .uploadDate(document.getUploadDate())
                .verificationStatus(document.getVerificationStatus())
                .build();
    }
}
