# Staticpast build of minecraft-prometheus-exporter

This fork builds the exporter for the SuegoFaults Minecraft server (https://github.com/Staticpast/SuegoFaults). The `staticpast` branch is an upstream release tag plus the small patches below. Nothing else changes.

## Patches on top of the upstream tag

- `jetty.version` 12.0.16 -> 12.0.41. 12.0.16 has open advisories: CVE-2026-2332, CVE-2025-11143, CVE-2026-8384, CVE-2026-6790, CVE-2026-10051 and CVE-2026-1605.
- `json-smart` 2.6.1 in `dependencyManagement`. `json-path` 2.9.0 brings json-smart 2.5.0, which has CVE-2024-57699.
- `org.mockito.version` 5.15.2 -> 5.20.0. The Byte Buddy in 5.15.2 does not support Java 25, and SuegoFaults builds on Java 25. Tests only: the jar does not change.

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
sha256sum target/minecraft-prometheus-exporter-<version>.jar
cp target/minecraft-prometheus-exporter-<version>.jar minecraft-prometheus-exporter-<version>-staticpast.jar
gh release create v<version>-staticpast.<n> minecraft-prometheus-exporter-<version>-staticpast.jar \
  --target staticpast --title "..." --notes "Minecraft up to: <version>"
```

The first line of the release notes must be `Minecraft up to: <version>`. SuegoFaults reads it to know which Minecraft versions a release supports. The exporter does not check the Minecraft version, so use the newest version that SuegoFaults runs or tests. The asset name ends in `-staticpast.jar`.

SuegoFaults pulls the release asset with `mise run plugins:third-party` (`minecraft-config/third-party.toml`).
