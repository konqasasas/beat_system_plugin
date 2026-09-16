# Repository guidelines

- This repository targets Minecraft Java Edition 26.2 and Spigot 26.2.
- Use `dev.konqasasas` as the Java package and Gradle group namespace. Do not introduce a different project namespace.
- Use Java 25. Keep the Gradle Java toolchain and compiled bytecode on Java 25 unless explicitly instructed otherwise.
- Do not use the Paper API.
- Use the public Spigot/Bukkit API by default.
- Use NMS, CraftBukkit implementation classes, or reflection only when a requirement cannot reasonably be met through the public API. Document the reason when doing so.
- Avoid deprecated APIs whenever a supported alternative exists.
- Before finishing work, run `./gradlew build` (or `gradlew.bat build` on Windows) and resolve failures caused by the change.
- Add tests when a change has behavior that can be tested appropriately.
- Do not modify or delete `run/world`, `run/world_nether`, or `run/world_the_end` without explicit user instructions.
- Separate commands, event listeners, services, and game logic when appropriate. Do not accumulate unrelated behavior in one large class.
- Do not commit generated server files, world data, logs, the Spigot server JAR, or deployed plugin JARs under `run/`.

## Spigot API references

* When a Spigot/Bukkit API detail is uncertain, do not guess from memory. Verify it against the current Spigot 26.2 API before implementing.
* Prefer official sources in the following order:

  1. Spigot Javadocs: https://hub.spigotmc.org/javadocs/spigot/
  2. Spigot source: https://hub.spigotmc.org/stash/projects/SPIGOT/repos/spigot/browse
  3. Spigot plugin development wiki: https://www.spigotmc.org/wiki/spigot-plugin-development/
  4. `plugin.yml` reference when working with plugin metadata, commands, permissions, or dependencies: https://www.spigotmc.org/wiki/plugin-yml/
* Do not assume that an API found in Paper documentation is available in Spigot.
* When using events, methods, classes, enums, `Material`, `EntityType`, or other version-sensitive API, verify that the API exists and is appropriate for Spigot 26.2 when there is any uncertainty.
* If an implementation depends on undocumented or implementation-specific behavior, call that out clearly rather than silently relying on it.
* Compilation success does not prove runtime correctness. For changes involving Bukkit/Spigot behavior, report any relevant checks that should be performed on the real Spigot 26.2 development server.
