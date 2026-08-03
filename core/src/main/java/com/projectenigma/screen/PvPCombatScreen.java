package com.projectenigma.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.projectenigma.ProjectEnigmaGame;
import com.projectenigma.Palette;
import com.projectenigma.UiRenderer;
import com.projectenigma.UtopiaAssets;
import com.projectenigma.model.BattleAction;
import com.projectenigma.model.HeroClass;
import com.projectenigma.network.HeroSnapshot;
import com.projectenigma.network.MatchStatus;
import com.projectenigma.network.PvPBattleState;
import com.projectenigma.network.PvPClient;
import com.projectenigma.network.PvPMatch;
import com.projectenigma.network.PvPOutcome;
import com.projectenigma.network.PvPServer;

import java.util.ArrayList;
import java.util.List;

/**
 * LAN PvP screen with host-authoritative turn handling and a shared utopian
 * pixel-art presentation. Rendering is deliberately kept separate from the
 * network branches so art and animation never change battle authority.
 *
 * <p>Created two ways:
 * <ul>
 *   <li>{@link #forHost} -- the machine running {@code PvPServer} owns the
 *       authoritative {@code PvPMatch} and drives it both from local input
 *       and from action packets relayed by {@code PvPServer}.</li>
 *   <li>{@link #forGuest} -- the joining machine owns no match state at
 *       all. It renders whatever {@code PvPBattleState} it last received
 *       and, on its turn, only ever sends a {@code PvPActionPacket} and
 *       waits.</li>
 * </ul>
 * Both roles share one class because the rendering, turn indicator, and
 * input handling are otherwise identical -- only "where does the next
 * state come from" differs, which is exactly what {@link #submitAction}
 * and the two listener implementations below isolate.
 */
public final class PvPCombatScreen extends AbstractGameScreen
        implements PvPServer.EventListener, PvPClient.EventListener {

    private static final int MAX_LOG_LINES = 6;
    private static final float ACTION_ANIMATION_DURATION = 1.05f;

    private final boolean isHost;
    private final int localPlayerIndex; // 0 = host, 1 = guest
    private final PvPMatch match; // non-null only for the host
    private final BattleAction[] actions = BattleAction.values();
    private final List<String> logLines = new ArrayList<>();
    private final OrthographicCamera camera;
    private final FitViewport viewport;

    private PvPBattleState state;
    private int selected;
    private boolean disconnectedLocally; // guest-only: true between onDisconnected() and the next onStateReceived()
    private float time;
    private float actionAnimationTime = ACTION_ANIMATION_DURATION;
    private int animatedActor = -1;
    private BattleAction animatedAction = BattleAction.ATTACK;

    private PvPCombatScreen(ProjectEnigmaGame game, boolean isHost, PvPMatch match, PvPBattleState initialState) {
        super(game);
        this.isHost = isHost;
        this.localPlayerIndex = isHost ? 0 : 1;
        this.match = match;
        this.state = initialState;
        addLog("The match begins. " + (isHost ? "You are Player 1." : "You are Player 2."));

        camera = new OrthographicCamera();
        viewport = new FitViewport(UiRenderer.WIDTH, UiRenderer.HEIGHT, camera);

        useInput(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (actionAnimationTime < ACTION_ANIMATION_DURATION) {
                    return true;
                }
                if (state.status() == MatchStatus.FINISHED) {
                    if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE || keycode == Input.Keys.ESCAPE) {
                        game.leavePvPMatch();
                    }
                    return true;
                }
                if (state.status() == MatchStatus.WAITING_FOR_RECONNECT || disconnectedLocally) {
                    if (keycode == Input.Keys.ESCAPE) {
                        abandon();
                    }
                    return true;
                }
                if (state.currentTurn() != localPlayerIndex) {
                    return false; // not our turn; only Esc-during-reconnect and end-of-match are handled above
                }
                if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
                    selected = Math.floorMod(selected - 1, actions.length);
                    return true;
                }
                if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
                    selected = (selected + 1) % actions.length;
                    return true;
                }
                if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_5) {
                    selected = keycode - Input.Keys.NUM_1;
                    submitAction(actions[selected]);
                    return true;
                }
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                    submitAction(actions[selected]);
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    submitAction(BattleAction.RUN); // RUN = surrender, same keybind as PvE's "try to run"
                    return true;
                }
                return false;
            }
        });
    }

    public static PvPCombatScreen forHost(ProjectEnigmaGame game, PvPMatch match) {
        return new PvPCombatScreen(game, true, match, match.currentState());
    }

    public static PvPCombatScreen forGuest(ProjectEnigmaGame game, PvPBattleState initialState) {
        return new PvPCombatScreen(game, false, null, initialState);
    }

    // ---- action submission -------------------------------------------------

    private void submitAction(BattleAction action) {
        startActionAnimation(localPlayerIndex, action);
        if (isHost) {
            applyState(match.applyAction(localPlayerIndex, action));
            game.pvpServer().broadcast(state);
        } else {
            game.pvpClient().sendAction(action);
            // No local mutation: wait for the host's broadcast via onStateReceived.
        }
    }

    private void abandon() {
        if (isHost) {
            applyState(match.abandon());
            game.pvpServer().broadcast(state);
        } else {
            game.pvpClient().sendAbandon();
        }
        game.leavePvPMatch();
    }

    private void applyState(PvPBattleState newState) {
        this.state = newState;
        for (String line : newState.log()) {
            addLog(line);
        }
    }

    private void addLog(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        logLines.add(message);
        while (logLines.size() > MAX_LOG_LINES) {
            logLines.remove(0);
        }
    }

    // ---- PvPServer.EventListener (host only ever receives these) ----------

    @Override
    public void onGuestConnected(boolean isReconnect) {
        if (isReconnect) {
            PvPBattleState resumed = match.resume();
            applyState(resumed);
            game.pvpServer().broadcast(resumed);
        }
    }

    @Override
    public void onGuestDisconnected() {
        applyState(match.pauseForDisconnect());
        // No one to broadcast to; the host's own screen reflects the pause immediately.
    }

    @Override
    public void onClassSelected(HeroClass guestClass) {
        // Late/duplicate class-select packets after the match has already
        // started are simply ignored.
    }

    @Override
    public void onActionReceived(BattleAction action) {
        startActionAnimation(1, action);
        applyState(match.applyAction(1, action)); // guest is always player 1
        game.pvpServer().broadcast(state);
    }

    @Override
    public void onAbandon() {
        applyState(match.abandon());
        game.pvpServer().broadcast(state);
    }

    // ---- PvPClient.EventListener (guest only ever receives these) ---------

    @Override
    public void onConnected(boolean isReconnect) {
        disconnectedLocally = false;
        // The host immediately re-broadcasts state on reconnect (see
        // onGuestConnected above); our view updates via onStateReceived.
    }

    @Override
    public void onDisconnected() {
        disconnectedLocally = true;
        addLog("Connection to host lost. Reconnecting...");
    }

    @Override
    public void onStateReceived(PvPBattleState newState) {
        disconnectedLocally = false;
        if (state != null) {
            int actor = state.currentTurn();
            if ((actor == 0 || actor == 1)
                    && (actor != animatedActor || actionAnimationTime >= ACTION_ANIMATION_DURATION)) {
                startActionAnimation(actor, inferAction(state, newState, actor));
            }
        }
        applyState(newState);
    }

    private void startActionAnimation(int actor, BattleAction action) {
        animatedActor = actor;
        animatedAction = action == null ? BattleAction.ATTACK : action;
        actionAnimationTime = 0f;
    }

    private static BattleAction inferAction(PvPBattleState before, PvPBattleState after, int actor) {
        HeroSnapshot oldActor = actor == 0 ? before.player0() : before.player1();
        HeroSnapshot newActor = actor == 0 ? after.player0() : after.player1();
        HeroSnapshot oldDefender = actor == 0 ? before.player1() : before.player0();
        HeroSnapshot newDefender = actor == 0 ? after.player1() : after.player0();

        if (newActor.mana() < oldActor.mana()) {
            return BattleAction.SKILL;
        }
        for (String line : after.log()) {
            String lower = line.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("potion")) {
                return BattleAction.POTION;
            }
            if (lower.contains("brace")) {
                return BattleAction.GUARD;
            }
            if (lower.contains("escape")) {
                return BattleAction.RUN;
            }
        }
        if (newDefender.health() < oldDefender.health()) {
            return BattleAction.ATTACK;
        }
        return BattleAction.GUARD;
    }

    // ---- rendering ----------------------------------------------------------

    @Override
    public void render(float delta) {
        float safeDelta = Math.min(delta, 0.1f);
        time += safeDelta;
        actionAnimationTime = Math.min(ACTION_ANIMATION_DURATION, actionAnimationTime + safeDelta);
        // Belt-and-suspenders: postRunnable already guarantees delivery before
        // the next frame, but draining here too is harmless and cheap.
        if (isHost && game.pvpServer() != null) {
            game.pvpServer().drainIncoming();
        } else if (!isHost && game.pvpClient() != null) {
            game.pvpClient().drainIncoming();
        }

        ScreenUtils.clear(Palette.VOID);
        viewport.apply();
        camera.update();

        drawArena();
        drawCombatants();
        drawInterface();
    }

    private void drawArena() {
        game.batch().setProjectionMatrix(camera.combined);
        game.batch().setColor(1f, 1f, 1f, 1f);
        game.batch().begin();
        game.batch().draw(game.assets().battleBackground(), 0f, 0f, UiRenderer.WIDTH, UiRenderer.HEIGHT);
        game.batch().end();
    }

    private void drawCombatants() {
        HeroSnapshot me = isHost ? state.player0() : state.player1();
        HeroSnapshot opponent = isHost ? state.player1() : state.player0();
        boolean myTurn = state.currentTurn() == localPlayerIndex;

        UtopiaAssets.BattlePose myPose = playerDefeated(localPlayerIndex)
                ? UtopiaAssets.BattlePose.DEFEAT : UtopiaAssets.BattlePose.IDLE;
        UtopiaAssets.BattlePose opponentPose = playerDefeated(1 - localPlayerIndex)
                ? UtopiaAssets.BattlePose.DEFEAT : UtopiaAssets.BattlePose.IDLE;
        float myFrameTime = time;
        float opponentFrameTime = time;

        if (actionAnimationTime < ACTION_ANIMATION_DURATION && animatedActor >= 0) {
            boolean localActs = animatedActor == localPlayerIndex;
            UtopiaAssets.BattlePose actionPose = poseFor(animatedAction);
            UtopiaAssets.BattlePose reactionPose = (animatedAction == BattleAction.ATTACK
                    || animatedAction == BattleAction.SKILL)
                    ? UtopiaAssets.BattlePose.HURT : UtopiaAssets.BattlePose.IDLE;
            if (localActs) {
                myPose = actionPose;
                myFrameTime = actionAnimationTime;
                opponentPose = playerDefeated(1 - localPlayerIndex)
                        ? UtopiaAssets.BattlePose.DEFEAT : reactionPose;
                opponentFrameTime = actionAnimationTime;
            } else {
                opponentPose = actionPose;
                opponentFrameTime = actionAnimationTime;
                myPose = playerDefeated(localPlayerIndex)
                        ? UtopiaAssets.BattlePose.DEFEAT : reactionPose;
                myFrameTime = actionAnimationTime;
            }
        }

        game.batch().setProjectionMatrix(camera.combined);
        game.batch().setColor(1f, 1f, 1f, 1f);
        game.batch().begin();
        game.batch().draw(game.assets().battleHeroFrame(me.heroClass(), myPose, myFrameTime, false),
                184f, 205f, 192f, 288f);
        game.batch().draw(game.assets().battleHeroFrame(opponent.heroClass(), opponentPose, opponentFrameTime, true),
                904f, 205f, 192f, 288f);
        game.batch().end();

        ShapeRenderer shapes = game.shapes();
        shapes.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiRenderer.panel(shapes, 100f, 470f, 390f, 125f, Palette.PANEL);
        UiRenderer.panel(shapes, 790f, 470f, 390f, 125f, Palette.PANEL);
        shapes.setColor(myTurn ? Palette.BLUE_LIGHT : Palette.BLUE);
        shapes.rect(100f, 585f, 390f, 10f);
        shapes.setColor(!myTurn ? Palette.ACCENT : Palette.WALL_EDGE);
        shapes.rect(790f, 585f, 390f, 10f);
        shapes.setColor(myTurn ? Palette.BLUE_LIGHT : Palette.WALL_EDGE);
        shapes.rect(205f, 200f, 150f, 7f);
        shapes.setColor(!myTurn ? Palette.ACCENT : Palette.WALL_EDGE);
        shapes.rect(925f, 200f, 150f, 7f);
        UiRenderer.bar(shapes, 125f, 520f, 340f, 24f, me.health(), me.maxHealth(), Palette.HEALTH);
        UiRenderer.bar(shapes, 125f, 486f, 340f, 18f, me.mana(), me.maxMana(), Palette.MANA);
        UiRenderer.bar(shapes, 815f, 520f, 340f, 24f, opponent.health(), opponent.maxHealth(), Palette.HEALTH);
        UiRenderer.bar(shapes, 815f, 486f, 340f, 18f, opponent.mana(), opponent.maxMana(), Palette.MANA);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        game.batch().setProjectionMatrix(camera.combined);
        game.batch().begin();
        UiRenderer.text(game.batch(), game.mediumFont(), "You (" + me.heroClass().displayName() + ")  Lv " + me.level(),
                125f, 575f, myTurn ? Palette.ACCENT : Palette.TEXT);
        UiRenderer.text(game.batch(), game.font(), "HP " + me.health() + "/" + me.maxHealth()
                + "   EN " + me.mana() + "/" + me.maxMana(), 125f, 558f, Palette.MUTED);
        UiRenderer.text(game.batch(), game.mediumFont(), "Opponent (" + opponent.heroClass().displayName() + ")  Lv " + opponent.level(),
                815f, 575f, !myTurn ? Palette.ACCENT : Palette.TEXT);
        UiRenderer.text(game.batch(), game.font(), "HP " + opponent.health() + "/" + opponent.maxHealth()
                + "   EN " + opponent.mana() + "/" + opponent.maxMana(), 815f, 558f, Palette.MUTED);
        game.batch().end();
    }

    private boolean playerDefeated(int playerIndex) {
        return (playerIndex == 0 && state.outcome() == PvPOutcome.GUEST_WINS)
                || (playerIndex == 1 && state.outcome() == PvPOutcome.HOST_WINS);
    }

    private static UtopiaAssets.BattlePose poseFor(BattleAction action) {
        return switch (action) {
            case ATTACK -> UtopiaAssets.BattlePose.ATTACK;
            case SKILL, POTION -> UtopiaAssets.BattlePose.SKILL;
            case GUARD, RUN -> UtopiaAssets.BattlePose.GUARD;
        };
    }

    private void drawInterface() {
        ShapeRenderer shapes = game.shapes();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiRenderer.panel(shapes, 22f, 18f, 490f, 170f, Palette.PANEL);
        UiRenderer.panel(shapes, 530f, 18f, 728f, 170f, Palette.PANEL);
        boolean canAct = state.status() == MatchStatus.IN_PROGRESS && state.currentTurn() == localPlayerIndex;
        if (canAct) {
            for (int i = 0; i < actions.length; i++) {
                float x = 37f + (i % 3) * 155f;
                float y = i < 3 ? 105f : 42f;
                shapes.setColor(i == selected ? Palette.PANEL_LIGHT : Palette.WALL);
                shapes.rect(x, y, 145f, 48f);
                if (i == selected) {
                    shapes.setColor(Palette.ACCENT);
                    shapes.rect(x, y, 6f, 48f);
                }
            }
        }
        shapes.end();

        game.batch().begin();
        if (canAct) {
            for (int i = 0; i < actions.length; i++) {
                float centerX = 109.5f + (i % 3) * 155f;
                float y = i < 3 ? 136f : 73f;
                UiRenderer.centeredText(game.batch(), game.font(), (i + 1) + "  " + actions[i].label(), centerX, y, Palette.TEXT);
            }
            UiRenderer.text(game.batch(), game.font(), actions[selected].description(), 550f, 174f, Palette.MUTED);
        } else {
            UiRenderer.centeredText(game.batch(), game.mediumFont(), statusHeadline(), 267f, 105f, Palette.MUTED);
        }

        float logY = 145f;
        for (String line : logLines) {
            UiRenderer.text(game.batch(), game.font(), line, 550f, logY, Palette.TEXT);
            logY -= 19f;
        }

        if (state.status() == MatchStatus.FINISHED) {
            UiRenderer.text(game.batch(), game.mediumFont(), outcomeHeadline(), 877f, 38f, Palette.GOLD);
        } else if (state.status() == MatchStatus.WAITING_FOR_RECONNECT || disconnectedLocally) {
            UiRenderer.text(game.batch(), game.font(), "Waiting for reconnection...    Esc: abandon", 550f, 34f, Palette.DANGER);
        } else {
            UiRenderer.text(game.batch(), game.font(), "W/S: select    Enter: act    1-5: hotkey    Esc: run",
                    550f, 34f, Palette.MUTED);
        }
        game.batch().end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private String statusHeadline() {
        if (state.status() == MatchStatus.WAITING_FOR_RECONNECT || disconnectedLocally) {
            return "Connection paused...";
        }
        return "Waiting for opponent...";
    }

    private String outcomeHeadline() {
        boolean iWon = (localPlayerIndex == 0 && state.outcome() == PvPOutcome.HOST_WINS)
                || (localPlayerIndex == 1 && state.outcome() == PvPOutcome.GUEST_WINS);
        if (state.outcome() == PvPOutcome.ABANDONED) {
            return "Match abandoned - press Enter";
        }
        return (iWon ? "Victory! " : "Defeat. ") + "Press Enter to continue";
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
