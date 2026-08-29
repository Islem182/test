package com.dgi.platform.service.impl;

import com.dgi.platform.dto.OcrCompareRequest;
import com.dgi.platform.dto.OcrVerificationResponse;
import com.dgi.platform.service.OcrService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OcrServiceImpl implements OcrService {

    @Override
    public OcrVerificationResponse analyzeDocument(String documentPath, String documentType) {
        Map<String, String> extracted = new HashMap<>();
        if ("CIN".equalsIgnoreCase(documentType) || "CIN_COPY".equalsIgnoreCase(documentType)) {
            extracted.put("cinNumber", "08923415");
            extracted.put("firstName", "Mohamed");
            extracted.put("lastName", "Ben Ali");
            extracted.put("birthDate", "1988-04-12");
            extracted.put("address", "Avenue Habib Bourguiba, Tunis");
        } else if ("PASSPORT".equalsIgnoreCase(documentType) || "PASSPORT_COPY".equalsIgnoreCase(documentType)) {
            extracted.put("passportNumber", "N1098452");
            extracted.put("firstName", "Mohamed");
            extracted.put("lastName", "Ben Ali");
            extracted.put("nationality", "TN");
        } else {
            extracted.put("businessName", "DGI Tech Solutions SARL");
            extracted.put("taxId", "1548962/A/M/000");
            extracted.put("registrationDate", "2024-01-15");
        }

        return OcrVerificationResponse.builder()
                .verification("SUCCESS")
                .confidenceScore(0.96)
                .documentType(documentType)
                .extractedData(extracted)
                .differences(Collections.emptyList())
                .build();
    }

    @Override
    public OcrVerificationResponse compareOcrWithUserData(OcrCompareRequest request) {
        OcrVerificationResponse analysis = analyzeDocument(request.getDocumentPath(), request.getDocumentType());
        Map<String, String> extracted = analysis.getExtractedData();
        List<String> differences = new ArrayList<>();

        if (request.getExpectedCin() != null && extracted.containsKey("cinNumber")) {
            if (!request.getExpectedCin().equalsIgnoreCase(extracted.get("cinNumber"))) {
                differences.add("CIN mismatch: User entered '" + request.getExpectedCin() + "', OCR extracted '" + extracted.get("cinNumber") + "'");
            }
        }

        if (request.getExpectedFirstName() != null && extracted.containsKey("firstName")) {
            if (!request.getExpectedFirstName().equalsIgnoreCase(extracted.get("firstName"))) {
                differences.add("First Name mismatch: User entered '" + request.getExpectedFirstName() + "', OCR extracted '" + extracted.get("firstName") + "'");
            }
        }

        if (request.getExpectedLastName() != null && extracted.containsKey("lastName")) {
            if (!request.getExpectedLastName().equalsIgnoreCase(extracted.get("lastName"))) {
                differences.add("Last Name mismatch: User entered '" + request.getExpectedLastName() + "', OCR extracted '" + extracted.get("lastName") + "'");
            }
        }

        String verificationStatus = differences.isEmpty() ? "SUCCESS" : "FAILED";

        return OcrVerificationResponse.builder()
                .verification(verificationStatus)
                .confidenceScore(differences.isEmpty() ? 0.96 : 0.65)
                .documentType(request.getDocumentType())
                .extractedData(extracted)
                .differences(differences)
                .build();
    }
}
