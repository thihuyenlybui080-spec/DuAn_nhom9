package vn.edu.vnu.auction.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.auction.common.network.NotificationMessage;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link NotificationListener}.
 * <p>
 * Kiểm tra việc đăng ký, hủy đăng ký và phân phối (dispatch) thông báo đến các trình xử lý
 * (handler) tương ứng.
 * </p>
 */
class NotificationListenerTest {

  private NotificationListener listener;

  @BeforeEach
  void setUp() throws Exception {
    listener = NotificationListener.getInstance();
  }

  @AfterEach
  void tearDown() throws Exception {
    // Reset Singleton instance
    Field instanceField = NotificationListener.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);
  }

  /**
   * Kiểm tra việc đăng ký và phân phối thông báo thành công.
   */
  @Test
  void testRegisterAndDispatch_Success() {
    int auctionId = 101;
    AtomicBoolean handlerCalled = new AtomicBoolean(false);

    // Define a consumer that sets the flag to true when called
    Consumer<NotificationMessage> handler = msg -> handlerCalled.set(true);

    listener.register(auctionId, handler);

    NotificationMessage mockMsg = mock(NotificationMessage.class);
    when(mockMsg.getAuctionId()).thenReturn(auctionId);

    listener.dispatch(mockMsg);

    assertTrue(handlerCalled.get(), "The registered handler should have been executed.");
  }

  /**
   * Kiểm tra trường hợp phân phối thông báo mà không có handler đăng ký.
   */
  @Test
  void testDispatch_NoHandlerFound() {
    NotificationMessage mockMsg = mock(NotificationMessage.class);
    when(mockMsg.getAuctionId()).thenReturn(999);

    // Should not throw any exception
    assertDoesNotThrow(() -> listener.dispatch(mockMsg),
        "Dispatching with no handler should be handled gracefully.");
  }

  /**
   * Kiểm tra việc hủy đăng ký (handler không được gọi sau khi unregister).
   */
  @Test
  void testUnregister_HandlerNotCalled() {
    int auctionId = 202;
    AtomicBoolean handlerCalled = new AtomicBoolean(false);
    Consumer<NotificationMessage> handler = msg -> handlerCalled.set(true);

    listener.register(auctionId, handler);
    listener.unregister(auctionId);

    NotificationMessage mockMsg = mock(NotificationMessage.class);
    when(mockMsg.getAuctionId()).thenReturn(auctionId);

    listener.dispatch(mockMsg);

    assertFalse(handlerCalled.get(), "The handler should not be called after unregistration.");
  }

  /**
   * Kiểm tra tính duy nhất của Singleton.
   */
  @Test
  void testGetInstance() {
    NotificationListener instance1 = NotificationListener.getInstance();
    NotificationListener instance2 = NotificationListener.getInstance();
    assertSame(instance1, instance2, "NotificationListener must be a singleton.");
  }
}