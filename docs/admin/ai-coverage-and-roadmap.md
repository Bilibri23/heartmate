# AI Coverage and Roadmap

Admins should use AI coverage analytics to identify gaps in the RoomBay knowledge base. A grounded answer has at least one retrieved citation; an ungrounded answer means no matching allowed document chunk was found for the user's role and question.

High ungrounded volume usually means one of three things: the docs do not cover the workflow, the user asked from the wrong role context, or retrieval did not match the user's wording.

When admins see repeated ungrounded questions, they should add or improve role-appropriate docs, run admin ingest, and retest with tenant, landlord, and admin personas.

Fine-tuning should shape tone, refusal behavior, and role discipline. It should not be used as the main store for RoomBay policies or workflows.

Future AI work should separate ADMIN from SUPER_ADMIN before exposing deeper platform maintenance, security, or incident-response knowledge.
