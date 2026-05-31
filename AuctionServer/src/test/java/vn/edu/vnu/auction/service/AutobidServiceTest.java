package vn.edu.vnu.auction.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import vn.edu.vnu.auction.ClientRegistry;
import vn.edu.vnu.auction.dao.AutoBidDAO;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.auto_bidding.AutoBidConfig;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.util.AuctionManager;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link AutobidService}.
 */
class AutobidServiceTest {

  private AutobidService autobidService;

  @BeforeEach
  void setUp() throws Exception {
    Field instanceField = AutobidService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    autobidService = AutobidService.getInstance();

    Field queuesField = AutobidService.class.getDeclaredField("queues");
    queuesField.setAccessible(true);
    ((Map<?, ?>) queuesField.get(autobidService)).clear();

    Field processingField = AutobidService.class.getDeclaredField("processing");
    processingField.setAccessible(true);
    ((Map<?, ?>) processingField.get(autobidService)).clear();
  }

  @AfterEach
  void tearDown() throws Exception {
    Field instanceField = AutobidService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);
  }

  @Test
  void testGetInstance() {
    AutobidService instance1 = AutobidService.getInstance();
    AutobidService instance2 = AutobidService.getInstance();

    assertNotNull(instance1, "Instance must not be null.");
    assertSame(instance1, instance2, "Only one instance of AutobidService is allowed.");
  }

  @Test
  void testEnableAutoBid_NotLeading_PlacesInitialBid() {
    Bidder mockBidder = Mockito.mock(Bidder.class);
    Mockito.when(mockBidder.getId()).thenReturn(1);
    Mockito.when(mockBidder.getName()).thenReturn("Tester");
    Mockito.when(mockBidder.isActive()).thenReturn(true); // Bắt buộc để qua cửa isBidderLocked

    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(100);
    Mockito.when(mockAuction.getCurrentPrice()).thenReturn(50.0);
    Mockito.when(mockAuction.getHighestBidder()).thenReturn(null);

    AutoBidConfig config = new AutoBidConfig(500.0, 10.0);
    BidService mockBidService = Mockito.mock(BidService.class);

    try (MockedStatic<AutoBidDAO> mockedDao = Mockito.mockStatic(AutoBidDAO.class);
        MockedStatic<BidService> mockedBidServiceStatic = Mockito.mockStatic(BidService.class)) {

      mockedBidServiceStatic.when(BidService::getInstance).thenReturn(mockBidService);

      autobidService.enableAutoBid(mockBidder, mockAuction, config);

      mockedDao.verify(() -> AutoBidDAO.saveAutoBid(100, 1, 500.0, 10.0), Mockito.times(1));
      Mockito.verify(mockBidService, Mockito.times(1))
          .processAutoBid(mockBidder, mockAuction, 60.0);
      assertTrue(autobidService.isAutoBidActive(100, 1), "Auto-bid must be recorded as active.");
    }
  }

  @Test
  void testEnableAutoBid_AlreadyLeading_SkipsInitialBid() {
    Bidder mockBidder = Mockito.mock(Bidder.class);
    Mockito.when(mockBidder.getId()).thenReturn(2);
    Mockito.when(mockBidder.getName()).thenReturn("Leading Bidder");
    Mockito.when(mockBidder.isActive()).thenReturn(true);

    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(200);
    Mockito.when(mockAuction.getCurrentPrice()).thenReturn(100.0);
    Mockito.when(mockAuction.getHighestBidder()).thenReturn(mockBidder);

    AutoBidConfig config = new AutoBidConfig(1000.0, 20.0);
    BidService mockBidService = Mockito.mock(BidService.class);

    try (MockedStatic<AutoBidDAO> mockedDao = Mockito.mockStatic(AutoBidDAO.class);
        MockedStatic<BidService> mockedBidServiceStatic = Mockito.mockStatic(BidService.class)) {

      mockedBidServiceStatic.when(BidService::getInstance).thenReturn(mockBidService);

      autobidService.enableAutoBid(mockBidder, mockAuction, config);

      mockedDao.verify(() -> AutoBidDAO.saveAutoBid(200, 2, 1000.0, 20.0), Mockito.times(1));
      Mockito.verify(mockBidService, Mockito.never())
          .processAutoBid(Mockito.any(), Mockito.any(), Mockito.anyDouble());
    }
  }

  @Test
  void testProcessQueue_MaxBidReached() {
    Bidder mockBidder = Mockito.mock(Bidder.class);
    Mockito.when(mockBidder.getId()).thenReturn(3);
    Mockito.when(mockBidder.getName()).thenReturn("Tester");
    Mockito.when(mockBidder.isActive()).thenReturn(true);

    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(300);
    Mockito.when(mockAuction.getCurrentPrice()).thenReturn(95.0);

    AutoBidConfig config = new AutoBidConfig(100.0, 10.0);

    BidService mockBidService = Mockito.mock(BidService.class);
    AuctionManager mockAuctionManager = Mockito.mock(AuctionManager.class);

    try (MockedStatic<AutoBidDAO> mockedDao = Mockito.mockStatic(AutoBidDAO.class);
        MockedStatic<BidService> mockedBidServiceStatic = Mockito.mockStatic(BidService.class);
        MockedStatic<AuctionManager> mockedManagerStatic = Mockito.mockStatic(AuctionManager.class)) {

      mockedManagerStatic.when(AuctionManager::getInstance).thenReturn(mockAuctionManager);
      Mockito.when(mockAuctionManager.getActive(300)).thenReturn(mockAuction);

      autobidService.enableAutoBid(mockBidder, mockAuction, config);
      autobidService.processQueue(300);

      assertFalse(autobidService.isAutoBidActive(300, 3),
          "Auto-bid must be deactivated due to exceeding the maximum bid.");
    }
  }

  @Test
  void testDisableAndClearAuction() {
    Bidder mockBidder = Mockito.mock(Bidder.class);
    Mockito.when(mockBidder.getId()).thenReturn(4);
    Mockito.when(mockBidder.getName()).thenReturn("Tester");
    Mockito.when(mockBidder.isActive()).thenReturn(true);

    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(400);

    AutoBidConfig config = new AutoBidConfig(500.0, 10.0);

    try (MockedStatic<AutoBidDAO> mockedDao = Mockito.mockStatic(AutoBidDAO.class)) {
      autobidService.enableAutoBid(mockBidder, mockAuction, config);
      assertTrue(autobidService.isAutoBidActive(400, 4));

      autobidService.disableAutoBid(400, 4);
      assertFalse(autobidService.isAutoBidActive(400, 4));
      mockedDao.verify(() -> AutoBidDAO.deleteAutoBid(400, 4), Mockito.times(2));

      autobidService.clearAuction(400);
      assertFalse(autobidService.isAutoBidActive(400, 4));
    }
  }

  @Test
  void testProcessQueue_Success_NotifiesClients() {
    Bidder mockBidder = Mockito.mock(Bidder.class);
    Mockito.when(mockBidder.getId()).thenReturn(5);
    Mockito.when(mockBidder.getName()).thenReturn("Tester");
    Mockito.when(mockBidder.isActive()).thenReturn(true);

    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(500);
    Mockito.when(mockAuction.getCurrentPrice()).thenReturn(200.0);

    AutoBidConfig config = new AutoBidConfig(1000.0, 50.0);

    BidService mockBidService = Mockito.mock(BidService.class);
    Mockito.when(mockBidService.processAutoBid(mockBidder, mockAuction, 250.0)).thenReturn(true);

    AuctionManager mockAuctionManager = Mockito.mock(AuctionManager.class);
    ClientRegistry mockRegistry = Mockito.mock(ClientRegistry.class);

    try (MockedStatic<AutoBidDAO> mockedDao = Mockito.mockStatic(AutoBidDAO.class);
        MockedStatic<BidService> mockedBidServiceStatic = Mockito.mockStatic(BidService.class);
        MockedStatic<AuctionManager> mockedManagerStatic = Mockito.mockStatic(AuctionManager.class);
        MockedStatic<ClientRegistry> mockedRegistryStatic = Mockito.mockStatic(ClientRegistry.class)) {

      mockedManagerStatic.when(AuctionManager::getInstance).thenReturn(mockAuctionManager);
      Mockito.when(mockAuctionManager.getActive(500)).thenReturn(mockAuction);

      mockedBidServiceStatic.when(BidService::getInstance).thenReturn(mockBidService);
      mockedRegistryStatic.when(ClientRegistry::getInstance).thenReturn(mockRegistry);

      autobidService.enableAutoBid(mockBidder, mockAuction, config);
      autobidService.processQueue(500);

      // Đổi thành atLeastOnce vì enableAutoBid và processQueue có thể gọi notify 2 lần
      Mockito.verify(mockRegistry, Mockito.atLeastOnce()).notifyAll(Mockito.eq(500), Mockito.any());
    }
  }

  @Test
  void testProcessQueue_HandlesException() {
    Bidder mockBidder = Mockito.mock(Bidder.class);
    Mockito.when(mockBidder.getId()).thenReturn(6);
    Mockito.when(mockBidder.getName()).thenReturn("Tester");
    Mockito.when(mockBidder.isActive()).thenReturn(true);

    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(600);
    Mockito.when(mockAuction.getCurrentPrice()).thenReturn(300.0);

    AutoBidConfig config = new AutoBidConfig(1000.0, 50.0);

    BidService mockBidService = Mockito.mock(BidService.class);
    Mockito.when(mockBidService.processAutoBid(Mockito.any(), Mockito.any(), Mockito.anyDouble()))
        .thenThrow(new RuntimeException("Database offline"));

    AuctionManager mockAuctionManager = Mockito.mock(AuctionManager.class);

    try (MockedStatic<AutoBidDAO> mockedDao = Mockito.mockStatic(AutoBidDAO.class);
        MockedStatic<BidService> mockedBidServiceStatic = Mockito.mockStatic(BidService.class);
        MockedStatic<AuctionManager> mockedManagerStatic = Mockito.mockStatic(AuctionManager.class)) {

      mockedManagerStatic.when(AuctionManager::getInstance).thenReturn(mockAuctionManager);
      Mockito.when(mockAuctionManager.getActive(600)).thenReturn(mockAuction);
      mockedBidServiceStatic.when(BidService::getInstance).thenReturn(mockBidService);

      autobidService.enableAutoBid(mockBidder, mockAuction, config);

      assertDoesNotThrow(() -> autobidService.processQueue(600),
          "Exceptions within the queue processing must be caught and logged safely.");
    }
  }

  @Test
  void testProcessQueue_EmptyOrNull() {
    AuctionManager mockAuctionManager = Mockito.mock(AuctionManager.class);

    try (MockedStatic<AuctionManager> mockedManagerStatic = Mockito.mockStatic(
        AuctionManager.class)) {
      mockedManagerStatic.when(AuctionManager::getInstance).thenReturn(mockAuctionManager);
      Mockito.when(mockAuctionManager.getActive(999)).thenReturn(null);

      assertDoesNotThrow(() -> autobidService.processQueue(999),
          "Processing an empty queue or null auction must safely return without errors.");
    }
  }

  @Test
  void testUpdate_TriggersExecutor() {
    assertDoesNotThrow(() -> autobidService.update(888, 100.0, "WinnerX"),
        "The update trigger from Observer must dispatch properly without exceptions.");
  }
}