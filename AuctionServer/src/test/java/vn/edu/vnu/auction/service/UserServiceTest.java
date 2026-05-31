package vn.edu.vnu.auction.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import vn.edu.vnu.auction.dao.UserDAO;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.model.entity.user.Seller;
import vn.edu.vnu.auction.model.entity.user.User;
import vn.edu.vnu.auction.model.entity.user.UserStatus;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link UserService}.
 * <p>
 * Đồng bộ cấu trúc kiểm thử theo chuẩn chung: sử dụng MockedStatic cho tầng DAO và Reflection để
 * giả lập các Service phụ thuộc.
 * </p>
 */
class UserServiceTest {

  private UserService userService;
  private AuctionService mockAuctionService;

  /**
   * Dọn dẹp trạng thái và giả lập AuctionService bằng Reflection trước mỗi bài test.
   */
  @BeforeEach
  void setUp() throws Exception {
    UserService.resetForTesting();
    AuctionService.resetForTesting();

    userService = UserService.getInstance();

    // Tráo AuctionService thật bằng Mock để kiểm soát logic phụ thuộc
    mockAuctionService = mock(AuctionService.class);

    Field auctionServiceField = UserService.class.getDeclaredField("auctionService");
    auctionServiceField.setAccessible(true);
    auctionServiceField.set(userService, mockAuctionService);
  }

  /**
   * Dọn dẹp lại hệ thống sau khi test xong.
   */
  @AfterEach
  void tearDown() {
    UserService.resetForTesting();
    AuctionService.resetForTesting();
  }

  /**
   * Kiểm tra cơ chế Singleton của lớp UserService.
   */
  @Test
  void testGetInstance() {
    UserService instance1 = UserService.getInstance();
    UserService instance2 = UserService.getInstance();

    assertNotNull(instance1, "Instance không được để null.");
    assertSame(instance1, instance2, "Chỉ được phép tồn tại một đối tượng UserService duy nhất.");
  }

  /**
   * Kiểm tra chức năng lấy toàn bộ danh sách người dùng.
   */
  @Test
  void testGetAllUsers() {
    User mockUser = mock(User.class);
    List<User> expectedUsers = Arrays.asList(mockUser);

    try (MockedStatic<UserDAO> mockedDao = Mockito.mockStatic(UserDAO.class)) {
      mockedDao.when(UserDAO::getAllUsers).thenReturn(expectedUsers);

      List<User> actualUsers = userService.getAllUsers();
      assertSame(expectedUsers, actualUsers, "Danh sách trả về phải khớp với dữ liệu từ DB.");
    }
  }

  /**
   * Kiểm tra chức năng lấy người dùng theo ID.
   */
  @Test
  void testGetUserById() {
    User mockUser = mock(User.class);

    try (MockedStatic<UserDAO> mockedDao = Mockito.mockStatic(UserDAO.class)) {
      mockedDao.when(() -> UserDAO.getUserById(99)).thenReturn(mockUser);

      User actualUser = userService.getUserById(99);
      assertSame(mockUser, actualUser, "Người dùng lấy ra phải khớp với dữ liệu từ DB.");
    }
  }

  /**
   * Kiểm tra chức năng khóa tài khoản (chuyển sang BANNED).
   */
  @Test
  void testToggleUserLock_ToBanned() {
    Bidder mockBidder = mock(Bidder.class);
    when(mockBidder.getId()).thenReturn(5);
    when(mockBidder.isActive()).thenReturn(true);

    try (MockedStatic<UserDAO> mockedDao = Mockito.mockStatic(UserDAO.class)) {
      userService.toggleUserLock(mockBidder);

      Mockito.verify(mockBidder, Mockito.times(1)).updateStatus(Mockito.any());
      mockedDao.verify(() -> UserDAO.updateUserStatus(5, UserStatus.BANNED), Mockito.times(1));
    }
  }

  /**
   * Kiểm tra chức năng mở khóa tài khoản (chuyển sang ACTIVE).
   */
  @Test
  void testToggleUserLock_ToActive() {
    Seller mockSeller = mock(Seller.class);
    when(mockSeller.getId()).thenReturn(10);
    when(mockSeller.isActive()).thenReturn(false);

    try (MockedStatic<UserDAO> mockedDao = Mockito.mockStatic(UserDAO.class)) {
      userService.toggleUserLock(mockSeller);

      Mockito.verify(mockSeller, Mockito.times(1)).updateStatus(Mockito.any());
      mockedDao.verify(() -> UserDAO.updateUserStatus(10, UserStatus.ACTIVE), Mockito.times(1));
    }
  }


  /**
   * Kiểm tra hệ quả khi Seller bị khóa: Hủy phiên RUNNING và gỡ phiên OPEN.
   */
  // =========================================================================
  // TEST LOGIC XỬ LÝ PHIÊN ĐẤU GIÁ KHI SELLER BỊ KHÓA (RESTRICTED/BANNED)
  // =========================================================================
  @Test
  void testApplyStatusSideEffects_SellerHasRunningAndOpenAuctions() throws Exception {
    // 1. Dữ liệu giả: Seller mục tiêu (PHẢI CÓ isActive = true để lọt vào nhánh BANNED)
    Seller targetSeller = org.mockito.Mockito.mock(Seller.class);
    org.mockito.Mockito.when(targetSeller.getId()).thenReturn(1);
    org.mockito.Mockito.when(targetSeller.getName()).thenReturn("BadSeller");
    org.mockito.Mockito.when(targetSeller.isActive()).thenReturn(true);

    // 2. Tạo phiên RUNNING (Kỳ vọng: Bị Cancel)
    Auction runningAuction = org.mockito.Mockito.mock(Auction.class);
    org.mockito.Mockito.when(runningAuction.getId()).thenReturn(101);
    org.mockito.Mockito.when(runningAuction.getSeller()).thenReturn(targetSeller);
    org.mockito.Mockito.when(runningAuction.getStatus()).thenReturn(AuctionStatus.RUNNING);

    // 3. Tạo phiên OPEN (Kỳ vọng: Bị Remove)
    Auction openAuction = org.mockito.Mockito.mock(Auction.class);
    org.mockito.Mockito.when(openAuction.getId()).thenReturn(102);
    org.mockito.Mockito.when(openAuction.getSeller()).thenReturn(targetSeller);
    org.mockito.Mockito.when(openAuction.getStatus()).thenReturn(AuctionStatus.OPEN);

    // 4. Nhồi mockAuctionService vào userService bằng Reflection
    java.lang.reflect.Field auctionServiceField = UserService.class.getDeclaredField(
        "auctionService");
    auctionServiceField.setAccessible(true);
    auctionServiceField.set(userService, mockAuctionService);

    // 5. Mock tĩnh UserDAO để KHÔNG bị lỗi kết nối Database khi chạy Test
    try (org.mockito.MockedStatic<vn.edu.vnu.auction.dao.UserDAO> mockedUserDAO = org.mockito.Mockito.mockStatic(
        vn.edu.vnu.auction.dao.UserDAO.class)) {

      org.mockito.Mockito.when(mockAuctionService.getActiveAuctions())
          .thenReturn(java.util.Arrays.asList(runningAuction, openAuction));

      // KÍCH HOẠT qua hàm public chính thức của hệ thống
      userService.toggleUserLock(targetSeller);

      // KIỂM CHỨNG
      org.mockito.Mockito.verify(mockAuctionService, org.mockito.Mockito.times(1))
          .cancelAuction(101);
      org.mockito.Mockito.verify(mockAuctionService, org.mockito.Mockito.times(1))
          .removeAuction(102);

      // Xác nhận thêm là DB đã nhận được lệnh Update
      mockedUserDAO.verify(
          () -> vn.edu.vnu.auction.dao.UserDAO.updateUserStatus(1, UserStatus.BANNED),
          org.mockito.Mockito.times(1));
    }
  }
}