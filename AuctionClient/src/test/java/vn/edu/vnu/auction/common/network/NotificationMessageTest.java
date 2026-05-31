package vn.edu.vnu.auction.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NotificationMessageTest {

  @Test
  void testConstructorAndGetters() {
    // Step 1: Chuẩn bị dữ liệu đầu vào.
    // Tớ dùng luôn hằng số (constant) có sẵn trong class của cậu cho chuẩn.
    String expectedType = NotificationMessage.TYPE_BID_UPDATED;
    int expectedAuctionId = 101;
    String expectedData = "Some payload data like a Bid object or String";

    // Step 2: Khởi tạo đối tượng
    NotificationMessage message = new NotificationMessage(expectedType, expectedAuctionId,
        expectedData);

    // Step 3: Kiểm tra xem getter có lấy ra đúng đồ đã cất vào không
    assertEquals(expectedType, message.getType(), "Loại thông báo (Type) phải khớp chính xác");
    assertEquals(expectedAuctionId, message.getAuctionId(),
        "Mã phiên đấu giá (Auction ID) phải khớp chính xác");
    assertEquals(expectedData, message.getData(), "Dữ liệu đính kèm (Data) phải khớp chính xác");
  }

  @Test
  void testToString() {
    // Step 1: Tạo một thông báo với type là AUCTION_ENDED và ID là 99.
    // Data truyền vào null vì hàm toString() của cậu không in ra phần data.
    NotificationMessage message = new NotificationMessage(NotificationMessage.TYPE_AUCTION_ENDED,
        99, null);

    // Step 2: Chuẩn bị chuỗi kết quả mong đợi dựa trên format code của cậu
    String expectedString = "Notification{type='AUCTION_ENDED',auctionId='99'}";

    // Step 3: Kiểm tra
    assertEquals(expectedString, message.toString(),
        "Hàm toString() phải in ra đúng format đã định dạng");
  }
}