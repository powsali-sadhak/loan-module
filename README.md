# LoanDesk

**LoanDesk is a portfolio project demonstrating a role-based employee loan workflow** built with Java, Spring Boot, Spring Security, React and MySQL. It covers the application lifecycle from eligibility checks and multi-step review to repayment tracking, supporting documents, service tickets and notifications.

> This is an independent learning and portfolio project. It is not an official government, employer or financial-institution product. All demo identities and records are synthetic. Do not use it to process real financial or personal data.

[![Maven tests](https://github.com/powsali-sadhak/loan-module/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/powsali-sadhak/loan-module/actions/workflows/maven.yml)
[![Application build](https://github.com/powsali-sadhak/loan-module/actions/workflows/loandesk-build.yml/badge.svg?branch=main)](https://github.com/powsali-sadhak/loan-module/actions/workflows/loandesk-build.yml)

## Project highlights

- **Loan eligibility:** service-group limits, financial-year calculations and a configurable 70% principal-recovery eligibility rule.
- **Approval workflow:** DDO review followed by HOO final approval, with forwarding, approval, rejection and revision requests.
- **Repayment management:** principal-only repayment records, borrower-selected monthly schedules and oldest-due-first allocation.
- **Document management:** private server-side storage, metadata, SHA-256 checksums, access checks and PDF/JPEG/PNG validation.
- **Support management:** employee tickets, staff assignment, status transitions, public comments and internal notes.
- **Notifications:** persistent in-app inbox with unread counts and event-based routing; optional experimental WhatsApp adapter for local testing.
- **Role-aware React UI:** employee workspace, approval desk, recovery history, support centre and workflow administration.

## Technology stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5, Spring Web, Spring Data JPA |
| Security | Spring Security, JWT, BCrypt |
| Database | MySQL 8+ |
| Frontend | React 18, Vite 6, React Router, Axios |
| UI and charts | Lucide React, Recharts |
| Build and CI | Maven, npm, GitHub Actions |
| Optional messaging prototype | Self-hosted Evolution API (Baileys / WhatsApp Web) |

## Roles and workflow

| Role | Main responsibilities |
|---|---|
| `EMPLOYEE` | Apply for loans, review personal applications, attach documents, select a recovery schedule and raise support tickets |
| `DDO` | Apply for a loan and review applications assigned to the DDO step |
| `HOO` | Apply for a loan and take final approval action when assigned |
| `SUPPORT` | Work the support queue, respond to tickets and manage ticket status within permissions |
| `ADMIN` | Configure approval workflows, assign support tickets and access administrative operations |

The current loan application policy requires **DDO review followed by HOO final approval**. An applicant cannot approve their own loan; officer applicants must be reviewed by another eligible officer.

## Demonstration eligibility rules

The project includes a sample policy to demonstrate financial eligibility logic:

| Service group | Annual entitlement ceiling |
|---|---:|
| A / B | ₹10,00,000 |
| C | ₹7,00,000 |
| D | ₹5,00,000 |

Each application has a ₹1,00,000 minimum. The financial year runs from 1 April to 31 March using the Asia/Kolkata time zone. A 70% aggregate principal-recovery test is applied before previously used entitlement can replenish. These are project demonstration rules, not a statement of any institution's current policy.

The detailed calculations and examples are in [docs/loan-eligibility-and-recovery.md](docs/loan-eligibility-and-recovery.md).

## Architecture

```text
React + Vite
    |
    | REST / JSON, multipart document uploads
    v
Spring Boot REST API
    |
    +-- Spring Security / JWT and role checks
    +-- Loan eligibility and approval workflow
    +-- Repayment and recovery scheduling
    +-- Support tickets and notifications
    +-- Document metadata ------------------+
    |                                        |
    v                                        v
MySQL database                    Private server-side file storage
                                             |
                                  Optional Evolution API adapter
```

## Run locally

### Prerequisites

- JDK 17
- MySQL 8.0 or later
- Node.js 20 or later and npm
- Git

### 1. Clone the repository

```powershell
git clone https://github.com/powsali-sadhak/loan-module.git
cd loan-module
```

### 2. Create a local database and user

Run these statements in MySQL, choosing a password of your own:

```sql
CREATE DATABASE loan_module;
CREATE USER 'loan_app'@'localhost' IDENTIFIED BY 'CHOOSE_A_LOCAL_PASSWORD';
GRANT ALL PRIVILEGES ON loan_module.* TO 'loan_app'@'localhost';
FLUSH PRIVILEGES;
```

Do not reuse a password from another project or commit it to the repository.

### 3. Configure and start the backend

Set the required environment variables in the same PowerShell session or in your IDE run configuration. Generate a fresh local JWT signing secret:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/loan_module"
$env:DB_USERNAME = "loan_app"
$env:DB_PASSWORD = "YOUR_LOCAL_MYSQL_PASSWORD"

$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$secretBytes = New-Object byte[] 48
$rng.GetBytes($secretBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($secretBytes)
$rng.Dispose()

$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
$env:LOAN_DOCUMENT_STORAGE = "./uploads/loan-documents"
$env:WHATSAPP_ENABLED = "false"
```

Start the backend from the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

The default profile is `local-demo`. It seeds synthetic demo accounts and enables the demo identity switcher. A locally configured `DEMO_PASSWORD` can be supplied when password-based demo login is needed; otherwise the seeder generates a random password that is not printed or committed.

### 4. Start the frontend

Open a second terminal:

```powershell
cd frontend
Copy-Item .env.example .env
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). The frontend API URL can be changed through `VITE_API_BASE_URL` in `frontend/.env`.

### Demo-mode security warning

The `local-demo` profile uses the `X-Demo-User-Id` header to switch between synthetic demo identities. This is a local development convenience, **not real authentication**. Do not expose the local-demo profile to the internet or use it for real users or real data.

For a non-demo deployment, set `SPRING_PROFILES_ACTIVE=prod`, provide a strong `JWT_SECRET` and `CORS_ALLOWED_ORIGINS`, and provision real user accounts securely. The production profile does not use the demo identity header or seed demo accounts.

## Supporting documents

The application supports up to 10 PDF, JPG, JPEG or PNG files per loan, with a default 10 MB per-file limit. The backend validates file signatures, stores generated filenames outside the public frontend directory and saves metadata plus SHA-256 checksums in MySQL.

See [docs/loan-documents.md](docs/loan-documents.md) for access rules, configuration and backup considerations. Back up both the database and the private file-storage directory.

## Notifications and WhatsApp prototype

The in-app notification inbox is the primary notification channel. The optional WhatsApp adapter can call a self-hosted Evolution API instance when enabled.

The Evolution API integration uses an unofficial WhatsApp Web/Baileys connection. It is provided only as an experimental personal-project integration, can stop working or risk account restrictions, and must not be used for unsolicited or production financial messages. It is disabled by default. See [docs/whatsapp-notifications.md](docs/whatsapp-notifications.md).

## Tests and builds

Run backend tests:

```powershell
.\mvnw.cmd test
```

Build the backend:

```powershell
.\mvnw.cmd -DskipTests package
```

Build the frontend:

```powershell
cd frontend
npm install
npm run build
```

GitHub Actions runs the Maven test suite and the Spring Boot / React build workflows.

## Repository layout

```text
src/main/java/          Spring Boot controllers, services, entities and security
src/main/resources/     Environment-driven application configuration
src/test/java/          Backend and security tests
frontend/src/           React application and UI
docs/                   Eligibility, recovery, document and notification notes
.github/workflows/      CI workflows
```

## Known limitations

- This is a portfolio prototype, not a production financial system.
- Local-demo identity switching is not suitable for internet-facing deployment.
- Production user provisioning, password reset, MFA, operational monitoring and deployment hardening are not supplied as a complete package.
- Document files use local/private filesystem storage. Multi-instance deployments need shared durable private storage or an object-storage adapter.
- Production deployments should add malware scanning, encrypted backups, retention controls, database migrations and a formal security review.
- The WhatsApp adapter is experimental and has not been represented as a guaranteed delivery channel.
- No open-source license has been selected for this repository yet.

## Contact

For project questions or collaboration, use the GitHub repository's Issues or Discussions features.
