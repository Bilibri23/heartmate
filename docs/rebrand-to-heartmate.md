# Rebrand to HeartMate

This project includes an automated script to rebrand the UI from "Room8" to "HeartMate" and switch the logo.

How to run
- Ensure you have Node.js installed.
- From the repository root, run:
  - `node scripts/rebrand-to-heartmate.mjs`

What it does
- Replaces visible brand text (Room8/ROOM8/room8) with "HeartMate".
- Updates any existing logo references (room8/roombay/etc.) to `/heartmate-logo.svg`.
- Updates any HTML `<title>` tags to "HeartMate".
- Installs `heartmate-logo.svg` into discovered `public/` directories.

Notes
- The script reports how many files were scanned/changed and where the logo was installed.
- If your header/navbar/layout uses a hardcoded asset path, it will be updated to `/heartmate-logo.svg`.
- If any files are missed, search for "Room8" in your codebase and update remaining instances to "HeartMate".

Rollback
- Use your VCS to revert changes if needed or rerun with adjustments.
