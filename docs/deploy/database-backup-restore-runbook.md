# RoomBay Database Backup And Restore Runbook

## Purpose

Define the minimum production recovery drill for Railway PostgreSQL so RoomBay can prove database backups are usable.

## Targets

- RPO: 24 hours for v1.
- RTO: 4 hours for v1.
- Restore drill cadence: monthly until automated backup freshness is visible in Admin Ops.

## Backup Check

1. Open Railway project.
2. Open the PostgreSQL service.
3. Confirm backups or snapshots are enabled.
4. Record the latest backup timestamp.
5. Confirm the app service is connected to the intended PostgreSQL service.

## Restore Drill

Use a non-production database for the drill.

1. Create or select a restore target PostgreSQL service.
2. Restore the latest production backup into that target.
3. Point a temporary backend environment at the restored database.
4. Start backend with:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`
   - `SPRING_FLYWAY_ENABLED=true`
5. Verify startup completes without schema validation errors.
6. Run smoke checks:
   - `GET /actuator/health`
   - admin login
   - listing search/feed
   - saved searches
   - notifications unread count
   - messaging conversations
   - AI graph stats
7. Record the restore completion time and any errors.

## Production Marker

After a successful drill, set:

```text
ROOMBAY_BACKUP_LAST_VERIFIED_AT=YYYY-MM-DDTHH:mm:ssZ
ROOMBAY_BACKUP_RPO_HOURS=24
```

Admin Operations Center reads this marker and warns when no drill timestamp is configured.

## Incident Restore

1. Freeze writes if data corruption is suspected.
2. Capture Railway app and database logs.
3. Restore to a new database first.
4. Validate schema and smoke checks.
5. Switch `SPRING_DATASOURCE_URL`, username, and password only after validation.
6. Keep the old database service until postmortem is complete.

## Do Not

- Do not run ad hoc destructive SQL on production during panic triage.
- Do not disable Flyway validation to force startup.
- Do not restore over the only production database without a verified copy.
