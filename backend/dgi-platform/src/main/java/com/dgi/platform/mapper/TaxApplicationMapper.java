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

    public TaxApplicationResponse toResponse(TaxApplication app) {
        if (app == null) return null;

        String name = null;
        if (app.getUser() != null) {
            String first = app.getUser().getFirstName();
            String last  = app.getUser().getLastName();
            if (first != null || last != null)
                name = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        }

        return TaxApplicationResponse.builder()
                .id(app.getId())
                .userId(app.getUser() != null ? app.getUser().getId() : null)
                .taxpayerName(name)
                .status(app.getStatus())
                .createdDate(app.getCreatedDate())
                .updatedDate(app.getUpdatedDate())
                .activities(app.getActivities() != null
                        ? app.getActivities().stream().map(this::toActivityDto).collect(Collectors.toList())
                        : Collections.emptyList())
                .documents(app.getDocuments() != null
                        ? app.getDocuments().stream().map(this::toDocumentDto).collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }

    public ActivityDto toActivityDto(Activity a) {
        if (a == null) return null;
        return ActivityDto.builder()
                .id(a.getId()).businessName(a.getBusinessName())
                .businessType(a.getBusinessType()).sector(a.getSector())
                .address(a.getAddress()).startDate(a.getStartDate())
                .build();
    }

    public DocumentDto toDocumentDto(Document d) {
        if (d == null) return null;
        return DocumentDto.builder()
                .id(d.getId()).type(d.getType())
                .filePath(d.getFilePath()).uploadDate(d.getUploadDate())
                .verificationStatus(d.getVerificationStatus())
                .build();
    }
}
