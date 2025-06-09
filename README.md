# Loan Module (Spring Boot + MySQL)

A role-based Loan Application Module designed for the West Bengal Government employee system using Spring Boot and MySQL. The system supports three roles — **Employee**, **DDO**, and **HOO** — following a defined approval workflow.

---

## 📌 Features

- Apply for loans (by Employee or DDO)
- Forward applications
- Approve, Reject, or Send back for revision
- View loan history and actions
- Role-based actions with access segregation

---

## 🛠 Tech Stack

- **Backend**: Java 17, Spring Boot, Spring Data JPA
- **Database**: MySQL
- **Tooling**: Maven, Postman
- **Version Control**: Git + GitHub

---

## 👥 Role Hierarchy
Employee
│
▼
DDO (Draw and Disbursing Officer)
│
▼
HOO (Head of Office)


- **Employee**: Applies for loans
- **DDO**: Can apply on behalf of employees, forward to HOO
- **HOO**: Can approve/reject/send back for revision

---

## 📑 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/loans/apply` | Apply for a loan |
| `POST` | `/api/loans/action` | Take action on a loan (Forward, Approve, Reject, Revise) |
| `GET`  | `/api/loans/{userId}` | View all loan applications related to a user |

### Sample: Apply for Loan

```json
POST /api/loans/apply
{
  "applicantId": 1,
  "appliedBy": 1,
  "amount": 50000,
  "purpose": "Home Renovation"
}

