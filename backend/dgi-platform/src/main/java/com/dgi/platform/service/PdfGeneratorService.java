package com.dgi.platform.service;

public interface PdfGeneratorService {
    byte[] generateRegistrationCertificate(Long taxpayerId);
    byte[] generateApplicationPdf(Long applicationId);
}
