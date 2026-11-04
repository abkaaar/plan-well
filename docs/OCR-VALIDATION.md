# OCR validation checklist (TextExtractor)

Use a **debug** build so Logcat tag `TextExtractor` prints `fullText` and block counts.

## Screenshots to attach / share

For each image: open extraction review, confirm Logcat text, and check that dates / times / venues appear as parser candidates when present.

1. **Instagram event flyer** — title, date, venue survive.
2. **X (Twitter) post** — body text and any date-like tokens survive.
3. **WhatsApp forward** — forwarded message text readable (not only UI chrome).
4. **Photo of a printed poster** — headline + date/venue despite perspective.
5. **Dense text screenshot** — multi-line body; top-to-bottom block order preserved.

## Automated coverage

- Unit: `TextExtractorTest` (fake recognizer — order, confidence mean, failure → empty).
- Instrumentation: `TextExtractorInstrumentedTest` with `androidTest/assets/ocr_sample.png`
  (`PlanWell OCR Fixture`, `15 March 2026`, `City Hall`).
