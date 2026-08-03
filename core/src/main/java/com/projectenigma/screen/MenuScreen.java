package com.projectenigma.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.projectenigma.ProjectEnigmaGame;
import com.projectenigma.Palette;
import com.projectenigma.UiRenderer;

public final class MenuScreen extends AbstractGameScreen {
    private static final String[] OPTIONS = {"New Game", "Continue", "Multiplayer", "Controls", "Quit"};
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private int selected;
    private boolean controlsVisible;
    private String notice = "";

    public MenuScreen(ProjectEnigmaGame game) {
        super(game);
        camera = new OrthographicCamera();
        viewport = new FitViewport(UiRenderer.WIDTH, UiRenderer.HEIGHT, camera);
        useInput(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (controlsVisible) {
                    if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.ENTER
                            || keycode == Input.Keys.SPACE) {
                        controlsVisible = false;
                    }
                    return true;
                }
                if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
                    selected = Math.floorMod(selected - 1, OPTIONS.length);
                    notice = "";
                    return true;
                }
                if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
                    selected = (selected + 1) % OPTIONS.length;
                    notice = "";
                    return true;
                }
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                    activateSelection();
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    game.quit();
                    return true;
                }
                return false;
            }
        });
    }

    private void activateSelection() {
        switch (selected) {
            case 0 -> game.showClassSelect();
            case 1 -> {
                if (!game.continueGame()) {
                    notice = "No valid save was found. Start a new game first.";
                }
            }
            case 2 -> game.showMultiplayerMenu();
            case 3 -> controlsVisible = true;
            case 4 -> game.quit();
            default -> throw new IllegalStateException("Unknown menu item");
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Palette.VOID);
        viewport.apply();
        camera.update();

        drawBackground();
        drawMenu();
        if (controlsVisible) {
            drawControls();
        }
    }

    private void drawBackground() {
        game.batch().setProjectionMatrix(camera.combined);
        game.batch().setColor(1f, 1f, 1f, 1f);
        game.batch().begin();
        game.batch().draw(game.assets().menuBackground(), 0f, 0f, UiRenderer.WIDTH, UiRenderer.HEIGHT);
        game.batch().end();

        ShapeRenderer shapes = game.shapes();
        shapes.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.04f, 0.07f, 0.10f, 0.16f);
        shapes.rect(0f, 0f, UiRenderer.WIDTH, UiRenderer.HEIGHT);
        shapes.setColor(0.11f, 0.15f, 0.19f, 0.88f);
        shapes.rect(0f, 0f, UiRenderer.WIDTH, 58f);
        shapes.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    private void drawMenu() {
        ShapeRenderer shapes = game.shapes();
        shapes.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiRenderer.panel(shapes, 60f, 66f, 520f, 590f, Palette.PANEL);
        shapes.setColor(Palette.ACCENT);
        shapes.rect(60f, 646f, 520f, 10f);
        shapes.setColor(Palette.BLUE);
        shapes.rect(60f, 66f, 8f, 580f);
        for (int i = 0; i < OPTIONS.length; i++) {
            float y = 408f - i * 64f;
            shapes.setColor(i == selected ? Palette.PANEL_LIGHT : Palette.WALL);
            shapes.rect(108f, y, 424f, 48f);
            if (i == selected) {
                shapes.setColor(Palette.ACCENT);
                shapes.rect(108f, y, 7f, 48f);
            }
        }
        shapes.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);

        game.batch().setProjectionMatrix(camera.combined);
        game.batch().begin();
        UiRenderer.centeredText(game.batch(), game.titleFont(), ProjectEnigmaGame.DISPLAY_NAME,
                320f, 590f, Palette.TEXT);
        UiRenderer.centeredText(game.batch(), game.font(), "UTOPIA PROTOCOL // PROCEDURAL RPG", 320f, 546f, Palette.BLUE_LIGHT);
        for (int i = 0; i < OPTIONS.length; i++) {
            boolean unavailable = i == 1 && !game.saves().hasSave();
            UiRenderer.centeredText(game.batch(), game.mediumFont(), OPTIONS[i], 320f,
                    442f - i * 64f, unavailable ? Palette.MUTED : Palette.TEXT);
        }
        UiRenderer.centeredText(game.batch(), game.font(), "W/S or arrows: select    Enter: confirm    Esc: quit",
                640f, 34f, Palette.TEXT);
        if (!notice.isEmpty()) {
            UiRenderer.centeredText(game.batch(), game.font(), notice, 320f, 94f, Palette.DANGER);
        }
        game.batch().end();
    }

    private void drawControls() {
        ShapeRenderer shapes = game.shapes();
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(0f, 0f, UiRenderer.WIDTH, UiRenderer.HEIGHT);
        UiRenderer.panel(shapes, 260f, 115f, 760f, 500f, Palette.PANEL_LIGHT);
        shapes.setColor(Palette.ACCENT);
        shapes.rect(260f, 605f, 760f, 10f);
        shapes.end();

        game.batch().begin();
        UiRenderer.centeredText(game.batch(), game.titleFont(), "KEYBOARD CONTROLS", 640f, 560f, Palette.TEXT);
        String controls = "EXPLORE\n"
                + "WASD / Arrow Keys  -  Move\n"
                + "E / Enter          -  Use the stairs\n"
                + "I / Tab            -  Inventory\n"
                + "P                  -  Drink a potion\n"
                + "Esc                -  Pause\n\n"
                + "COMBAT\n"
                + "W/S / Arrow Keys   -  Select an action\n"
                + "1-5                -  Action hotkeys\n"
                + "Enter / Space      -  Confirm";
        UiRenderer.wrappedText(game.batch(), game.mediumFont(), controls, 360f, 500f, 570f, Palette.TEXT);
        UiRenderer.centeredText(game.batch(), game.font(), "Press Enter or Esc to return", 640f, 145f, Palette.MUTED);
        game.batch().end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
