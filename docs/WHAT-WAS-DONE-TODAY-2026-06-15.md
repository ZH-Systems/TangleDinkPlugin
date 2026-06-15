# What Was Done Today - 2026-06-15

## Documentation

- Updated `README.md` to explicitly explain how this project differs from the original Dink plugin
- Added a dedicated section describing the fork-specific changes:
  - Tangle Crew rebrand/package identity
  - Clan-event-focused workflow
  - Sister-site integration
  - Remote clan event commands and polling
  - Clan-event webhook override behavior
  - In-client clan event overlay support

## Notes

- This change was documentation-only
- No plugin behavior or source logic was changed in this session

## Audit

- Reviewed the repo for forbidden patterns:
  - Java reflection
  - JNI / native library loading
  - External process execution
  - Runtime downloading/loading of executable code
- Result:
  - Reflection usage was identified and then removed from the audited code paths
  - No JNI/native loading patterns were found
  - No external process execution patterns were found
  - Network fetches exist for JSON/config data, but no runtime code download/classloading patterns were found

## Remediation

- Removed reflection-based config import/export handling from `SettingsManager`
- Reworked config migration reads to use raw stored config strings instead of reflective type metadata
- Replaced `TypeToken`-based JSON parsing in the main plugin paths with class-based or manual JSON parsing
- Removed reflective test setup from `SettingsManagerImportTest`
- Removed remaining explicit `TypeToken` usages from:
  - `RemoteEventManager`
  - `Utils`
  - `ItemSearcher`
  - `AbstractRarityService`
  - `RarityCalculator`

## Verification Notes

- A source search now returns no explicit `java.lang.reflect`, `ReflectiveOperationException`, `getMethod(...)`, or `TypeToken` usage under `src`
- `./gradlew test` could not be fully validated on this machine because the installed JDK is `jdk-25`, while this project targets Java 11 and Lombok crashes during compilation under JDK 25 before javac reaches project code
