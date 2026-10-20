# Permissions model

This describes who can do what in TrustVault, and why the rules are shaped this way. The server enforces every rule. The screens only hide controls a role cannot use.

## Roles

| Role | What they can do | What they cannot do |
|------|------------------|---------------------|
| **Owner** | Upload documents. Download, grant, revoke and delete documents they own. See who has access to their documents. | Read documents owned by someone else unless granted. See the activity log. |
| **Viewer** | See and download documents someone has granted them. | Upload, share, delete, or see documents with no grant. |
| **Administrator** | See the activity log, permission changes, and the audit chain check. See document metadata (name, owner, category, size, date, status) and search it. | Download, read contents, share, upload or delete documents. A refused attempt is logged. |

A user's role decides what kind of action they may attempt. Whether they can touch a particular document depends on ownership or a grant.

## Per-document grants

A grant connects one user to one document with one permission:

- **READ** lets the user see the document in the list, and download it.
- **EDIT** includes READ. Editing the file's contents is not built yet, so EDIT currently has the same effect as READ, and this is recorded as a known limit.
- Only the owner can create, change or remove grants. A grant holder cannot pass access on to someone else.
- Deleting a document is owner-only. No grant permits deletion.

Granting the same user again changes their permission. Revoking removes the grant at once.

## The decision

Every request for a document passes through one method, `AccessService`:

- `can(user, document, permission)` checks, in MySQL on every call, that the document exists and is not deleted, that the user is active, and that the user owns it or holds a grant with a permission that covers the action.
- `require(...)` calls `can`. If the answer is no, it records a DENIED entry in the audit log and returns **404**.
- `requireOwner(...)` is used for grant, revoke, list-grants and delete. It returns 404 for a non-owner, and records the attempt.

A missing document, a forbidden document and a deleted document all return the same 404 response, so a caller cannot tell them apart.

## Listing and searching

The document list and the search use the same visibility rule as `can`. A user sees exactly the documents they could download. The admin metadata list is a separate, admin-only query that returns no contents.

## Revocation

Grants are not cached in the token. A token only proves who the user is, and the role claim is informational. When an owner removes a grant, the next request by that user returns 404, with the same token. This was tested: the same token downloaded a file with a grant (200), then got 404 after the grant was deleted.

## Why these choices

- **No folder inheritance.** Inherited access is harder to audit: a single change to a folder would grant or remove access to many documents at once, with no per-document record. Per-document grants are easy to review and revoke.
- **No groups.** Groups create a second route to access that is harder to review. They are a planned extension, and each group membership would need its own audit entry.
- **Owners are not superusers.** An owner cannot see the audit log, and cannot read another owner's documents.
- **Administrators see metadata, not contents.** The brief asks for an administrative view of activity. Contents belong to owners and their grant holders, so an administrator cannot read them, even by a direct request. Each metadata view is audited.
- **Refusals return 404, not 403.** Revealing that a document exists but is forbidden would let a caller map documents they cannot read. The one exception is an upload refused for a viewer, where no document exists yet (see THREATS.md, accepted risk A2).

## Known limits

- EDIT does not yet allow changing a file's contents, so its only effect today is the same as READ. Content editing is a planned feature.
- Ownership cannot be transferred. Planned.
- Grants do not expire. Planned, with expiry checked in `AccessService`.
- Admins cannot be granted document contents. If that were ever needed, it would be a deliberate, audited design change.