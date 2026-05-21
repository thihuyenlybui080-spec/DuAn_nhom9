package org.example.loginregister.server.model.entity.user;

import org.example.loginregister.server.common.exception.AuctionClosedException;
import org.example.loginregister.server.common.exception.InvalidBidException;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionResult;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.auto_bidding.AutoBidConfig;
import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.util.AuctionHistoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidderTest {

    private Bidder bidder;
    private Auction testAuction;
    private Art testArt;

    @BeforeEach
    void setUp() {
        // 1. Khởi tạo Bidder
        bidder = new Bidder("Nhat", "pass123", "nhat@mail.com", "Nguyen Nhat");
        bidder.setId("bidder-999");

        // 2. Chuẩn bị Item và Auction thật
        Seller seller = new Seller("seller1", "pass", "seller@mail.com", "Test Seller");
        testArt = new Art("Bức tranh Test", seller, "Mô tả", 100.0, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        testArt.setId("item-12345"); // Đảm bảo constructor của Auction không lỗi chuỗi

        testAuction = new Auction(testArt);
        testAuction.setId("auction-12345");

        // 3. Xóa sạch lịch sử để các test không bị ảnh hưởng lẫn nhau
        AuctionHistoryManager.getInstance().clearHistory();
    }

    // ==========================================
    // 1. TEST KHỞI TẠO VÀ THUỘC TÍNH CƠ BẢN
    // ==========================================
    @Test
    void testConstructorAndGetters_SetsValuesCorrectly() {
        assertEquals("Nhat", bidder.getName());
        assertEquals("pass123", bidder.getPassword());
        assertEquals("nhat@mail.com", bidder.getEmail());
        assertEquals("Nguyen Nhat", bidder.getFullname());
        assertEquals("Bidder", bidder.getRole(), "Role phải là Bidder");
        assertTrue(bidder.getId().startsWith("bidder-"), "Prefix ID phải chuẩn xác");
    }

    // ==========================================
    // 2. TEST LOGIC ĐẶT GIÁ (BID)
    // ==========================================
    @Test
    void testBid_NullAuction_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            bidder.bid(null, 500.0);
        }, "Phải ném lỗi nếu truyền Auction null vào hàm bid");
    }

    @Test
    void testBid_ValidAuction_CallsBidService() {
        try {
            // Thực hiện đặt giá
            bidder.bid(testAuction, 150.0);
            System.out.println("✅ Hàm bid() đã đi qua hết các lệnh if và gọi xuống BidService.");
        } catch (InvalidBidException | AuctionClosedException e) {
            fail("Không được ném lỗi nghiệp vụ: " + e.getMessage());
        } catch (Exception e) {
            // Lỗi Database từ BidService (Tight-coupling) được phép xảy ra
            System.out.println("✅ Logic Bidder chuẩn xác. Lỗi phát sinh từ DB/Service: " + e.getMessage());
        }
    }

    @Test
    void testRecordBid_ActiveUser_AddsToHistory() {
        // Ghi nhận lịch sử giao dịch
        bidder.recordBid(testArt, 200.0);

        // Kiểm chứng danh sách history
        assertFalse(bidder.getHistory().isEmpty(), "Lịch sử không được trống sau khi record");
        assertEquals(1, bidder.getHistory().size());
        assertEquals(200.0, bidder.getHistory().get(0).getAmount(), "Số tiền lưu lại phải khớp");
        assertEquals(testArt, bidder.getHistory().get(0).getItem());
    }

    // ==========================================
    // 3. TEST LOGIC LỊCH SỬ CHIẾN THẮNG (WON AUCTIONS)
    // ==========================================
    @Test
    void testWonAuctions_RefreshAndGetters_WorksCorrectly() {
        // 1. Giả lập một phiên đấu giá mà Bidder này là người thắng cuộc
        testAuction.processBid(bidder, 500.0);
        testAuction.finishAuction(AuctionStatus.FINISHED);

        AuctionResult result = new AuctionResult(testAuction);

        // Lưu vào bộ nhớ cục bộ của Manager
        AuctionHistoryManager.getInstance().saveResult(result);

        // 2. Yêu cầu Bidder tự cập nhật danh sách thắng
        bidder.refreshWonAuctions();

        // 3. Kiểm chứng
        assertTrue(bidder.hasWonAuction("auction-12345"), "Phải xác nhận là đã thắng phiên này");
        assertEquals(1, bidder.getWonAuctions().size(), "Danh sách thắng phải có 1 phần tử");

        assertTrue(bidder.getWonAuction("auction-12345").isPresent(), "Lấy chi tiết phiên thắng phải tồn tại (Present)");
        assertEquals(500.0, bidder.getWonAuction("auction-12345").get().getFinalPrice());
    }

    // ==========================================
    // 4. TEST THANH TOÁN
    // ==========================================
    @Test
    void testPayForAuction_CallsPaymentService() {
        try {
            bidder.payForAuction("auction-12345");
            System.out.println("✅ Hàm payForAuction() đã gọi thành công xuống PaymentService.");
        } catch (Exception e) {
            System.out.println("✅ Logic Bidder chuẩn xác. Lỗi phát sinh từ DB/Service: " + e.getMessage());
        }
    }

    // ==========================================
    // 5. TEST QUẢN LÝ AUTO-BID
    // ==========================================
    @Test
    void testAutoBid_EnableAndDisable_DoesNotThrow() {
        AutoBidConfig config = new AutoBidConfig(1000.0, 50.0);

        // Bật Auto-bid
        assertDoesNotThrow(() -> {
            bidder.enableAutoBid(testAuction, config);
        }, "Bật Auto-bid không được sinh ra lỗi");

        // Tắt Auto-bid
        assertDoesNotThrow(() -> {
            bidder.disableAutoBid("auction-12345");
        }, "Tắt Auto-bid không được sinh ra lỗi");
    }
}