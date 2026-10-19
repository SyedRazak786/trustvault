# Permissions model

## Roles
| Role | Can do |
|---|---|
| **Administrator** | Manage users and roles. View the activity and permission-change dashboard. Cannot read document contents unless also granted access as a user. |
| **Owner** | Upload documents. Full control over documents they own: read, edit, grant, revoke, delete. |
| **Viewer** | Sees only documents explicitly granted to them. Read-only unless granted EDIT. Cannot upload. |

A user's role decides what they may try to do. Whether they can touch a specific document depends on the grant.

## Per-document grants
Each grant links one document to one user with one permission:
- `READ`: view metadata and download.
- `EDIT`: everything READ allows, plus replace content and rename. EDIT implies READ.
- Delete is owner-only. No grant permits deletion.
- Grant and revoke are owner-only. A user with EDIT cannot share further.

## The decision
Every request to a document goes through `AccessService.require(user, document, permission, action)`. It returns allowed only when all of these hold:
1. The document exists and is not deleted.
2. The user is active.
3. Either the user owns the document, or a grant exists with a permission that covers the action.

A refusal is written to the audit log and returns 404. The 404 is the same whether the document is missing or merely forbidden.

## Why these choices
- **No folder inheritance yet.** Inheritance makes it harder to see why someone has access. Per-document grants are easier to audit and to revoke. Folders can be added later as a grant source, and each inherited grant would need to be recorded in the audit trail.
- **No groups yet.** Groups add a second path to access that is harder to review. They are on the roadmap.
- **Grants are read on every request.** This costs a database query per request. It is what makes revocation immediate.
- **Owners are not superusers.** An owner cannot read the audit trail or other owners' documents.

## Revocation
Deleting a grant row takes effect on the next request. There is no cache of permissions. Tests in `RevocationTest` confirm this.

## Open questions
- Should an owner be able to transfer ownership? Not built yet.
- Should expiring grants be supported? Planned, with the expiry checked in `AccessService`.
