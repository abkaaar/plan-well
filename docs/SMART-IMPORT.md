# Smart import (share → review → save)

## Flow

1. System share opens `ShareReceiverActivity`.
2. `SmartImportPipeline` OCR / text → entities → classify → `ImportedEntry`.
3. User reviews editable kind / title / date / notes / chips, then Save.
4. `SaveImportedEntryUseCase` creates a Task and schedules a reminder when due is in the future.

## Network

- **Only** optional Open Graph link previews (`OgMetadataExtractor` + jsoup).
- Toggle: **Settings → Fetch link previews** (default on).
- Airplane mode: image/text import still works; URL shares skip OG with no error dialog.

## Manual acceptance

- Instagram event flyer → review → Save → task on Home + reminder.
- Plain text “pay rent by the 5th” → TASK with date.
- Airplane mode image share end-to-end; URL share no error toast for OG.
