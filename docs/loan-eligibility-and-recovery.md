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

Each individual application must be at least ₹1,00,000 and cannot exceed the applicant's group ceiling. The group ceiling is also the maximum annual entitlement.

DDO and HOO demo accounts are assigned Groups B and A respectively. The applicant's group is stored on the `users.service_group` column.

## Financial-year calculation

The current Indian financial year is used: 1 April through 31 March. The server uses the Asia/Kolkata time zone.

Non-rejected applications in the current financial year reserve entitlement. Pending approval and revision-requested applications are included, so an applicant cannot bypass the limit by submitting multiple pending applications. Rejected applications release their reservation.

## 70% principal recovery rule

Repayments are recorded as principal-only amounts in the `loan_repayments` table. Only approved loans can receive repayment entries. A repayment cannot exceed the remaining principal.

Repayments do not replenish entitlement until at least 70% of the applicant's aggregate principal used in the current financial year has been recovered. Once the threshold is met, available entitlement is calculated from the group's cap minus outstanding principal. This allows a borrower who has repaid 70% to reapply for an amount that fits within the restored headroom.

For example, a Group A/B borrower uses ₹10,00,000 and recovers ₹7,00,000. Their outstanding principal is ₹3,00,000, so up to ₹7,00,000 of headroom is restored.

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

## Approval hierarchy and officer applicants

The mandatory workflow is exactly two steps:

1. DDO review.
2. HOO final approval.

Employees, DDOs and HOOs may apply. The applicant cannot take any workflow action on their own application. Therefore, a DDO applicant must be reviewed by a different DDO and then a HOO; an HOO applicant must be reviewed by a DDO and then a different HOO.

## Local-demo security note

The current local demo branch permits unauthenticated API access and accepts demo user IDs in request bodies. These checks support local workflow testing only; they are not production authorization. Before deployment, restore JWT authorization and derive the applicant, approver and repayment recorder identities from the authenticated principal rather than trusting client-supplied IDs.
