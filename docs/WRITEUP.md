# TrustVault write-up

## What I built

TrustVault is a document platform where a file can only be reached by a person who was granted it, and where every upload, download, change, deletion, permission change and refused attempt is recorded.

It has three roles:

- **Owners** upload documents, share them with named people (READ or EDIT), remove that access, and delete their documents.
- **Viewers** see and download only what has been shared with them.
- **Administrators** see the activity log, the permission changes, and a metadata list of documents (name, owner, category, size, date, status), and can check the audit chain. They cannot read document contents.

The full permission model is in `docs/PERMISSIONS.md`, and every endpoint with its rule is in `docs/API.md`.

## The central design decision

Every decision about a document goes through one method, `AccessService`. Controllers do not make access decisions. The list and the search use the same visibility rule as a download, so a user cannot see a name they cannot open.

The server decides on every request. Grants are read from MySQL each time, and tokens carry only the user's ID and role. That is what makes revocation immediate: I tested it by downloading with a token, deleting the grant, and retrying with the same token, which then returned 404.

Refusals return 404 whether the document is missing, deleted or forbidden, so a caller cannot map documents they cannot read.

## How the audit trail is protected

It is protected in two layers, and I tested both:

1. **Database triggers and account permissions.** The application's database account can only add and read audit rows. Triggers block UPDATE and DELETE, even for root. I confirmed this with `SHOW GRANTS` and with a blocked update.
2. **A hash chain.** Each row stores a hash of its own content and the previous row's hash. The admin check recomputes the whole chain. In a tamper test, I used root to drop the update trigger and change one row. The check reported the chain broken at that exact row, and rows before it still verified.

The second layer is what matters if someone with database access bypasses the first. It is not perfect. Someone with root access could rewrite every row and recompute every hash. I recorded this as an accepted risk in `THREATS.md`, with the planned fix: publish the latest hash somewhere the database administrator cannot write.

During the test I found the update trigger had been missing. The app account could have changed rows at that point. I restored it and confirmed the block. I have kept this in the threat record, because it's exactly the kind of gap a real review would find.

## How I used AI, and where it was wrong

I used an AI assistant throughout, to write code, explain errors, and draft the documentation. I directed it step by step, and I tested each step rather than accepting the output. The mistakes that mattered most were in access and in the audit trail, and they are recorded in `PROMPTS.md`.

The clearest examples:

- A class was placed inside the wrong file, causing a compile error. I found it from the compiler message and fixed it by checking each file.
- The first download refusals returned 401 instead of 404. The security rules were blocking Spring's error page, so the refusal looked like a token problem. I found this by testing with curl, and allowed the error path.
- The first version of the access check was placed in the controller, where a new endpoint could skip it. Moving it to one service method, and writing tests that fail when it is removed, is how I confirmed it could not be skipped.
- One version of the file writer mixed PowerShell text into a Java file. The compiler caught it, and I rewrote the file directly.

In each case, I did not rely on the output being correct. I ran a test that would fail if the assistant was wrong.

## What the tests prove

- Six cross-user tests in `CrossUserAccessTest` cover a stranger's download, a stranger's list, a grant on one document opening another, the owner's own access, revocation with the same token, and the audit record of a refusal.
- I removed the access check and ran the tests. Four of the six failed, and the stranger received another user's file. I restored the check, and all six passed.
- Manual tests covered upload type checking (an executable header is refused, a text file named `.exe` is stored as `.txt`), owner-only upload and delete, and the admin checks.

## What is unfinished

- **Editing file contents.** EDIT currently behaves like READ. Content editing is not built.
- **Single-document endpoint.** The screens find a document in the list, because there is no `GET /api/documents/{id}` yet.
- **Login rate limiting.** Every failed attempt is logged, but nothing slows repeated guessing yet.
- **Logging admin reads.** Refused admin requests are logged. Successful views of the activity log are not yet.
- **Purge and retention.** Deleted files stay on disk.
- **Demo passwords.** The three demo accounts share one password. They are for local testing only, and must be changed before any public deployment.
- **Missing-actor rows.** Some earlier download entries have no actor recorded. I have not yet found the cause, and it is listed as open in `THREATS.md`.
- **Deployment.** The system runs locally. A public URL is still to be set up.
- **Size limit.** The 10 MB limit is enforced in the controller, but I have not tested a file over the limit, so Spring may reject it first.

## Honest gaps, in short

The most important gap is the one in the audit trail: a database administrator can rewrite the whole chain. Everything else in the access model is tested. I'd rather state that clearly than claim the trail is tamper-proof.