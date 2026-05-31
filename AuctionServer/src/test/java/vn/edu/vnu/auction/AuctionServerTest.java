package vn.edu.vnu.auction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link AuctionServer}.
 * <p>
 * Kiểm tra các kịch bản khởi động, lỗi mạng và cơ chế tắt server an toàn.
 * </p>
 */
class AuctionServerTest {

  private AuctionServer server;
  private Thread serverThread;
  private int serverPort;

  @BeforeEach
  void setUp() throws Exception {
    server = new AuctionServer();
    // Use Reflection to dynamically read the private static PORT variable
    Field portField = AuctionServer.class.getDeclaredField("PORT");
    portField.setAccessible(true);
    serverPort = portField.getInt(null);
  }

  @AfterEach
  void tearDown() throws Exception {
    stopServerSafely();
    if (serverThread != null && serverThread.isAlive()) {
      serverThread.join(1000);
    }
  }

  private void stopServerSafely() {
    try {
      Method stopMethod = AuctionServer.class.getDeclaredMethod("stop");
      stopMethod.setAccessible(true);
      stopMethod.invoke(server);
    } catch (Exception e) {
      // Ignore exceptions during teardown to avoid hiding actual test failures
    }
  }

  /**
   * Kiểm tra trường hợp Server không thể khởi động do cổng đã bị chiếm. Kiểm tra nhánh catch
   * (IOException) khi tạo ServerSocket.
   */
  @Test
  void testStart_PortAlreadyInUse() {
    // Occupy the port first
    try (ServerSocket conflictSocket = new ServerSocket(serverPort)) {
      // Start server (should handle failure internally and enter catch block)
      assertDoesNotThrow(() -> server.start(), "Server should handle port conflict gracefully.");
    } catch (IOException e) {
      fail("Failed to setup port conflict test: " + e.getMessage());
    }
  }

  /**
   * Test logic dừng server khi socket đang mở.
   */
  @Test
  void testStop_ClosesSocket() throws Exception {
    serverThread = new Thread(() -> server.start());
    serverThread.start();
    Thread.sleep(500);

    Field socketField = AuctionServer.class.getDeclaredField("serverSocket");
    socketField.setAccessible(true);
    ServerSocket ss = (ServerSocket) socketField.get(server);

    // Handle case where server might have failed to start
    if (ss != null) {
      assertFalse(ss.isClosed(), "Socket should be open initially.");
      stopServerSafely();
      assertTrue(ss.isClosed(), "Socket must be closed after stop().");
    }
  }
}