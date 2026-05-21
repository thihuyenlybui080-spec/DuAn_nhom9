package org.example.loginregister.server.model.factory;

import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.item.Vehicle;
import org.example.loginregister.server.model.entity.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VehicleFactoryTest {

    private VehicleFactory factory;
    private Seller testSeller;

    @BeforeEach
    void setUp() {

        factory = new VehicleFactory();


        testSeller = new Seller("seller1", "pass123", "seller@email.com", "Test Seller");
    }

    @Test
    void testCreateItem_ReturnsVehicleInstance_WithCorrectData() {

        String expectedName = "Mercedes-Benz S-Class";
        String expectedDesc = "Xe siêu lướt, đời 2026, màu đen";
        double expectedPrice = 3500000.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(10);


        Item createdItem = factory.createItem(
                expectedName,
                testSeller,
                expectedDesc,
                expectedPrice,
                startTime,
                endTime
        );


        assertNotNull(createdItem, "Item sinh ra không được null");


        assertTrue(createdItem instanceof Vehicle, "Factory phải sinh ra đúng đối tượng thuộc class Vehicle");


        assertEquals("Vehicle", createdItem.getCategory(), "Category của Item phải là Vehicle");


        assertEquals(expectedName, createdItem.getItemName(), "Tên sản phẩm bị sai");
        assertEquals(testSeller, createdItem.getSeller(), "Seller bị gán sai");
        assertEquals(expectedDesc, createdItem.getDescription(), "Mô tả sản phẩm bị sai");
        assertEquals(expectedPrice, createdItem.getStartingPrice(), "Giá khởi điểm bị sai");
        assertEquals(startTime, createdItem.getStartTime(), "Thời gian bắt đầu bị sai");
        assertEquals(endTime, createdItem.getEndTime(), "Thời gian kết thúc bị sai");
    }
}