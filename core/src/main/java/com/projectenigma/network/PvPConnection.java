package com.projectenigma.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * One TCP connection, wrapped for sending/receiving Java objects. Shared by
 * {@link PvPServer} (host side) and {@link PvPClient} (guest side) so the
 * actual socket/stream handling exists in exactly one place.
 *
 * <h2>Why the output stream is created before the input stream</h2>
 * {@code ObjectOutputStream}'s constructor writes a small stream header
 * immediately. {@code ObjectInputStream}'s constructor <em>blocks</em>
 * until it has read the peer's header. If both ends of a socket try to
 * construct their {@code ObjectInputStream} first, each side blocks
 * waiting for a header the other side hasn't written yet -- a deadlock.
 * The fix is the same on both ends: construct and flush the output stream
 * first, then construct the input stream. That is exactly the order below,
 * and it's why both {@link PvPServer} and {@link PvPClient} go through
 * this one class instead of each managing streams themselves.
 *
 * <p>Deliberately package-private -- this is plumbing, not part of the
 * public network API.
 */
final class PvPConnection implements AutoCloseable {

    interface Listener {
        /** Called on this connection's own reader thread -- never the render thread. */
        void onReceived(Object payload);

        /** Called on the reader thread when the peer closes or a read fails. Not called after a local {@link #close()}. */
        void onClosed();
    }

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Thread readerThread;
    private volatile boolean closing;

    PvPConnection(Socket socket, Listener listener) throws IOException {
        this.socket = socket;
        socket.setTcpNoDelay(true); // small, latency-sensitive turn messages; no benefit to Nagle coalescing
        this.out = new ObjectOutputStream(socket.getOutputStream());
        out.flush(); // see class Javadoc: must happen before the peer's ObjectInputStream is constructed
        this.in = new ObjectInputStream(socket.getInputStream());
        this.readerThread = new Thread(() -> readLoop(listener), "pvp-connection-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop(Listener listener) {
        try {
            while (!closing) {
                Object payload = in.readObject();
                listener.onReceived(payload);
            }
        } catch (IOException | ClassNotFoundException exception) {
            // Expected on a normal close (the blocking readObject() unblocks
            // with an exception once the socket closes); only worth telling
            // the listener about it if nobody asked for this connection to
            // close.
            if (!closing) {
                listener.onClosed();
            }
        }
    }

    /** Thread-safe: callable from any thread, but in practice only ever called from the render thread. */
    synchronized void send(Object payload) throws IOException {
        out.writeObject(payload);
        out.flush();
        // Prevents the output stream's internal object cache from growing
        // across a whole match's worth of independent PvPBattleState/packet
        // objects; harmless since every payload sent here is freshly
        // constructed rather than a mutated, previously-sent instance.
        out.reset();
    }

    boolean isOpen() {
        return !closing && !socket.isClosed() && socket.isConnected();
    }

    @Override
    public void close() {
        closing = true;
        try {
            socket.close(); // unblocks the reader thread's readObject() call
        } catch (IOException ignored) {
            // Already closed or closing; nothing meaningful to do.
        }
    }
}
