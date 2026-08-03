package com.projectenigma.network;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

/**
 * Thin seam between this package's background network threads and libGDX's
 * main/render thread.
 *
 * <p>In the running game, {@code Gdx.app} is set once the libGDX
 * {@code Application} has called {@code create()}, and
 * {@link Application#postRunnable} marshals a {@code Runnable} onto the
 * render thread while {@link Application#log} writes to the platform log.
 *
 * <p>Off-render-thread contexts -- JUnit tests such as
 * {@code PvPEndToEndTest}, or any future headless/dedicated server that
 * never creates a libGDX {@code Application} at all -- have no
 * {@code Gdx.app}. Rather than let every caller in {@link PvPServer} and
 * {@link PvPClient} NPE on that, this class falls back to running the task
 * immediately on the calling (network) thread and logging to
 * {@link System#out}. That fallback is safe here specifically because
 * everything this package hands to {@code post} only touches the
 * already-thread-safe {@code ConcurrentLinkedQueue} + listener plumbing in
 * {@link PvPServer}/{@link PvPClient} -- it would not be safe to rely on for
 * direct Scene2D/GL work.
 */
final class MainThreadGateway {
    private MainThreadGateway() {
    }

    static void post(Runnable task) {
        Application app = Gdx.app;
        if (app != null) {
            app.postRunnable(task);
        } else {
            task.run();
        }
    }

    static void log(String tag, String message) {
        Application app = Gdx.app;
        if (app != null) {
            app.log(tag, message);
        } else {
            System.out.println("[" + tag + "] " + message);
        }
    }
}
