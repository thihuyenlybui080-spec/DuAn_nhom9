package vn.edu.vnu.auction.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.model.entity.AuctionResult;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.user.User;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link AuctionHistoryManager}.
 * <p>
 * Đảm bảo các chức năng quản lý, lưu trữ và cập nhật trạng thái của lịch sử đấu giá hoạt động chuẩn
 * xác.
 * </p>
 */
class AuctionHistoryManagerTest {

  private AuctionHistoryManager manager;

  /**
   * Khởi tạo và dọn dẹp lịch sử trước mỗi bài test để đảm bảo tính cô lập.
   */
  @BeforeEach
  void setUp() {
    manager = AuctionHistoryManager.getInstance();
    manager.clearHistory();
  }

  /**
   * Dọn dẹp lại lịch sử sau mỗi bài test để giải phóng bộ nhớ.
   */
  @AfterEach
  void tearDown() {
    manager.clearHistory();
  }

  /**
   * Kiểm tra cơ chế Singleton của lớp quản lý lịch sử.
   */
  @Test
  void testGetInstance() {
    AuctionHistoryManager instance1 = AuctionHistoryManager.getInstance();
    AuctionHistoryManager instance2 = AuctionHistoryManager.getInstance();

    assertNotNull(instance1, "Instance không được để null");
    assertSame(instance1, instance2,
        "Hệ thống chỉ được phép tồn tại duy nhất một đối tượng AuctionHistoryManager");
  }

  /**
   * Kiểm tra chức năng lưu và lấy kết quả của một phiên đấu giá.
   */
  @Test
  void testSaveAndGetResult() {
    AuctionResult mockResult = Mockito.mock(AuctionResult.class);
    Mockito.when(mockResult.getAuctionId()).thenReturn(101);

    manager.saveResult(mockResult);

    AuctionResult retrievedResult = manager.getResult(101);
    assertNotNull(retrievedResult, "Kết quả đấu giá lấy ra không được để null");
    assertEquals(101, retrievedResult.getAuctionId(),
        "ID của kết quả phải khớp với dữ liệu đã lưu");
  }

  /**
   * Kiểm tra chức năng lưu kết quả sẽ bỏ qua nếu dữ liệu truyền vào là null (tránh
   * NullPointerException).
   */
  @Test
  void testSaveResult_NullInput() {
    manager.saveResult(null);

    List<AuctionResult> allResults = manager.getAllResults();
    assertTrue(allResults.isEmpty(), "Danh sách lịch sử phải trống nếu dữ liệu truyền vào là null");
  }

  /**
   * Kiểm tra chức năng lấy toàn bộ danh sách kết quả đấu giá.
   */
  @Test
  void testGetAllResults() {
    AuctionResult result1 = Mockito.mock(AuctionResult.class);
    Mockito.when(result1.getAuctionId()).thenReturn(1);

    AuctionResult result2 = Mockito.mock(AuctionResult.class);
    Mockito.when(result2.getAuctionId()).thenReturn(2);

    manager.saveResult(result1);
    manager.saveResult(result2);

    List<AuctionResult> allResults = manager.getAllResults();
    assertEquals(2, allResults.size(), "Danh sách phải chứa chính xác 2 kết quả");
    assertTrue(allResults.contains(result1), "Danh sách phải chứa kết quả của phiên 1");
    assertTrue(allResults.contains(result2), "Danh sách phải chứa kết quả của phiên 2");
  }

  /**
   * Kiểm tra chức năng lọc danh sách kết quả đấu giá theo người chiến thắng.
   */
  @Test
  void testGetResultsByWinner() {
    User targetWinner = Mockito.mock(User.class);
    User otherWinner = Mockito.mock(User.class);

    // Tạo kết quả 1: Người thắng là targetWinner
    AuctionResult result1 = Mockito.mock(AuctionResult.class);
    Mockito.when(result1.getAuctionId()).thenReturn(10);
    Mockito.when(result1.getWinner()).thenReturn(targetWinner);

    // Tạo kết quả 2: Người thắng là otherWinner
    AuctionResult result2 = Mockito.mock(AuctionResult.class);
    Mockito.when(result2.getAuctionId()).thenReturn(20);
    Mockito.when(result2.getWinner()).thenReturn(otherWinner);

    manager.saveResult(result1);
    manager.saveResult(result2);

    // Lọc theo targetWinner
    List<AuctionResult> filteredResults = manager.getResultsByWinner(targetWinner);

    assertEquals(1, filteredResults.size(),
        "Chỉ có 1 phiên đấu giá thuộc về người chiến thắng này");
    assertTrue(filteredResults.contains(result1),
        "Danh sách lọc phải chứa đúng phiên đấu giá của người chiến thắng");
  }

  /**
   * Kiểm tra chức năng cập nhật trạng thái (updateStatus) của một kết quả đấu giá đã lưu.
   */
  @Test
  void testUpdateStatus() {
    AuctionResult mockResult = Mockito.mock(AuctionResult.class);
    Mockito.when(mockResult.getAuctionId()).thenReturn(500);

    // Lưu kết quả vào bộ nhớ
    manager.saveResult(mockResult);

    // Gọi hàm cập nhật trạng thái thành PAID
    manager.updateStatus(500, AuctionStatus.PAID);

    // Xác minh xem hàm setStatus() của đối tượng mockResult có được gọi với tham số PAID hay không
    Mockito.verify(mockResult, Mockito.times(1)).setStatus(AuctionStatus.PAID);
  }

  /**
   * Kiểm tra chức năng xóa toàn bộ lịch sử đấu giá trong bộ nhớ.
   */
  @Test
  void testClearHistory() {
    AuctionResult mockResult = Mockito.mock(AuctionResult.class);
    Mockito.when(mockResult.getAuctionId()).thenReturn(999);

    manager.saveResult(mockResult);
    assertFalse(manager.getAllResults().isEmpty(), "Danh sách không được trống trước khi xóa");

    manager.clearHistory();
    assertTrue(manager.getAllResults().isEmpty(),
        "Danh sách phải trống rỗng sau khi gọi hàm clearHistory()");
  }
}