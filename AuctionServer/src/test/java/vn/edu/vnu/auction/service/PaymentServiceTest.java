package vn.edu.vnu.auction.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import vn.edu.vnu.auction.dao.AuctionDAO;
import vn.edu.vnu.auction.model.entity.AuctionResult;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.util.AuctionHistoryManager;
import vn.edu.vnu.auction.util.AuctionManager;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link PaymentService}.
 * <p>
 * Đảm bảo logic xử lý thanh toán và lập lịch hạn chót thanh toán (deadline) hoạt động chính xác,
 * bao gồm cả việc tương tác với Database.
 * </p>
 */
class PaymentServiceTest {

  private PaymentService paymentService;

  /**
   * Dọn dẹp và làm mới toàn bộ các Singleton trước mỗi bài test.
   */
  @BeforeEach
  void setUp() {
    PaymentService.resetForTesting();
    AuctionManager.resetForTesting();
    AuctionHistoryManager.getInstance().clearHistory();
    paymentService = PaymentService.getInstance();
  }

  /**
   * Dọn dẹp lại hệ thống sau khi test xong để giải phóng bộ nhớ.
   */
  @AfterEach
  void tearDown() {
    PaymentService.resetForTesting();
    AuctionManager.resetForTesting();
    AuctionHistoryManager.getInstance().clearHistory();
  }

  /**
   * Kiểm tra cơ chế Singleton của lớp PaymentService.
   */
  @Test
  void testGetInstance() {
    PaymentService instance1 = PaymentService.getInstance();
    PaymentService instance2 = PaymentService.getInstance();

    assertNotNull(instance1, "Instance không được để null.");
    assertSame(instance1, instance2,
        "Hệ thống chỉ được phép tồn tại duy nhất một đối tượng PaymentService.");
  }

  /**
   * Kiểm tra việc lập lịch hủy phiên nếu người dùng không thanh toán. Sử dụng ArgumentCaptor để lấy
   * tác vụ (Runnable) ra và chạy thử để kiểm chứng logic bên trong.
   */
  @Test
  void testSchedulePaymentDeadline_ExecutesCancellation() throws Exception {
    // Bước 1: Chuẩn bị dữ liệu mock trong bộ nhớ
    AuctionResult mockResult = Mockito.mock(AuctionResult.class);
    Mockito.when(mockResult.getAuctionId()).thenReturn(200);
    Mockito.when(mockResult.getStatus()).thenReturn(AuctionStatus.FINISHED);
    AuctionHistoryManager.getInstance().saveResult(mockResult);

    // Bước 2: Dùng Reflection để tráo AuctionManager thật bằng một mock (để lấy ra Scheduler giả)
    AuctionManager mockManager = Mockito.mock(AuctionManager.class);
    ScheduledExecutorService mockScheduler = Mockito.mock(ScheduledExecutorService.class);
    Mockito.when(mockManager.getScheduler()).thenReturn(mockScheduler);

    Field managerField = PaymentService.class.getDeclaredField("auctionManager");
    managerField.setAccessible(true);
    managerField.set(paymentService, mockManager);

    // Bước 3: Gọi hàm lập lịch
    paymentService.schedulePaymentDeadline(200);

    // Bước 4: Tóm gọn (capture) cái hàm nặc danh (Runnable) mà code của cậu vừa nhét vào Scheduler
    ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
    Mockito.verify(mockScheduler)
        .schedule(taskCaptor.capture(), Mockito.eq(86400L), Mockito.eq(TimeUnit.SECONDS));

    Runnable scheduledTask = taskCaptor.getValue();

    // Bước 5: Chạy thử tác vụ đó và giả lập Database để xem nó có thực sự update thành CANCELED không
    try (MockedStatic<AuctionDAO> mockedDao = Mockito.mockStatic(AuctionDAO.class)) {
      scheduledTask.run();

      Mockito.verify(mockResult, Mockito.times(1)).setStatus(AuctionStatus.CANCELED);
      mockedDao.verify(() -> AuctionDAO.updateAuctionStatus(200, AuctionStatus.CANCELED),
          Mockito.times(1));
    }
  }

  /**
   * Kiểm tra chức năng thanh toán sẽ trả về false nếu dữ liệu bị mất cả trong RAM lẫn Database.
   */
  @Test
  void testProcessPayment_NotInMemory_NotFoundInDB() {
    Bidder mockBidder = Mockito.mock(Bidder.class);

    // Giả lập DB trả về null
    try (MockedStatic<AuctionDAO> mockedDao = Mockito.mockStatic(AuctionDAO.class)) {
      mockedDao.when(() -> AuctionDAO.getAuctionById(404)).thenReturn(null);

      boolean result = paymentService.processPayment(mockBidder, 404);

      assertFalse(result, "Phải trả về false nếu không tìm thấy phiên đấu giá trong Database.");
    }
  }

  /**
   * Kiểm tra chức năng thanh toán sẽ chặn những người dùng mạo danh (không phải người thắng cuộc).
   */
  @Test
  void testProcessPayment_NotWinner() {
    Bidder requestBidder = Mockito.mock(Bidder.class);
    Mockito.when(requestBidder.getId()).thenReturn(1);
    Mockito.when(requestBidder.getName()).thenReturn("Hacker");

    Bidder actualWinner = Mockito.mock(Bidder.class);
    Mockito.when(actualWinner.getId()).thenReturn(2);

    AuctionResult mockResult = Mockito.mock(AuctionResult.class);
    Mockito.when(mockResult.getAuctionId()).thenReturn(10);
    Mockito.when(mockResult.getWinner()).thenReturn(actualWinner);

    AuctionHistoryManager.getInstance().saveResult(mockResult);

    boolean success = paymentService.processPayment(requestBidder, 10);

    assertFalse(success,
        "Phải trả về false nếu người yêu cầu thanh toán không phải người chiến thắng.");
  }

  /**
   * Kiểm tra chức năng thanh toán sẽ trả về true ngay lập tức nếu phiên này đã được thanh toán
   * (PAID) trước đó.
   */
  @Test
  void testProcessPayment_AlreadyPaid() {
    Bidder mockBidder = Mockito.mock(Bidder.class);
    Mockito.when(mockBidder.getId()).thenReturn(1);
    Mockito.when(mockBidder.getName()).thenReturn("Winner");

    AuctionResult mockResult = Mockito.mock(AuctionResult.class);
    Mockito.when(mockResult.getAuctionId()).thenReturn(20);
    Mockito.when(mockResult.getWinner()).thenReturn(mockBidder);
    Mockito.when(mockResult.getStatus()).thenReturn(AuctionStatus.PAID);

    AuctionHistoryManager.getInstance().saveResult(mockResult);

    boolean success = paymentService.processPayment(mockBidder, 20);

    assertTrue(success, "Phải trả về true nếu trạng thái của phiên đã là PAID.");
  }

  /**
   * Kiểm tra chức năng thanh toán sẽ chặn hành vi trả tiền khi phiên đấu giá chưa thực sự kết
   * thúc.
   */
  @Test
  void testProcessPayment_NotFinished() {
    Bidder mockBidder = Mockito.mock(Bidder.class);
    Mockito.when(mockBidder.getId()).thenReturn(1);

    AuctionResult mockResult = Mockito.mock(AuctionResult.class);
    Mockito.when(mockResult.getAuctionId()).thenReturn(30);
    Mockito.when(mockResult.getWinner()).thenReturn(mockBidder);
    Mockito.when(mockResult.getStatus()).thenReturn(AuctionStatus.RUNNING);

    AuctionHistoryManager.getInstance().saveResult(mockResult);

    boolean success = paymentService.processPayment(mockBidder, 30);

    assertFalse(success, "Phải trả về false nếu trạng thái của phiên không phải là FINISHED.");
  }

  /**
   * Kiểm tra kịch bản thanh toán thành công mỹ mãn.
   */
  @Test
  void testProcessPayment_Success() {
    Bidder mockBidder = Mockito.mock(Bidder.class);
    Mockito.when(mockBidder.getId()).thenReturn(1);
    Mockito.when(mockBidder.getName()).thenReturn("Happy Winner");

    Item mockItem = Mockito.mock(Item.class);
    Mockito.when(mockItem.getItemName()).thenReturn("Golden Watch");

    AuctionResult mockResult = Mockito.mock(AuctionResult.class);
    Mockito.when(mockResult.getAuctionId()).thenReturn(100);
    Mockito.when(mockResult.getWinner()).thenReturn(mockBidder);
    Mockito.when(mockResult.getStatus()).thenReturn(AuctionStatus.FINISHED);
    Mockito.when(mockResult.getFinalPrice()).thenReturn(5000.0);
    Mockito.when(mockResult.getItem()).thenReturn(mockItem);

    AuctionHistoryManager.getInstance().saveResult(mockResult);

    // Giả lập kết nối thành công với Database
    try (MockedStatic<AuctionDAO> mockedDao = Mockito.mockStatic(AuctionDAO.class)) {
      boolean success = paymentService.processPayment(mockBidder, 100);

      assertTrue(success, "Thanh toán hợp lệ phải trả về true.");

      // Đảm bảo Database đã được gọi để cập nhật trạng thái
      mockedDao.verify(() -> AuctionDAO.updateAuctionStatus(100, AuctionStatus.PAID),
          Mockito.times(1));

      // Đảm bảo dữ liệu trên RAM cũng được cập nhật trạng thái
      Mockito.verify(mockResult, Mockito.times(1)).setStatus(AuctionStatus.PAID);
    }
  }
}