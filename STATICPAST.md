# Staticpast build of minecraft-prometheus-exporter

This fork builds the exporter for the SuegoFaults Minecraft server (https://github.com/Staticpast/SuegoFaults). The `staticpast` branch is an upstream release tag plus the small patches below. Nothing else changes.

## Patches on top of the upstream tag

- `jetty.version` 12.0.16 -> 12.1.15. 12.0.16 has open advisories: CVE-2026-2332, CVE-2025-11143, CVE-2026-8384, CVE-2026-6790, CVE-2026-10051 and CVE-2026-1605.
- `json-path.version` 2.9.0 -> 3.0.0. json-path 2.9.0 brings json-smart 2.5.0, which has CVE-2024-57699. json-path 3.0.0 brings json-smart 2.6.0.
- `org.mockito.version` 5.15.2 -> 5.20.0. The Byte Buddy in 5.15.2 does not support Java 25, and SuegoFaults builds on Java 25. Tests only: the jar does not change.
- `CompressionHandler` with `GzipCompression` in place of `GzipHandler`, which Jetty 12.1 marks for removal.
- `paper-api.version` 1.20.4 -> 26.3, Java release 17 -> 25, Kotlin 2.1.10 -> 2.4.21. SuegoFaults runs Paper 26.3 on Java 25, and the build then finds API changes. The jar needs Java 25. Villager types and professions are registry values in 26.3, so the tests get empty registries from `EmptyRegistryAccess`.
- Upstream PR #319, cherry-picked: the tick duration metrics report NaN before the first tick, not `/ by zero` (upstream issue #310).
- Player names come from the user cache, and each name is read once. For an offline player, `getName()` reads the player data file on the main thread, and a scrape then stalls the server (upstream issue #314).

Keep one commit per patch. Then a rebase drops a patch that upstream already has, or you drop its commit.

The plugin version stays the upstream release number.

## Follow upstream

```bash
git fetch upstream --tags
git rebase --onto <new upstream tag> <old upstream tag> staticpast
# Re-check each patch: drop it when upstream already has the fix.
```

## Build and release

```bash
mvn -q package
cp target/minecraft-prometheus-exporter-<version>.jar minecraft-prometheus-exporter-<version>-staticpast.jar
gh release create v<version>-staticpast.<n> minecraft-prometheus-exporter-<version>-staticpast.jar \
  --target staticpast --title "<title>" --notes "Minecraft up to: <version>"
```

The first line of the release notes must be `Minecraft up to: <version>`. SuegoFaults reads it to know which Minecraft versions a release supports. The exporter does not check the Minecraft version, so use the newest version that SuegoFaults runs or tests. The asset name ends in `-staticpast.jar`.

SuegoFaults pulls the release asset with `mise run plugins:third-party` (`minecraft-config/third-party.toml`).
