package vn.edu.vnu.auction.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import vn.edu.vnu.auction.common.network.Response;
import vn.edu.vnu.auction.model.entity.user.User;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử (Unit Test) cho {@link AuctionClientService}.
 */
class AuctionClientServiceTest {

    private AuctionClientService service;
    private ConnectionManager mockConnectionManager;
    private MessageRouter mockMessageRouter;
    private ObjectOutputStream mockOutputStream;

    // Quản lý MockedStatic ở cấp class để dễ dàng đóng (close) sau mỗi bài test
    private MockedStatic<ConnectionManager> mockedConnectionManager;
    private MockedStatic<MessageRouter> mockedMessageRouter;

    @BeforeEach
    void setUp() throws Exception {
        // Reset Singleton
        Field instanceField = AuctionClientService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
        service = AuctionClientService.getInstance();

        mockConnectionManager = mock(ConnectionManager.class);
        mockMessageRouter = mock(MessageRouter.class);
        mockOutputStream = mock(ObjectOutputStream.class);

        mockedConnectionManager = mockStatic(ConnectionManager.class);
        mockedConnectionManager.when(ConnectionManager::getInstance).thenReturn(mockConnectionManager);

        mockedMessageRouter = mockStatic(MessageRouter.class);
        mockedMessageRouter.when(MessageRouter::getInstance).thenReturn(mockMessageRouter);

        when(mockConnectionManager.isConnected()).thenReturn(true);
        when(mockConnectionManager.getOutputStream()).thenReturn(mockOutputStream);
    }

    @AfterEach
    void tearDown() {
        // Bắt buộc phải đóng các MockedStatic để không làm hỏng các bài test của class khác
        if (mockedConnectionManager != null) mockedConnectionManager.close();
        if (mockedMessageRouter != null) mockedMessageRouter.close();
    }

    @Test
    void testLogin_Success() throws Exception {
        User mockUser = mock(User.class);
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockResponse.getData()).thenReturn(mockUser);
        when(mockResponse.getRequestId()).thenReturn("dummy-id"); // Tránh log error mismatch

        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        queue.add(mockResponse);
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(queue);

        User user = service.login("test", "pass");

        assertNotNull(user);
        assertEquals(mockUser, user);
    }

    @Test
    void testLogin_Failure() throws Exception {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getMessage()).thenReturn("Invalid credentials");

        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        queue.add(mockResponse);
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(queue);

        Exception e = assertThrows(RuntimeException.class, () -> service.login("bad", "wrong"));
        assertEquals("Invalid credentials", e.getMessage());
    }

    @Test
    void testSendRequest_ReconnectsSuccessfully() throws Exception {
        when(mockConnectionManager.isConnected()).thenReturn(false);
        when(mockConnectionManager.reconnect()).thenReturn(true);

        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(true);

        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        queue.add(mockResponse);
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(queue);

        assertDoesNotThrow(() -> service.getAllAuctions());
        verify(mockConnectionManager, times(1)).reconnect();
    }

    @Test
    void testSendRequest_ReconnectFails() {
        when(mockConnectionManager.isConnected()).thenReturn(false);
        when(mockConnectionManager.reconnect()).thenReturn(false);

        Exception e = assertThrows(RuntimeException.class, () -> service.getAllAuctions());
        assertTrue(e.getMessage().contains("Cannot connect to server"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSendRequest_Timeout() throws Exception {
        // Dùng mock(Queue) thay vì new Queue() để tránh việc hàm test bị đứng hình 30 giây
        LinkedBlockingQueue<Response> mockQueue = mock(LinkedBlockingQueue.class);
        when(mockQueue.poll(anyLong(), any(TimeUnit.class))).thenReturn(null);
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(mockQueue);

        Exception e = assertThrows(RuntimeException.class, () -> service.getAllAuctions());
        assertTrue(e.getMessage().contains("Request timeout"));
    }

    @Test
    void testSendRequest_IOException() throws Exception {
        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(queue);

        // Giả lập lỗi mạng khi đang ghi dữ liệu vào OutputStream
        doThrow(new IOException("Network crash")).when(mockOutputStream).writeObject(any());}


    @Test
    void testRegister_Success() {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(true);
        // Register trả về Map chứa userId
        java.util.Map<String, Object> respData = new java.util.HashMap<>();
        respData.put("userId", 99);
        when(mockResponse.getData()).thenReturn(respData);

        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        queue.add(mockResponse);
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(queue);

        int userId = service.register("user", "pass", "Name", "email", "phone", "M", "Bidder");
        assertEquals(99, userId);
    }

    @Test
    void testGetAllAuctions_Success() {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockResponse.getData()).thenReturn(new java.util.ArrayList<>());

        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        queue.add(mockResponse);
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(queue);

        java.util.List<?> auctions = service.getAllAuctions();
        assertNotNull(auctions);
    }

    @Test
    void testPlaceBid_Success() {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(true);
        vn.edu.vnu.auction.model.entity.Auction mockAuction = mock(vn.edu.vnu.auction.model.entity.Auction.class);
        when(mockResponse.getData()).thenReturn(mockAuction);

        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        queue.add(mockResponse);
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(queue);

        vn.edu.vnu.auction.model.entity.Auction result = service.placeBid(1, 1, 500.0);
        assertNotNull(result);
    }

    @Test
    void testEnableAndDisableAutoBid_Success() {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(true);

        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        queue.add(mockResponse);
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(queue);

        assertDoesNotThrow(() -> service.enableAutoBid(1, 1, 1000.0, 50.0));

        // Thêm 1 response nữa cho lệnh disable
        queue.add(mockResponse);
        assertDoesNotThrow(() -> service.disableAutoBid(1, 1));
    }

    @Test
    void testWatchAndLeaveAuction() {
        // Hàm watch và leave chỉ gửi request, không ép kiểu data trả về,
        // Dùng 1 response giả (không ném lỗi timeout là được)
        Response mockResponse = mock(Response.class);
        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        queue.add(mockResponse);
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(queue);

        assertDoesNotThrow(() -> service.watchAuction(1));

        queue.add(mockResponse);
        assertDoesNotThrow(() -> service.leaveAuction(1));
    }

    @Test
    void testGenericMethod_Failure() {
        // Test chung cho trường hợp server trả về lỗi (isSuccess = false) cho các hàm khác
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(false);
        when(mockResponse.getMessage()).thenReturn("Server Error");

        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        queue.add(mockResponse);
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(queue);

        assertThrows(RuntimeException.class, () -> service.cancelAuction(1));
    }

/**
 * Quét nhánh Success cho tất cả các hàm trả về List
 */
@Test
void testListReturns_Success() {
    Response mockResponse = mock(Response.class);
    when(mockResponse.isSuccess()).thenReturn(true);
    when(mockResponse.getData()).thenReturn(new java.util.ArrayList<>());
    when(mockResponse.getRequestId()).thenReturn("dummy");

    when(mockMessageRouter.registerRequest(anyString())).thenAnswer(inv -> {
        LinkedBlockingQueue<Response> q = new LinkedBlockingQueue<>();
        q.add(mockResponse);
        return q;
    });

    assertNotNull(service.getAuctionsBySeller(1));
    assertNotNull(service.getItemsBySeller(1));
    assertNotNull(service.getAllUsers());
    assertNotNull(service.getBidHistory(1));
    assertNotNull(service.getBidsByAuction(1));
    assertNotNull(service.getBidderHistory(1));
    assertNotNull(service.getWonAuctions(1));
}

    /**
     * Quét nhánh Success cho tất cả các hàm trả về Auction
     */
    @Test
    void testObjectReturns_Success() {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockResponse.getData()).thenReturn(mock(vn.edu.vnu.auction.model.entity.Auction.class));
        when(mockResponse.getRequestId()).thenReturn("dummy");

        when(mockMessageRouter.registerRequest(anyString())).thenAnswer(inv -> {
            LinkedBlockingQueue<Response> q = new LinkedBlockingQueue<>();
            q.add(mockResponse);
            return q;
        });

        assertNotNull(service.cancelAuction(1));
        assertNotNull(service.getAuctionById(1));
        assertNotNull(service.forceEndAuction(1));
        assertNotNull(service.createItemAndAuction(mock(vn.edu.vnu.auction.model.entity.item.Item.class)));
    }

    /**
     * Quét nhánh Success riêng cho hàm deleteItem (trả về Item)
     */
    @Test
    void testDeleteItem_Success() {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(true);
        // Trả về đúng kiểu Item
        when(mockResponse.getData()).thenReturn(mock(vn.edu.vnu.auction.model.entity.item.Item.class));
        when(mockResponse.getRequestId()).thenReturn("dummy");

        when(mockMessageRouter.registerRequest(anyString())).thenAnswer(inv -> {
            LinkedBlockingQueue<Response> q = new LinkedBlockingQueue<>();
            q.add(mockResponse);
            return q;
        });

        assertNotNull(service.deleteItem(1));
    }

    /**
     * Test riêng cho payAuction (trả về boolean)
     */
    @Test
    void testPayAuction_Success() {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockResponse.getRequestId()).thenReturn("dummy");

        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        queue.add(mockResponse);
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(queue);

        assertTrue(service.payAuction(1));
    }

    /**
     * Test riêng cho checkAutoBid (trả về Map)
     */
    @Test
    void testCheckAutoBid_Success() {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockResponse.getData()).thenReturn(new java.util.HashMap<String, Object>());
        when(mockResponse.getRequestId()).thenReturn("dummy");

        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        queue.add(mockResponse);
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(queue);

        assertNotNull(service.checkAutoBid(1, 1));
    }

    /**
     * Test riêng cho toggleUserLock (void, update status)
     */
    @Test
    void testToggleUserLock_Success() {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(true);
        User returnUser = mock(User.class);
        when(mockResponse.getData()).thenReturn(returnUser);
        when(mockResponse.getRequestId()).thenReturn("dummy");

        LinkedBlockingQueue<Response> queue = new LinkedBlockingQueue<>();
        queue.add(mockResponse);
        when(mockMessageRouter.registerRequest(anyString())).thenReturn(queue);

        User testUser = mock(User.class);
        assertDoesNotThrow(() -> service.toggleUserLock(testUser));
        verify(testUser).updateStatus(any());
    }

/**
 * Quét nhánh Failure (Ném Exception) cho TẤT CẢ các hàm còn lại
 */
@Test
void testAllWrappers_FailureBranch() {
    Response failResp = mock(Response.class);
    when(failResp.isSuccess()).thenReturn(false);
    when(failResp.getMessage()).thenReturn("Server Error");
    when(failResp.getRequestId()).thenReturn("dummy");

    when(mockMessageRouter.registerRequest(anyString())).thenAnswer(inv -> {
        LinkedBlockingQueue<Response> q = new LinkedBlockingQueue<>();
        q.add(failResp);
        return q;
    });

    assertThrows(RuntimeException.class, () -> service.getAuctionsBySeller(1));
    assertThrows(RuntimeException.class, () -> service.getItemsBySeller(1));
    assertThrows(RuntimeException.class, () -> service.cancelAuction(1));
    assertThrows(RuntimeException.class, () -> service.getAllUsers());
    assertThrows(RuntimeException.class, () -> service.toggleUserLock(mock(User.class)));
    assertThrows(RuntimeException.class, () -> service.createItemAndAuction(mock(vn.edu.vnu.auction.model.entity.item.Item.class)));
    assertThrows(RuntimeException.class, () -> service.forceEndAuction(1));
    assertThrows(RuntimeException.class, () -> service.getBidHistory(1));
    assertThrows(RuntimeException.class, () -> service.getBidsByAuction(1));
    assertThrows(RuntimeException.class, () -> service.getBidderHistory(1));
    assertThrows(RuntimeException.class, () -> service.deleteItem(1));
    assertThrows(RuntimeException.class, () -> service.getWonAuctions(1));
    assertThrows(RuntimeException.class, () -> service.payAuction(1));

    // Riêng getAuctionById thì return null thay vì throw RuntimeException
    assertNull(service.getAuctionById(1));
}

    @Test
    void testSendRequest_IdMismatchBranch() {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(true);
        // Trả về một ID sai lệch hoàn toàn so với ID lúc gửi để ép nhảy vào nhánh if(mismatch)
        when(mockResponse.getRequestId()).thenReturn("WRONG-ID-1234");
        when(mockResponse.getData()).thenReturn(new java.util.ArrayList<>());

        when(mockMessageRouter.registerRequest(anyString())).thenAnswer(inv -> {
            LinkedBlockingQueue<Response> q = new LinkedBlockingQueue<>();
            q.add(mockResponse);
            return q;
        });

        // Không sập là được, code sẽ in ra log ERROR nhưng vẫn chạy tiếp
        assertNotNull(service.getAllAuctions());
    }

    @Test
    void testCheckAutoBid_NullDataBranch() {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccess()).thenReturn(true);
        // Trả về null cho data để ép nhảy qua nhánh fail của hàm checkAutoBid
        when(mockResponse.getData()).thenReturn(null);
        when(mockResponse.getRequestId()).thenReturn("dummy");

        when(mockMessageRouter.registerRequest(anyString())).thenAnswer(inv -> {
            LinkedBlockingQueue<Response> q = new LinkedBlockingQueue<>();
            q.add(mockResponse);
            return q;
        });

        assertNull(service.checkAutoBid(1, 1));
    }}
