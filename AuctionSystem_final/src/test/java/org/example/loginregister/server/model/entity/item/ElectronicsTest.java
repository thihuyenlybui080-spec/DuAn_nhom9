package org.example.loginregister.server.model.entity.item;

import org.example.loginregister.server.model.entity.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ElectronicsTest {

    private Seller testSeller;

    @BeforeEach
    void setUp() {

        testSeller = new Seller("seller1", "pass123", "seller@email.com", "Test Seller");
    }

    @Test
    void testConstructorAndGetters_SetsValuesCorrectly() {

        String expectedName = "MacBook Pro M3";
        String expectedDesc = "Laptop Apple cao cấp";
        double expectedPrice = 3000.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(5);


        Electronics electronics = new Electronics(
                expectedName,
                testSeller,
                expectedDesc,
                expectedPrice,
                startTime,
                endTime
        );


        assertEquals(expectedName, electronics.getItemName(), "Tên sản phẩm bị gán sai");
        assertEquals(testSeller, electronics.getSeller(), "Seller bị gán sai");
        assertEquals(expectedDesc, electronics.getDescription(), "Mô tả bị gán sai");
        assertEquals(expectedPrice, electronics.getStartingPrice(), "Giá khởi điểm bị gán sai");
        assertEquals(startTime, electronics.getStartTime(), "Thời gian bắt đầu bị gán sai");
        assertEquals(endTime, electronics.getEndTime(), "Thời gian kết thúc bị gán sai");


        assertNotNull(electronics.getId());
        assertTrue(electronics.getId().startsWith("item-"), "ID sinh ra phải chứa prefix 'item-'");
    }

    @Test
    void testGetCategory_ReturnsElectronics() {

        Electronics electronics = new Electronics(
                "Tai nghe Sony WH-1000XM5",
                testSeller,
                "Chống ồn chủ động",
                350.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(2)
        );


        assertEquals("Electronics", electronics.getCategory(), "Hàm getCategory() phải trả về chuẩn xác chuỗi 'Electronics'");
    }
}