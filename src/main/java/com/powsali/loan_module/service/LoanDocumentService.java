package com.powsali.loan_module.service;

import com.powsali.loan_module.dto.LoanDocumentResponseDTO;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.LoanDocument;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.Role;
import com.powsali.loan_module.repository.LoanDocumentRepository;
import com.powsali.loan_module.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LoanDocumentService {

    private static final long DEFAULT_MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final long MAX_FILENAME_LENGTH = 255;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".jpg", ".jpeg", ".png");

    private final LoanRepository loans;
    private final LoanDocumentRepository documents;
    private final LoanDocumentStorageService storage;

    @Value("${app.documents.max-file-size-bytes:10485760}")
    private long maxFileSize;

    @Value("${app.documents.max-files-per-loan:10}")
    private long maxFilesPerLoan;

    @Transactional
    public LoanDocumentResponseDTO upload(Long loanId, MultipartFile file, User actor) {
        LoanApplication loan = findLoan(loanId);
        requireDocumentAccess(loan, actor);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a document to upload");
        }
        if (file.getSize() > effectiveMaxFileSize()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "A document cannot exceed " + (effectiveMaxFileSize() / (1024 * 1024)) + " MB");
        }
        if (documents.countByLoanApplication_Id(loanId) >= maxFilesPerLoan) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A maximum of " + maxFilesPerLoan + " documents is allowed per loan application");
        }

        String originalName = safeOriginalFilename(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only PDF, JPG, JPEG and PNG documents are supported");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read the uploaded document", exception);
        }
        String detectedContentType = detectContentType(content);
        if (detectedContentType == null || !extensionMatches(extension, detectedContentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "The file content does not match its file extension or is not a supported document");
        }
        String suppliedContentType = file.getContentType();
        if (suppliedContentType != null
                && !suppliedContentType.isBlank()
                && !"application/octet-stream".equalsIgnoreCase(suppliedContentType)
                && !detectedContentType.equalsIgnoreCase(suppliedContentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "The uploaded document has an unexpected content type");
        }

        String storedFilename = null;
        try {
            storedFilename = storage.store(content, extension);
            LoanDocument saved = documents.saveAndFlush(LoanDocument.builder()
                    .loanApplication(loan)
                    .uploadedBy(actor)
                    .originalFilename(originalName)
                    .storedFilename(storedFilename)
                    .contentType(detectedContentType)
                    .fileSize(content.length)
                    .sha256(sha256(content))
                    .build());
            return toResponse(saved);
        } catch (IOException exception) {
            if (storedFilename != null) storage.deleteQuietly(storedFilename);
            throw new IllegalStateException("Could not store the uploaded document", exception);
        } catch (RuntimeException exception) {
            if (storedFilename != null) storage.deleteQuietly(storedFilename);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<LoanDocumentResponseDTO> list(Long loanId, User viewer) {
        LoanApplication loan = findLoan(loanId);
        requireDocumentAccess(loan, viewer);
        return documents.findByLoanApplication_IdOrderByUploadedAtDesc(loanId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StoredLoanDocument get(Long loanId, Long documentId, User viewer) {
        LoanApplication loan = findLoan(loanId);
        requireDocumentAccess(loan, viewer);
        LoanDocument document = documents.findByIdAndLoanApplication_Id(documentId, loanId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Loan document not found"));

        Path file = storage.resolve(document.getStoredFilename());
        if (!Files.isRegularFile(file)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "The stored document is missing");
        }
        return new StoredLoanDocument(document, file);
    }

    private LoanApplication findLoan(Long loanId) {
        return loans.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Loan application not found"));
    }

    private void requireDocumentAccess(LoanApplication loan, User viewer) {
        if (viewer == null) {
            throw new AccessDeniedException("An authenticated user is required");
        }
        boolean applicant = loan.getApplicant() != null
                && loan.getApplicant().getId().equals(viewer.getId());
        boolean submitter = loan.getAppliedBy() != null
                && loan.getAppliedBy().getId().equals(viewer.getId());
        boolean administrator = viewer.getRole() == Role.ADMIN;
        boolean assignedApprover = (viewer.getRole() == Role.DDO || viewer.getRole() == Role.HOO)
                && loan.getStatus() != null
                && loan.getStatus().name().equals("PENDING_APPROVAL")
                && loan.getCurrentApproverRole() == viewer.getRole();

        if (!applicant && !submitter && !administrator && !assignedApprover) {
            throw new AccessDeniedException(
                    "You are not allowed to access documents for this loan application");
        }
    }

    private String safeOriginalFilename(String supplied) {
        if (supplied == null || supplied.isBlank()) {
            throw new IllegalArgumentException("The uploaded document must have a filename");
        }
        String normalized = supplied.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_").trim();
        if (name.isBlank() || name.length() > MAX_FILENAME_LENGTH) {
            throw new IllegalArgumentException("Filename must be between 1 and 255 characters");
        }
        return name;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private String detectContentType(byte[] bytes) {
        if (bytes.length >= 5
                && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D'
                && bytes[3] == 'F' && bytes[4] == '-') {
            return "application/pdf";
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        byte[] pngSignature = new byte[]{
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (bytes.length >= pngSignature.length) {
            for (int i = 0; i < pngSignature.length; i++) {
                if (bytes[i] != pngSignature[i]) return null;
            }
            return "image/png";
        }
        return null;
    }

    private boolean extensionMatches(String extension, String contentType) {
        return switch (contentType) {
            case "application/pdf" -> ".pdf".equals(extension);
            case "image/jpeg" -> ".jpg".equals(extension) || ".jpeg".equals(extension);
            case "image/png" -> ".png".equals(extension);
            default -> false;
        };
    }

    private long effectiveMaxFileSize() {
        return maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_FILE_SIZE;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private LoanDocumentResponseDTO toResponse(LoanDocument document) {
        return new LoanDocumentResponseDTO(
                document.getId(),
                document.getLoanApplication().getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getFileSize(),
                document.getSha256(),
                document.getUploadedBy().getName(),
                document.getUploadedAt());
    }

    public record StoredLoanDocument(LoanDocument metadata, Path path) {
    }
}
