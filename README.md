# PROJECT Enigma

PROJECT Enigma is a desktop Java/libGDX RPG inspired by the gameplay loop of
[hdescottes/GdxGame](https://github.com/hdescottes/GdxGame). It keeps the
top-down exploration, class selection, inventory, progression, profiles, and
turn-based battles while replacing fixed outdoor TMX maps with deterministic
procedural dungeons. Every menu and gameplay action can be completed with a
keyboard; a mouse is not required. LAN multiplayer supports a host and one
guest in host-authoritative turn-based battles.

The project intentionally does not redistribute the reference game's Sword of
Mana maps, music, or sprite assets. It now includes an original utopian
pixel-art presentation built around white/off-white floors, dark graphite wall
architecture, cool grey, signal red, and electric blue.
See `SCI_FI_ENEMY_ART.md` for the enemy roster, compatibility mapping, and art prompt.

## Implemented gameplay

- Seeded room-and-corridor dungeon generation with guaranteed connectivity
- A distant, reachable staircase on every floor
- Fog of exploration, collision, camera following, chests, loot, and enemies
- Five sci-fi operatives—Sentinel, Hacker, Sniper, Enforcer, and Bio-Medic—with distinct statistics and tech skills
- Turn-based Attack, Tech Skill, Guard, Potion, and Run actions
- Enemy scaling, bosses every fifth floor, experience, levels, gold, and drops
- Four utopian sci-fi enemy archetypes: Recon Drone, Aegis Robot, Helix Cyborg, and Enhanced Warden
- Inventory/status overlay and field potion use
- New game, continue, automatic saves, manual saves, and safe window-close saves
- Complete keyboard control across menus, exploration, overlays, and combat
- LAN Host/Join flow with reconnect handling and synchronized PvP state
- 64x64 utopian dungeon tiles with dark graphite walls and red/blue procedural accent variation
- Four-direction idle/walk animation sheets for all heroes and enemies
- Idle, attack, skill, guard, hurt, and defeat battle animations
- Matching 1280x720 rooftop menu and atrium battle backgrounds
- Pure Java model tests for dungeon connectivity, placement, and combat rules

## Requirements

- JDK 17 or newer
- An internet connection the first time Gradle downloads libGDX dependencies

No separate libGDX SDK installation is needed. Gradle retrieves libGDX for the
project.

## Run the game

Open a terminal in the project root.

### Windows

Command Prompt:

```bat
gradlew.bat lwjgl3:run
```

PowerShell or the default VS Code terminal:

```powershell
.\gradlew.bat lwjgl3:run
```

Run that command from **Command Prompt, PowerShell, or the VS Code terminal**
while the terminal is open in the `PROJECT Enigma` folder. Do not double-click
`gradlew.bat`; it is Gradle's command wrapper and a double-clicked window closes
as soon as the process ends. If you prefer to double-click a file, use
`RUN_GAME_WINDOWS.bat` instead. It runs the correct task and pauses if a build
error occurs so the full message remains visible.

### macOS or Linux

```bash
./gradlew lwjgl3:run
```

The Gradle run task automatically adds `-XstartOnFirstThread` on macOS.

### VS Code

1. Install **Extension Pack for Java**.
2. Open this entire `PROJECT Enigma` folder, not only `core` or `lwjgl3`.
3. Allow VS Code to import the Gradle project.
4. Run the Gradle task `lwjgl3 > application > run`, or use the terminal command
   above.

## Build a runnable jar

```bash
./gradlew lwjgl3:dist
```

The jar is created at `lwjgl3/build/libs/PROJECT-Enigma.jar`.

Run it on Windows/Linux with:

```bash
java -jar lwjgl3/build/libs/PROJECT-Enigma.jar
```

On macOS, use:

```bash
java -XstartOnFirstThread -jar lwjgl3/build/libs/PROJECT-Enigma.jar
```

## Keyboard controls

### Menus

| Action | Keys |
| --- | --- |
| Select | `W`/`S` or arrow keys |
| Confirm | `Enter` or `Space` |
| Back | `Esc` |

### Dungeon exploration

| Action | Keys |
| --- | --- |
| Move | `WASD` or arrow keys |
| Descend stairs | `E` or `Enter` while on the stairs |
| Inventory | `I` or `Tab` |
| Drink potion | `P` |
| Pause/save/menu | `Esc` |

### Combat

| Action | Keys |
| --- | --- |
| Select action | `W`/`S` or arrow keys |
| Confirm | `Enter` or `Space` |
| Direct action | Number keys `1` through `5` |
| Attempt to run | `Esc` |

## Saves

The save is stored at `save/project-enigma-save.json`, relative to the project
working directory. The game saves after important loot, victories, floor
changes, manual saves, periodic movement, and normal window closure. A corrupt
save is rejected safely instead of crashing the game. Saves from the earlier
build are detected and migrated automatically when loaded.

## Artwork and animation

All runtime artwork is under `assets/utopia`. `UtopiaAssets.java` owns the
textures for the lifetime of the game, applies nearest-neighbour filtering,
selects sprite frames, creates opponent-facing regions once during loading,
and disposes the shared textures on shutdown. Screens never allocate textures
per frame or per screen transition.

The dungeon uses the 64x64 tile atlas and the battle screens use 128x192 frame
grids. The Gradle `lwjgl3:dist` task embeds the complete `assets` directory in
the runnable jar.

## Tests

Run the normal Gradle tests:

```bash
./gradlew test
```

The dependency-free logic smoke test is also available:

```bash
./verify-logic.sh
```

It checks 250 generated dungeon seeds for full connectivity and also exercises
session population and lethal combat resolution.

## Project structure

```text
core/
  src/main/java/com/dungeonrpg/
    model/       Pure dungeon, session, hero, enemy, and battle rules
    screen/      Menu, class selection, dungeon, combat, and game-over screens
    UtopiaAssets.java
    ProjectEnigmaGame.java
assets/
  utopia/         Tiles, backgrounds, hero/enemy sheets, and manifests
lwjgl3/
  src/main/java/com/dungeonrpg/lwjgl3/
    Lwjgl3Launcher.java
REFERENCE_AUDIT.md
verify-logic.sh
```

See [REFERENCE_AUDIT.md](REFERENCE_AUDIT.md) for the source review and the bugs
that informed this implementation. See [ART_INTEGRATION.md](ART_INTEGRATION.md)
for the exact atlas, animation, screen, and packaging mappings.

## License

GPL-3.0. The reference repository is also GPL-3.0; attribution is retained here
because this project was built from its design and source review.
