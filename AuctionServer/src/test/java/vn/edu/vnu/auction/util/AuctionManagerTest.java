package vn.edu.vnu.auction.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.model.entity.Auction;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link AuctionManager}.
 * <p>
 * Đảm bảo các chức năng quản lý trạng thái phiên đấu giá (thêm, xóa, lấy danh sách) và cơ chế
 * Singleton hoạt động chính xác.
 * </p>
 */
class AuctionManagerTest {

  /**
   * Dọn dẹp trạng thái của AuctionManager trước mỗi bài test để đảm bảo tính cô lập.
   */
  @BeforeEach
  void setUp() {
    AuctionManager.resetForTesting();
  }

  /**
   * Dọn dẹp trạng thái của AuctionManager sau mỗi bài test để giải phóng tài nguyên.
   */
  @AfterEach
  void tearDown() {
    AuctionManager.resetForTesting();
  }

  /**
   * Kiểm tra xem phương thức getInstance có trả về đúng một đối tượng duy nhất (Singleton) hay
   * không.
   */
  @Test
  void testGetInstance_ReturnsSingleton() {
    AuctionManager instance1 = AuctionManager.getInstance();
    AuctionManager instance2 = AuctionManager.getInstance();

    assertNotNull(instance1, "Instance của manager không được để null");
    assertSame(instance1, instance2, "Nhiều lần gọi getInstance() phải trả về cùng một đối tượng");
  }

  /**
   * Kiểm tra chức năng lưu và lấy một phiên đấu giá đang diễn ra (active).
   */
  @Test
  void testPutAndGetActive() {
    AuctionManager manager = AuctionManager.getInstance();

    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(10);

    manager.putActive(mockAuction);

    Auction retrievedAuction = manager.getActive(10);
    assertNotNull(retrievedAuction, "Phiên đấu giá lấy ra không được để null");
    assertEquals(10, retrievedAuction.getId(),
        "ID của phiên đấu giá lấy ra phải khớp với ID đã lưu");
  }

  /**
   * Kiểm tra chức năng lưu phiên đấu giá sẽ bỏ qua các đối tượng có ID không hợp lệ (nhỏ hơn hoặc
   * bằng 0).
   */
  @Test
  void testPutActive_IgnoresInvalidId() {
    AuctionManager manager = AuctionManager.getInstance();

    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(0);

    manager.putActive(mockAuction);

    assertNull(manager.getActive(0),
        "Các phiên đấu giá có ID <= 0 không được phép đăng ký vào bộ nhớ");
  }

  /**
   * Kiểm tra chức năng gỡ bỏ một phiên đấu giá khỏi bộ nhớ active.
   */
  @Test
  void testRemoveActive() {
    AuctionManager manager = AuctionManager.getInstance();

    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(99);

    manager.putActive(mockAuction);
    assertNotNull(manager.getActive(99), "Phiên đấu giá phải tồn tại trong bộ nhớ lúc ban đầu");

    manager.removeActive(99);
    assertNull(manager.getActive(99),
        "Phiên đấu giá phải bị xóa khỏi bộ nhớ sau khi gọi removeActive()");
  }

  /**
   * Kiểm tra chức năng lấy toàn bộ danh sách các phiên đấu giá đang diễn ra.
   */
  @Test
  void testGetAllActive() {
    AuctionManager manager = AuctionManager.getInstance();

    Auction auction1 = Mockito.mock(Auction.class);
    Mockito.when(auction1.getId()).thenReturn(1);

    Auction auction2 = Mockito.mock(Auction.class);
    Mockito.when(auction2.getId()).thenReturn(2);

    manager.putActive(auction1);
    manager.putActive(auction2);

    Collection<Auction> activeAuctions = manager.getAllActive();

    assertEquals(2, activeAuctions.size(), "Collection phải chứa chính xác 2 phiên đấu giá active");
    assertTrue(activeAuctions.contains(auction1), "Collection phải chứa auction1");
    assertTrue(activeAuctions.contains(auction2), "Collection phải chứa auction2");
  }

  /**
   * Kiểm tra chức năng tắt bộ lập lịch (scheduler) một cách an toàn.
   */
  @Test
  void testShutdownScheduler() {
    AuctionManager manager = AuctionManager.getInstance();
    ScheduledExecutorService scheduler = manager.getScheduler();

    assertNotNull(scheduler, "Scheduler phải được khởi tạo");
    assertFalse(scheduler.isShutdown(), "Scheduler phải đang chạy lúc ban đầu");

    manager.shutdown();

    assertTrue(scheduler.isShutdown(),
        "Scheduler phải bị tắt hoàn toàn sau khi gọi hàm shutdown()");
  }
}