package org.example.loginregister.server.model.entity.item;

import org.example.loginregister.server.model.entity.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VehicleTest {

    private Seller testSeller;

    @BeforeEach
    void setUp() {

        testSeller = new Seller("seller1", "pass123", "seller@email.com", "Test Seller");
    }

    @Test
    void testConstructorAndGetters_SetsValuesCorrectly() {

        String expectedName = "Porsche 911 GT3";
        String expectedDesc = "Siêu xe thể thao màu vàng";
        double expectedPrice = 250000.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(10);


        Vehicle vehicle = new Vehicle(
                expectedName,
                testSeller,
                expectedDesc,
                expectedPrice,
                startTime,
                endTime
        );


        assertEquals(expectedName, vehicle.getItemName(), "Tên sản phẩm bị gán sai");
        assertEquals(testSeller, vehicle.getSeller(), "Seller bị gán sai");
        assertEquals(expectedDesc, vehicle.getDescription(), "Mô tả bị gán sai");
        assertEquals(expectedPrice, vehicle.getStartingPrice(), "Giá khởi điểm bị gán sai");
        assertEquals(startTime, vehicle.getStartTime(), "Thời gian bắt đầu bị gán sai");
        assertEquals(endTime, vehicle.getEndTime(), "Thời gian kết thúc bị gán sai");


        assertNotNull(vehicle.getId());
        assertTrue(vehicle.getId().startsWith("item-"), "ID sinh ra phải chứa prefix 'item-'");
    }

    @Test
    void testGetCategory_ReturnsVehicle() {

        Vehicle vehicle = new Vehicle(
                "Honda SH 150i",
                testSeller,
                "Xe máy tay ga cao cấp",
                4000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(2)
        );


        assertEquals("Vehicle", vehicle.getCategory(), "Hàm getCategory() phải trả về chuẩn xác chuỗi 'Vehicle'");
    }
}