# Prompts log

This file records how AI was used, where it went wrong, and what was done instead. Each entry links to the commit that fixed the problem. Replace the placeholder commit links with your real ones.

## 2026-10-19: Security core scaffolding
- **Asked:** Build a Spring Boot download endpoint with a single authorisation check and an append-only audit trail.
- **Produced:** A download controller that checked the grant inside the controller, and an audit table with no triggers.
- **Wrong:** The check lived in the controller, so any new endpoint could skip it. The audit table could be updated by the application account.
- **Fixed:** Moved the decision into `AccessService.require`, added triggers that block UPDATE and DELETE, and added a test that fails when the check is removed.
- **Commit:** `<link>`

## 2026-10-19: Access-check bypass in list and search
- **Asked:** Add a document list and search endpoint.
- **Produced:** A query that filtered by owner or grant, but did not check `users.active` or the deleted flag.
- **Wrong:** Deactivated users and deleted documents still appeared in results.
- **Fixed:** Shared one visibility rule between list, search and `AccessService`, and added tests for both cases.
- **Commit:** `<link>`

## 2026-10-19: Audit writes not covering denials
- **Asked:** Add audit logging to the download and delete endpoints.
- **Produced:** Audit entries only for successful actions.
- **Wrong:** Refused attempts, which are the most useful evidence, were not recorded.
- **Fixed:** `AccessService.require` records every denial before returning 404. Added `refusedAttemptIsRecordedInAudit` test.
- **Commit:** `<link>`

## 2026-10-19: Upload validation trusted the declared content type
- **Asked:** Add a file upload endpoint.
- **Produced:** Stored the client-declared Content-Type and used the original filename as the storage path.
- **Wrong:** A file could be an executable labelled as a PDF. A crafted filename could escape the storage directory.
- **Fixed:** Magic-byte check against the declared type, server-generated storage keys, and a path containment check. Added `UploadValidationTest`.
- **Commit:** `<link>`

## 2026-10-19: Token contained permissions
- **Asked:** Add login and put the user's grants in the JWT for speed.
- **Produced:** Tokens that carried a list of document IDs.
- **Wrong:** Revoking a grant would not take effect until the token expired.
- **Fixed:** Tokens carry only the user ID and role. Grants are read from the database on every request. Added `RevocationTest`.
- **Commit:** `<link>`

## Reusable context and instructions
- Standing instruction given to the tool: "Never place an authorisation check in a controller. Every decision goes through AccessService and every refusal is audited."
- Review checklist applied to all AI output: Does every route to document data pass through AccessService? Does the test fail when the check is removed? Can the app account change audit rows?
