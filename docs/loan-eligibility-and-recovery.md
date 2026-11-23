# Loan eligibility and recovery policy

This policy is implemented by the backend and mirrored in the LoanDesk application form.

## Applicant eligibility

Employees, DDO officers and HOO officers may apply. The applicant's service group determines the amount ceiling:

| Service group | Maximum entitlement |
|---|---:|
| A | ₹10,00,000 |
| B | ₹10,00,000 |
| C | ₹7,00,000 |
| D | ₹5,00,000 |

Each individual application must be at least ₹1,00,000 and cannot exceed the applicant's group ceiling. The group ceiling is the maximum annual entitlement and the maximum allowed outstanding principal exposure.

DDO and HOO demo accounts are assigned Groups B and A respectively. The applicant's group is stored on the `users.service_group` column.

## Financial-year calculation

The current Indian financial year is used: 1 April through 31 March. The server uses the Asia/Kolkata time zone.

Non-rejected applications in the current financial year reserve annual entitlement. Pending approval and revision-requested applications are included, so an applicant cannot bypass the limit by submitting multiple pending applications. Rejected applications release their reservation.

## 70% principal recovery rule

Repayments are recorded as principal-only amounts in the `loan_repayments` table. Only approved loans can receive repayment entries. A repayment cannot exceed the remaining principal.

The 70% recovery test considers the applicant's aggregate principal across all non-rejected loans, including loans carried over from earlier financial years. Repayments reduce outstanding principal, but they do not replenish entitlement until at least 70% of aggregate principal already availed has been recovered.

Once the threshold is met, available entitlement is calculated from both:
- the current financial year's group entitlement less current-year outstanding principal; and
- the group ceiling less total outstanding principal across all loans.

The lower available amount is enforced. This prevents a new financial year from automatically erasing an unrecovered loan balance.

Example: a Group A/B borrower has used ₹10,00,000 and recovered ₹7,00,000. Their total outstanding principal is ₹3,00,000, so up to ₹7,00,000 of headroom is restored. If they borrow that ₹7,00,000, their total outstanding exposure returns to ₹10,00,000 and another application is blocked until further recovery meets the 70% test again.

The eligibility endpoint is:

`GET /api/loans/eligibility/{applicantId}`

## Repayment entry endpoints

A DDO, HOO or ADMIN account other than the borrower may record a principal repayment against an approved loan:

`POST /api/loans/{loanId}/repayments`

Request body:

```json
{
  "recordedById": 8,
  "amount": 700000,
  "remarks": "Salary recovery reference"
}
```

Repayment history:

`GET /api/loans/{loanId}/repayments`

## Borrower-selected recovery schedule (zero interest)

After a loan is approved, the borrower chooses the monthly principal recovery amount in the Recovery History tab. LoanDesk calculates the duration as the number of instalments required to cover the outstanding principal:

\`instalment count = ceiling(outstanding principal / borrower-selected monthly amount)\`

The first due date is the first day of the next calendar month (Asia/Kolkata); later instalments are due on the first day of each following month. The final instalment is reduced to the exact remaining principal when needed. No interest, processing charge or other amount is added. Total scheduled recovery equals the outstanding principal only.

The schedule is stored in \`loan_recovery_installments\`. Each row carries the scheduled amount, due date, amount paid, payment date and status. An instalment is marked PAID only when the full scheduled amount is covered; a partial payment is shown as PARTIALLY_PAID. Officers' recorded repayments are allocated oldest-due-first, including advance payments. A borrower can configure one schedule per approved loan.

Endpoints:

- \`POST /api/loans/{loanId}/recovery-schedule\` — borrower submits \`applicantId\` and \`monthlyAmount\`.
- \`GET /api/loans/recovery-schedules?viewerId={id}\` — borrowers see their own schedules; DDO/HOO/ADMIN see the department register.

## Approval hierarchy and officer applicants

The mandatory workflow is exactly two steps:

1. DDO review.
2. HOO final approval.

Employees, DDOs and HOOs may apply. The applicant cannot take any workflow action on their own application. Therefore, a DDO applicant must be reviewed by a different DDO and then a HOO; an HOO applicant must be reviewed by a DDO and then a different HOO.

## Security profiles

The `local-demo` profile uses the development-only `X-Demo-User-Id` identity switcher so the portfolio UI can be explored without individual login. This header is not authentication and must never be exposed as a production access mechanism.

Non-demo profiles require JWT bearer authentication. Loan applicant, approver and repayment recorder identities are derived from the authenticated principal rather than trusted from request-supplied actor IDs. Production deployments still require account provisioning, strong environment-managed secrets, HTTPS, database migrations, monitoring and a security review.
