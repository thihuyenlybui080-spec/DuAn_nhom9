package org.example.loginregister.server.util;

import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.model.entity.user.Seller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

    private AuctionManager manager;
    private Auction testAuction1;
    private Auction testAuction2;

    @BeforeEach
    void setUp() {

        AuctionManager.resetForTesting();
        manager = AuctionManager.getInstance();


        Seller seller = new Seller("seller1", "pass", "sel@email.com", "Test Seller");

        Art art1 = new Art("Tranh 1", seller, "Mô tả 1", 100.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        art1.setId("item-111");
        testAuction1 = new Auction(art1);
        testAuction1.setId("auction-111");

        Art art2 = new Art("Tranh 2", seller, "Mô tả 2", 200.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        art2.setId("item-222");
        testAuction2 = new Auction(art2);
        testAuction2.setId("auction-222");
    }

    @AfterEach
    void tearDown() {

        AuctionManager.resetForTesting();
    }

    @Test
    void testGetInstance_ReturnsSingleton() {
        AuctionManager instance1 = AuctionManager.getInstance();
        AuctionManager instance2 = AuctionManager.getInstance();


        assertSame(instance1, instance2, "Hàm getInstance() phải luôn trả về cùng một đối tượng (Singleton)");
    }

    @Test
    void testPutAndGetActive_ValidAuction_StoresCorrectly() {

        manager.putActive(testAuction1);


        Auction retrieved = manager.getActive("auction-111");
        assertNotNull(retrieved, "Phải lấy ra được phiên đấu giá vừa lưu");
        assertEquals(testAuction1, retrieved, "Phiên lấy ra phải khớp với phiên đã lưu");
    }

    @Test
    void testPutActive_NullAuction_IsIgnored() {

        manager.putActive(null);


        assertTrue(manager.getAllActive().isEmpty(), "Truyền null vào hàm putActive() thì phải bị bỏ qua");
    }

    @Test
    void testRemoveActive_ExistingAuction_RemovesSuccessfully() {

        manager.putActive(testAuction1);


        manager.removeActive("auction-111");


        assertNull(manager.getActive("auction-111"), "Sau khi xóa, lấy lại phải ra null");
        assertTrue(manager.getAllActive().isEmpty(), "Danh sách phải trống sau khi xóa phần tử duy nhất");
    }

    @Test
    void testGetAllActive_ReturnsAllStoredAuctions() {
        manager.putActive(testAuction1);
        manager.putActive(testAuction2);


        assertEquals(2, manager.getAllActive().size(), "Phải chứa chính xác 2 phiên đang chạy");
        assertTrue(manager.getAllActive().contains(testAuction1));
        assertTrue(manager.getAllActive().contains(testAuction2));
    }

    @Test
    void testShutdown_StopsScheduler() {
        manager.shutdown();


        assertTrue(manager.getScheduler().isShutdown(), "Scheduler phải bị ngắt sau khi gọi shutdown()");
    }

    @Test
    void testResetForTesting_ClearsInstanceAndData() {

        manager.putActive(testAuction1);


        AuctionManager.resetForTesting();


        AuctionManager newManager = AuctionManager.getInstance();


        assertNotSame(manager, newManager, "resetForTesting() phải hủy instance cũ và tạo ra instance mới");
        assertTrue(newManager.getAllActive().isEmpty(), "Instance mới phải có danh sách rỗng hoàn toàn");
    }
}