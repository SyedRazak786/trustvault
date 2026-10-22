# Threat model

Status key:
- **Closed**: fixed and tested.
- **Mitigated**: the code addresses it, but the test is partial or manual.
- **Open**: known, not yet fixed.
- **Accepted**: knowingly left, with the reason given.

Evidence is listed as the test that shows it, or the manual check run during development.

## Access and sharing

| # | Threat | Status | Evidence |
|---|--------|--------|----------|
| 1 | A caller impersonates another user with a forged identity | Closed | Identity comes only from a signed token (HMAC-SHA512). A forged or missing token returns 401. |
| 2 | A token is altered or forged | Closed | The signature is checked with the server's key on every request. Manual test: a fake token returns 401. |
| 3 | A revoked grant still works with an existing token | Closed | Grants are read from MySQL on every request. Tested: the same token got 200, then 404 after the grant was deleted. |
| 4 | A stranger downloads another user's document | Closed | `strangerCannotDownloadAnotherUsersDocument`. With the check removed, this test fails (status 200). |
| 5 | A stranger sees documents in the list or search | Closed | `strangerSeesNoDocumentsInTheList`. The list uses the same visibility rule as download. |
| 6 | A grant on one document opens another | Closed | `grantOnOneDocumentDoesNotOpenAnother`. |
| 7 | A refusal reveals whether a document exists | Closed for documents | Missing, forbidden and deleted documents return 404 with the same body. Upload refusal for viewers returns 403 (see A2). |
| 8 | A grant holder shares the document further | Mitigated | Grant, revoke, delete and capsule changes call `requireOwner`. Tested for a viewer only. An EDIT holder has not been tested. |
| 9 | A non-owner deletes a document | Closed | Tested: viewer delete refused (`DELETE DENIED not owner`). |
| 10 | A deleted document is still reachable | Closed | Tested: download after soft delete returned 404, and the list no longer showed it. |
| 11 | An expired or exhausted grant still works | Closed | Smart File Capsule. Tested: the second download with a limit of 1 returned 404. Expiry is enforced in the same rule. |
| 12 | Autopilot acts without the owner's agreement | Closed | Autopilot only proposes. A change runs only when the owner confirms, and the confirmation checks ownership again. Tested. |
| 13 | A proposal is reused by another user | Closed | Autopilot tokens are tied to the user ID and the exact action. A proposal from another user is refused. |
| 14 | A user who is not the owner uses Autopilot on a document | Closed | Tested: the propose step checks ownership and refuses a non-owner. |

## Files

| # | Threat | Status | Evidence |
|---|--------|--------|----------|
| 15 | Path traversal through the filename or storage key | Mitigated | Stored names are random UUIDs. The uploaded name is never used as a path, and each path is checked to stay inside the storage root. Not tested with crafted names. |
| 16 | A malicious file disguised by its name | Closed | The type is detected from the file's bytes. An executable header was refused with 415. A text file named `.exe` was stored as `.txt`. |
| 17 | An oversized upload exhausts the server | Mitigated | A 10 MB limit in the controller and in Spring's settings. Not tested with a real large file. |
| 18 | A stored file is changed on the server | Closed | File DNA. The SHA-256 fingerprint is checked on every download and on Verify file. Tested: a changed file returned 404 and was recorded as `DOWNLOAD DENIED file integrity check failed`. |
| 19 | Files uploaded before File DNA have no fingerprint | Accepted | Their check is skipped, so they are not protected until re-uploaded (A10). |
| 20 | Sensitive numbers are shared without the owner noticing | Mitigated | Privacy Guardian scans text uploads for card, Aadhaar-style and PAN-style patterns and warns the owner. It is pattern-based: it can miss content and can flag harmless numbers (see A11). |
| 21 | Non-text files are not scanned for sensitive content | Accepted | PDFs and images are not read by the scanner. The screen tells the owner to check them (A12). |

## Audit trail

| # | Threat | Status | Evidence |
|---|--------|--------|----------|
| 22 | The application edits or deletes audit rows | Closed | The app account has only SELECT and INSERT on `audit_log` (checked with `SHOW GRANTS`). Triggers block UPDATE and DELETE. Tested: a root UPDATE was blocked (`ERROR 1644`). |
| 23 | A database administrator edits an audit row | Detected, not prevented | Tested: root dropped the update trigger and changed row 75. `verify` returned `intact: False, firstBadId: 75`, and rows 1 to 74 still verified. See A1. |
| 24 | The update trigger is removed silently | Mitigated | Found during testing, then restored and verified. The chain check still detects edits. No automated check for missing triggers yet. |
| 25 | An administrator reads document contents | Closed | Admin download returned 404 and was logged. The admin list shows names, owners and dates only, and every view is audited. |
| 26 | Admin reads are not all recorded | Open | Refused admin requests are logged, and the metadata list is logged. Successful reads of the activity log are not yet logged. |
| 27 | Some early download records have no actor | Open | Download rows 68 to 74 show an empty actor. The current code records the user ID. The cause has not been found. |

## Accounts and sign-in

| # | Threat | Status | Evidence |
|---|--------|--------|----------|
| 28 | Login reveals whether an email exists | Mitigated | The same message is returned for an unknown email and a wrong password. Tested for the wrong-password case. |
| 29 | Password guessing on the login | Open | No rate limit yet. Every failed attempt is logged as `LOGIN DENIED`, so the attack can be seen. |
| 30 | Sign-up reveals whether an email is registered | Accepted | Sign-up returns 409 for a duplicate email, which tells a caller the email exists (A7). |
| 31 | Public sign-up is abused to create many accounts | Open | No rate limit or email verification at sign-up. |
| 32 | A role is set by the browser at sign-up | Closed | Tested: a sign-up request with `role: ADMIN` created an OWNER. The server ignores any role sent by the browser. |
| 33 | Password reset reveals whether an account exists | Closed | The request returns the same message for a real and an unknown email. Tested. |
| 34 | A reset token is reused or guessed | Closed | Tokens are random, stored only as SHA-256 hashes, expire after 30 minutes, and work once. A fake token returns 400. |
| 35 | A reset token is printed or leaked by the system | Closed | The token is never written to the log, the API response, or the database in plain text. Support delivers it outside the system (see A9). |
| 36 | A stolen sign-in token is used after sign-out | Accepted | Tokens last 15 minutes. The browser clears its token on sign-out, but the server has no denylist (A3). |
| 37 | A password change does not sign out other sessions | Accepted | Old tokens last up to 15 minutes (A8). |
| 38 | Demo accounts with shared passwords | Open | The three demo accounts share one password. They exist only for local testing. Change every password before any public deployment. |

## Email

| # | Threat | Status | Evidence |
|---|--------|--------|----------|
| 39 | Email reveals a document's name or contents | Closed | The share email says only that a document was shared and asks the recipient to sign in. Tested: the message received contains no file name or contents. |
| 40 | A failed email undoes the share | Closed | The share is saved first. A failed send is logged and the share stands. |
| 41 | The mail key is written into a project file | Closed | The key is read from environment variables only and is not in any committed file. |
| 42 | Mail is sent to someone who is not a user | Closed | The recipient must be an active TrustVault account. The address is read from the database. |
| 43 | The mail connection trusts any certificate | Accepted, development only | `spring.mail.properties.mail.smtp.ssl.trust` is set to the Brevo host, because a local network tool replaces the certificate on the development computer. It must be removed before deployment (A13). |
| 44 | The test mail endpoint is used to send mail to anyone | Closed | It is administrator-only and audited. |

## Website and assistant

| # | Threat | Status | Evidence |
|---|--------|--------|----------|
| 45 | Third-party map sends visitor IP addresses to OpenStreetMap | Accepted | The Contact page loads an OpenStreetMap frame, which receives the visitor's IP. No account or document data is sent (A6). |
| 46 | The assistant reveals secrets or acts on documents | Closed | The assistant is rule-based and fixed. It holds no secrets, reads no documents, and changes no data. It never asks for passwords or tokens. |
| 47 | Scripts on the page read the session token | Mitigated | The token is held in memory, not in localStorage. React escapes output and no raw HTML is rendered. |
| 48 | Privacy and Terms are not reviewed | Open | Both pages are marked as drafts. They must be reviewed before real customers use the service. |

## Accepted risks, with reasons

- **A1. A database administrator can rewrite the whole audit chain.** The chain detects any edit that does not recompute every later hash. A full rewrite passes. Planned: publish the latest hash to a store the database administrator cannot write to.
- **A2. Upload refusal for viewers returns 403, not 404.** No document exists yet, so there is nothing to hide.
- **A3. A stolen token works for up to 15 minutes after sign-out.** The lifetime limits the window. A server-side denylist is planned.
- **A4. Timing side channel on existence checks.** Low value to an attacker for this product.
- **A5. Deleted files stay on disk.** Keeping them preserves the audit trail's links to real records. Retention and purge are planned, with a written policy.
- **A6. Third-party map.** The embedded map receives the visitor's IP address. Accepted because the page is public and holds no user data.
- **A7. Sign-up reveals whether an email is registered.** Accepted for now. Email verification at sign-up is planned, which would remove the difference.
- **A8. Password change does not sign out other sessions.** Old tokens last at most 15 minutes.
- **A9. Support delivers reset tokens by a trusted route outside the system.** The system records that a token was issued and marked delivered, but it cannot confirm the user received it.
- **A10. Files uploaded before File DNA have no fingerprint.** They are not protected until re-uploaded.
- **A11. The privacy scanner is pattern-based.** It can miss sensitive content, and it can flag harmless numbers. The owner makes the final decision, and nothing is blocked.
- **A12. PDFs and images are not scanned for sensitive content.** The screen tells the owner to check them before sharing.
- **A13. Development-only mail trust setting.** The line `spring.mail.properties.mail.smtp.ssl.trust` is for the development computer only. It is removed before deployment, and mail is then verified on the live server.

## Found while building

- Refusals first returned 401, not 404, because Spring's error page was blocked by the security rules. Fixed by allowing the error path.
- An executable header was accepted as text in an early design. The type is now detected from the bytes.
- The update trigger was found missing during the tamper test. Restored and verified.
- A `JwtService` class was pasted into the wrong file, causing a compile error. Fixed.
- A PowerShell here-string wrote its own closing text into a Java file. Fixed by rewriting the file in the editor.
- The admin reset queue initially returned the wrong count for expired tokens. Corrected by checking both the used flag and the expiry.

## How to check these claims

- Remove the access check in `DocumentController.download`, then run `mvn clean test`. Four of six cross-user tests fail. Restore the check and all six pass.
- Run the tamper test: edit one audit row with root, then call `/api/admin/audit/verify`. It reports `intact: false` at that row.
- Change a stored file, then download or verify it. The download is refused with 404, and the Verify file button reports the change.