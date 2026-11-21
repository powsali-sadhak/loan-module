# Employee Loan Management API

A role-based employee loan application backend built with Java 17, Spring Boot 3.5, Spring Data JPA, Spring Security, JWT and MySQL.

## Current capabilities

- Employee and DDO loan application submission
- Configurable, ordered approval workflows managed by an administrator
- Role-checked forward, approve, reject and revision actions
- Loan action history
- JWT-authenticated API access
- Authenticated user identity is taken from the JWT principal for application and approval actions

## Roles

- `EMPLOYEE`: submits and views their own loan applications
- `DDO`, `HOO`: can act when their role is the current configured approval step
- `ADMIN`: manages approval workflow definitions

## Configurable approval workflow

An administrator creates a workflow with one or more ordered approval steps. Each step has a role and display label. Only one workflow can be active at a time. New loan applications are attached to the active workflow and start at its first step.

Example workflow configuration:

```json
{
  "name": "Standard Employee Loan",
  "description": "Standard multi-level loan approval",
  "active": true,
  "steps": [
    { "stepOrder": 1, "approverRole": "DDO", "label": "DDO Verification" },
    { "stepOrder": 2, "approverRole": "HOO", "label": "HOO Approval" }
  ]
}
```

### Workflow administration endpoints

All workflow administration endpoints require an authenticated user with the `ADMIN` role.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/admin/workflows` | List configured workflows |
| `POST` | `/api/admin/workflows` | Create a workflow |
| `PUT` | `/api/admin/workflows/{workflowId}` | Update a workflow |
| `POST` | `/api/admin/workflows/{workflowId}/activate` | Activate a workflow |

Approval step order must be sequential starting at 1. `EMPLOYEE` and `ADMIN` are not valid approver roles.

## Loan endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/loans/apply` | Submit a loan application |
| `POST` | `/api/loans/action` | Forward, approve, reject or request revision |
| `GET` | `/api/loans/{loanId}` | View an application |
| `GET` | `/api/loans/user/{userId}` | View applications for an employee |
| `GET` | `/api/loans/{loanId}/actions` | View movement/action history |

### Submit a loan

`POST /api/loans/apply`

```json
{
  "applicantId": 42,
  "amount": 50000,
  "purpose": "Home renovation"
}
```

The authenticated JWT identity supplies `appliedBy`; clients must not send an actor ID.

### Take an approval action

`POST /api/loans/action`

```json
{
  "loanId": 1001,
  "actionType": "FORWARDED",
  "remarks": "Verified the supporting documents"
}
```

Supported action types: `FORWARDED`, `APPROVED`, `REJECTED`, `REVISION_REQUESTED`.

The authenticated user's role must match the current workflow step. `FORWARDED` moves the application to the next step. `APPROVED` is only permitted at the final step. Rejected and approved applications are terminal.

## Database and configuration

The application currently uses Hibernate schema update for local development. Configure database credentials through environment variables before deployment; never commit real credentials.

Recommended environment configuration:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/loan_module}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

## Important next steps before production

- Move all secrets to environment variables / a secret manager and rotate any credentials previously committed.
- Add database migrations (Flyway or Liquibase) instead of relying on `ddl-auto=update`.
- Add integration tests for workflow configuration, role authorization and every transition.
- Add a revision resubmission path and immutable workflow versioning for already-submitted applications.
- Add issue/ticket management, attachment storage, notifications, monitoring and deployment configuration.
- Run the Maven test suite and security review before production deployment.
