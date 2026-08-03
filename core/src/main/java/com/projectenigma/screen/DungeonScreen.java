package com.projectenigma.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.projectenigma.ProjectEnigmaGame;
import com.projectenigma.Palette;
import com.projectenigma.UiRenderer;
import com.projectenigma.UtopiaAssets;
import com.projectenigma.model.DungeonChest;
import com.projectenigma.model.DungeonEnemy;
import com.projectenigma.model.DungeonMap;
import com.projectenigma.model.GameSession;
import com.projectenigma.model.TileType;

import java.util.List;

public final class DungeonScreen extends AbstractGameScreen {
    private static final float WORLD_WIDTH = 20f;
    private static final float WORLD_HEIGHT = 11.25f;
    private static final float HELD_MOVE_DELAY = 0.095f;
    private static final String[] PAUSE_OPTIONS = {"Resume", "Save Game", "Main Menu", "Quit"};

    private final GameSession session;
    private final OrthographicCamera worldCamera;
    private final OrthographicCamera uiCamera;
    private final FitViewport worldViewport;
    private final FitViewport uiViewport;
    private final boolean[][] explored;

    private float renderedPlayerX;
    private float renderedPlayerY;
    private float moveRepeatTimer;
    private boolean inventoryVisible;
    private boolean pauseVisible;
    private int pauseSelection;
    private String notice = "";
    private float noticeTime;
    private float worldAnimationTime;
    private UtopiaAssets.Direction facing = UtopiaAssets.Direction.DOWN;

    public DungeonScreen(ProjectEnigmaGame game, GameSession session) {
        super(game);
        this.session = session;
        DungeonMap map = session.dungeon();
        explored = new boolean[map.width()][map.height()];
        renderedPlayerX = session.playerX + 0.5f;
        renderedPlayerY = session.playerY + 0.5f;

        worldCamera = new OrthographicCamera();
        worldViewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, worldCamera);
        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(UiRenderer.WIDTH, UiRenderer.HEIGHT, uiCamera);

        useInput(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (pauseVisible) {
                    return handlePauseInput(keycode);
                }
                if (inventoryVisible) {
                    return handleInventoryInput(keycode);
                }
                if (keycode == Input.Keys.ESCAPE) {
                    pauseVisible = true;
                    pauseSelection = 0;
                    return true;
                }
                if (keycode == Input.Keys.I || keycode == Input.Keys.TAB) {
                    inventoryVisible = true;
                    return true;
                }
                if (keycode == Input.Keys.P) {
                    drinkPotion();
                    return true;
                }
                if (keycode == Input.Keys.E || keycode == Input.Keys.ENTER) {
                    useCurrentTile();
                    return true;
                }

                int[] direction = directionForKey(keycode);
                if (direction != null) {
                    tryMove(direction[0], direction[1]);
                    moveRepeatTimer = 0.23f;
                    return true;
                }
                return false;
            }
        });
    }

    private boolean handlePauseInput(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            pauseVisible = false;
            return true;
        }
        if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
            pauseSelection = Math.floorMod(pauseSelection - 1, PAUSE_OPTIONS.length);
            return true;
        }
        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
            pauseSelection = (pauseSelection + 1) % PAUSE_OPTIONS.length;
            return true;
        }
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
            switch (pauseSelection) {
                case 0 -> pauseVisible = false;
                case 1 -> {
                    game.saveGame();
                    setNotice("Game saved.");
                    pauseVisible = false;
                }
                case 2 -> game.abandonToMenu();
                case 3 -> game.quit();
                default -> throw new IllegalStateException("Unknown pause item");
            }
            return true;
        }
        return false;
    }

    private boolean handleInventoryInput(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.I || keycode == Input.Keys.TAB) {
            inventoryVisible = false;
            return true;
        }
        if (keycode == Input.Keys.P || keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
            drinkPotion();
            return true;
        }
        return false;
    }

    private void drinkPotion() {
        int before = session.hero.health;
        if (session.hero.usePotion()) {
            setNotice("Potion restores " + (session.hero.health - before) + " HP.");
            game.saveGame();
        } else if (session.hero.potions <= 0) {
            setNotice("No potions remain.");
        } else {
            setNotice("Health is already full.");
        }
    }

    private void useCurrentTile() {
        if (!session.isAtExit()) {
            setNotice("There is nothing to use here.");
            return;
        }
        session.beginNextFloor();
        game.saveGame();
        game.showDungeon();
    }

    private void tryMove(int dx, int dy) {
        facing = directionFromDelta(dx, dy);
        int targetX = session.playerX + dx;
        int targetY = session.playerY + dy;
        if (!session.canMoveTo(targetX, targetY)) {
            return;
        }

        DungeonEnemy enemy = session.enemyAt(targetX, targetY);
        if (enemy != null) {
            game.startCombat(enemy);
            return;
        }

        session.movePlayerTo(targetX, targetY);
        DungeonChest chest = session.chestAt(targetX, targetY);
        if (chest != null && !chest.opened) {
            List<String> loot = session.openChest(chest);
            setNotice(String.join("\n", loot));
            game.saveGame();
        } else if (session.isAtExit()) {
            setNotice("Stairs found. Press E or Enter to descend.");
        } else if (session.stepsTaken % 25 == 0) {
            game.saveGame();
        }
    }

    private static int[] directionForKey(int keycode) {
        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
            return new int[]{-1, 0};
        }
        if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
            return new int[]{1, 0};
        }
        if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
            return new int[]{0, 1};
        }
        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
            return new int[]{0, -1};
        }
        return null;
    }

    private static UtopiaAssets.Direction directionFromDelta(int dx, int dy) {
        if (dx < 0) {
            return UtopiaAssets.Direction.LEFT;
        }
        if (dx > 0) {
            return UtopiaAssets.Direction.RIGHT;
        }
        if (dy > 0) {
            return UtopiaAssets.Direction.UP;
        }
        return UtopiaAssets.Direction.DOWN;
    }

    private void updateHeldMovement(float delta) {
        if (pauseVisible || inventoryVisible) {
            return;
        }
        moveRepeatTimer -= delta;
        if (moveRepeatTimer > 0f) {
            return;
        }

        int key = -1;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            key = Input.Keys.LEFT;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            key = Input.Keys.RIGHT;
        } else if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            key = Input.Keys.UP;
        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            key = Input.Keys.DOWN;
        }
        int[] direction = directionForKey(key);
        if (direction != null) {
            tryMove(direction[0], direction[1]);
            moveRepeatTimer = HELD_MOVE_DELAY;
        }
    }

    private void setNotice(String text) {
        notice = text == null ? "" : text;
        noticeTime = 3.4f;
    }

    @Override
    public void render(float delta) {
        float safeDelta = Math.min(delta, 0.1f);
        worldAnimationTime += safeDelta;
        updateHeldMovement(safeDelta);
        renderedPlayerX = MathUtils.lerp(renderedPlayerX, session.playerX + 0.5f, Math.min(1f, safeDelta * 14f));
        renderedPlayerY = MathUtils.lerp(renderedPlayerY, session.playerY + 0.5f, Math.min(1f, safeDelta * 14f));
        if (noticeTime > 0f) {
            noticeTime -= safeDelta;
        } else {
            notice = "";
        }

        ScreenUtils.clear(Palette.VOID);
        updateWorldCamera();
        drawDungeon();
        drawHud();
        if (inventoryVisible) {
            drawInventory();
        }
        if (pauseVisible) {
            drawPauseMenu();
        }
    }

    private void updateWorldCamera() {
        DungeonMap map = session.dungeon();
        float halfWidth = worldViewport.getWorldWidth() / 2f;
        float halfHeight = worldViewport.getWorldHeight() / 2f;
        float cameraX = MathUtils.clamp(renderedPlayerX, halfWidth, map.width() - halfWidth);
        float cameraY = MathUtils.clamp(renderedPlayerY, halfHeight, map.height() - halfHeight);
        worldCamera.position.set(cameraX, cameraY, 0f);
        worldCamera.update();
        worldViewport.apply();
    }

    private void drawDungeon() {
        DungeonMap map = session.dungeon();
        revealNearbyTiles(map);
        int minX = Math.max(0, (int) (worldCamera.position.x - worldViewport.getWorldWidth() / 2f) - 1);
        int maxX = Math.min(map.width() - 1, (int) (worldCamera.position.x + worldViewport.getWorldWidth() / 2f) + 1);
        int minY = Math.max(0, (int) (worldCamera.position.y - worldViewport.getWorldHeight() / 2f) - 1);
        int maxY = Math.min(map.height() - 1, (int) (worldCamera.position.y + worldViewport.getWorldHeight() / 2f) + 1);

        game.batch().setProjectionMatrix(worldCamera.combined);
        game.batch().begin();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (!explored[x][y]) {
                    continue;
                }
                boolean visible = isVisible(x, y);
                if (visible) {
                    game.batch().setColor(1f, 1f, 1f, 1f);
                } else {
                    game.batch().setColor(0.30f, 0.35f, 0.40f, 1f);
                }
                if (map.tileAt(x, y) == TileType.FLOOR) {
                    game.batch().draw(game.assets().floorTile(x, y), x, y, 1f, 1f);
                } else {
                    game.batch().draw(game.assets().wallTile(x, y,
                                    map.tileAt(x, y + 1) == TileType.FLOOR,
                                    map.tileAt(x, y - 1) == TileType.FLOOR,
                                    map.tileAt(x + 1, y) == TileType.FLOOR,
                                    map.tileAt(x - 1, y) == TileType.FLOOR),
                            x, y, 1f, 1f);
                }
            }
        }
        game.batch().setColor(1f, 1f, 1f, 1f);
        drawExit(map);
        drawChests();
        drawEnemies();
        drawPlayer();
        game.batch().end();
    }

    private void revealNearbyTiles(DungeonMap map) {
        int radius = 8;
        for (int x = Math.max(0, session.playerX - radius); x <= Math.min(map.width() - 1, session.playerX + radius); x++) {
            for (int y = Math.max(0, session.playerY - radius); y <= Math.min(map.height() - 1, session.playerY + radius); y++) {
                int dx = x - session.playerX;
                int dy = y - session.playerY;
                if (dx * dx + dy * dy <= radius * radius) {
                    explored[x][y] = true;
                }
            }
        }
    }

    private boolean isVisible(int x, int y) {
        int dx = x - session.playerX;
        int dy = y - session.playerY;
        return dx * dx + dy * dy <= 64;
    }

    private void drawExit(DungeonMap map) {
        int x = map.exit().x();
        int y = map.exit().y();
        if (!explored[x][y]) {
            return;
        }
        if (!isVisible(x, y)) {
            game.batch().setColor(0.30f, 0.35f, 0.40f, 1f);
        }
        game.batch().draw(game.assets().stairsDownTile(), x, y, 1f, 1f);
        game.batch().setColor(1f, 1f, 1f, 1f);
    }

    private void drawChests() {
        for (DungeonChest chest : session.chests) {
            if (!explored[chest.x][chest.y]) {
                continue;
            }
            if (!isVisible(chest.x, chest.y)) {
                game.batch().setColor(0.30f, 0.35f, 0.40f, 1f);
            }
            game.batch().draw(game.assets().chestTile(chest.opened), chest.x, chest.y, 1f, 1f);
            game.batch().setColor(1f, 1f, 1f, 1f);
        }
    }

    private void drawEnemies() {
        for (DungeonEnemy enemy : session.enemies) {
            if (!enemy.isAlive() || !isVisible(enemy.x, enemy.y)) {
                continue;
            }
            game.batch().draw(game.assets().worldEnemyFrame(enemy.type, worldAnimationTime),
                    enemy.x, enemy.y, 1f, 1.5f);
        }
    }

    private void drawPlayer() {
        boolean moving = Math.abs(renderedPlayerX - (session.playerX + 0.5f)) > 0.015f
                || Math.abs(renderedPlayerY - (session.playerY + 0.5f)) > 0.015f;
        game.batch().draw(game.assets().worldHeroFrame(session.hero.heroClass, facing, moving, worldAnimationTime),
                renderedPlayerX - 0.5f, renderedPlayerY - 0.5f, 1f, 1.5f);
    }

    private void drawHud() {
        uiViewport.apply();
        uiCamera.update();
        ShapeRenderer shapes = game.shapes();
        shapes.setProjectionMatrix(uiCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiRenderer.panel(shapes, 20f, 575f, 390f, 125f, Palette.PANEL);
        UiRenderer.panel(shapes, 930f, 620f, 330f, 80f, Palette.PANEL);
        UiRenderer.bar(shapes, 142f, 654f, 240f, 18f, session.hero.health, session.hero.maxHealth, Palette.HEALTH);
        UiRenderer.bar(shapes, 142f, 622f, 240f, 18f, session.hero.mana, session.hero.maxMana, Palette.MANA);
        UiRenderer.bar(shapes, 142f, 590f, 240f, 14f, session.hero.experience,
                session.hero.experienceForNextLevel(), Palette.XP);
        shapes.end();

        game.batch().setProjectionMatrix(uiCamera.combined);
        game.batch().begin();
        UiRenderer.text(game.batch(), game.mediumFont(), session.hero.heroClass.displayName() + "  Lv " + session.hero.level,
                36f, 687f, Palette.TEXT);
        UiRenderer.text(game.batch(), game.font(), "HP  " + session.hero.health + "/" + session.hero.maxHealth,
                36f, 670f, Palette.TEXT);
        UiRenderer.text(game.batch(), game.font(), "EN  " + session.hero.mana + "/" + session.hero.maxMana,
                36f, 638f, Palette.TEXT);
        UiRenderer.text(game.batch(), game.font(), "XP  " + session.hero.experience + "/" + session.hero.experienceForNextLevel(),
                36f, 605f, Palette.TEXT);
        UiRenderer.text(game.batch(), game.mediumFont(), "Dungeon Floor " + session.floorNumber, 954f, 682f, Palette.TEXT);
        UiRenderer.text(game.batch(), game.font(), "Enemies " + session.enemies.size() + "    Gold " + session.hero.gold
                + "    Potions " + session.hero.potions, 954f, 646f, Palette.MUTED);
        UiRenderer.centeredText(game.batch(), game.font(), "Move: WASD/arrows   Inventory: I/Tab   Potion: P   Pause: Esc",
                640f, 27f, Palette.MUTED);
        if (!notice.isEmpty()) {
            UiRenderer.centeredText(game.batch(), game.mediumFont(), notice, 640f, 94f, Palette.TEXT);
        }
        game.batch().end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawInventory() {
        ShapeRenderer shapes = game.shapes();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(0f, 0f, UiRenderer.WIDTH, UiRenderer.HEIGHT);
        UiRenderer.panel(shapes, 330f, 145f, 620f, 430f, Palette.PANEL_LIGHT);
        shapes.setColor(Palette.ACCENT);
        shapes.rect(330f, 565f, 620f, 10f);
        UiRenderer.bar(shapes, 510f, 352f, 330f, 22f, session.hero.health, session.hero.maxHealth, Palette.HEALTH);
        UiRenderer.bar(shapes, 510f, 305f, 330f, 22f, session.hero.mana, session.hero.maxMana, Palette.MANA);
        shapes.end();

        game.batch().begin();
        UiRenderer.centeredText(game.batch(), game.titleFont(), "INVENTORY", 640f, 525f, Palette.TEXT);
        UiRenderer.text(game.batch(), game.mediumFont(), session.hero.heroClass.displayName() + " - Level " + session.hero.level,
                415f, 455f, Palette.TEXT);
        UiRenderer.text(game.batch(), game.font(), "Attack: " + session.hero.attack + "    Defense: " + session.hero.defense,
                415f, 416f, Palette.MUTED);
        UiRenderer.text(game.batch(), game.font(), "Health", 415f, 370f, Palette.TEXT);
        UiRenderer.text(game.batch(), game.font(), "Mana", 415f, 323f, Palette.TEXT);
        UiRenderer.text(game.batch(), game.mediumFont(), "Potions: " + session.hero.potions, 415f, 260f, Palette.GOLD);
        UiRenderer.text(game.batch(), game.mediumFont(), "Gold: " + session.hero.gold, 690f, 260f, Palette.GOLD);
        UiRenderer.centeredText(game.batch(), game.font(), "P / Enter: use potion    I / Tab / Esc: close",
                640f, 180f, Palette.MUTED);
        game.batch().end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawPauseMenu() {
        ShapeRenderer shapes = game.shapes();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.75f);
        shapes.rect(0f, 0f, UiRenderer.WIDTH, UiRenderer.HEIGHT);
        UiRenderer.panel(shapes, 450f, 150f, 380f, 440f, Palette.PANEL_LIGHT);
        shapes.setColor(Palette.ACCENT);
        shapes.rect(450f, 580f, 380f, 10f);
        for (int i = 0; i < PAUSE_OPTIONS.length; i++) {
            float y = 375f - i * 60f;
            shapes.setColor(i == pauseSelection ? Palette.PANEL_LIGHT : Palette.WALL);
            shapes.rect(500f, y, 280f, 44f);
            if (i == pauseSelection) {
                shapes.setColor(Palette.ACCENT);
                shapes.rect(500f, y, 6f, 44f);
            }
        }
        shapes.end();

        game.batch().begin();
        UiRenderer.centeredText(game.batch(), game.titleFont(), "PAUSED", 640f, 535f, Palette.TEXT);
        for (int i = 0; i < PAUSE_OPTIONS.length; i++) {
            UiRenderer.centeredText(game.batch(), game.mediumFont(), PAUSE_OPTIONS[i], 640f,
                    407f - i * 60f, Palette.TEXT);
        }
        UiRenderer.centeredText(game.batch(), game.font(), "Esc: resume", 640f, 178f, Palette.MUTED);
        game.batch().end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height, false);
        uiViewport.update(width, height, true);
    }
}
