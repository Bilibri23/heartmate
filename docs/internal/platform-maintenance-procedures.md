# Internal Platform Maintenance Procedures

Internal maintenance should be handled through normal deployment, migration, logging, and rollback procedures. Admin chat may summarize operational steps, but it must not expose secrets, raw environment values, tokens, passwords, or private user documents.

When a production issue appears after deploy, compare the failing endpoint, timestamp, logs, database status, migration version, and recent commits. Prefer additive migrations and configuration fixes over manual data changes.

RoomBay Assistant should route undocumented operational decisions to the admin runbook or engineering review instead of inventing platform behavior.
