# TrustVault write-up

## What it is

TrustVault is a document platform where a file can only be reached by the people it was shared with, and where every action is recorded and can be checked.

Three roles: **owners** upload and share, **viewers** see and download what they are given, and **administrators** review activity and the audit chain, and see document details but never contents.

## What it does

- **Accounts:** sign-up as an owner, sign-in, profile, password change, and password reset by a one-time link that expires in 30 minutes.
- **Sharing:** share with a named person as READ or EDIT, remove access at once, and be notified by email that a file was shared, with no file name or contents in the message.
- **Protection of the file itself:** a SHA-256 fingerprint is taken at upload and checked on every download and on the Verify file button. A changed file is refused and recorded.
- **Smart File Capsule:** an owner can give a recipient an expiry date or a download limit, enforced by the server on every request.
- **Trust Trail:** the owner sees a timeline of a document's events and whether its record is intact.
- **Privacy Guardian:** text uploads are scanned for card, Aadhaar-style and PAN-style patterns, and the owner is warned. Nothing is blocked.
- **Autopilot:** the assistant turns a plain request into a proposed action, and nothing happens until the owner confirms. It uses the same rules as the buttons.
- **Audit:** every upload, download, verify, share, removal, deletion, sign-in and refusal is recorded, protected by database rules and a hash chain.
- **Public website and assistant:** a welcome page, how it works, about, security, contact with a map of Guntur, help, privacy and terms, and a rule-based assistant that answers common questions.

## The central design decision

Every decision about a document goes through one method, `AccessService`. Controllers do not decide access. The list, the search, the download and the verify check all use the same rule, so a user cannot see a name they cannot open.

The server decides on every request. Grants are read from the database each time, and tokens carry only the user's ID and role. That is what makes revocation immediate. A capsule limit uses the same rule, so an exhausted grant stops working everywhere at once.

Refusals return 404 whether a document is missing, forbidden, expired or deleted, so a caller cannot map documents they cannot read.

## How the audit trail is protected

Two layers, both tested.

1. **Database rules.** The application's database account can only add and read audit rows. Triggers block UPDATE and DELETE, even for root.
2. **A hash chain.** Each row stores a hash of its content and the previous row's hash. The admin check recomputes the whole chain. In a tamper test, a root session dropped the trigger and changed one row. The check reported the chain broken at that exact row, and the rows before it still verified.

The second layer is what matters if someone bypasses the first. It is not perfect: someone with root access could rewrite every row and recompute every hash. That is recorded as an accepted risk, with the planned fix of publishing the latest hash somewhere the database administrator cannot write.

During the tamper test I found the update trigger had been missing, so the application account could have changed rows at that point. I restored it and confirmed the block. The threat record keeps that finding, because a real review would find the same gap.

## How I used AI, and where it was wrong

I used an AI assistant throughout, to write code, explain errors and draft documentation. I directed it step by step and tested each step rather than accepting the output. The mistakes that mattered most were in access and in the audit trail:

- A class was placed inside the wrong file and caused a compile error. I found it from the compiler message and fixed it.
- The first download refusals returned 401 instead of 404, because the security rules blocked Spring's error page. I found it by testing with curl and allowed the error path.
- An early access check sat in a controller, where a new endpoint could skip it. Moving it to one service method, with tests that fail when it is removed, is how I confirmed it could not be skipped.
- A PowerShell command wrote its own closing text into a Java file. The compiler caught it, and I rewrote the file directly.
- The mail connection failed with a certificate error on my computer. I diagnosed it from the full error message and set a development-only trust setting, which is recorded to be removed before deployment.

In each case I did not rely on the output being correct. I ran a test that would fail if the assistant was wrong.

## What the tests prove

- Six cross-user tests cover a stranger's download, a stranger's list, a grant on one document opening another, the owner's own access, revocation with the same token, and the audit record of a refusal.
- I removed the access check and ran the tests. Four of the six failed, and the stranger received another user's file. I restored the check, and all six passed.
- Manual tests cover upload type checks, owner-only upload and delete, the capsule limit (the second download was refused), File DNA (a changed file was refused and recorded), the privacy scan, Autopilot's propose-and-confirm flow, the admin reset queue, and the share email.

## What is unfinished

- **Editing file contents.** EDIT currently behaves like READ.
- **A single-document endpoint.** The screens find a document in the list.
- **Login rate limiting, and email verification at sign-up.**
- **Logging successful admin reads.**
- **Scanning PDFs and images** for sensitive content.
- **Purge and retention.** Deleted files stay on disk.
- **Demo passwords.** The three demo accounts share one password, for local testing only.
- **The development mail trust setting.** It must be removed before deployment.
- **Deployment and a public address.** The system runs locally for now.

## Honest gaps, in short

The most important gap is the audit trail: a database administrator can rewrite the whole chain. Everything else in the access model is tested. I would rather state that plainly than claim the trail is tamper-proof.