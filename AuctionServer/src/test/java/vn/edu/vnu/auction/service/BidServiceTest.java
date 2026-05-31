package vn.edu.vnu.auction.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import vn.edu.vnu.auction.common.exception.InvalidBidException;
import vn.edu.vnu.auction.dao.AuctionDAO;
import vn.edu.vnu.auction.dao.BidDAO;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.BidTransaction;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.util.AuctionManager;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link BidService}.
 */
class BidServiceTest {

  private BidService bidService;
  private AuctionManager mockAuctionManager;
  private AuctionService mockAuctionService;

  @BeforeEach
  void setUp() throws Exception {
    BidService.resetForTesting();
    AuctionManager.resetForTesting();
    AuctionService.resetForTesting();

    bidService = BidService.getInstance();

    mockAuctionManager = Mockito.mock(AuctionManager.class);
    mockAuctionService = Mockito.mock(AuctionService.class);

    Field managerField = BidService.class.getDeclaredField("auctionManager");
    managerField.setAccessible(true);
    managerField.set(bidService, mockAuctionManager);

    Field serviceField = BidService.class.getDeclaredField("auctionService");
    serviceField.setAccessible(true);
    serviceField.set(bidService, mockAuctionService);
  }

  @AfterEach
  void tearDown() {
    BidService.resetForTesting();
    AuctionManager.resetForTesting();
    AuctionService.resetForTesting();
  }

  @Test
  void testGetInstance() {
    BidService instance1 = BidService.getInstance();
    BidService instance2 = BidService.getInstance();

    assertNotNull(instance1, "Instance không được để null.");
    assertSame(instance1, instance2, "Chỉ được phép tồn tại một đối tượng BidService duy nhất.");
  }

  @Test
  void testPlaceBid_AuctionNotActive() throws Exception {
    Mockito.when(mockAuctionManager.getActive(99)).thenReturn(null);

    Bidder mockBidder = Mockito.mock(Bidder.class);
    boolean result = bidService.placeBid(99, mockBidder, 100.0);

    assertFalse(result, "Hàm phải trả về false nếu không tìm thấy phiên đấu giá.");
  }

  @Test
  void testPlaceBid_AuctionNotRunning() {
    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getStatus()).thenReturn(AuctionStatus.OPEN);
    Mockito.when(mockAuctionManager.getActive(1)).thenReturn(mockAuction);

    Bidder mockBidder = Mockito.mock(Bidder.class);

    assertThrows(InvalidBidException.class, () -> bidService.placeBid(1, mockBidder, 500.0),
        "Phải ném ra InvalidBidException nếu phiên đấu giá chưa bắt đầu.");
  }

  @Test
  void testPlaceBid_Success() throws Exception {
    int auctionId = 10;
    int bidderId = 5;
    double bidAmount = 1000.0;

    Item mockItem = Mockito.mock(Item.class);
    Bidder mockBidder = Mockito.mock(Bidder.class);
    Mockito.when(mockBidder.getId()).thenReturn(bidderId);

    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(auctionId);
    Mockito.when(mockAuction.getStatus()).thenReturn(AuctionStatus.RUNNING);
    Mockito.when(mockAuction.getItem()).thenReturn(mockItem);
    Mockito.when(mockAuction.getBids()).thenReturn(new ArrayList<>());

    Mockito.when(mockAuctionManager.getActive(auctionId)).thenReturn(mockAuction);

    try (MockedStatic<BidDAO> mockedDao = Mockito.mockStatic(BidDAO.class)) {
      mockedDao.when(() -> BidDAO.insertBid(auctionId, bidderId, bidAmount)).thenReturn(true);

      boolean result = bidService.placeBid(auctionId, mockBidder, bidAmount);

      assertTrue(result, "Hàm phải trả về true khi đặt giá thành công.");
      Mockito.verify(mockAuction, Mockito.times(1)).placeBid(Mockito.any(), Mockito.eq(false));
      Mockito.verify(mockAuction, Mockito.times(1)).notifyObservers();
    }
  }

  @Test
  void testPlaceBid_DatabaseFailure_Rollback() {
    int auctionId = 20;
    int bidderId = 7;
    double bidAmount = 1500.0;

    Item mockItem = Mockito.mock(Item.class);
    Bidder mockBidder = Mockito.mock(Bidder.class);
    Mockito.when(mockBidder.getId()).thenReturn(bidderId);

    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(auctionId);
    Mockito.when(mockAuction.getStatus()).thenReturn(AuctionStatus.RUNNING);
    Mockito.when(mockAuction.getItem()).thenReturn(mockItem);
    Mockito.when(mockAuction.getCurrentPrice()).thenReturn(500.0);

    List<BidTransaction> mockBids = new ArrayList<>();
    Mockito.when(mockAuction.getBids()).thenReturn(mockBids);

    Mockito.when(mockAuctionManager.getActive(auctionId)).thenReturn(mockAuction);

    try (MockedStatic<BidDAO> mockedDao = Mockito.mockStatic(BidDAO.class)) {
      mockedDao.when(() -> BidDAO.insertBid(auctionId, bidderId, bidAmount)).thenReturn(false);

      assertThrows(InvalidBidException.class,
          () -> bidService.placeBid(auctionId, mockBidder, bidAmount),
          "Phải ném ra ngoại lệ InvalidBidException do lỗi DB.");

      Mockito.verify(mockAuction, Mockito.times(1)).setCurrentPrice(500.0);
    }
  }

  @Test
  void testProcessAutoBid_HandlesExceptions() {
    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.getId()).thenReturn(30);
    Mockito.when(mockAuction.getStatus()).thenReturn(AuctionStatus.OPEN);

    Mockito.when(mockAuctionManager.getActive(30)).thenReturn(mockAuction);

    Bidder mockBidder = Mockito.mock(Bidder.class);

    boolean result = bidService.processAutoBid(mockBidder, mockAuction, 2000.0);

    assertFalse(result, "Hàm phải bắt lỗi và trả về false nếu đặt giá tự động thất bại.");
  }

  @Test
  void testApplyAntiSnipe_Extended() {
    Auction mockAuction = Mockito.mock(Auction.class);
    Mockito.when(mockAuction.tryExtendForAntiSnipe(Mockito.anyLong(), Mockito.anyLong()))
        .thenReturn(true);
    Mockito.when(mockAuction.getSecondsRemaining()).thenReturn(100L);

    Item mockItem = Mockito.mock(Item.class);
    Mockito.when(mockAuction.getItem()).thenReturn(mockItem);

    try (MockedStatic<AuctionDAO> mockedAuctionDao = Mockito.mockStatic(AuctionDAO.class)) {
      bidService.applyAntiSnipe(mockAuction);

      Mockito.verify(mockAuctionService, Mockito.times(1)).scheduleEnd(mockAuction, 100L);
      mockedAuctionDao.verify(
          () -> AuctionDAO.updateAuctionEndTime(Mockito.anyInt(), Mockito.any()), Mockito.times(1));
    }
  }

  @Test
  void testGetBidsByAuction() {
    int auctionId = 55;
    List<BidTransaction> expectedList = new ArrayList<>();

    try (MockedStatic<BidDAO> mockedDao = Mockito.mockStatic(BidDAO.class)) {
      mockedDao.when(() -> BidDAO.getBidsByAuction(auctionId)).thenReturn(expectedList);

      List<BidTransaction> actualList = bidService.getBidsByAuction(auctionId);

      assertSame(expectedList, actualList, "Danh sách trả về phải lấy từ DAO.");
    }
  }
}