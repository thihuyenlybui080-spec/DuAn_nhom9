package org.example.loginregister.client.socket;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * SocketClient — dùng chung cho tất cả Controller phía Client.
 *
 * Thay thế hoàn toàn DatabaseConfig ở phía client.
 * Client KHÔNG kết nối MySQL trực tiếp nữa.
 *
 * Cách dùng:
 *   String response = SocketClient.send("LOGIN|username|password");
 *   if (response.startsWith("OK")) { ... }
 */
public class SocketClient {

    // ✅ SỬA thành IP máy Server (máy cài MySQL)
    private static final String SERVER_IP   = "192.168.1.13";
    private static final int    SERVER_PORT = 9999;

    /**
     * Gửi 1 request tới server, nhận 1 dòng response.
     * @param request chuỗi lệnh, vd: "LOGIN|admin|123456"
     * @return response từ server, vd: "OK|Nguyen Van A|BIDDER"
     */
    public static String send(String request) {
        try (
                Socket socket           = new Socket(SERVER_IP, SERVER_PORT);
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        ) {
            out.println(request);
            return in.readLine();

        } catch (Exception e) {
            return "ERROR|Unable to connect to server, please try again!";
        }
    }
}
