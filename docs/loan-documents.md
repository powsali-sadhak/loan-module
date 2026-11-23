# Secure loan document management

LoanDesk stores supporting documents on the backend file system and stores their metadata in MySQL. Files are not placed in the React public directory and are never served by a static web server.

## Supported files and limits

- PDF (`.pdf`)
- JPEG (`.jpg`, `.jpeg`)
- PNG (`.png`)
- Maximum size: 10 MB per file by default
- Maximum count: 10 files per loan application by default

The backend checks the extension, the file's binary signature and the supplied MIME type. It creates an unpredictable server-generated storage filename and calculates a SHA-256 checksum. The original filename is kept only as display metadata.

## API endpoints

All endpoints require an authenticated user.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/loans/{loanId}/documents` | Upload one multipart file using form field `document` |
| `GET` | `/api/loans/{loanId}/documents` | List metadata for the loan's documents |
| `GET` | `/api/loans/{loanId}/documents/{documentId}/content` | Preview the document inline |

The applicant, the user who submitted the application, the administrator, and the DDO/HOO currently responsible for a pending approval step can view documents. The same users can upload files. Access is checked against the authenticated principal and loan record; IDs supplied by the browser do not establish identity.

## Local configuration

By default, documents are stored at:

`./uploads/loan-documents`

Configure another location and limits with environment variables:

```powershell
$env:LOAN_DOCUMENT_STORAGE = "D:\LoanDeskData\loan-documents"
$env:LOAN_DOCUMENT_MAX_FILE_SIZE = "10485760"
$env:LOAN_DOCUMENT_MAX_FILES = "10"
$env:SPRING_MULTIPART_MAX_FILE_SIZE = "10MB"
$env:SPRING_MULTIPART_MAX_REQUEST_SIZE = "11MB"
```

Restart Spring Boot after changing these values. The `/uploads/` directory is ignored by Git.

## Data and deployment notes

- Hibernate schema update creates the `loan_documents` metadata table in the local development database.
- Back up both MySQL and the document storage directory. Restoring only the database will not restore file contents.
- In multi-instance deployments, all backend instances must share durable storage or the storage implementation must be replaced by a private object-storage adapter.
- Do not expose the storage directory through Nginx, Apache or the React static directory.
- The current validation checks file signatures and MIME metadata. A production deployment should also add malware scanning, encrypted storage/backups, retention rules and audited deletion.
- Production deployments should use Flyway/Liquibase migrations rather than relying on Hibernate schema update.

## UI

The application detail screen and approval desk show a Supporting documents panel. Eligible users can upload a file and preview or download any attached document. The interface displays uploader, file size and upload time.
