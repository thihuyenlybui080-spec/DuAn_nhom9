package vn.edu.vnu.auction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.common.network.NotificationMessage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link ClientRegistry}.
 * <p>
 * Đảm bảo hệ thống quản lý danh sách client đăng ký xem đấu giá hoạt động chính xác,
 * gửi thông báo thành công và tự động dọn dẹp các luồng (stream) bị đứt kết nối.
 * </p>
 */
class ClientRegistryTest {

    private ClientRegistry registry;

    /**
     * Dọn dẹp và khởi tạo lại trạng thái của Singleton trước mỗi bài test.
     */
    @BeforeEach
    void setUp() throws Exception {
        // Sử dụng Reflection để reset biến instance private static thành null
        Field instanceField = ClientRegistry.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        registry = ClientRegistry.getInstance();
    }

    /**
     * Lấy danh sách registry (Map) lưu trữ bên trong class để kiểm tra bằng Reflection.
     */
    @SuppressWarnings("unchecked")
    private Map<Integer, Set<ObjectOutputStream>> getInternalRegistry() throws Exception {
        Field mapField = ClientRegistry.class.getDeclaredField("registry");
        mapField.setAccessible(true);
        return (Map<Integer, Set<ObjectOutputStream>>) mapField.get(registry);
    }

    /**
     * Kiểm tra cơ chế Singleton của ClientRegistry.
     */
    @Test
    void testGetInstance() {
        ClientRegistry instance1 = ClientRegistry.getInstance();
        ClientRegistry instance2 = ClientRegistry.getInstance();

        assertNotNull(instance1, "Instance không được để null.");
        assertSame(instance1, instance2, "Chỉ được phép tồn tại một instance duy nhất.");
    }

    /**
     * Kiểm tra chức năng đăng ký (register) và hủy đăng ký (unregister) một client.
     */
    @Test
    void testRegisterAndUnregister() throws Exception {
        Map<Integer, Set<ObjectOutputStream>> internalMap = getInternalRegistry();
        ObjectOutputStream mockStream = Mockito.mock(ObjectOutputStream.class);
        int auctionId = 101;

        // Đăng ký stream vào phiên 101
        registry.register(auctionId, mockStream);

        assertTrue(internalMap.containsKey(auctionId), "Registry phải chứa auctionId sau khi đăng ký.");
        assertTrue(internalMap.get(auctionId).contains(mockStream), "Set phải chứa stream vừa đăng ký.");

        // Hủy đăng ký stream
        registry.unregister(auctionId, mockStream);

        assertFalse(internalMap.containsKey(auctionId), "Registry phải tự động xóa luôn auctionId nếu Set bị trống.");
    }

    /**
     * Kiểm tra chức năng hủy đăng ký một stream khỏi toàn bộ các phiên đấu giá (khi client ngắt kết nối).
     */
    @Test
    void testUnregisterAll() throws Exception {
        Map<Integer, Set<ObjectOutputStream>> internalMap = getInternalRegistry();
        ObjectOutputStream targetStream = Mockito.mock(ObjectOutputStream.class);
        ObjectOutputStream otherStream = Mockito.mock(ObjectOutputStream.class);

        // targetStream tham gia 2 phiên, otherStream tham gia 1 phiên
        registry.register(1, targetStream);
        registry.register(2, targetStream);
        registry.register(2, otherStream);

        // Gọi hàm xóa targetStream khỏi tất cả
        registry.unregisterAll(targetStream);

        assertFalse(internalMap.get(1).contains(targetStream), "Stream mục tiêu phải bị xóa khỏi phiên 1.");
        assertFalse(internalMap.get(2).contains(targetStream), "Stream mục tiêu phải bị xóa khỏi phiên 2.");
        assertTrue(internalMap.get(2).contains(otherStream), "Stream của người khác không được bị ảnh hưởng.");
    }

    /**
     * Kiểm tra chức năng gửi thông báo (notifyAll) thành công đến các client đang xem phiên.
     */
    @Test
    void testNotifyAll_Success() throws IOException {
        ObjectOutputStream mockStream1 = Mockito.mock(ObjectOutputStream.class);
        ObjectOutputStream mockStream2 = Mockito.mock(ObjectOutputStream.class);
        int auctionId = 500;

        registry.register(auctionId, mockStream1);
        registry.register(auctionId, mockStream2);

        NotificationMessage message = new NotificationMessage(NotificationMessage.TYPE_BID_UPDATED, auctionId, "Data");

        registry.notifyAll(auctionId, message);

        // Dùng Mockito.verify để đảm bảo hàm writeObject và flush đã được gọi để gửi dữ liệu đi
        Mockito.verify(mockStream1, Mockito.times(1)).writeObject(message);
        Mockito.verify(mockStream1, Mockito.times(1)).flush();
        Mockito.verify(mockStream2, Mockito.times(1)).writeObject(message);
        Mockito.verify(mockStream2, Mockito.times(1)).flush();
    }

    /**
     * Kiểm tra chức năng notifyAll sẽ tự động dọn dẹp các stream bị lỗi mạng (ném ra IOException).
     */
    @Test
    void testNotifyAll_RemovesBrokenStream() throws Exception {
        Map<Integer, Set<ObjectOutputStream>> internalMap = getInternalRegistry();
        ObjectOutputStream brokenStream = Mockito.mock(ObjectOutputStream.class);
        ObjectOutputStream goodStream = Mockito.mock(ObjectOutputStream.class);
        int auctionId = 999;

        // Giả lập brokenStream sẽ bị lỗi khi cố gắng writeObject
        Mockito.doThrow(new IOException("Lỗi đứt mạng")).when(brokenStream).writeObject(Mockito.any());

        registry.register(auctionId, brokenStream);
        registry.register(auctionId, goodStream);

        NotificationMessage message = new NotificationMessage(NotificationMessage.TYPE_AUCTION_ENDED, auctionId, "Data");

        // Gọi hàm gửi thông báo
        registry.notifyAll(auctionId, message);

        Set<ObjectOutputStream> streams = internalMap.get(auctionId);

        // Kiểm tra xem brokenStream có bị hàm removeIf() xóa đi hay không
        assertFalse(streams.contains(brokenStream), "Stream bị lỗi mạng phải bị gỡ khỏi danh sách.");
        assertTrue(streams.contains(goodStream), "Stream hoạt động tốt phải được giữ lại.");
    }
}