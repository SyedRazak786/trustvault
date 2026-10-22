# Prompts log

This file records how AI was used, where it went wrong, and what was done instead. Each entry links to the commit that fixed the problem.

## 2026-10-19: Login and token design
- **Asked:** Add login that checks BCrypt hashes, issues a short-lived token, and records every attempt in the audit log.
- **Produced:** A login that returned a different message for an unknown email than for a wrong password, and did not record refusals.
- **Wrong:** The message told an attacker whether an email existed. Refused attempts were not logged, which is the evidence the brief asks for.
- **Fixed:** Both cases return the same 401 message, and every refusal is written to the audit log with its reason. Verified by querying `audit_log` after correct and wrong logins.
- **Commit:** `9ae10ef`

## 2026-10-19: Token checking pasted into the wrong file
- **Asked:** Add token checking, which required editing several files.
- **Produced:** Code for `JwtService` was placed inside `AuthController.java`, causing a duplicate-class compile error.
- **Wrong:** The same class in two files, from an unclear instruction about where each file belonged.
- **Fixed:** Restored `AuthController.java` to the controller alone and put `JwtService` in its own file. Checked each file with `Select-String` before rebuilding.
- **Commit:** `9ae10ef`

## 2026-10-20: Refusals returned 401 instead of 404
- **Asked:** Make a refused document request return 404, the same answer as a missing document.
- **Produced:** A controller that threw 404, which the security filter changed to 401.
- **Wrong:** The brief requires one answer for both cases. A 401 also suggested a token problem rather than a permissions one.
- **Fixed:** Allowed the error path through the security rules, so the 404 reaches the caller. Confirmed with curl.
- **Commit:** `9203b5c`

## 2026-10-20: PowerShell wrote text into a Java file
- **Asked:** Write the access service file from a PowerShell here-string.
- **Produced:** A file with PowerShell text mixed into the Java, causing compile errors.
- **Wrong:** The closing line of the here-string was included in the file.
- **Fixed:** Replaced the file in the editor, checked for stray lines with `Select-String`, and rebuilt.
- **Commit:** `cab726c`

## 2026-10-20: Cross-user tests that needed the check to fail
- **Asked:** Write tests that fail when the authorisation check is removed.
- **Produced:** Tests that depended on test users created in the wrong order.
- **Fixed:** Verified by commenting out the check: four of six tests failed, which shows they depend on the check. Restored it, and all six pass.
- **Commit:** `98361e4`

## 2026-10-21: Missing download counter
- **Asked:** Enforce a download limit from the owner's capsule.
- **Produced:** A download endpoint that never counted downloads, so the limit was not enforced.
- **Wrong:** The counter line had been dropped in a rewrite, and the test showed the second download succeeding.
- **Fixed:** Restored the counter after an allowed download, and tested: the second download returned 404.
- **Commit:** `6a226ad`

## 2026-10-22: Mail refused by the certificate check
- **Asked:** Send email through the Brevo mail service.
- **Produced:** A mailer that logged only the exception type, so the real reason was hidden.
- **Wrong:** The log hid the cause, which was a TLS certificate the Java runtime did not trust on this computer.
- **Fixed:** Logged the mail server's reason, then set a development-only trust setting for the Brevo host, recorded for removal before deployment.
- **Commit:** `68ba6ba`

## Reusable context and instructions
- Standing rule given to the tool: every document decision goes through `AccessService`, and every refusal is audited.
- Checklist applied to AI output: does every route to document data pass through `AccessService`, does the test fail with the check removed, can the application account change audit rows, does any message carry file names or contents, and does any secret appear in a committed file.