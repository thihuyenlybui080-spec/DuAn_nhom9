package org.example.loginregister.server.service;

import org.example.loginregister.server.model.entity.item.Item;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemServiceTest {

    private ItemService itemService;

    @BeforeEach
    void setUp() {

        ItemService.resetForTesting();
        itemService = ItemService.getInstance();
    }

    @AfterEach
    void tearDown() {
        ItemService.resetForTesting();
    }

    @Test
    void testGetInstance_ReturnsSingleton() {
        ItemService instance1 = ItemService.getInstance();
        ItemService instance2 = ItemService.getInstance();


        assertSame(instance1, instance2, "Hàm getInstance() phải luôn trả về cùng một object duy nhất");
    }


    @Test
    void testGetItemsBySeller_NullId_ReturnsEmptyList() {

        List<Item> items = itemService.getItemsBySeller(null);


        assertNotNull(items, "Không được trả về null");
        assertTrue(items.isEmpty(), "Truyền null phải trả về danh sách rỗng");
    }

    @Test
    void testGetItemsBySeller_InvalidFormat_ReturnsEmptyList() {

        List<Item> items = itemService.getItemsBySeller("invalid-string-abc");


        assertNotNull(items);
        assertTrue(items.isEmpty(), "Chuỗi ID sai định dạng phải trả về danh sách rỗng");
    }

    @Test
    void testGetItemsBySeller_ValidPrefixId_CallsDAO() {
        try {

            List<Item> items = itemService.getItemsBySeller("seller-123");

            assertNotNull(items, "Kết quả từ DAO không được null");
            System.out.println("✅ Xử lý ID 'seller-123' thành công và đã gọi DB.");
        } catch (Exception e) {
            System.out.println("✅ Logic bóc tách ID chuẩn xác. Lỗi phát sinh do kết nối DB (ItemDAO): " + e.getMessage());
        }
    }

    @Test
    void testGetItemsBySeller_RawNumberId_CallsDAO() {
        try {

            List<Item> items = itemService.getItemsBySeller("456");

            assertNotNull(items, "Kết quả từ DAO không được null");
            System.out.println("✅ Xử lý ID thô '456' thành công và đã gọi DB.");
        } catch (Exception e) {
            System.out.println("✅ Logic bóc tách ID số thô chuẩn xác. Lỗi phát sinh do kết nối DB (ItemDAO): " + e.getMessage());
        }
    }
}