# Threat model

Status key: **Closed** (fixed and tested), **Mitigated** (reduced, with a test or note), **Open** (known and not yet fixed), **Accepted** (knowingly left, with reason).

| # | Threat | Status | What was done / why |
|---|--------|--------|---------------------|
| 1 | Caller impersonates another user via a forged or replayed identity header | Closed once JWT auth is live | Identity comes only from a verified, signed, short-lived JWT. The development header must be removed before deployment. Test: a request with a forged header returns 401. |
| 2 | Revoked grant still works through an existing token | Closed | Grants are read from MySQL on every request. No permissions are stored in tokens. Test: `RevocationTest`. |
| 3 | Refused user learns a document exists (404 vs 403) | Closed | Every refusal returns 404 with the same body, whether the document is missing, deleted or forbidden. |
| 4 | Path traversal through storage key or filename | Closed | Storage keys are server-generated UUIDs. The resolved path is normalised and checked to be inside `STORAGE_ROOT`. The original filename is only used in a header, never in a path. |
| 5 | Application rewrites or deletes audit rows | Mitigated | The app account has INSERT and SELECT only. Triggers block UPDATE and DELETE even for accounts that have them. |
| 6 | Database administrator rewrites audit rows and recomputes hashes | Accepted for now | A hash chain detects edits that do not recompute the chain, but a full rewrite can pass. Planned: publish the latest hash periodically to a store the database admin cannot write to. |
| 7 | Deleted document remains downloadable | Closed | Soft delete filters every read path. Test: deleted documents return 404 for all users including the owner. |
| 8 | Owner account deactivated but still able to act | Closed | Every permission check requires `users.active = TRUE`, including owner checks. |
| 9 | Malicious upload (type spoofing, oversized file, hostile filename) | Mitigated | Size limit enforced by the server. Magic-byte check against the declared type. Filenames sanitised for display. Test: `UploadValidationTest`. |
| 10 | Timing side channel reveals whether a document exists | Accepted | Existence checks are a single indexed query, so the timing difference is small. Low value to an attacker for this product. |
| 11 | Brute force on login | Open | Rate limiting on `/api/auth/login` is planned. Until added, use strong passwords and monitor DENIED login entries in the audit log. |
| 12 | Stolen JWT used after logout | Accepted | Tokens are short-lived (15 minutes). A server-side denylist is planned. |
| 13 | Search leaks names of documents the caller cannot read | Closed | Search and list queries use the same visibility rule as `AccessService`. Test: a stranger's search returns no results for a private document. |
| 14 | Cross-site request forgery on state-changing endpoints | Mitigated | JWT in the `Authorization` header, not a cookie, so browsers do not attach it automatically. |
| 15 | Sensitive values in logs | Mitigated | Passwords, tokens and file contents are never logged. Audit `detail` holds only a short reason. |
| 16 | Admin reads document contents through the admin view | Accepted by design | Admins see activity metadata (who, what, when). They do not get a route to file contents. Admin access to content would itself be audited if added. |
| 17 | AI suggestion widens access without a person | Closed by design | The AI feature (if built) only suggests. Any grant needs a human confirmation, and both the suggestion and the decision are audited. |

## Threats found while building (add as you go)
- _List each issue you find and the commit that fixed it._
