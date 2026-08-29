package com.dgi.platform.mapper;

import com.dgi.platform.dto.ComplaintResponse;
import com.dgi.platform.entity.Complaint;
import org.springframework.stereotype.Component;

@Component
public class ComplaintMapper {

    public ComplaintResponse toResponse(Complaint complaint) {
        if (complaint == null) return null;

        String taxpayerName = complaint.getTaxpayer() != null ?
                complaint.getTaxpayer().getFirstName() + " " + complaint.getTaxpayer().getLastName() : null;

        return ComplaintResponse.builder()
                .id(complaint.getId())
                .taxpayerId(complaint.getTaxpayer() != null ? complaint.getTaxpayer().getId() : null)
                .taxpayerName(taxpayerName)
                .subject(complaint.getSubject())
                .message(complaint.getMessage())
                .status(complaint.getStatus())
                .response(complaint.getResponse())
                .createdAt(complaint.getCreatedAt())
                .build();
    }
}
