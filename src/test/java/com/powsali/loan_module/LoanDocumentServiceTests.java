package com.powsali.loan_module;

import com.powsali.loan_module.dto.LoanDocumentResponseDTO;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.LoanDocument;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.LoanStatus;
import com.powsali.loan_module.entity.enums.Role;
import com.powsali.loan_module.repository.LoanDocumentRepository;
import com.powsali.loan_module.repository.LoanRepository;
import com.powsali.loan_module.service.LoanDocumentService;
import com.powsali.loan_module.service.LoanDocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoanDocumentServiceTests {

    @TempDir
    Path storageDirectory;

    private LoanRepository loans;
    private LoanDocumentRepository documents;
    private LoanDocumentService service;
    private User applicant;
    private LoanApplication loan;

    @BeforeEach
    void setUp() {
        loans = mock(LoanRepository.class);
        documents = mock(LoanDocumentRepository.class);
        LoanDocumentStorageService storage =
                new LoanDocumentStorageService(storageDirectory.toString());
        service = new LoanDocumentService(loans, documents, storage);
        ReflectionTestUtils.setField(service, "maxFileSize", 10L * 1024 * 1024);
        ReflectionTestUtils.setField(service, "maxFilesPerLoan", 10L);

        applicant = new User();
        applicant.setId(101L);
        applicant.setName("Test Applicant");
        applicant.setRole(Role.EMPLOYEE);

        loan = new LoanApplication();
        loan.setId(42L);
        loan.setApplicant(applicant);
        loan.setStatus(LoanStatus.PENDING_APPROVAL);
        when(loans.findById(42L)).thenReturn(Optional.of(loan));
        when(documents.countByLoanApplication_Id(42L)).thenReturn(0L);
    }

    @Test
    void rejectsPdfExtensionWhenContentIsNotPdf() {
        MockMultipartFile upload = new MockMultipartFile(
                "document", "supporting.pdf", "application/pdf",
                "this is not a PDF".getBytes());

        assertThrows(ResponseStatusException.class,
                () -> service.upload(42L, upload, applicant));
        verify(documents, never()).saveAndFlush(any(LoanDocument.class));
        assertEquals(0, fileCount());
    }

    @Test
    void storesValidPdfOutsideThePublicFrontendDirectory() throws Exception {
        byte[] pdf = "%PDF-1.7\nLoanDesk test document".getBytes();
        MockMultipartFile upload = new MockMultipartFile(
                "document", "supporting.pdf", "application/pdf", pdf);

        when(documents.saveAndFlush(any(LoanDocument.class))).thenAnswer(invocation -> {
            LoanDocument document = invocation.getArgument(0);
            document.setId(7L);
            document.setUploadedAt(LocalDateTime.now());
            return document;
        });

        LoanDocumentResponseDTO response = service.upload(42L, upload, applicant);

        assertEquals(7L, response.documentId());
        assertEquals("supporting.pdf", response.fileName());
        assertEquals("application/pdf", response.contentType());
        assertEquals(pdf.length, response.fileSize());
        assertEquals(1, fileCount());
        try (var paths = Files.list(storageDirectory)) {
            assertTrue(paths.findFirst().orElseThrow().getFileName().toString()
                    .matches("[0-9a-fA-F-]{36}\\.pdf"));
        }
    }

    @Test
    void preventsUnrelatedEmployeeFromReadingLoanDocuments() {
        User unrelated = new User();
        unrelated.setId(202L);
        unrelated.setName("Other Employee");
        unrelated.setRole(Role.EMPLOYEE);

        assertThrows(AccessDeniedException.class,
                () -> service.list(42L, unrelated));
        verify(documents, never()).findByLoanApplication_IdOrderByUploadedAtDesc(42L);
    }

    @Test
    void enforcesMaximumDocumentsPerLoan() {
        when(documents.countByLoanApplication_Id(42L)).thenReturn(10L);
        MockMultipartFile upload = new MockMultipartFile(
                "document", "supporting.pdf", "application/pdf",
                "%PDF-1.7".getBytes());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.upload(42L, upload, applicant));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(documents, never()).saveAndFlush(any(LoanDocument.class));
    }

    private long fileCount() throws Exception {
        try (var files = Files.list(storageDirectory)) {
            return files.count();
        }
    }
}
