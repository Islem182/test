package com.dgi.platform.mapper;

import com.dgi.platform.dto.ComplaintResponse;
import com.dgi.platform.entity.Complaint;
import org.springframework.stereotype.Component;

@Component
public class ComplaintMapper {

    public ComplaintResponse toResponse(Complaint c) {
        if (c == null) return null;
        String name = null;
        if (c.getUser() != null) {
            String f = c.getUser().getFirstName();
            String l = c.getUser().getLastName();
            if (f != null || l != null)
                name = ((f != null ? f : "") + " " + (l != null ? l : "")).trim();
        }
        return ComplaintResponse.builder()
                .id(c.getId())
                .userId(c.getUser() != null ? c.getUser().getId() : null)
                .userName(name)
                .subject(c.getSubject()).message(c.getMessage())
                .status(c.getStatus()).response(c.getResponse())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
