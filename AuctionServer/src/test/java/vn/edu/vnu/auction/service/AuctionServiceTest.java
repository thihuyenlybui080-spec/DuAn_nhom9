package vn.edu.vnu.auction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import vn.edu.vnu.auction.dao.AuctionDAO;
import vn.edu.vnu.auction.dao.AutoBidDAO;
import vn.edu.vnu.auction.dao.ItemDAO;
import vn.edu.vnu.auction.dao.UserDAO;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionResult;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Seller;
import vn.edu.vnu.auction.model.entity.user.User;
import vn.edu.vnu.auction.util.AuctionHistoryManager;
import vn.edu.vnu.auction.util.AuctionManager;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link AuctionService}.
 */
class AuctionServiceTest {

  private AuctionService auctionService;
  private AuctionManager mockAuctionManager;
  private ScheduledExecutorService mockScheduler;

  @BeforeEach
  void setUp() throws Exception {
    AuctionService.resetForTesting();
    AuctionManager.resetForTesting();
    PaymentService.resetForTesting();
    AuctionHistoryManager.getInstance().clearHistory();

    auctionService = AuctionService.getInstance();

    // Giả lập (Mock) AuctionManager
    mockAuctionManager = Mockito.mock(AuctionManager.class);
    mockScheduler = Mockito.mock(ScheduledExecutorService.class);
    Mockito.when(mockAuctionManager.getScheduler()).thenReturn(mockScheduler);

    Field managerField = AuctionService.class.getDeclaredField("auctionManager");
    managerField.setAccessible(true);
    managerField.set(auctionService, mockAuctionManager);

    // BỔ SUNG QUAN TRỌNG: Mock PaymentService để chặn gọi xuống DB khi End Auction
    PaymentService mockPaymentService = Mockito.mock(PaymentService.class);
    Field paymentField = AuctionService.class.getDeclaredField("paymentService");
    paymentField.setAccessible(true);
    paymentField.set(auctionService, mockPaymentService);
  }

  @AfterEach
  void tearDown() {
    AuctionService.resetForTesting();
    AuctionManager.resetForTesting();
    PaymentService.resetForTesting();
    AuctionHistoryManager.getInstance().clearHistory();
  }

  @Test
  void testGetInstance() {
    AuctionService instance1 = AuctionService.getInstance();
    AuctionService instance2 = AuctionService.getInstance();

    assertNotNull(instance1, "Instance must not be null.");
    assertSame(instance1, instance2, "Only one instance of AuctionService is allowed.");
  }

  @Test
  void testStartAuction_Success() {
    Item mockItem = Mockito.mock(Item.class);
    Mockito.when(mockItem.getItemName()).thenReturn("Test Item");
    Mockito.when(mockItem.getSellerId()).thenReturn(10);
    Mockito.when(mockItem.getStartingPrice()).thenReturn(500.0);
    Mockito.when(mockItem.getStartTime()).thenReturn(LocalDateTime.now().plusHours(1));
    Mockito.when(mockItem.getEndTime()).thenReturn(LocalDateTime.now().plusHours(2));

    // Bổ sung chặn UserDAO để không chọc xuống DB
    try (MockedStatic<ItemDAO> mockedItemDao = Mockito.mockStatic(ItemDAO.class);
        MockedStatic<AuctionDAO> mockedAuctionDao = Mockito.mockStatic(AuctionDAO.class);
        MockedStatic<UserDAO> mockedUserDao = Mockito.mockStatic(UserDAO.class)) {

      mockedItemDao.when(() -> ItemDAO.insertItem(mockItem, 10)).thenReturn(100);
      mockedAuctionDao.when(() -> AuctionDAO.insertAuction(
          Mockito.eq(100), Mockito.anyDouble(), Mockito.anyLong(), Mockito.any())
      ).thenReturn(200);

      Seller mockSeller = Mockito.mock(Seller.class);
      mockedUserDao.when(() -> UserDAO.getUserById(10)).thenReturn(mockSeller);

      Auction createdAuction = auctionService.startAuction(mockItem);

      assertNotNull(createdAuction, "Created auction must not be null.");
      assertEquals(200, createdAuction.getId(), "Auction ID must match DB value.");
      Mockito.verify(mockScheduler, Mockito.times(1)).schedule(
          Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.eq(TimeUnit.SECONDS)
      );
    }
  }

  @Test
  void testCancelAuction_InMemory() {
    int auctionId = 55;
    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(auctionId);

    Item mockItem = Mockito.mock(Item.class);
    Mockito.when(mockItem.getItemName()).thenReturn("Mock Item"); // Ngăn lỗi NPE
    Mockito.when(mockAuction.getItem()).thenReturn(mockItem);

    Mockito.when(mockAuctionManager.getActive(auctionId)).thenReturn(mockAuction);

    try (MockedStatic<AuctionDAO> mockedDao = Mockito.mockStatic(AuctionDAO.class)) {
      auctionService.cancelAuction(auctionId);

      Mockito.verify(mockAuction, Mockito.times(1)).setStatus(AuctionStatus.CANCELED);
      mockedDao.verify(() -> AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.CANCELED),
          Mockito.times(1));
      Mockito.verify(mockAuctionManager, Mockito.times(1)).removeActive(auctionId);
    }
  }

  @Test
  void testEndAuction_Forced() {
    int auctionId = 99;
    Item mockItem = Mockito.mock(Item.class);
    Mockito.when(mockItem.getId()).thenReturn(111);
    Mockito.when(mockItem.getItemName()).thenReturn("Mock Item"); // Ngăn lỗi NPE

    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(auctionId);
    Mockito.when(mockAuction.getItem()).thenReturn(mockItem);

    Mockito.when(mockAuctionManager.getActive(auctionId)).thenReturn(mockAuction);

    try (MockedStatic<AuctionDAO> mockedDao = Mockito.mockStatic(AuctionDAO.class)) {
      Auction endedAuction = auctionService.endAuction(auctionId, true);

      assertNotNull(endedAuction, "Ended auction must not be null.");
      Mockito.verify(mockAuction, Mockito.times(1)).finishAuction(AuctionStatus.FINISHED);
      mockedDao.verify(() -> AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.FINISHED),
          Mockito.times(1));
      Mockito.verify(mockAuctionManager, Mockito.times(1)).removeActive(auctionId);
    }
  }

  @Test
  void testGetAuction_FallbackToDB() {
    int auctionId = 77;
    Auction dbAuction = Mockito.mock(Auction.class);
    Mockito.when(dbAuction.getId()).thenReturn(auctionId);

    Mockito.when(mockAuctionManager.getActive(auctionId)).thenReturn(null);

    try (MockedStatic<AuctionDAO> mockedDao = Mockito.mockStatic(AuctionDAO.class)) {
      mockedDao.when(() -> AuctionDAO.getAuctionById(auctionId)).thenReturn(dbAuction);
      Auction retrievedAuction = auctionService.getAuction(auctionId);

      assertSame(dbAuction, retrievedAuction, "Returned auction must match DB data.");
    }
  }

  @Test
  void testRemoveAuction() {
    int auctionId = 33;

    try (MockedStatic<AuctionDAO> mockedDao = Mockito.mockStatic(AuctionDAO.class)) {
      auctionService.removeAuction(auctionId);

      Mockito.verify(mockAuctionManager, Mockito.times(1)).removeActive(auctionId);
      mockedDao.verify(() -> AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.CANCELED),
          Mockito.times(1));
    }
  }

  @Test
  void testGetAllAuctions() {
    List<User> mockUsers = new ArrayList<>();
    Auction mockAuction = Mockito.mock(Auction.class);
    List<Auction> expectedAuctions = Arrays.asList(mockAuction);

    try (MockedStatic<UserDAO> mockedUserDao = Mockito.mockStatic(UserDAO.class);
        MockedStatic<AuctionDAO> mockedAuctionDao = Mockito.mockStatic(AuctionDAO.class)) {

      mockedUserDao.when(UserDAO::getAllUsers).thenReturn(mockUsers);
      mockedAuctionDao.when(() -> AuctionDAO.getAllAuctions(mockUsers))
          .thenReturn(expectedAuctions);

      List<Auction> actualAuctions = auctionService.getAllAuctions();
      assertEquals(expectedAuctions, actualAuctions, "Returned list must match data from DAO.");
    }
  }

  @Test
  void testGetAuctionsBySeller() {
    int sellerId = 5;
    List<User> mockUsers = new ArrayList<>();
    List<Auction> expectedAuctions = Arrays.asList(Mockito.mock(Auction.class));

    try (MockedStatic<UserDAO> mockedUserDao = Mockito.mockStatic(UserDAO.class);
        MockedStatic<AuctionDAO> mockedAuctionDao = Mockito.mockStatic(AuctionDAO.class)) {

      mockedUserDao.when(UserDAO::getAllUsers).thenReturn(mockUsers);
      mockedAuctionDao.when(() -> AuctionDAO.getAuctionsBySeller(sellerId, mockUsers))
          .thenReturn(expectedAuctions);

      List<Auction> actualAuctions = auctionService.getAuctionsBySeller(sellerId);
      assertSame(expectedAuctions, actualAuctions,
          "Returned list must match seller data from DAO.");
    }
  }

  @Test
  void testGetAuctionsBySeller_InvalidId() {
    List<Auction> actualAuctions = auctionService.getAuctionsBySeller(-1);
    assertTrue(actualAuctions.isEmpty(), "Must return empty list for invalid seller ID.");
  }

  @Test
  void testGetWonAuctions() {
    int bidderId = 8;
    List<User> mockUsers = new ArrayList<>();

    Auction mockAuction = Mockito.mock(Auction.class);
    Item mockItem = Mockito.mock(Item.class);
    Mockito.when(mockItem.getItemName()).thenReturn("Mock Item"); // Ngăn lỗi NPE
    Mockito.when(mockAuction.getItem()).thenReturn(mockItem);
    List<Auction> dbAuctions = Arrays.asList(mockAuction);

    try (MockedStatic<UserDAO> mockedUserDao = Mockito.mockStatic(UserDAO.class);
        MockedStatic<AuctionDAO> mockedAuctionDao = Mockito.mockStatic(AuctionDAO.class)) {

      mockedUserDao.when(UserDAO::getAllUsers).thenReturn(mockUsers);
      mockedAuctionDao.when(() -> AuctionDAO.getWonAuctionsByBidder(bidderId, mockUsers))
          .thenReturn(dbAuctions);

      List<AuctionResult> wonResults = auctionService.getWonAuctions(bidderId);

      assertFalse(wonResults.isEmpty(), "Won results list must not be empty.");
      assertEquals(1, wonResults.size(), "Must contain exactly 1 mapped result.");
    }
  }

  @Test
  void testGetActiveAuctions_ReschedulesCorrectly() {
    List<User> mockUsers = new ArrayList<>();

    Auction runningAuction = Mockito.mock(Auction.class);
    Item mockItem = Mockito.mock(Item.class);
    Mockito.when(runningAuction.getItem()).thenReturn(mockItem);
    Mockito.when(mockItem.getStartTime()).thenReturn(LocalDateTime.now().minusHours(1));
    Mockito.when(mockItem.getEndTime()).thenReturn(LocalDateTime.now().plusHours(1));
    Mockito.when(mockItem.getItemName()).thenReturn("Mock Item"); // Ngăn lỗi NPE

    List<Auction> activeList = Arrays.asList(runningAuction);

    // Bổ sung chặn AutoBidDAO để tránh chọc xuống Database khôi phục thiết lập
    try (MockedStatic<UserDAO> mockedUserDao = Mockito.mockStatic(UserDAO.class);
        MockedStatic<AuctionDAO> mockedAuctionDao = Mockito.mockStatic(AuctionDAO.class);
        MockedStatic<AutoBidDAO> mockedAutoBidDao = Mockito.mockStatic(AutoBidDAO.class)) {

      mockedUserDao.when(UserDAO::getAllUsers).thenReturn(mockUsers);
      mockedAuctionDao.when(() -> AuctionDAO.getActiveAuctions(mockUsers)).thenReturn(activeList);

      mockedAutoBidDao.when(() -> AutoBidDAO.getAutoBidsByAuction(Mockito.anyInt()))
          .thenReturn(Collections.emptyMap());

      List<Auction> results = auctionService.getActiveAuctions();

      assertFalse(results.isEmpty(), "Active auctions list must not be empty.");
      Mockito.verify(mockAuctionManager, Mockito.times(1)).putActive(runningAuction);
      Mockito.verify(runningAuction, Mockito.times(1)).setStatus(AuctionStatus.RUNNING);
    }
  }
}