package com.dgi.platform.service;

import com.dgi.platform.dto.DocumentDto;
import com.dgi.platform.entity.Document;

import java.util.List;

public interface DocumentService {
    DocumentDto uploadDocument(Long applicationId, DocumentDto documentDto);
    List<DocumentDto> getDocumentsByApplication(Long applicationId);
}
