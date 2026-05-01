package org.example.loginregister.server.util;

import org.example.loginregister.server.model.entity.AuctionResult;
import org.example.loginregister.server.model.entity.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionHistoryManager {

    private static final AuctionHistoryManager instance = new AuctionHistoryManager();
    private final Map<String, AuctionResult> completedAuctions = new ConcurrentHashMap<>();

    private AuctionHistoryManager() {}   // private constructor cho Singleton

    public static AuctionHistoryManager getInstance() {
        return instance;
    }


    // Lưu kết quả phiên đấu giá khi kết thúc
    public void saveResult(AuctionResult result) {
        if (result != null) {
            completedAuctions.put(result.getAuctionId(), result);
            System.out.println("AUCTION RESULT SAVED: " + result);
        }
    }


    //Lấy kết quả của một phiên theo ID
    public AuctionResult getResult(String auctionId) {
        return completedAuctions.get(auctionId);
    }


    // Lấy toàn bộ lịch sử đấu giá
    public List<AuctionResult> getAllResults() {
        return new ArrayList<>(completedAuctions.values());
    }


    //Lấy lịch sử theo người thắng (tùy chọn)

    public List<AuctionResult> getResultsByWinner(User winner) {
        List<AuctionResult> list = new ArrayList<>();
        for (AuctionResult r : completedAuctions.values()) {
            if (r.getWinner() != null && r.getWinner().equals(winner)) {
                list.add(r);
            }
        }
        return list;
    }


    //Xóa lịch sử (dùng khi test hoặc reset hệ thống)
    public void clearHistory() {
        completedAuctions.clear();
    }
}
