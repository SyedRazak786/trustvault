# Deployment

Deploy early in the week, and test from outside before the final day.

## Steps from a clean clone
1. Provision MySQL 8 and create the database.
2. Run `database/schema.sql` as the admin user, then `database/setup-accounts.sql`.
3. Create a storage directory outside the repository, owned by the application user, with permissions `700`. Example: `/var/trustvault/files`.
4. Set environment variables: `DB_USER`, `DB_PASSWORD`, `STORAGE_ROOT`, `JWT_SECRET`.
5. Remove the development identity header and confirm login is the only way in.
6. Build and run: `cd backend && mvn -DskipTests package`, then `java -jar target/trustvault.jar --spring.profiles.active=prod`.
7. Build the frontend with `npm run build` and serve the output over HTTPS.
8. Load demo data with `database/seed.sql` only on a demo instance, and change the seed passwords.

## Checks after deploy
- Log in as the viewer account and attempt to download a document it was not granted. Expect 404.
- Run `scripts/verify-isolation.sh` against the deployed URL.
- Confirm `/api/admin/audit/verify` reports the chain intact.

## Hosting notes
Use a persistent volume for both the database and `STORAGE_ROOT`. Files on an ephemeral container disk will disappear on redeploy.
