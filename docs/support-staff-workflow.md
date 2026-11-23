# LoanDesk support staff workflow

## Local demo accounts

The non-production demo initializer seeds these support operators:

| Username | Name | Role | Password |
|---|---|---|---|
| support1 | Priya Sharma | SUPPORT | `Demo@12345` |
| support2 | Rahul Sen | SUPPORT | `Demo@12345` |
| admin | System Administrator | ADMIN | `Demo@12345` |

These credentials are for the local demo only. Do not use them in a deployed environment.

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

The current demo uses the selected user ID header to resolve the acting database user because JWT enforcement is intentionally disabled in the local branch. This is not authentication. Before deployment, restore JWT-backed authorization and derive the actor from the authenticated principal. Keep demo accounts and the demo-user directory disabled in production.
