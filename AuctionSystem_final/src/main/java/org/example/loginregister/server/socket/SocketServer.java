package org.example.loginregister.server.socket;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * SocketServer — chạy trên máy Server (máy bạn).
 * Lắng nghe kết nối từ các máy Client qua TCP.
 *
 * Cách chạy: gọi SocketServer.start() trong main() hoặc khi khởi động app.
 */
public class SocketServer {

    private static final int PORT = 9999;
    private static boolean running = false;

    public static void start() {
        if (running) return;
        running = true;

        // Chạy server trên thread riêng, không block UI
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Server started, listening on port " + PORT);

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress());

                    // Mỗi client xử lý trên 1 thread riêng
                    new Thread(new ClientHandler(clientSocket)).start();
                }

            } catch (Exception e) {
                System.err.println("Server error: " + e.getMessage());
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();
    }

    public static void stop() {
        running = false;
    }
}
