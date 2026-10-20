# API reference

Every endpoint except `POST /api/auth/login` needs a signed token in the header:

Tokens last 15 minutes and contain only the user's ID and role. The server reads the user's grants from the database on every request, so access changes take effect at once, without a new token.

The "Who may call" column describes the rule the server enforces. The screens hide controls a role cannot use, but that is cosmetic only.

## Authentication

| Method | Path | Who may call | Purpose | Responses |
|--------|------|--------------|---------|-----------|
| POST | `/api/auth/login` | Anyone | Exchange email and password for a token | 200 with `token`, `email`, `role`. 401 for an unknown email, a wrong password, or an inactive account (same message in all three). 400 if fields are missing. Every attempt is audited. |
| GET | `/api/auth/me` | Any signed-in user | Return the caller's ID and role | 200 with `id`, `role`. 401 without a valid token. |

## Documents

| Method | Path | Who may call | Purpose | Responses |
|--------|------|--------------|---------|-----------|
| GET | `/api/documents` | Any signed-in user | List documents the caller owns or holds a grant for, newest first | 200 with rows of `id`, `name`, `category`, `content_type`, `size_bytes`, `created_at`. Deleted documents and documents with no grant never appear. |
| POST | `/api/documents` | Owner only | Upload a file (multipart field `file`, optional `category`) | 201 with `id`, `name`, `contentType`. 400 if no file. 403 for a non-owner (see note). 413 if over 10 MB. 415 if the bytes are not PDF, PNG, JPEG or plain UTF-8 text. |
| GET | `/api/documents/{id}/download` | Owner, or a user with a READ or EDIT grant | Download the file | 200 with the file and a `Content-Disposition` attachment header. 404 for a missing, deleted or forbidden document (same body in all cases). |
| DELETE | `/api/documents/{id}` | Owner only | Soft-delete the document | 204. 404 for a non-owner or a missing document. The file stays on disk (see THREATS.md, A5). |

## Grants (sharing)

| Method | Path | Who may call | Purpose | Responses |
|--------|------|--------------|---------|-----------|
| GET | `/api/documents/{id}/grants` | Owner only | List who has access, and at what level | 200 with rows of `user_id`, `email`, `permission`, `granted_by`. 404 for anyone else. |
| POST | `/api/documents/{id}/grants` | Owner only | Give an active user READ or EDIT. Repeating the call changes the permission. Body: `{"email": "...", "permission": "READ"}` | 201 with `documentId`, `userId`, `permission`. 400 for an unknown or inactive email, or a permission other than READ or EDIT. 404 for a non-owner. |
| DELETE | `/api/documents/{id}/grants/{userId}` | Owner only | Remove a user's access. Takes effect on that user's next request. | 204. 404 if the caller is not the owner, or there is no such grant. |

## Administration

Administrator endpoints return 403 with `Administrators only.` for any other role. The refusal is written to the audit log.

| Method | Path | Who may call | Purpose | Responses |
|--------|------|--------------|---------|-----------|
| GET | `/api/admin/documents?q=` | Administrator only | Document metadata: name, owner, category, size, dates, status. The optional `q` matches name or category. No contents, no download links. Audited as `ADMIN_VIEW_DOCUMENTS`. | 200, at most 500 rows. |
| GET | `/api/admin/activity?limit=` | Administrator only | Recent audit entries across the system | 200. `limit` is 1 to 500, default 100. |
| GET | `/api/admin/permissions?limit=` | Administrator only | Grant and revoke entries only | 200. Same `limit` rules. |
| GET | `/api/admin/audit/verify` | Administrator only | Recompute the audit hash chain | 200 with `intact`, `rowsChecked`, `firstBadId`. |

## Error responses

| Status | Meaning |
|--------|---------|
| 400 | The request is malformed or missing a required field. |
| 401 | No token, an invalid or expired token, or a failed sign-in. |
| 403 | The role is not allowed to perform this action at all (upload by a non-owner, admin endpoints by a non-admin). |
| 404 | A document is missing, deleted, or not visible to the caller. Deliberately the same for all three. |
| 413 | The uploaded file is larger than 10 MB. |
| 415 | The file's content is not an allowed type. |

## Notes

- **403 on upload.** A viewer calling `POST /api/documents` gets 403, not 404, because no document exists yet, so there is nothing to conceal. This is recorded in THREATS.md as accepted risk A2.
- **File type.** The server decides the type from the file's bytes, never from its name or the browser's declared type.
- **No single-document read.** There is no `GET /api/documents/{id}` endpoint yet. The screens find a document in the visible list. This is a known gap, listed in WRITEUP.md.
- **Audit.** Every request above that changes state, and every refusal, is recorded in `audit_log` with the actor, action, document and reason.