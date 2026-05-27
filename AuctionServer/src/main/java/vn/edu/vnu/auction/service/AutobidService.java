package vn.edu.vnu.auction.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.dao.AutoBidDAO;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.auto_bidding.AutoBidAgent;
import vn.edu.vnu.auction.model.entity.auto_bidding.AutoBidConfig;
import vn.edu.vnu.auction.model.entity.user.Bidder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutobidService {
    private static Logger logger = LoggerFactory.getLogger(AutobidService.class);
    private static AutobidService instance;
    private final Map<String, AutoBidAgent> agents = new ConcurrentHashMap<>();

    public static AutobidService getInstance(){
        if(instance == null){
            instance = new AutobidService();
        }
        return instance;
    }

    public void enableAutoBid(Bidder bidder, Auction auction, AutoBidConfig config) {
        String key = auction.getId() + "-" + bidder.getId();
        AutoBidAgent existing = agents.get(key);
        if(existing != null) existing.stop();
        AutoBidDAO.saveAutoBid(auction.getId(), bidder.getId(), config.getMaxBid(), config.getIncrement());
        AutoBidAgent agent = new AutoBidAgent(bidder, auction,config);
        agents.put(key, agent);
        logger.debug("AutoBid enabled: bidder={}, auction={}", bidder.getName(), auction.getId());
    }

    public void disableAutoBid(int auctionId, int bidderId) {
        String key = auctionId + "-" + bidderId;
        AutoBidAgent agent = agents.remove(key);
        if (agent != null) agent.stop();
        AutoBidDAO.deleteAutoBid(auctionId, bidderId);
        logger.info("AutoBid disabled: bidderId={}, auctionId={}", bidderId, auctionId);
 }
}
