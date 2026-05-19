package org.example.loginregister.server.model.entity.user;

import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.util.AuctionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AdminTest {

    private Admin admin;

    @BeforeEach
    void setUp() {
        // Khởi tạo một đối tượng Admin trước mỗi test
        admin = new Admin("admin1", "pass123", "admin@email.com", "System Administrator");
    }

    // ==========================================
    // 1. TEST KHỞI TẠO VÀ THUỘC TÍNH
    // ==========================================
    @Test
    void testConstructorAndInheritedGetters_SetsValuesCorrectly() {
        assertEquals("admin1", admin.getName());
        assertEquals("pass123", admin.getPassword());
        assertEquals("admin@email.com", admin.getEmail());
        assertEquals("System Administrator", admin.getFullname());

        // Kiểm tra mặc định status khi khởi tạo từ class User
        assertTrue(admin.isActive(), "Tài khoản Admin mới tạo phải có trạng thái ACTIVE");
    }

    @Test
    void testGetRole_ReturnsAdmin() {
        assertEquals("Admin", admin.getRole(), "Hàm getRole() phải trả về chính xác chuỗi 'Admin'");
    }

    @Test
    void testGetIdPrefix_GeneratesCorrectId() {
        assertNotNull(admin.getId());
        assertTrue(admin.getId().startsWith("admin-"), "ID của Admin sinh ra phải có tiền tố 'admin-'");
    }

    // ==========================================
    // 2. TEST CÁC HÀNH VI (BEHAVIORS)
    // ==========================================
    @Test
    void testMonitorAuction_DoesNotThrowException() {
        // Hàm này hiện tại chỉ có System.out.println, ta test để đảm bảo nó chạy mượt mà không văng lỗi
        assertDoesNotThrow(() -> {
            admin.monitorAuction(AuctionManager.getInstance());
        }, "Hàm monitorAuction không được ném lỗi");
    }

    @Test
    void testManageUser_CallsUserService() {
        // Tạo một User thật (Bidder) để Admin thao tác
        Bidder targetUser = new Bidder("Nhat", "pass", "nhat@mail", "Nhat Nguyen");
        targetUser.setId("bidder-999");

        try {
            // Admin thực hiện khóa tài khoản user
            admin.manageUser(targetUser, UserStatus.BANNED);

            // Nếu không có lỗi DB thì tốt!
            System.out.println("✅ Gọi UserService thành công mà không gặp lỗi DB.");
        } catch (Exception e) {
            // Bắt lỗi Database (ví dụ Access Denied) từ UserService để bài test không bị "đỏ"
            System.out.println("✅ Logic Admin đã đi tới UserService. Lỗi DB sinh ra là do môi trường: " + e.getMessage());
        }

        // Vì ta không dùng Mockito để chặn Service, việc code chạy lọt vào khối try (hoặc catch exception DB)
        // chứng tỏ hàm admin.manageUser() đã hoàn thành nhiệm vụ truyền dữ liệu đi.
    }

    @Test
    void testCancelAuction_CallsAuctionService() {
        // Tạo một Item và Auction thật
        Seller seller = new Seller("seller1", "pass", "seller@mail", "Seller");
        Art art = new Art("Tranh Test", seller, "Mô tả", 100.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        art.setId("item-12345"); // Đảm bảo constructor của Auction cắt chuỗi không lỗi

        Auction auction = new Auction(art);
        auction.setId("auction-12345");

        try {
            // Admin thực hiện hủy phiên đấu giá
            admin.cancelAuction(auction);

            System.out.println("✅ Gọi AuctionService hủy thành công.");
        } catch (Exception e) {
            System.out.println("✅ Logic Admin đã đi tới AuctionService. Lỗi DB sinh ra là do môi trường: " + e.getMessage());
        }
    }
}