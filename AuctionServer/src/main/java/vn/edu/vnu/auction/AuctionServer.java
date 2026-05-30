package vn.edu.vnu.auction;

import vn.edu.vnu.auction.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server chính của hệ thống đấu giá
 *
 * <p> luồng hoạt động
 * <pre>
 *     ServerSocket lắng nghe port 8080
 *     khi có client kết nối:
 *     - tạo clientHandle mới
 *     - submit vào ThreadPool để chạy
 *     - tiếp tục lắng nghe client tiếp theo
 * </pre>
 * </p>
 */
public class AuctionServer {
    private static final Logger logger = LoggerFactory.getLogger(AuctionServer.class);

    private static int PORT;
    static {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("src/main/resources/config.properties")) {
            props.load(fis);
            PORT = Integer.parseInt(props.getProperty("server.port", "8080"));
        } catch (IOException e) {
            logger.warn("Failed to load config.properties, using default port 8080", e);
            PORT = 8080;
        }
    }
    private ServerSocket serverSocket;
    private final ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();
    private volatile boolean running = false;

    /**
     * Khởi động server
     * chạy vòng lặp vô hạn cho đến khi gọi stop()
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            logger.info("Server started on port {}", PORT);

            AuctionService.getInstance().getActiveAuctions();
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    logger.info("New Client connected: " +
                            clientSocket.getInetAddress().getHostAddress()
                            + ":" + clientSocket.getPort());
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    threadPool.submit(clientHandler);
                }
                catch (IOException e) {
                    if (!running) {
                        break;
                    }
                    logger.warn("Error accepting client", e);
                }
            }
        }catch (IOException e){
            logger.warn("Cannot start server on port {} ", PORT, e);
        } finally {
            shutdown();
        }
    }

    /**
     * Dừng server
     * gọi khi muốn tắt server
     */
    private void stop(){
        running = false;
        try{
            if(serverSocket == null || !serverSocket.isClosed()){
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.warn("Error closing server socket: {}", e.getMessage());
        }
    }

    /**
     * Tắt thread pool và dọn dẹp tài nguyên.
     */
    private void shutdown(){
        threadPool.shutdown();
        logger.info("AuctionServer stopped");
    }

    /**
     * Điểm nhập chính cho ứng dụng AuctionServer.
     * <p>
     * Khởi tạo server, thiết lập shutdown hook để kết thúc một cách an toàn,
     * và bắt đầu lắng nghe kết nối từ client.
     * </p>
     *
     * @param args đối số dòng lệnh (không sử dụng)
     */
    public static void main(String[] args) {
        final AuctionServer server = new AuctionServer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down server");
            try(Connection conn = vn.edu.vnu.auction.database.DatabaseConfig.getConnection();
                java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA wal_checkpoint(TRUNCATE)");
                logger.info("WAL checkpoint completed");
            }
            catch (Exception e) {
                logger.error("WAL checkpoint failed: {}", e.getMessage());
            }
            server.stop();
        }));
        server.start();
    }
}

