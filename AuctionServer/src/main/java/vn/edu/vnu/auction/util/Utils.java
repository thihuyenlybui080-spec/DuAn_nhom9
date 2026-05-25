package vn.edu.vnu.auction.util;

public class Utils {
    public static int parseDbId(String entityId) {
        try {
            String[] parts = entityId.split("-");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception e) {
            return -1;
        }
    }
}
