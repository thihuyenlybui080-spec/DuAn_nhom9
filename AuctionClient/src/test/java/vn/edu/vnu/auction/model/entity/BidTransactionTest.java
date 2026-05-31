package vn.edu.vnu.auction.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Bidder;

/**
 * Lớp kiểm thử (Unit Test) dành cho Entity {@link BidTransaction}.
 * <p>
 * Đảm bảo hệ thống ghi nhận đúng thông tin của một lượt đặt giá bao gồm:
 * người đặt (Bidder), sản phẩm (Item), mức giá (Amount) và thời gian đặt (Timestamp).
 * </p>
 */
class BidTransactionTest {

  private BidTransaction transaction;
  private Bidder mockBidder;
  private Item mockItem;

  /**
   * Thiết lập dữ liệu giả lập trước mỗi kịch bản kiểm thử.
   * Sử dụng Mockito để tạo các đối tượng Bidder và Item ảo, giúp cô lập bài test.
   */
  @BeforeEach
  void setUp() {
    // Bước 1: Tạo các đối tượng giả lập (mock) cho Bidder và Item
    mockBidder = Mockito.mock(Bidder.class);
    mockItem = Mockito.mock(Item.class);

    // Bước 2: Khởi tạo đối tượng BidTransaction với mức giá là 500.0
    transaction = new BidTransaction(mockBidder, mockItem, 500.0);
  }

  /**
   * Kiểm tra hàm khởi tạo (Constructor) và các hàm Getter cơ bản.
   * Kỳ vọng: Dữ liệu truyền vào phải được lưu trữ chính xác và thời gian (timestamp) phải tự động sinh ra.
   */
  @Test
  void testConstructorAndInitialGetters() {
    // Kiểm tra xem các đối tượng mock và mức giá có được lưu đúng không
    assertEquals(mockBidder, transaction.getBidder(), "The bidder should match the injected mock");
    assertEquals(mockItem, transaction.getItem(), "The item should match the injected mock");
    assertEquals(500.0, transaction.getAmount(), "The bid amount should be 500.0");

    // Kiểm tra xem thời gian tạo giao dịch (timestamp) đã được hệ thống tự động sinh ra chưa
    assertNotNull(transaction.getTimestamp(), "The timestamp must be generated automatically and not be null");
  }
}