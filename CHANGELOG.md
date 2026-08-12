# Changelog

## 2026-08-12 — stationary GPS refresh

- Added a foreground location service that owns GPS and network-provider callbacks.
- Requests updates with a zero-metre distance filter so stationary users still receive
  five-second callbacks; upload policy retains the five-second throttle.
- Rejects cached fixes older than 120 seconds before uploading them to the family server.
- Adds build-time `SERVER_BASE_URL` and `MOBILE_APP_TOKEN` settings and sends the
  `X-Mobile-App-Token` header when configured.
- Added JVM coverage for stationary-update configuration, five-second throttling, and
  stale cached-fix rejection.

Files changed: `app/src/main/java/com/example/blindglassesapp/MainActivity.kt`,
`app/src/main/java/com/example/blindglassesapp/network/FamilyEndpoints.kt`,
`app/src/main/java/com/example/blindglassesapp/server/`,
`app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, and
`app/src/test/java/com/example/blindglassesapp/server/`.
