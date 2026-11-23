# WhatsApp notifications (personal-project setup)

LoanDesk can optionally send the same event notifications shown in the in-app inbox to a user's WhatsApp number. The integration is disabled by default and uses a provider adapter so the project can move to another provider later.

## Open-source provider

This prototype uses [Evolution API](https://github.com/evolution-foundation/evolution-api), self-hosted with its Baileys / WhatsApp Web connection. It exposes an HTTP API that the Spring Boot application calls.

**Important:** Evolution API's Baileys connection is an unofficial integration with WhatsApp Web. It is not affiliated with or endorsed by WhatsApp, and it may stop working or cause the linked WhatsApp account to be restricted. Use only for personal development and messages to numbers whose owners have explicitly opted in. Do not use it for bulk, unsolicited, sensitive, or production notifications. For a production deployment, replace this adapter with Meta's official WhatsApp Cloud API.

## 1. Run Evolution API locally

Install Docker Desktop, then follow the current Docker installation instructions in the [Evolution API repository](https://github.com/evolution-foundation/evolution-api) or its [Docker installation guide](https://github.com/evolution-foundation/evolution-docs/blob/main/docs/01-Get%20Started/Installation/Docker.md).

Expose Evolution API on host port **8085** (the LoanDesk Spring Boot API uses 8080). Configure a strong API key and persistent volumes for its instance/session data. Do not expose the API publicly without HTTPS and access controls.

Open the Evolution API manager or Swagger page shown by your installation. Create a WhatsApp instance named `loandesk` using the Baileys integration, request its QR code, and scan it from WhatsApp on a phone you control:

**WhatsApp → Linked devices → Link a device**

Wait until the instance reports a connected/open state before testing delivery. Keep Evolution API's session storage persistent so the linked session survives container restarts.

## 2. Configure the LoanDesk backend

Set these environment variables before starting Spring Boot:

```powershell
$env:WHATSAPP_ENABLED = "true"
$env:WHATSAPP_BASE_URL = "http://localhost:8085"
$env:WHATSAPP_INSTANCE = "loandesk"
$env:WHATSAPP_API_KEY = "replace-with-your-evolution-api-key"
```

Then start the Spring Boot application as usual. The API key is read only by the backend; do not put it in React environment variables or commit it to Git.

To disable WhatsApp delivery, set `WHATSAPP_ENABLED=false` or omit the variables. In-app notifications continue to work either way.

## 3. Add a recipient number and opt in

In the local demo UI, select a demo identity and open the WhatsApp icon in the top bar. Enter a number in international format, for example `+91XXXXXXXXXX`, and tick the consent checkbox.

The settings are stored per demo user in MySQL. A ten-digit Indian number is also accepted and stored with the +91 country code. WhatsApp delivery is skipped when the user has not opted in, has no valid number, or WhatsApp integration is disabled.

The settings endpoint is local-demo-only:

- `PATCH /api/demo/users/{userId}/whatsapp-settings`
- Requires the matching `X-Demo-User-Id` header.
- Request: `{"mobileNumber":"+91XXXXXXXXXX","whatsappNotificationsEnabled":true}`

Do not expose this demo endpoint in a production profile. A production release needs authenticated profile management, number ownership verification, an auditable consent record, and opt-out controls.

## 4. Message delivery behaviour

LoanDesk sends a short notification title and message, followed by a prompt to sign in for details. The existing recipient rules remain in `NotificationService`: applicant / DDO / HOO loan alerts, and employee-requester support alerts only. Internal support notes are not sent.

The HTTP call is best-effort: a WhatsApp outage or bad API key is logged but does not roll back loan or support operations. A successful API response means the provider accepted the send request; it is not proof that the recipient's phone displayed or read the message.

## 5. Manual provider smoke test

Use the Evolution API Swagger UI or send a request to its send-text endpoint. For current Evolution API versions, the endpoint is:

`POST /message/sendText/loandesk`

with the `apikey` header and a body containing the recipient number (digits only, including country code) and text. Refer to the documentation shipped with the exact Evolution API version you installed if its request schema differs.

