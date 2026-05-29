package vn.edu.vnu.auction.model.entity.item;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử cho {@link Other}.
 * Kiểm tra việc khởi tạo đối tượng và lấy danh mục (category).
 */
class OtherTest {

    @Test
    void testConstructorAndGetCategory() {
        String name = "Test Item";
        int sellerId = 1;
        String desc = "Description";
        double price = 100.0;
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);

        // Khởi tạo đúng với constructor trong Other.java
        Other other = new Other(name, sellerId, desc, price, start, end);

        assertNotNull(other);
        assertEquals("Other", other.getCategory(), "Category should be 'Other'");
        assertEquals(name, other.getItemName());
        assertEquals(price, other.getStartingPrice());
    }
}