# API reference

Every endpoint requires a valid JWT in the `Authorization: Bearer` header unless marked public. "Who may call" describes the server-side rule, not just the UI.

| Method | Path | Purpose | Who may call |
|---|---|---|---|
| POST | `/api/auth/login` | Exchange email and password for a token | Public |
| GET | `/api/documents` | List documents the caller can read | Any active user (returns only visible documents) |
| POST | `/api/documents` | Upload a document | OWNER or ADMIN, active |
| GET | `/api/documents/{id}` | Document metadata | Owner, or user with READ or EDIT grant |
| GET | `/api/documents/{id}/download` | Download file contents | Owner, or user with READ or EDIT grant |
| PUT | `/api/documents/{id}/content` | Replace file contents | Owner, or user with EDIT grant |
| DELETE | `/api/documents/{id}` | Soft-delete a document | Owner only |
| GET | `/api/documents/search?name=&owner=&category=&from=&to=` | Search visible documents | Any active user (results limited to visible documents) |
| POST | `/api/documents/{id}/grants` | Grant READ or EDIT to a user | Owner only |
| DELETE | `/api/documents/{id}/grants/{userId}` | Revoke a grant | Owner only |
| GET | `/api/admin/activity` | Activity and permission-change view | ADMIN only |
| GET | `/api/admin/audit/verify` | Verify the audit hash chain | ADMIN only |

## Error responses
- `401`: no valid token.
- `404`: document missing, deleted, or not visible to the caller. Identical body in all three cases.
- `403`: role does not allow the action at all (for example, a viewer trying to upload). Does not reveal document existence, since no document ID is involved.
- `400`: upload failed validation (size, type, or filename).
