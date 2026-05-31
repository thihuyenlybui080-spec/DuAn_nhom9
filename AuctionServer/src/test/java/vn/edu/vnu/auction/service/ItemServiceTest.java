package vn.edu.vnu.auction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import vn.edu.vnu.auction.dao.ItemDAO;
import vn.edu.vnu.auction.model.entity.item.Item;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link ItemService}.
 * <p>
 * Kiểm tra các chức năng quản lý sản phẩm bằng cách giả lập (mock) tầng Database (ItemDAO).
 * </p>
 */
class ItemServiceTest {

  private ItemService itemService;

  /**
   * Dọn dẹp trạng thái Singleton trước mỗi bài test để đảm bảo tính cô lập.
   */
  @BeforeEach
  void setUp() {
    ItemService.resetForTesting();
    itemService = ItemService.getInstance();
  }

  /**
   * Dọn dẹp lại hệ thống sau khi test xong để giải phóng bộ nhớ.
   */
  @AfterEach
  void tearDown() {
    ItemService.resetForTesting();
  }

  /**
   * Kiểm tra cơ chế Singleton của lớp ItemService.
   */
  @Test
  void testGetInstance() {
    ItemService instance1 = ItemService.getInstance();
    ItemService instance2 = ItemService.getInstance();

    assertNotNull(instance1, "Instance không được để null.");
    assertSame(instance1, instance2, "Chỉ được phép tồn tại một đối tượng ItemService duy nhất.");
  }

  /**
   * Kiểm tra chức năng lấy danh sách sản phẩm với ID người bán hợp lệ.
   */
  @Test
  void testGetItemsBySeller_ValidId() {
    int sellerId = 5;
    Item mockItem1 = Mockito.mock(Item.class);
    Item mockItem2 = Mockito.mock(Item.class);
    List<Item> expectedItems = Arrays.asList(mockItem1, mockItem2);

    try (MockedStatic<ItemDAO> mockedDao = Mockito.mockStatic(ItemDAO.class)) {
      mockedDao.when(() -> ItemDAO.getItemsBySeller(sellerId)).thenReturn(expectedItems);

      List<Item> actualItems = itemService.getItemsBySeller(sellerId);

      assertEquals(2, actualItems.size(), "Danh sách trả về phải có đúng 2 sản phẩm.");
      assertSame(expectedItems, actualItems, "Danh sách trả về phải khớp với dữ liệu từ DAO.");
    }
  }

  /**
   * Kiểm tra chức năng lấy danh sách sản phẩm với ID không hợp lệ.
   */
  @Test
  void testGetItemsBySeller_InvalidId() {
    List<Item> actualItems = itemService.getItemsBySeller(-1);
    assertTrue(actualItems.isEmpty(), "Phải trả về danh sách rỗng nếu ID người bán nhỏ hơn 0.");
  }

  /**
   * Kiểm tra chức năng xóa sản phẩm.
   */
  @Test
  void testDeleteItem() {
    int itemId = 10;

    try (MockedStatic<ItemDAO> mockedDao = Mockito.mockStatic(ItemDAO.class)) {
      itemService.deleteItem(itemId);
      mockedDao.verify(() -> ItemDAO.deleteItem(itemId), Mockito.times(1));
    }
  }
}