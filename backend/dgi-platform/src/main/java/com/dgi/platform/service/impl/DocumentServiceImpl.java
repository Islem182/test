package com.dgi.platform.service.impl;

import com.dgi.platform.dto.DocumentDto;
import com.dgi.platform.entity.Document;
import com.dgi.platform.entity.TaxApplication;
import com.dgi.platform.entity.VerificationStatus;
import com.dgi.platform.exception.ResourceNotFoundException;
import com.dgi.platform.mapper.TaxApplicationMapper;
import com.dgi.platform.repository.DocumentRepository;
import com.dgi.platform.repository.TaxApplicationRepository;
import com.dgi.platform.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final TaxApplicationRepository taxApplicationRepository;
    private final TaxApplicationMapper taxApplicationMapper;

    @Override
    @Transactional
    public DocumentDto uploadDocument(Long applicationId, DocumentDto documentDto) {
        TaxApplication application = taxApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("TaxApplication", "id", applicationId));

        Document document = Document.builder()
                .application(application)
                .type(documentDto.getType())
                .filePath(documentDto.getFilePath())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        Document saved = documentRepository.save(document);
        return taxApplicationMapper.toDocumentDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDto> getDocumentsByApplication(Long applicationId) {
        return documentRepository.findByApplicationId(applicationId).stream()
                .map(taxApplicationMapper::toDocumentDto)
                .collect(Collectors.toList());
    }
}
