# LoanDesk support staff workflow

## Local demo accounts

The local-only demo initializer seeds synthetic support identities `support1` and `support2`, plus an administrator account. Use the in-app demo identity switcher to explore the local workflow.

No shared demo password is committed. Set `DEMO_PASSWORD` locally if password-based demo login is needed; otherwise the initializer generates a random password that is not displayed.

## Staff dashboard

Open **Operations → Support staff desk** (route `/support/desk`).

- SUPPORT users see unassigned tickets and tickets assigned to themselves.
- ADMIN users see the full ticket register and can assign or reassign an open ticket to either SUPPORT account.
- A SUPPORT user can pick up an unassigned ticket by selecting **Start working**. The server assigns that ticket to the current support user when the status moves to `IN_PROGRESS`.
- Support staff can post public replies or internal notes.
- An internal note is hidden from non-staff users.
- A support officer must enter a resolution summary before moving a ticket to `RESOLVED`.
- The requester can subsequently close a resolved ticket from the existing status endpoint.

## Suggested local test

1. Switch to `employee1` and raise a support ticket in **Support centre**.
2. Switch to `admin`, open **Support staff desk**, select the new ticket and assign it to `support1`.
3. Switch to `support1`; the ticket should appear in that support officer's queue.
4. Select it, start working, post a reply, and optionally add an internal note.
5. Add a resolution summary and mark the ticket resolved.
6. Switch back to `employee1`; confirm the requester can see public replies but not internal notes, then close the resolved ticket.

## Local-demo security

The `local-demo` profile uses the development-only `X-Demo-User-Id` header to resolve the selected synthetic demo identity. This header is not authentication and must never be exposed as a production access mechanism. Non-demo profiles require JWT bearer authentication, and support actions use the authenticated principal. Demo account seeding and demo-user endpoints are disabled in production.
