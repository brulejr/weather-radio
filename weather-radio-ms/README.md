# Weather Radio Phase 1

Phase 1 stub service for an offline NOAA Weather Radio processing pipeline.

## What this includes

- Spring Boot 3.5 + Kotlin 2.2 project
- Stubbed radio, SAME, and transcript domain model
- Seeded in-memory demo data at startup
- REST endpoints for report, radio status, SAME, and transcript
- Basic controller test

## Run

```bash
./gradlew bootRun
```

Or if you do not yet have a Gradle wrapper installed locally:

```bash
gradle bootRun
```

## Endpoints

- `GET /api/weather/report`
- `GET /api/weather/radio/status`
- `GET /api/weather/radio/latest-same`
- `GET /api/weather/radio/latest-transcript`
- `GET /actuator/health`

## Notes

This is a stubbed phase intended to validate the REST contract before adding:

1. live SDR capture
2. SAME decoding
3. offline speech transcription
4. forecast cleanup and fusion
