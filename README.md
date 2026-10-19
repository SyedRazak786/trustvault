# TrustVault

Secure document sharing with a real audit trail. A document is reachable only by a user who was explicitly granted it, and every upload, download, change, deletion, permission change and refused attempt is recorded in a hash-chained audit log.

## Stack
- Backend: Java 17, Spring Boot, Spring Security (JWT), JDBC, MySQL 8
- Frontend: React + TypeScript
- Tests: JUnit 5, Spring MockMvc

## Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8
- Node.js 18+ (frontend only)

## Database setup
1. Create the database and run `database/schema.sql` as the admin account.
2. Run `database/setup-accounts.sql` to create the application account. It has INSERT and SELECT on `audit_log` only, with no UPDATE or DELETE.
3. Optionally run `database/seed.sql` to load demo accounts and documents.

## Environment variables
| Variable | Purpose |
|---|---|
| `DB_USER` | Application database user |
| `DB_PASSWORD` | Application database password |
| `STORAGE_ROOT` | Directory for uploaded files, outside the repository |
| `JWT_SECRET` | Signing key for tokens, at least 32 random bytes |

## Run the tests
```
cd backend
mvn test
```
The tests include cross-user access checks, revocation checks, audit integrity checks and upload validation checks.

## Run locally
```
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev
```

## Seed accounts
| Role | Email | Password |
|---|---|---|
| Administrator | admin@trustvault.test | (set in seed.sql) |
| Owner | owner@trustvault.test | (set in seed.sql) |
| Viewer | viewer@trustvault.test | (set in seed.sql) |

Replace these passwords on any deployed instance.

## Deployment
See `docs/DEPLOYMENT.md`.

## Documentation
- `THREATS.md`: threat model, mitigations, accepted risks
- `PROMPTS.md`: log of AI prompts, what was wrong, and the fixes
- `docs/PERMISSIONS.md`: roles and the grant model
- `docs/API.md`: every endpoint and who may call it
- `docs/WRITEUP.md`: what was built, what was not, and what is unfinished
- `docs/DEMO-SCRIPT.md`: five-minute demo outline
