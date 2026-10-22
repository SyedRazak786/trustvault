# Permissions model

This describes who can do what in TrustVault, and why the rules are shaped this way. The server enforces every rule. The screens only hide controls a role cannot use.

## Roles

| Role | Can do | Cannot do |
|------|--------|-----------|
| **Owner** | Upload documents. Download, verify, share (READ or EDIT), remove access, set capsule conditions (expiry and download limit), view the Trust Trail, and delete their own documents. Use Autopilot on their own documents. | See the activity log. Read another owner's documents unless granted. |
| **Viewer** | See and download documents shared with them, while the grant is valid. Verify a document. | Upload, share, delete, change conditions, view the Trust Trail, or see documents with no grant. |
| **Administrator** | See the activity log, permission changes, the audit chain check, a metadata list of documents, and the reset queue. Mark a reset as delivered. Send a test email. | Download, read contents, share, upload or delete documents. |

A user's role decides what kind of action they may attempt. Whether they can touch a particular document depends on ownership or a valid grant.

## Per-document grants

A grant connects one user to one document with one permission:

- **READ** lets the user see the document, download it and verify it.
- **EDIT** includes READ. Editing file contents is not built yet, so EDIT currently has the same effect as READ. This is a known limit.
- Only the owner can create, change or remove grants, set capsule conditions, or view who has access. A grant holder cannot share further.
- Deleting a document is owner-only. No grant permits deletion.

## Smart File Capsule

A grant can carry two conditions, set by the owner:

- **An expiry date.** After it passes, the grant stops working and the document disappears from that person's list.
- **A download limit.** Each allowed download counts against it. When the limit is used up, the grant stops working.

The owner is never limited by a capsule. Both conditions are checked on the server, on every request, in the same rule as the grant itself.

## The decision

Every request for a document passes through one method, `AccessService`:

- `can(user, document, permission)` checks, in MySQL on every call, that the document exists and is not deleted, that the user is active, and that the user is the owner or holds a grant that covers the action and has not expired or run out of downloads.
- `require(...)` calls `can`. If the answer is no, it records a DENIED entry and returns **404**.
- `requireOwner(...)` is used for grant, revoke, list-grants, capsule and delete. It returns 404 for a non-owner, and records the attempt.

A missing document, a forbidden document, an expired grant, an exhausted grant and a deleted document all return the same 404, so a caller cannot tell them apart.

## Autopilot

Autopilot has no permissions of its own. It proposes an action only for a document the signed-in owner owns, and it runs only when the owner confirms. The confirmation checks ownership again and applies the same rules as the normal endpoints. A proposal is tied to the user who made it, so another user cannot use it.

## Listing and searching

The document list and the search use the same visibility rule as download, including the capsule conditions. A user sees exactly the documents they could download. The admin metadata list is a separate, admin-only query that returns no contents.

## Revocation

Grants are not cached in the token. A token only proves who the user is. When an owner removes a grant, the next request by that user returns 404, with the same token. This was tested.

## Why these choices

- **No folder inheritance.** Inherited access is harder to audit: one change to a folder would grant or remove access to many documents with no per-document record. Per-document grants are easy to review and revoke.
- **No groups.** Groups create a second route to access that is harder to review. They are a planned extension.
- **Owners are not superusers.** An owner cannot see the audit log, and cannot read another owner's documents.
- **Administrators see metadata, not contents.** The brief asks for an administrative view of activity. Contents belong to owners and their grant holders.
- **Refusals return 404, not 403.** Revealing that a document exists but is forbidden would let a caller map documents they cannot read. The one exception is an upload refused for a viewer, where no document exists yet.
- **Capsule conditions live on the grant.** They belong to one recipient and one document, so they can be set and removed without affecting anyone else.
- **Share notifications name no file.** The recipient is told that a document was shared and to sign in, never what it is.

## Known limits

- EDIT does not yet change file contents, so today it behaves like READ.
- Ownership cannot be transferred. Planned.
- Groups and folder inheritance are not built.
- The download limit counts downloads, not views. A user who views a list does not use up a limit.