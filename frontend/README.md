# LoanDesk frontend

React + Vite UI for the Spring Boot employee loan management API.

## Run locally
1. Start the Spring Boot API on http://localhost:8080.
2. Copy `.env.example` to `.env` and adjust the API URL.
3. Run `npm install`, then `npm run dev`.
4. Open http://localhost:5173.

## Demo mode
The local demo identity selector is enabled by default. It sends actor IDs in request bodies for loan actions and application submissions, matching the local test-only backend variant. This is NOT authentication. Do not expose demo mode to a public network.

When using the committed JWT backend, set `VITE_DEMO_MODE=false` and use the login screen. Ticket and workflow endpoints rely on authenticated principals and role checks.

## Screens
- Overview dashboard
- Apply for a loan and view personal loans
- Approval queue with forward / approve / reject / request revision
- Loan details and action timeline
- Support ticket creation, tracking and staff queue
- Admin workflow designer (ordered role-based steps)

The current backend does not expose a general-purpose list-all-loans endpoint. The approval screen therefore uses a small set of loan IDs entered by staff or records opened from the dashboard, rather than pretending there is an endpoint that does not exist.