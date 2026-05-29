package vn.edu.vnu.auction.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử dành cho {@link ConnectionManager}.
 * Sử dụng ServerSocket nội bộ để giả lập kết nối thật, giải quyết triệt để
 * vấn đề block IO của ObjectInputStream.
 */
class ConnectionManagerTest {

    private ConnectionManager manager;
    private ServerSocket dummyServer;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Reset Singleton
        resetSingleton();
        manager = ConnectionManager.getInstance();

        // 2. Mở một Server thật ở một cổng ngẫu nhiên chưa ai dùng (port 0)
        dummyServer = new ServerSocket(0);
        int port = dummyServer.getLocalPort();

        // 3. Ép ConnectionManager kết nối vào Server test này
        Field hostField = ConnectionManager.class.getDeclaredField("serverHost");
        hostField.setAccessible(true);
        hostField.set(manager, "127.0.0.1");

        Field portField = ConnectionManager.class.getDeclaredField("serverPort");
        portField.setAccessible(true);
        portField.set(manager, port);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (manager != null) manager.disconnect();
        if (dummyServer != null && !dummyServer.isClosed()) dummyServer.close();
        if (serverThread != null && serverThread.isAlive()) serverThread.interrupt();
        resetSingleton();
    }

    private void resetSingleton() throws Exception {
        Field instanceField = ConnectionManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    /**
     * Khởi động server ảo để "đón" kết nối từ ConnectionManager.
     */
    private void startDummyServer() {
        serverThread = new Thread(() -> {
            try {
                Socket client = dummyServer.accept();
                // Phải tạo OutputStream trước và flush để Client không bị treo khi tạo InputStream
                ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                oos.flush();
                new ObjectInputStream(client.getInputStream());
            } catch (IOException e) {
                // Bỏ qua lỗi khi server đóng
            }
        });
        serverThread.start();
    }

    @Test
    void testConnect_Success() {
        startDummyServer(); // Bật server ảo

        boolean result = manager.connect();

        assertTrue(result, "Connection should be successful with the real dummy server.");
        assertTrue(manager.isConnected(), "Manager should report as connected.");
        assertNotNull(manager.getOutputStream(), "OutputStream should be initialized.");
        assertNotNull(manager.getInputStream(), "InputStream should be initialized.");
    }

    @Test
    void testConnect_Failure() throws Exception {
        // Đóng sập server ảo ngay trước khi kết nối để tạo lỗi Connection Refused
        dummyServer.close();

        boolean result = manager.connect();

        assertFalse(result, "Connection should return false on failure.");
        assertFalse(manager.isConnected(), "Manager should report as disconnected.");
    }

    @Test
    void testReconnect() {
        startDummyServer(); // Bật server ảo

        boolean result = manager.reconnect();

        assertTrue(result, "Reconnect should succeed when server is available.");
    }

    @Test
    void testGetInstance() {
        ConnectionManager instance1 = ConnectionManager.getInstance();
        ConnectionManager instance2 = ConnectionManager.getInstance();
        assertSame(instance1, instance2, "ConnectionManager must be a singleton.");
    }


    @Test
    void testLoadConfig_ValidExternalYaml() throws Exception {
        // 1. Tạo file YAML tạm thời ở thư mục gốc
        java.io.File tempFile = new java.io.File("application.yaml");
        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            writer.write("server:\n  ip: '192.168.1.99'\n  port: '9090'");
        }

        try {
            // 2. Dùng Reflection gọi lại hàm private loadConfig
            java.lang.reflect.Method loadMethod = ConnectionManager.class.getDeclaredMethod("loadConfig");
            loadMethod.setAccessible(true);
            loadMethod.invoke(manager);

            // 3. Kiểm tra xem IP và Port có thực sự ăn theo cấu hình không
            java.lang.reflect.Field hostField = ConnectionManager.class.getDeclaredField("serverHost");
            hostField.setAccessible(true);
            assertEquals("192.168.1.99", hostField.get(manager));

            java.lang.reflect.Field portField = ConnectionManager.class.getDeclaredField("serverPort");
            portField.setAccessible(true);
            assertEquals(9090, portField.get(manager));
        } finally {
            // 4. Xóa file sau khi test xong để không làm bẩn dự án
            tempFile.delete();
        }
    }

    @Test
    void testLoadConfig_InvalidPortBranch() throws Exception {
        // Tạo YAML nhưng cố tình ghi port bằng CHỮ để ép nhánh NumberFormatException
        java.io.File tempFile = new java.io.File("application.yaml");
        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            writer.write("server:\n  ip: ' '\n  port: 'ABC_NOT_A_NUMBER'");
        }

        try {
            java.lang.reflect.Method loadMethod = ConnectionManager.class.getDeclaredMethod("loadConfig");
            loadMethod.setAccessible(true);
            assertDoesNotThrow(() -> loadMethod.invoke(manager));
        } finally {
            tempFile.delete();
        }
    }

    @Test
    void testLoadConfig_EmptyOrMissingKeys() throws Exception {
        // Tạo YAML rỗng hoặc thiếu key "server" để vét các nhánh if (config == null)
        java.io.File tempFile = new java.io.File("application.yaml");
        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            writer.write("randomKey: 'randomValue'");
        }

        try {
            java.lang.reflect.Method loadMethod = ConnectionManager.class.getDeclaredMethod("loadConfig");
            loadMethod.setAccessible(true);
            assertDoesNotThrow(() -> loadMethod.invoke(manager));
        } finally {
            tempFile.delete();
        }
    }

    @Test
    void testLoadConfig_IOExceptionBranch() throws Exception {

        java.io.File tempFile = new java.io.File("application.yaml");
        tempFile.createNewFile();
        tempFile.setReadable(false); // Cấm đọc -> Sẽ ném ra lỗi Access Denied / FileNotFoundException

        try {
            java.lang.reflect.Method loadMethod = ConnectionManager.class.getDeclaredMethod("loadConfig");
            loadMethod.setAccessible(true);
            assertDoesNotThrow(() -> loadMethod.invoke(manager));
        } finally {
            tempFile.setReadable(true); // Trả lại quyền để test tự dọn dẹp xóa file
            tempFile.delete();
        }
    }

    @Test
    void testLoadConfig_ExceptionBranch() throws Exception {

        java.io.File tempFile = new java.io.File("application.yaml");
        try (java.io.FileWriter w = new java.io.FileWriter(tempFile)) {
            // YAML cấm tuyệt đối dùng phím Tab để lùi đầu dòng, ta dùng nó để ép lỗi YAMLException
            w.write("server:\n\tip: localhost");
        }

        try {
            java.lang.reflect.Method loadMethod = ConnectionManager.class.getDeclaredMethod("loadConfig");
            loadMethod.setAccessible(true);
            assertDoesNotThrow(() -> loadMethod.invoke(manager));
        } finally {
            tempFile.delete();
        }
    }

    @Test
    void testLoadConfig_FinallyCloseExceptionBranch() throws Exception {

        java.io.File tempFile = new java.io.File("application.yaml");
        try (java.io.FileWriter w = new java.io.FileWriter(tempFile)) {
            w.write("server:\n  ip: localhost");
        }

        // Mock tĩnh class FileInputStream để nó tự ném lỗi mỗi khi bị gọi hàm close()
        try (org.mockito.MockedConstruction<java.io.FileInputStream> mockIn = org.mockito.Mockito.mockConstruction(java.io.FileInputStream.class, (mock, context) -> {
            doThrow(new java.io.IOException("Close Error")).when(mock).close();
        })) {
            java.lang.reflect.Method loadMethod = ConnectionManager.class.getDeclaredMethod("loadConfig");
            loadMethod.setAccessible(true);
            assertDoesNotThrow(() -> loadMethod.invoke(manager));
        } finally {
            tempFile.delete();
        }
    }

    @Test
    void testDisconnect_NullStreamsBranch() {
        // Gọi disconnect ngay khi vừa khởi tạo (các stream và socket đều đang null)
        // Sẽ vét sạch các nhánh màu vàng "!= null" ở các dòng 139, 140, 141
        assertDoesNotThrow(() -> manager.disconnect());
    }

    @Test
    void testDisconnect_IOExceptionBranch() throws Exception {
        // Ép nhánh màu đỏ dòng 144: Bắt lỗi IOException khi đang đóng Socket
        java.net.Socket mockSocket = mock(java.net.Socket.class);
        when(mockSocket.isClosed()).thenReturn(false);
        // Cài cắm để khi gọi hàm close() nó sẽ ném ra lỗi mạng
        doThrow(new java.io.IOException("Fake closing error")).when(mockSocket).close();

        // Dùng reflection "nhét" lén cái mockSocket này vào biến private của manager
        java.lang.reflect.Field socketField = ConnectionManager.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        socketField.set(manager, mockSocket);

        // Chạy disconnect, code sẽ sập ở nhánh try và nhảy vào catch đỏ lòm
        assertDoesNotThrow(() -> manager.disconnect());
    }

    @Test
    void testLoadConfig_TotalMissingBranch() throws Exception {
        // Đảm bảo không có file external nào cản đường
        new java.io.File("application.yaml").delete();

        // Ép dòng 61 (input == null): Đánh lừa Thread Context ClassLoader
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // Tạo một ClassLoader "ngu ngơ", không biết tìm resources ở đâu
            Thread.currentThread().setContextClassLoader(new ClassLoader() {
                @Override
                public java.io.InputStream getResourceAsStream(String name) {
                    return null; // Luôn trả về null, ép code nhảy vào nhánh warn "not found"
                }
            });

            java.lang.reflect.Method loadMethod = ConnectionManager.class.getDeclaredMethod("loadConfig");
            loadMethod.setAccessible(true);
            assertDoesNotThrow(() -> loadMethod.invoke(manager));
        } finally {
            // Trả lại ClassLoader xịn cho hệ thống
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Test
    void testLoadConfig_ForceExceptionBranch() throws Exception {
        // Tạo file YAML với "server" là một String thay vì Map để ép lỗi ClassCastException
        java.io.File tempFile = new java.io.File("application.yaml");
        try (java.io.FileWriter w = new java.io.FileWriter(tempFile)) {
            w.write("server: 'I am a string, not a map'");
        }

        try {
            java.lang.reflect.Method loadMethod = ConnectionManager.class.getDeclaredMethod("loadConfig");
            loadMethod.setAccessible(true);

            // Gọi hàm, lỗi ép kiểu bên trong sẽ bị văng ra và bị catch (Exception e) tóm gọn
            // Hoàn toàn không đụng chạm gì đến chữ "yaml" ở đây nữa!
            assertDoesNotThrow(() -> loadMethod.invoke(manager));
        } finally {
            tempFile.delete();
        }
    }
}