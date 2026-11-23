package com.powsali.loan_module.controller;

import com.powsali.loan_module.dto.LoanDocumentResponseDTO;
import com.powsali.loan_module.security.CustomUserDetails;
import com.powsali.loan_module.service.LoanDocumentService;
import com.powsali.loan_module.service.LoanDocumentService.StoredLoanDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/loans/{loanId}/documents")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class LoanDocumentController {

    private final LoanDocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EMPLOYEE','DDO','HOO','ADMIN')")
    public ResponseEntity<LoanDocumentResponseDTO> upload(
            @PathVariable Long loanId,
            @RequestPart("document") MultipartFile document,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(documentService.upload(
                loanId, document, principal.getUser()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','DDO','HOO','ADMIN')")
    public ResponseEntity<List<LoanDocumentResponseDTO>> list(
            @PathVariable Long loanId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(documentService.list(loanId, principal.getUser()));
    }

    @GetMapping("/{documentId}/content")
    @PreAuthorize("hasAnyRole('EMPLOYEE','DDO','HOO','ADMIN')")
    public ResponseEntity<Resource> content(
            @PathVariable Long loanId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal CustomUserDetails principal) throws IOException {

        StoredLoanDocument stored = documentService.get(
                loanId, documentId, principal.getUser());
        Resource resource = new UrlResource(stored.path().toUri());
        String safeFilename = stored.metadata().getOriginalFilename();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.metadata().getContentType()))
                .contentLength(stored.metadata().getFileSize())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(safeFilename, StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }
}
