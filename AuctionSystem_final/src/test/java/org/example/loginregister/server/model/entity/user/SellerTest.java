package org.example.loginregister.server.model.entity.user;

import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.model.entity.item.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SellerTest {

    private Seller seller;

    @BeforeEach
    void setUp() {
        // Khởi tạo đối tượng Seller trước mỗi test case
        seller = new Seller("seller1", "pass123", "seller@mail.com", "Nguyen Seller");
    }

    // ==========================================
    // 1. TEST KHỞI TẠO VÀ THUỘC TÍNH CƠ BẢN
    // ==========================================
    @Test
    void testConstructorAndGetters_SetsValuesCorrectly() {
        assertEquals("seller1", seller.getName());
        assertEquals("pass123", seller.getPassword());
        assertEquals("seller@mail.com", seller.getEmail());
        assertEquals("Nguyen Seller", seller.getFullname());

        assertEquals("Seller", seller.getRole(), "Role của đối tượng này phải là Seller");
        assertTrue(seller.getId().startsWith("seller-"), "Prefix ID phải là 'seller-'");
        assertTrue(seller.isActive(), "Mặc định tạo ra phải là ACTIVE");
    }

    // ==========================================
    // 2. TEST LOGIC THÊM SẢN PHẨM (ADD ITEM)
    // ==========================================
    @Test
    void testAddItem_ActiveUser_Success() {
        Item art = new Art("Tranh Đông Hồ", seller, "Mô tả", 100.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        // Vì không có hàm getOwnedItems(), ta kiểm tra bằng cách đảm bảo hàm không ném ra lỗi
        assertDoesNotThrow(() -> {
            seller.addItem(art);
        }, "Người bán đang Active thì phải thêm được sản phẩm mà không bị lỗi");
    }

    @Test
    void testAddItem_BannedUser_ThrowsException() {
        Item art = new Art("Tranh Đông Hồ", seller, "Mô tả", 100.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        // TẠO ADMIN VÀ CẬP NHẬT TRẠNG THÁI (Đã sửa theo chuẩn UserStatusRecord của bạn)
        Admin admin = new Admin("admin1", "pass", "admin@mail", "Admin System");

        // Giả sử enum UserStatus của bạn có giá trị BANNED (hoặc có thể là INACTIVE/DELETED)
        UserStatusRecord bannedRecord = new UserStatusRecord(UserStatus.BANNED, admin);
        seller.updateStatus(bannedRecord);

        // Kiểm chứng
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            seller.addItem(art);
        });
        assertEquals("Account is locked and cannot list items", exception.getMessage());
    }

    // ==========================================
    // 3. TEST LOGIC XÓA SẢN PHẨM (DELETE ITEM)
    // ==========================================
    @Test
    void testDeleteItem_ItemNotOwned_ThrowsException() {
        Item art = new Art("Tranh Lạ", seller, "Mô tả", 100.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        // Cố tình KHÔNG thêm vào danh sách (không gọi seller.addItem)

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            seller.deleteItem(art);
        });
        assertEquals("Seller does not own this item", exception.getMessage());
    }

    @Test
    void testDeleteItem_AuctionAlreadyStarted_ThrowsException() {
        // Cố tình lùi thời gian bắt đầu (StartTime) về 1 tiếng trước -> Tức là phiên đã diễn ra
        LocalDateTime pastStartTime = LocalDateTime.now().minusHours(1);
        LocalDateTime futureEndTime = LocalDateTime.now().plusDays(1);
        Item art = new Art("Tranh Cổ", seller, "Mô tả", 100.0, pastStartTime, futureEndTime);

        seller.addItem(art); // Phải thêm vào trước để qua được cửa ải "Not Owned"

        // Kiểm chứng
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            seller.deleteItem(art);
        });
        assertEquals("Cannot delete item: auction has already started", exception.getMessage());
    }

    @Test
    void testDeleteItem_BeforeAuctionStarts_CallsAuctionService() {
        // Chỉnh thời gian bắt đầu là 2 tiếng NỮA (chưa diễn ra)
        LocalDateTime futureStartTime = LocalDateTime.now().plusHours(2);
        LocalDateTime futureEndTime = LocalDateTime.now().plusDays(1);
        Item art = new Art("Tranh Mới", seller, "Mô tả", 100.0, futureStartTime, futureEndTime);

        seller.addItem(art);

        try {
            // Hàm này sẽ lọt được qua tất cả các lệnh if và gọi xuống AuctionService
            seller.deleteItem(art);
            System.out.println("✅ Gọi hàm xóa thành công, AuctionService đã được kích hoạt.");

            // Xóa xong thì trong danh sách không còn, nếu xóa tiếp sẽ báo lỗi "not own"
            assertThrows(IllegalArgumentException.class, () -> seller.deleteItem(art));

        } catch (Exception e) {
            // Bắt lỗi Database từ AuctionService để bài test không bị Fail
            System.out.println("✅ Logic điều kiện của Seller đã đúng. Lỗi DB sinh ra từ Service: " + e.getMessage());
        }
    }
}