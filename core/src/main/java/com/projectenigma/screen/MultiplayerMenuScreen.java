package com.projectenigma.screen;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.projectenigma.ProjectEnigmaGame;
import com.projectenigma.Palette;
import com.projectenigma.UiRenderer;

import java.io.IOException;

/**
 * "Main Menu -&gt; Multiplayer -&gt; Host or Join" per the Phase 1 UI flow.
 * Once hosting starts or a connection is initiated, this screen just shows
 * status text; {@code ProjectEnigmaGame}'s PvP listeners handle the actual
 * transition onward to {@code ClassSelectScreen} once a connection lands
 * (see ProjectEnigmaGame.hostPvPMatch/joinPvPMatch) -- this screen does not
 * need to poll anything itself, because {@code PvPServer}/{@code PvPClient}
 * schedule their callbacks via {@code Gdx.app.postRunnable}, which libGDX
 * runs before the next frame regardless of which screen is active.
 */
public final class MultiplayerMenuScreen extends AbstractGameScreen {
    private enum Mode { MENU, HOSTING_WAIT, JOIN_INPUT, JOIN_CONNECTING }

    private static final String[] OPTIONS = {"Host", "Join", "Back"};

    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private Mode mode = Mode.MENU;
    private int selected;
    private final StringBuilder ipInput = new StringBuilder("127.0.0.1");
    private String notice = "";

    public MultiplayerMenuScreen(ProjectEnigmaGame game) {
        super(game);
        camera = new OrthographicCamera();
        viewport = new FitViewport(UiRenderer.WIDTH, UiRenderer.HEIGHT, camera);
        useInput(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                return switch (mode) {
                    case MENU -> handleMenuInput(keycode);
                    case JOIN_INPUT -> handleJoinInput(keycode);
                    case HOSTING_WAIT, JOIN_CONNECTING -> handleWaitingInput(keycode);
                };
            }
        });
    }

    private boolean handleMenuInput(int keycode) {
        if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
            selected = Math.floorMod(selected - 1, OPTIONS.length);
            return true;
        }
        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
            selected = (selected + 1) % OPTIONS.length;
            return true;
        }
        if (keycode == Input.Keys.ESCAPE) {
            game.showMenu();
            return true;
        }
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
            switch (selected) {
                case 0 -> startHosting();
                case 1 -> {
                    mode = Mode.JOIN_INPUT;
                    notice = "";
                }
                case 2 -> game.showMenu();
                default -> throw new IllegalStateException("Unknown menu item");
            }
            return true;
        }
        return false;
    }

    /** Used by Lwjgl3Launcher's {@code --host} command-line flag (see DESIGN.md, "Setup instructions"). */
    public static MultiplayerMenuScreen autoHost(ProjectEnigmaGame game) {
        MultiplayerMenuScreen screen = new MultiplayerMenuScreen(game);
        screen.startHosting();
        return screen;
    }

    /** Used by Lwjgl3Launcher's {@code --connect <ip>} command-line flag. */
    public static MultiplayerMenuScreen autoJoin(ProjectEnigmaGame game, String hostAddress) {
        MultiplayerMenuScreen screen = new MultiplayerMenuScreen(game);
        screen.ipInput.setLength(0);
        screen.ipInput.append(hostAddress);
        game.joinPvPMatch(hostAddress);
        screen.mode = Mode.JOIN_CONNECTING;
        return screen;
    }

    private void startHosting() {
        try {
            game.hostPvPMatch();
            mode = Mode.HOSTING_WAIT;
            notice = "";
        } catch (IOException exception) {
            notice = "Could not start hosting: " + exception.getMessage();
        }
    }

    private boolean handleJoinInput(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            mode = Mode.MENU;
            return true;
        }
        if (keycode == Input.Keys.BACKSPACE) {
            if (ipInput.length() > 0) {
                ipInput.deleteCharAt(ipInput.length() - 1);
            }
            return true;
        }
        if (keycode == Input.Keys.PERIOD || keycode == Input.Keys.NUMPAD_DOT) {
            ipInput.append('.');
            return true;
        }
        if (keycode >= Input.Keys.NUM_0 && keycode <= Input.Keys.NUM_9) {
            ipInput.append((char) ('0' + (keycode - Input.Keys.NUM_0)));
            return true;
        }
        if (keycode == Input.Keys.ENTER) {
            if (ipInput.length() == 0) {
                notice = "Enter the host's IP address.";
                return true;
            }
            game.joinPvPMatch(ipInput.toString());
            mode = Mode.JOIN_CONNECTING;
            notice = "";
            return true;
        }
        return false;
    }

    private boolean handleWaitingInput(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            if (mode == Mode.JOIN_CONNECTING && game.pvpClient() != null) {
                game.pvpClient().cancelPendingConnection();
            }
            game.leavePvPMatch();
            return true;
        }
        return false;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Palette.VOID);
        viewport.apply();
        camera.update();

        game.batch().setProjectionMatrix(camera.combined);
        game.batch().setColor(1f, 1f, 1f, 1f);
        game.batch().begin();
        game.batch().draw(game.assets().menuBackground(), 0f, 0f, UiRenderer.WIDTH, UiRenderer.HEIGHT);
        game.batch().end();

        ShapeRenderer shapes = game.shapes();
        shapes.setProjectionMatrix(camera.combined);
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.04f, 0.07f, 0.10f, 0.34f);
        shapes.rect(0f, 0f, UiRenderer.WIDTH, UiRenderer.HEIGHT);
        UiRenderer.panel(shapes, 365f, 130f, 550f, 480f, Palette.PANEL);
        shapes.setColor(Palette.ACCENT);
        shapes.rect(365f, 600f, 550f, 10f);
        shapes.setColor(Palette.BLUE);
        shapes.rect(365f, 130f, 7f, 470f);
        if (mode == Mode.MENU) {
            for (int i = 0; i < OPTIONS.length; i++) {
                float y = 400f - i * 66f;
                shapes.setColor(i == selected ? Palette.PANEL_LIGHT : Palette.WALL);
                shapes.rect(465f, y, 350f, 48f);
                if (i == selected) {
                    shapes.setColor(Palette.ACCENT);
                    shapes.rect(465f, y, 6f, 48f);
                }
            }
        }
        shapes.end();
        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);

        game.batch().setProjectionMatrix(camera.combined);
        game.batch().begin();
        UiRenderer.centeredText(game.batch(), game.titleFont(), "MULTIPLAYER", 640f, 580f, Palette.TEXT);
        switch (mode) {
            case MENU -> {
                for (int i = 0; i < OPTIONS.length; i++) {
                    UiRenderer.centeredText(game.batch(), game.mediumFont(), OPTIONS[i], 640f, 434f - i * 66f, Palette.TEXT);
                }
                UiRenderer.centeredText(game.batch(), game.font(), "W/S: select    Enter: confirm    Esc: back",
                        640f, 220f, Palette.MUTED);
            }
            case JOIN_INPUT -> {
                UiRenderer.centeredText(game.batch(), game.mediumFont(), "Host IP address:", 640f, 420f, Palette.TEXT);
                UiRenderer.centeredText(game.batch(), game.titleFont(), ipInput + "_", 640f, 350f, Palette.ACCENT);
                UiRenderer.centeredText(game.batch(), game.font(),
                        "Digits and . to edit    Backspace: delete    Enter: connect    Esc: cancel",
                        640f, 250f, Palette.MUTED);
            }
            case HOSTING_WAIT -> {
                UiRenderer.centeredText(game.batch(), game.mediumFont(),
                        "Hosting on port " + ProjectEnigmaGame.PVP_DEFAULT_PORT, 640f, 420f, Palette.TEXT);
                UiRenderer.centeredText(game.batch(), game.font(),
                        "Share your LAN IP address with the other player.", 640f, 385f, Palette.MUTED);
                UiRenderer.centeredText(game.batch(), game.mediumFont(), "Waiting for an opponent...", 640f, 330f, Palette.ACCENT);
                UiRenderer.centeredText(game.batch(), game.font(), "Esc: cancel", 640f, 260f, Palette.MUTED);
            }
            case JOIN_CONNECTING -> {
                UiRenderer.centeredText(game.batch(), game.mediumFont(), "Connecting to " + ipInput + "...", 640f, 400f, Palette.TEXT);
                UiRenderer.centeredText(game.batch(), game.font(), "Retrying automatically every 3 seconds.", 640f, 360f, Palette.MUTED);
                UiRenderer.centeredText(game.batch(), game.font(), "Esc: cancel", 640f, 260f, Palette.MUTED);
            }
        }
        if (!notice.isEmpty()) {
            UiRenderer.centeredText(game.batch(), game.font(), notice, 640f, 150f, Palette.DANGER);
        }
        game.batch().end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
