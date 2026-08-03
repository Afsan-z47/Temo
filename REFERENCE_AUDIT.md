# Reference project source audit

Reference: [hdescottes/GdxGame](https://github.com/hdescottes/GdxGame)

This was a source-level review of the supplied archive and the public repository.
The repository's README already reports five visible problems: incorrect item
stacking, a broken menu-to-game fade-in, drag selection offset, occasional HUD
spacing, and inconsistent position reset when closing the window. The review
below found the underlying causes plus additional lifecycle and gameplay faults.

## Defects found

| Severity | Area | Finding in the reference source | Resolution in PROJECT Enigma |
| --- | --- | --- | --- |
| Critical | Shutdown | `GdxGame.dispose()` calls `gameScreen.dispose()` even when no game has been created, which can throw a null-pointer exception when the app closes from the main menu. | Screen disposal is null-safe and centralized. |
| Critical | Screen lifecycle | `TransitionScreen.dispose()` disposes both the old and next screens. The next screen may still be active or reused, so this risks disposing live stages and textures. | A single screen-switch method owns disposal of only the screen being replaced. |
| Critical | Battle lifecycle | `BattleScreen.dispose()` also disposes the shared `PlayerHUD`, although the same HUD is reused after returning to the map. | Screens do not own or dispose another screen's reusable state. Gameplay state lives in `GameSession`. |
| High | Battle escape | The successful-run branch calls the same `removeEntities()` method as victory, so escaping removes the enemy from the map. | Escape returns to the dungeon and leaves the living enemy in place with its current HP. |
| High | Enemy state | Battle creates a fresh enemy from `EntityFactory` rather than battling the exact map entity. This separates battle HP/state from the encountered instance. | Combat receives and mutates the exact `DungeonEnemy` selected on the map. |
| High | Inventory use | Consumable handling calls both `removeActor(item)` and `remove(item)`. The second path decrements the count again even though the actor was already removed. | Inventory is an explicit numeric model; one potion use performs one decrement. |
| High | Inventory capacity | `doesInventoryHaveSpace()` increments the loop index inside the `else` and again in the `for` loop, skipping every second slot. The duplication exists in map and battle inventory UIs. | Capacity is derived from model values; there is no actor-tree slot counting. |
| High | Map safety | Missing TMX layers are logged, but constructor helpers later iterate `spawnsLayer` and other nullable layers. A malformed map can therefore still crash. | Generated maps are validated data structures with no optional collision layer. |
| Medium | Fade-in | `FadeInTransitionEffect` renders the next screen before its `show()` setup has run; the call to `next.show()` is commented out. This explains the reported broken transition. | The rebuild uses direct, deterministic screen changes with no pre-show rendering. |
| Medium | Input state | `PlayerInputComponent.clear()` clears movement, quit, and option flags but omits `INTERACT`, allowing interaction input to remain stuck across a battle transition. | Input is event-based per screen; no static key-state map survives a transition. |
| Medium | Drag offset | Drag positioning uses coordinates from the item actor while the containing stage may be scaled. This can separate the rendered drag actor from the pointer. | Inventory is fully keyboard-driven and does not rely on scaled drag coordinates. |
| Medium | Entity removal | `BattleScreen.removeEntities()` mutates the entity array during an enhanced loop and matches by shared entity ID; repeated IDs can remove the wrong or multiple entities. | Enemies have stable unique IDs and are removed through an iterator after victory. |
| Medium | Window close/save | Save behavior is conditional on whether `Esc` happens to be pressed during the save notification, so window-close and keyboard-exit paths produce different position results. | Both paths call the same save routine and preserve the exact dungeon position. |
| Low | Observer cleanup | Several `removeAllObservers()` methods remove from the same array being iterated, which can skip observers. | The rebuild avoids observer collections; screen input is attached and detached explicitly. |
| Low | Keyboard coverage | Exploration has WASD/arrows, but main menus, inventory, conversations, and battle actions primarily depend on Scene2D click/drag listeners. | Every screen and overlay has arrows/WASD, confirm, back, and action hotkeys. |

## Architectural constraints in the reference

- World logic is tightly coupled to hand-authored TMX layer names, portal object
  names, and two fixed map types. Replacing fields with generated dungeons would
  otherwise require synthetic TMX layers or invasive map-manager changes.
- Collision, selection, encounters, rendering, and save notifications communicate
  through string messages and shared singletons, making state ownership difficult
  to verify.
- The supplied map art is acknowledged as originating from a commercial GBA
  title. PROJECT Enigma therefore uses original runtime-drawn presentation and no
  reference map/music assets.

## Rebuild decision

Rather than placing a procedural generator inside the nullable TMX-layer path,
PROJECT Enigma separates pure Java rules from libGDX rendering:

- `DungeonGenerator` and `DungeonMap` own generation and reachability.
- `GameSession` owns the exact hero, floor, enemy, chest, and save state.
- `BattleEngine` resolves one explicit action at a time without UI dependencies.
- Screens render that state and own only their keyboard processor and viewport.

This directly addresses the reviewed failure modes while keeping the reference
game's intended RPG loop.
