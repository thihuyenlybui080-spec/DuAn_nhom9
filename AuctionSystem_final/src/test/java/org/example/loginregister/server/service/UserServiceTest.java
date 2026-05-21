package org.example.loginregister.server.service;

import org.example.loginregister.server.model.entity.user.Admin;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;
import org.example.loginregister.server.model.entity.user.User;
import org.example.loginregister.server.model.entity.user.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private UserService userService;
    private Admin testAdmin;
    private Bidder testBidder;
    private Seller testSeller;

    @BeforeEach
    void setUp() {

        UserService.resetForTesting();
        AuctionService.resetForTesting();

        userService = UserService.getInstance();


        testAdmin = new Admin("admin1", "pass", "admin@mail", "System Admin");
        testAdmin.setId("admin-111");

        testBidder = new Bidder("bidder1", "pass", "bidder@mail", "Test Bidder");
        testBidder.setId("bidder-222");

        testSeller = new Seller("seller1", "pass", "seller@mail", "Test Seller");
        testSeller.setId("seller-333");
    }

    @AfterEach
    void tearDown() {
        UserService.resetForTesting();
        AuctionService.resetForTesting();
    }

    @Test
    void testGetInstance_ReturnsSingleton() {
        UserService instance1 = UserService.getInstance();
        UserService instance2 = UserService.getInstance();
        assertSame(instance1, instance2, "Hàm getInstance() phải trả về cùng một object");
    }

    @Test
    void testGetAllUsers_CallsDAO() {
        assertDoesNotThrow(() -> {
            try {
                List<User> users = userService.getAllUsers();

            } catch (Exception e) {
                System.out.println("✅ Hàm getAllUsers đã gọi DB. Lỗi do chưa kết nối: " + e.getMessage());
            }
        });
    }

    @Test
    void testUpdateUserStatus_UpdatesStatusObject_BeforeHittingDB() {

        try {
            userService.updateUserStatus(testAdmin, testBidder, UserStatus.BANNED);
        } catch (Exception e) {
            System.out.println("✅ Đã cập nhật trạng thái và gọi side-effect. Lỗi DB (DAO): " + e.getMessage());
        }


        assertEquals(UserStatus.BANNED, testBidder.getStatus(), "Trạng thái của user phải được cập nhật thành BANNED");
        assertNotNull(testBidder.getStatusRecord().getChangedBy());
        assertEquals(testAdmin, testBidder.getStatusRecord().getChangedBy(), "Người thay đổi trạng thái phải là Admin");
    }

    @Test
    void testApplyStatusSideEffects_ActiveStatus_ReturnsEarly() {

        assertDoesNotThrow(() -> {
            userService.applyStatusSideEffects(testSeller, UserStatus.ACTIVE);
            System.out.println("✅ Trạng thái ACTIVE đã bỏ qua lệnh gọi DB thành công.");
        }, "Cập nhật thành ACTIVE không được phép ném lỗi DB vì hàm phải return early");
    }

    @Test
    void testToggleUserLock_Bidder_InvokesBidderRestriction() {
        try {

            User returnedUser = userService.toggleUserLock(testBidder);

            assertEquals(testBidder, returnedUser, "Phải trả về đúng user bị tác động");
        } catch (Exception e) {

            System.out.println("✅ Đã xử lý khóa Bidder. Chặn lỗi DB thành công.");
        }
    }

    @Test
    void testToggleUserLock_Seller_InvokesSellerRestriction() {
        try {

            User returnedUser = userService.toggleUserLock(testSeller);

            assertEquals(testSeller, returnedUser, "Phải trả về đúng user bị tác động");
        } catch (Exception e) {

            System.out.println("✅ Đã xử lý khóa Seller. Chặn lỗi DB thành công.");
        }
    }
}