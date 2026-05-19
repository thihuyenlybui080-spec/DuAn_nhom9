package org.example.loginregister.server.model.factory;

import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ArtFactoryTest {

    private ArtFactory factory;
    private Seller testSeller;

    @BeforeEach
    void setUp() {

        factory = new ArtFactory();


        testSeller = new Seller("seller1", "pass123", "seller@email.com", "Test Seller");
    }

    @Test
    void testCreateItem_ReturnsArtInstance_WithCorrectData() {

        String expectedName = "Bức tranh Mona Lisa";
        String expectedDesc = "Tuyệt tác nghệ thuật thế kỷ";
        double expectedPrice = 5000.0;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(3);


        Item createdItem = factory.createItem(
                expectedName,
                testSeller,
                expectedDesc,
                expectedPrice,
                startTime,
                endTime
        );


        assertNotNull(createdItem, "Item sinh ra không được null");


        assertTrue(createdItem instanceof Art, "Factory phải sinh ra đúng đối tượng thuộc class Art");


        assertEquals("Art", createdItem.getCategory(), "Category của Item phải là Art");


        assertEquals(expectedName, createdItem.getItemName(), "Tên sản phẩm bị sai");
        assertEquals(testSeller, createdItem.getSeller(), "Seller bị gán sai");
        assertEquals(expectedDesc, createdItem.getDescription(), "Mô tả sản phẩm bị sai");
        assertEquals(expectedPrice, createdItem.getStartingPrice(), "Giá khởi điểm bị sai");
        assertEquals(startTime, createdItem.getStartTime(), "Thời gian bắt đầu bị sai");
        assertEquals(endTime, createdItem.getEndTime(), "Thời gian kết thúc bị sai");
    }
}