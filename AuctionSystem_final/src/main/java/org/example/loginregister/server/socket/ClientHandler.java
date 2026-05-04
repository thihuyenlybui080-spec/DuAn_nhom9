package org.example.loginregister.server.socket;

import org.example.loginregister.server.db.DatabaseConfig;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * ClientHandler — xử lý từng client kết nối vào server.
 *
 * Giao thức đơn giản (text-based):
 *   Client gửi:  "LỆNH|param1|param2|..."
 *   Server trả:  "OK|dữ liệu" hoặc "ERROR|lý do"
 *
 * Các lệnh hiện tại:
 *   LOGIN|username|password
 *   REGISTER|username|password|email|fullName|gender|phone|role
 */
public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        ) {
            String request;
            while ((request = in.readLine()) != null) {
                System.out.println("Received: " + request);
                String response = handleRequest(request);
                out.println(response);
            }
        } catch (Exception e) {
            System.err.println("ClientHandler error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private String handleRequest(String request) {
        String[] parts = request.split("\\|");
        if (parts.length == 0) return "ERROR|Invalid request";

        switch (parts[0]) {
            case "LOGIN":    return handleLogin(parts);
            case "REGISTER": return handleRegister(parts);
            default:         return "ERROR|Unknown command";
        }
    }

    // ========== LOGIN ==========
    // Client gửi: LOGIN|username|password
    // Server trả: OK|fullName|role  hoặc  ERROR|lý do
    private String handleLogin(String[] parts) {
        if (parts.length < 3) return "ERROR|Missing parameters";

        String username = parts[1];
        String password = parts[2];

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT full_name, role FROM users WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String fullName = rs.getString("full_name");
                String role     = rs.getString("role");
                return "OK|" + fullName + "|" + role;
            } else {
                return "ERROR|Incorrect username or password!";
            }

        } catch (Exception e) {
            return "Unable to connect to server, please try again!";
        }
    }

    // ========== REGISTER ==========
    // Client gửi: REGISTER|username|password|email|fullName|gender|phone|role
    // Server trả: OK|Registration successful!  hoặc  ERROR|lý do
    private String handleRegister(String[] parts) {
        if (parts.length < 8) return "ERROR|Missing parameters";

        String username = parts[1];
        String password = parts[2];
        String email    = parts[3];
        String fullName = parts[4];
        String gender   = parts[5];
        String phone    = parts[6];
        String role     = parts[7];

        try (Connection conn = DatabaseConfig.getConnection()) {

            // Kiểm tra username trùng
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                return "ERROR|Username already exists, please choose another!";
            }

            // INSERT
            String insertSql = "INSERT INTO users (username, password, email, full_name, gender, phone, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setString(1, username);
            insertStmt.setString(2, password);
            insertStmt.setString(3, email);
            insertStmt.setString(4, fullName);
            insertStmt.setString(5, gender);
            insertStmt.setString(6, phone);
            insertStmt.setString(7, role);
            insertStmt.executeUpdate();

            return "OK|Registration successful!";

        } catch (Exception e) {
            return "Unable to connect to server, please try again!";
        }
    }
}
