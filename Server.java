
package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Spec items 11, 13, 14: start/stop the server, handle client connections and
 * client requests. Can be started and stopped repeatedly (see ServerGUI), or
 * run headless through main().
 */
public class Server {

    public static final int PORT = 5000;

    private ServerSocket serverSocket;
    private ExecutorService clientPool;
    private Thread acceptThread;
    private final Set<Socket> openClients = ConcurrentHashMap.newKeySet();
    private volatile boolean running = false;

    public boolean isRunning() {
        return running;
    }

    /**
     * Opens the port and starts accepting clients in the background. Returns
     * as soon as the server is listening; throws if the database or the port
     * is not available so the caller can show a clear message.
     */
    public synchronized void start() throws IOException {
        if (running) return;

        // Fail fast with a clear message if MySQL isn't reachable.
        DatabaseManager startupCheck = new DatabaseManager();
        boolean dbOk = startupCheck.isConnected();
        startupCheck.disconnect();
        if (!dbOk) {
            throw new IOException("Cannot connect to the database. Is MySQL running and is schema.sql imported?");
        }

        serverSocket = new ServerSocket(PORT);
        clientPool = Executors.newCachedThreadPool();
        running = true;
        System.out.println("[Server] Started on port " + PORT);

        acceptThread = new Thread(this::acceptLoop, "iwish-accept");
        acceptThread.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] Client connected: " + clientSocket.getInetAddress());
                openClients.add(clientSocket);
                clientPool.execute(() -> {
                    try {
                        new ClientHandler(clientSocket).run();
                    } finally {
                        openClients.remove(clientSocket);
                    }
                });
            } catch (IOException e) {
                if (running) {
                    System.err.println("[Server] Error accepting client: " + e.getMessage());
                }
                // if !running, this exception is expected (socket closed by stop())
            }
        }
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            System.err.println("[Server] Error closing server socket: " + e.getMessage());
        }
        // Drop every connected client too, so "Stop" really stops everything.
        for (Socket s : openClients) {
            try { s.close(); } catch (IOException ignored) { }
        }
        openClients.clear();
        if (clientPool != null) clientPool.shutdownNow();
        System.out.println("[Server] Stopped.");
    }

    /** Headless mode (no window). Use ServerGUI for the Start/Stop screen. */
    public static void main(String[] args) {
        Server server = new Server();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("[Server] Could not start: " + e.getMessage());
        }
    }
}
