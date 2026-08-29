package com.dgi.platform.controller;

import com.dgi.platform.service.PdfGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Official PDF Certificates", description = "PDF Document & Tax Certificate Generation")
public class PdfController {

    private final PdfGeneratorService pdfGeneratorService;

    @GetMapping("/certificate/{taxpayerId}")
    @Operation(summary = "Generate and download official DGI Registration Certificate PDF")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable Long taxpayerId) {
        byte[] pdfBytes = pdfGeneratorService.generateRegistrationCertificate(taxpayerId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DGI_Certificate_" + taxpayerId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
