# LoanDesk frontend

React 18 + Vite frontend for the LoanDesk Spring Boot API.

## Local development

1. Start the backend and MySQL database using the root [README](../README.md).
2. Copy `.env.example` to `.env`.
3. Set `VITE_API_BASE_URL` if the backend is not running at `http://localhost:8080`.
4. Run:

```powershell
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

## Demo mode and identity handling

The root application defaults to `local-demo` and the frontend demo switcher uses the backend's development-only `X-Demo-User-Id` identity header. This is not authentication and must only be used for local portfolio exploration. Do not expose the local-demo profile to a public network.

For non-demo deployments, set `SPRING_PROFILES_ACTIVE=prod`, use a strong environment-managed `JWT_SECRET`, provision real users securely and configure the frontend to use the authenticated login flow. Never place database credentials, JWT signing keys or messaging API keys in `VITE_*` variables.

## Main screens

- Role-aware overview dashboard
- Loan application and eligibility view
- Personal application history and detail timeline
- DDO / HOO approval desk
- Repayment and recovery history
- Supporting document upload, preview and download
- Employee support centre and staff ticket desk
- Admin approval-workflow designer
- In-app notification inbox and optional local WhatsApp settings

## Build

```powershell
npm run build
```
