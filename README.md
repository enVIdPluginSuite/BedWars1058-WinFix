# BedWars1058-WinFix

Addon for [BedWars1058](https://github.com/andrei1058/BedWars1058) that fixes the end-of-game sidebar
showing raw `{winnerTeamName}`, `{topPlayerDisplayName}`, `{topValue}` ... text.

When a game ends, the addon rebuilds every player's end-game sidebar with the real state of the match:

* **Winner** placeholders (`{winnerTeamColor}`, `{winnerTeamName}`, `{winnerTeamLetter}`) use the actual winning team.
* **Top rows** (`{topTeamColor}`, `{topTeamName}`, `{topPlayerName}`, `{topPlayerDisplayName}`, `{topValue}`,
  `{topValue-<statistic>}`) are filled from the match statistics, one row per player.
  * Fewer players than rows in your language file: unused rows are removed.
  * More players than rows: the last row is repeated while the sidebar has room (max 15 lines).
* If BedWars1058's statistics holder is missing or empty, the addon falls back to its own kill counter.
* The sidebar is re-applied if BedWars1058 rebuilds it while the arena is restarting (player join / leave).

Your language file does not need to change; the addon reads the same `sidebar.<group>.restarting.*` lines.

## Install

Drop `BedWars1058-WinFix-<version>.jar` into `plugins/` and restart. The config is created at `plugins/BedWars1058/Addons/WinFix/config.yml`.
Works on any server version BedWars1058 itself runs on (built against the Bukkit 1.8.8 API, no NMS).

## Build

```
./gradlew build
```

The jar is written to `build/libs/`. Requires JDK 11+ to run Gradle.

The BedWars1058 API is resolved in this order:

1. `-PbedwarsApiJar=/path/to/jar`
2. any jar in `libs/` (for example your server's `BedWars1058-<version>.jar`)
3. `com.andrei1058.bedwars:bedwars-api:25.2` from `https://repo.andrei1058.com/releases/`

Kotlin 2.2.21, Java 8 bytecode, Kotlin runtime relocated to `dev.etran.bedwarswinfix.libs.kotlin`.

## Config

See that `config.yml` (`order-by`, `hide-zero-values`, `expand-rows`, `max-sidebar-lines`, `refresh-delay-ticks`, `debug`).

## Developer note
This plugin is coded with AI assistance and architected completely by me. Regardless, use at your own risk.

## License
This plugin is under the MIT License, check the [License](LICENSE) file for more details.
