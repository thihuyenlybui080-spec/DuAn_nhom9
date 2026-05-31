package vn.edu.vnu.auction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import vn.edu.vnu.auction.common.exception.DuplicateUsernameException;
import vn.edu.vnu.auction.common.network.Request;
import vn.edu.vnu.auction.common.network.Response;
import vn.edu.vnu.auction.dao.UserDAO;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.user.User;
import vn.edu.vnu.auction.service.AuctionService;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link ClientHandler}.
 * <p>
 * Kiểm thử khả năng giao tiếp mạng (Socket) và định tuyến chính xác các logic nghiệp vụ xử lý
 * Request/Response của hệ thống.
 * </p>
 */
class ClientHandlerTest {

  private ServerSocket serverSocket;
  private Socket clientSocket;
  private Socket handlerSocket;
  private ObjectOutputStream clientOut;
  private ObjectInputStream clientIn;
  private Thread handlerThread;

  // ClientHandler instance used specifically for testing internal logic without network
  private ClientHandler logicHandler;

  /**
   * Khởi tạo kết nối mạng và đối tượng xử lý trước mỗi bài test.
   */
  @BeforeEach
  void setUp() throws IOException, InterruptedException {
    // --- 1. SETUP FOR NETWORK TEST ---
    serverSocket = new ServerSocket(0);
    int port = serverSocket.getLocalPort();

    Thread acceptThread = new Thread(() -> {
      try {
        handlerSocket = serverSocket.accept();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    acceptThread.start();

    clientSocket = new Socket("localhost", port);
    acceptThread.join();

    ClientHandler networkHandler = new ClientHandler(handlerSocket);
    handlerThread = new Thread(networkHandler);
    handlerThread.start();

    clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
    clientOut.flush();
    clientIn = new ObjectInputStream(clientSocket.getInputStream());

    // --- 2. SETUP FOR LOGIC TEST (No Socket needed) ---
    logicHandler = new ClientHandler(new Socket()); // Empty Socket to prevent initialization errors
  }

  /**
   * Dọn dẹp tài nguyên mạng sau khi test.
   */
  @AfterEach
  void tearDown() throws IOException, InterruptedException {
    if (clientOut != null) {
      clientOut.close();
    }
    if (clientIn != null) {
      clientIn.close();
    }
    if (clientSocket != null) {
      clientSocket.close();
    }
    if (handlerSocket != null) {
      handlerSocket.close();
    }
    if (serverSocket != null) {
      serverSocket.close();
    }

    if (handlerThread != null && handlerThread.isAlive()) {
      handlerThread.join(2000);
    }
  }

  /**
   * Hàm hỗ trợ dùng Reflection để gọi trực tiếp vào phương thức private handleRequest().
   */
  private Response invokeHandleRequest(Request request) throws Exception {
    Method method = ClientHandler.class.getDeclaredMethod("handleRequest", Request.class);
    method.setAccessible(true);
    return (Response) method.invoke(logicHandler, request);
  }

  // ===================================================================================
  // PART 1: NETWORK ROUTING TEST
  // ===================================================================================

  /**
   * Kiểm tra trường hợp Client gửi một Request với action không tồn tại.
   */
  @Test
  void testHandleUnknownAction() throws Exception {
    Request ghostRequest = new Request("ACTION_GHOST_123", null);
    clientOut.writeObject(ghostRequest);
    clientOut.flush();

    Response response = (Response) clientIn.readObject();

    assertFalse(response.isSuccess(), "Response must be marked as failed.");
    assertTrue(response.getMessage().contains("Unknow action"),
        "Error must report unknown action.");
  }

  /**
   * Kiểm tra luồng Client ngắt kết nối đột ngột.
   */
  @Test
  void testClientDisconnect_TerminatesHandler() throws Exception {
    clientSocket.close();
    handlerThread.join(2000);
    assertFalse(handlerThread.isAlive(), "Thread must stop automatically on disconnect.");
  }

  // ===================================================================================
  // PART 2: BUSINESS LOGIC TEST (Network Bypass)
  // ===================================================================================

  /**
   * Kiểm tra logic đăng nhập thành công.
   */
  @Test
  void testHandleLogin_Success() throws Exception {
    Map<String, String> creds = new HashMap<>();
    creds.put("username", "testuser");
    creds.put("password", "pass123");
    Request req = new Request(Request.ACTION_LOGIN, creds);

    User mockUser = Mockito.mock(User.class);
    Mockito.when(mockUser.isActive()).thenReturn(true);

    try (MockedStatic<UserDAO> mockedDao = Mockito.mockStatic(UserDAO.class)) {
      mockedDao.when(() -> UserDAO.getUserByCredentials("testuser", "pass123"))
          .thenReturn(mockUser);

      Response res = invokeHandleRequest(req);

      assertTrue(res.isSuccess(), "Login with correct data must succeed.");
      assertSame(mockUser, res.getData(), "Returned data must be a User object.");
    }
  }

  /**
   * Kiểm tra logic đăng nhập thất bại do tài khoản bị khóa (BANNED).
   */
  @Test
  void testHandleLogin_AccountLocked() throws Exception {
    Map<String, String> creds = new HashMap<>();
    creds.put("username", "badboy");
    creds.put("password", "123");
    Request req = new Request(Request.ACTION_LOGIN, creds);

    User mockUser = Mockito.mock(User.class);
    Mockito.when(mockUser.isActive()).thenReturn(false); // Account locked

    try (MockedStatic<UserDAO> mockedDao = Mockito.mockStatic(UserDAO.class)) {
      mockedDao.when(() -> UserDAO.getUserByCredentials("badboy", "123")).thenReturn(mockUser);

      Response res = invokeHandleRequest(req);

      assertFalse(res.isSuccess(), "Login with locked account must fail.");
      assertEquals("This account has been locked", res.getMessage(),
          "Must have account locked message.");
    }
  }

  /**
   * Kiểm tra logic đăng ký người dùng mới thành công.
   */
  @Test
  void testHandleRegister_Success() throws Exception {
    Map<String, String> info = new HashMap<>();
    info.put("username", "newuser");
    info.put("password", "pass");
    info.put("role", "BIDDER");

    Request req = new Request(Request.ACTION_REGISTER, info);

    User mockUser = Mockito.mock(User.class);
    Mockito.when(mockUser.getId()).thenReturn(10);

    try (MockedStatic<UserDAO> mockedDao = Mockito.mockStatic(UserDAO.class)) {
      mockedDao.when(() -> UserDAO.registerUser(
          Mockito.eq("newuser"), Mockito.eq("pass"), Mockito.any(),
          Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq("BIDDER")
      )).thenReturn(mockUser);

      Response res = invokeHandleRequest(req);

      assertTrue(res.isSuccess(), "Registration must succeed.");
      assertNotNull(res.getData(), "Must return data containing new user ID.");
    }
  }

  /**
   * Kiểm tra logic đăng ký thất bại do trùng tên đăng nhập.
   */
  @Test
  void testHandleRegister_DuplicateUsername() throws Exception {
    Map<String, String> info = new HashMap<>();
    info.put("username", "existuser");
    info.put("password", "pass");
    info.put("role", "BIDDER");
    Request req = new Request(Request.ACTION_REGISTER, info);

    try (MockedStatic<UserDAO> mockedDao = Mockito.mockStatic(UserDAO.class)) {
      mockedDao.when(
              () -> UserDAO.registerUser(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                  Mockito.any(), Mockito.any(), Mockito.any()))
          .thenThrow(new DuplicateUsernameException("User exists"));

      Response res = invokeHandleRequest(req);

      assertFalse(res.isSuccess(), "Registration must fail.");
      assertEquals("Username already exists", res.getMessage(),
          "Must report duplicate username error.");
    }
  }

  /**
   * Kiểm tra logic lấy danh sách toàn bộ các phiên đấu giá.
   */
  @Test
  void testHandleGetAuctions() throws Exception {
    Request req = new Request(Request.ACTION_GET_AUCTIONS, null);
    List<Auction> expectedAuctions = Arrays.asList(Mockito.mock(Auction.class));

    AuctionService mockService = Mockito.mock(AuctionService.class);
    Mockito.when(mockService.getAllAuctions()).thenReturn(expectedAuctions);

    try (MockedStatic<AuctionService> mockedServiceStatic = Mockito.mockStatic(
        AuctionService.class)) {
      mockedServiceStatic.when(AuctionService::getInstance).thenReturn(mockService);

      Response res = invokeHandleRequest(req);

      assertTrue(res.isSuccess(), "Retrieving list must succeed.");
      assertSame(expectedAuctions, res.getData(), "Returned data must match data from Service.");
    }
  }

  /**
   * Kiểm tra logic hủy phiên đấu giá theo ID do client gửi lên.
   */
  @Test
  void testHandleCancelAuction() throws Exception {
    Request req = new Request(Request.ACTION_CANCEL_AUCTION, 99);
    Auction mockAuction = Mockito.mock(Auction.class);

    AuctionService mockService = Mockito.mock(AuctionService.class);
    Mockito.when(mockService.getAuction(99)).thenReturn(mockAuction);

    try (MockedStatic<AuctionService> mockedServiceStatic = Mockito.mockStatic(
        AuctionService.class)) {
      mockedServiceStatic.when(AuctionService::getInstance).thenReturn(mockService);

      Response res = invokeHandleRequest(req);

      assertTrue(res.isSuccess(), "Canceling auction must report success.");
      Mockito.verify(mockService, Mockito.times(1)).cancelAuction(99);
    }
  }
}