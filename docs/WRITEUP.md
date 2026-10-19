# TrustVault write-up

## What was built
TrustVault is a document platform with three roles (administrator, owner, viewer), per-document READ and EDIT grants, upload with validation, search by name, owner, category and date, and an append-only, hash-chained audit trail. Every refused attempt is recorded.

## The central design decision
All authorisation goes through one method, `AccessService.require`. Controllers do not make access decisions. Lists and search use the same visibility rule. The server decides on every request, and grants are read from the database each time, so revocation takes effect immediately.

## What was deliberately not built
- Folder inheritance and groups, to keep the grant model easy to audit.
- Public or external sharing links. These create access that outlives the grant holder's relationship with the owner.
- Admin access to file contents. Admins see activity metadata only.
- Automatic AI sharing. Any AI suggestion requires human confirmation.

## How AI was used and where it was wrong
AI tools were used for scaffolding, tests and documentation. The most serious errors were in authorisation: a check placed in a controller where another endpoint could skip it, a token that carried permissions and so delayed revocation, and an upload path that trusted the declared content type. Each was caught by writing a test that fails when the check is removed. Details are in `PROMPTS.md`.

## What is unfinished
- Rate limiting on login (THREATS #11).
- Server-side token denylist (THREATS #12).
- External anchoring of the audit hash chain (THREATS #6).
- Full version history and recovery of deleted documents.
- Retention policies and purge jobs.

## Honest gaps
Anyone with database administrator access can rewrite the audit trail if they also recompute the hash chain. The design accepts this for now and documents the planned mitigation.
