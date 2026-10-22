# API reference

Every endpoint except the public ones needs a signed token:

Tokens last 15 minutes and contain only the user's ID and role. Grants are read from the database on every request, so access changes take effect at once.

"Who may call" is the rule the server enforces. The screens hide controls a role cannot use, but that is cosmetic only.

## Public (no sign-in)

| Method | Path | Purpose | Responses |
|--------|------|---------|-----------|
| POST | `/api/auth/login` | Exchange email and password for a token | 200 with `token`, `email`, `role`. 401 for an unknown email, a wrong password or an inactive account (same message). |
| POST | `/api/auth/signup` | Create an account. Always an OWNER; any role sent is ignored. | 201. 400 for an invalid email or a password outside 12 to 128 characters. 409 if the email is registered. |
| POST | `/api/auth/reset/request` | Ask for a reset link. Same reply for any email. | 200 with a generic message. |
| POST | `/api/auth/reset/confirm` | Set a new password with a one-time token | 204. 400 for an invalid, used or expired token. |
| POST | `/api/assistant/chat` | Ask the assistant a question. Holds no secrets and changes no data. | 200 with `answer` and `links`. |

## Account (any signed-in user)

| Method | Path | Purpose | Responses |
|--------|------|---------|-----------|
| GET | `/api/auth/me` | The caller's ID and role | 200 |
| GET | `/api/account/me` | Read the caller's profile | 200 with `id`, `email`, `role`, `display_name`, `phone` |
| PUT | `/api/account/me` | Update display name and phone. Email and role cannot be changed. | 200. 400 for an invalid phone. |
| POST | `/api/account/password` | Change the password. Needs the current password. | 204. 400 if the current password is wrong or the new one is too short. |

## Documents

| Method | Path | Who may call | Purpose | Responses |
|--------|------|--------------|---------|-----------|
| GET | `/api/documents` | Any signed-in user | Documents the caller owns or holds an active grant for | 200. Expired or used-up grants are not listed. |
| POST | `/api/documents` | Owner | Upload a file (`file`, optional `category`) | 201 with `id`, `name`, `contentType`, `privacyNote`, `privacyWarnings`. 403 for non-owners. 413 over 10 MB. 415 for a type not allowed. |
| GET | `/api/documents/{id}/download` | Owner, or an active grant | Download the file. Counts against a download limit. | 200. 404 for missing, deleted, forbidden, exhausted or integrity-failed documents (same body). |
| GET | `/api/documents/{id}/verify` | Owner, or an active grant | File DNA: check the stored file against its upload fingerprint. Does not count as a download. | 200 with `intact` and `result`. 404 if forbidden. |
| GET | `/api/documents/{id}/trail` | Owner or administrator | Trust Trail: the document's events and whether the record is intact | 200 with `events` and `chainIntact`. 404 for anyone else. |
| DELETE | `/api/documents/{id}` | Owner | Soft-delete the document | 204. 404 for non-owners. |

## Sharing and capsule

| Method | Path | Who may call | Purpose | Responses |
|--------|------|--------------|---------|-----------|
| GET | `/api/shares` | Any signed-in user | Documents other owners have shared with the caller | 200 |
| GET | `/api/documents/{id}/grants` | Owner | Who has access, with capsule conditions | 200 |
| POST | `/api/documents/{id}/grants` | Owner | Share with an active user as READ or EDIT. The recipient is emailed a notice that names no file. Body: `email`, `permission`. | 201. 400 for an unknown email or a bad permission. 404 for non-owners. |
| PATCH | `/api/documents/{id}/grants/{userId}` | Owner | Smart File Capsule: set `expiresAt` (ISO date-time or null) and `downloadLimit` (1 or more, or null) | 200. 404 if there is no grant. |
| DELETE | `/api/documents/{id}/grants/{userId}` | Owner | Remove access. Takes effect on the next request. | 204 |

## Autopilot

| Method | Path | Who may call | Purpose | Responses |
|--------|------|--------------|---------|-----------|
| POST | `/api/autopilot/propose` | Owner | Turn a plain request into a proposed action. Changes nothing. | 200 with `summary`, `action` and `token`, or a question if the request is unclear. 404 for non-owners. |
| POST | `/api/autopilot/confirm` | Owner | Run a proposed action after the owner confirms. Same rules as the normal endpoints. | 200 with `done`. 403 if the proposal belongs to someone else. 404 if not owner. |

## Administration

Administrator endpoints return 403 with "Administrators only." for other roles. Every refusal is audited.

| Method | Path | Purpose | Responses |
|--------|------|---------|-----------|
| GET | `/api/admin/documents?q=` | Document metadata only: name, owner, category, size, dates. No contents. | 200 |
| GET | `/api/admin/activity?limit=` | Recent audit entries | 200 |
| GET | `/api/admin/permissions?limit=` | Grant and revoke entries | 200 |
| GET | `/api/admin/audit/verify` | Recompute the audit hash chain | 200 with `intact`, `rowsChecked`, `firstBadId` |
| GET | `/api/admin/reset-queue` | Pending password reset requests, without tokens | 200 |
| POST | `/api/admin/reset-queue/{id}/delivered` | Mark a reset as delivered by support. Does not change a password. | 204. 404 if not pending or already marked. |
| POST | `/api/admin/mail-test` | Send one test email to check mail | 200 with `sent` |

## Error responses

| Status | Meaning |
|--------|---------|
| 400 | Malformed request or a missing field |
| 401 | No token, or an invalid or expired token |
| 403 | The role is not allowed to do this at all |
| 404 | Missing, deleted, not visible, exhausted, or integrity-failed. Deliberately the same body for all. |
| 409 | Email already registered (sign-up) |
| 413 | File over 10 MB |
| 415 | File type not allowed |

## Notes

- **File type** is decided from the file's bytes, never its name.
- **Privacy Guardian** scans text uploads only. PDFs and images are not scanned, and the response says so.
- **Verify and download** use the same access rule. Verify does not count against a download limit.
- **No single-document read endpoint** yet. The screens find a document in the visible list.
- **Audit**: every state change and every refusal is recorded.