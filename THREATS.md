# Threat model

Status key:
- **Closed**: fixed and tested.
- **Mitigated**: the code addresses it, but the test is missing or incomplete.
- **Open**: known, not yet fixed.
- **Accepted**: knowingly left, with the reason given.

## Threats and status

| # | Threat | Status | Evidence |
|---|--------|--------|----------|
| 1 | A caller impersonates another user with a forged identity | Closed | Identity comes only from a signed token (HMAC-SHA512). No header is trusted. A fake token returns 401 (tested manually). Commit `<hash>`. |
| 2 | A token is altered or forged | Closed | The signature is checked with the server's key on every request. A tampered token returns 401 (tested manually). Automated test not yet written. |
| 3 | A revoked grant still works with an existing token | Closed | Grants are read from MySQL on every request. Tested: the same token got 200, then 404 after the grant was deleted. `RevocationTest` / `revokedGrantStopsWorkingWithTheSameToken`. Commit `<hash>`. |
| 4 | A stranger downloads another user's document | Closed | `strangerCannotDownloadAnotherUsersDocument`. With the check removed, this test fails (status 200). Commit `<hash>`. |
| 5 | A stranger sees documents in the list or search | Closed | `strangerSeesNoDocumentsInTheList`. The list and the search use one visibility rule (`listVisible`). |
| 6 | A grant on one document opens another | Closed | `grantOnOneDocumentDoesNotOpenAnother`. |
| 7 | A refusal reveals whether a document exists | Closed for documents | Missing, forbidden and deleted documents all return 404 with the same body. Tested with curl. The upload refusal for viewers returns 403 (see Accepted #A2). |
| 8 | A user with EDIT permission shares the document further | Mitigated | Grant, revoke and delete call `requireOwner`. Tested only for a viewer (READ): `GRANT DENIED not owner` in the audit log. An EDIT holder has not been tested. |
| 9 | A non-owner deletes a document | Closed | Tested: viewer delete refused (`DELETE DENIED not owner`). Owner delete succeeded and the document disappeared. |
| 10 | A deleted document is still reachable | Closed | Tested: download after soft delete returned 404, and the list no longer showed it. |
| 11 | Path traversal through the filename or storage key | Mitigated | Stored names are random UUIDs. The uploaded name is never used in a path, and each path is checked to stay inside the storage root. Not tested with crafted names. |
| 12 | A malicious file disguised by its name | Closed | Type is detected from the file's bytes. An executable header was refused with 415. A text file named `.exe` was stored as `text/plain` with the name corrected to `.txt`. Tested. |
| 13 | An oversized upload exhausts the server | Mitigated | 10 MB limit in the controller and in Spring's settings. Not tested with a real large file, and Spring may reject it before our check runs. |
| 14 | The application edits or deletes audit rows | Closed | The app account has only SELECT and INSERT on `audit_log` (checked with `SHOW GRANTS`). Triggers block UPDATE and DELETE. Tested: a root UPDATE was blocked (`ERROR 1644`). |
| 15 | A database administrator edits an audit row | Detected, not prevented | Tested: root dropped the update trigger and changed row 75. `verify` returned `intact: False, firstBadId: 75`, and rows 1 to 74 still verified. See Accepted #A1. |
| 16 | The update trigger is removed silently | Mitigated | Found during testing: the update trigger was missing, and the app account could have changed rows. Restored and verified. The chain check still detects edits. No automated check for missing triggers yet. |
| 17 | Login reveals whether an email exists | Mitigated | The same message is returned for an unknown email and a wrong password. Tested for the wrong-password case only. |
| 18 | Password guessing on the login | Open | No rate limit yet. Every failed attempt is logged as `LOGIN DENIED`, so the attack can be seen. Planned: rate limit per email and per address. |
| 19 | A stolen token is used after sign-out | Accepted | Tokens last 15 minutes. The browser clears its token on sign-out, but the server has no denylist. See Accepted #A3. |
| 20 | SQL injection | Closed | Every query uses bound parameters. User input is never concatenated into SQL. |
| 21 | Cross-site request forgery | Accepted | Tokens are sent in a header, not a cookie, so browsers do not attach them automatically. |
| 22 | Scripts on the page read the session token | Mitigated | The token is held in memory, not in localStorage. React escapes output and no raw HTML is rendered. A script injection would still read the token while the page is open. |
| 23 | Demo accounts with known or shared passwords | Open | The three demo accounts share one password. They exist only under the `demo` profile. Before any public deployment, change every password and do not run the demo seeder. |
| 24 | An administrator reads document contents | Closed | Admin download returned 404 and was logged as `DOWNLOAD DENIED`. The admin metadata list shows names and owners only, and it is audited. |
| 25 | Timing differences reveal whether a document exists | Accepted | A single indexed lookup, so the difference is small. Low value for this product. See Accepted #A4. |
| 26 | Admin successful reads are not recorded | Open | Refused admin requests are logged, but a successful view of activity or permissions is not. The metadata list is logged. Planned: log every admin read. |
| 27 | Deleted files stay on disk | Accepted | Soft delete hides them, but nothing removes the file. A purge and retention policy are not built. See Accepted #A5. |
| 28 | Some older download audit rows have no actor | Open | Rows 68 to 74 show an empty actor. Those rows were written during testing. The current code records `user.id()`, so the cause must be found before submission. |

## Accepted risks (with reasons)

- **A1. A database administrator can rewrite the whole audit chain.** The chain detects any edit that does not recompute every later hash. A full rewrite passes. Planned mitigation: publish the latest hash periodically to a store the database administrator cannot write to.
- **A2. Upload refusals for viewers return 403, not 404.** No document exists yet, so there is nothing to hide. The response reveals only that viewers cannot upload, which is public product behaviour.
- **A3. A stolen token works for up to 15 minutes after sign-out.** Short lifetime limits the window. A server-side denylist is planned.
- **A4. Timing side channel.** Low value to an attacker for this product.
- **A5. Deleted files are kept on disk.** Keeping them preserves the audit trail's links to real records. Retention and purge will be added with a written policy.
- **A6. Admins see document names, owners, categories and dates.** The brief requires an administrative view of activity. Contents stay with owners and grant holders, and every metadata view is audited.

## Found while building

- Refusals first returned 401, not 404, because Spring's error page was blocked by the security rules. Fixed by allowing the error path. Tested.
- An executable header was accepted as text on the first attempt design. Type detection now reads the file's bytes. Tested.
- The update trigger was missing during the tamper test. Restored and verified. Commit `<hash>`.
- A `JwtService` class was pasted into the wrong file, causing a compile error. Fixed. Commit `<hash>`.
- A PowerShell here-string wrote its own closing text into a Java file. Fixed by rewriting the file in the editor. Commit `<hash>`.

## How to check these claims

- Remove the check in `DocumentController.download`, then run `mvn clean test`. Four of six cross-user tests fail. Restore the check and all six pass.
- Run the tamper test in `docs/DEMO-SCRIPT.md`, or the steps in the write-up, to see `intact: False` at the edited row.