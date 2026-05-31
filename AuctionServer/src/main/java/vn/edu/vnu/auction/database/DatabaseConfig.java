package vn.edu.vnu.auction.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ================================================================ CẤU HÌNH KẾT NỐI DATABASE
 * ================================================================ Đọc cấu hình từ file
 * config.propertie đặt cùng thư mục với .jar Nếu không có file ngoài thì đọc file mặc định bên
 * trong jar. tạo một bể chứa kết nối, chứa sẵn 20 kết nối, mỗi khi gọi => bốc một kết nối có sẵn
 * Nội dung config.properties: db.host=localhost db.port=3306 db.name=loginregister db.user=root
 * db.pass=root ================================================================
 */
public class DatabaseConfig {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

  private static volatile DatabaseConfig instance;
  private static final Object lock = new Object();

  private final String DB_NAME;
  private final DataSource jdbcDataSource;

  private DatabaseConfig() {
    Properties props = new Properties();

    // Ưu tiên 1: đọc config.properties bên NGOÀI jar (cùng thư mục với .jar)
    File externalConfig = new File("config.properties");
    if (externalConfig.exists()) {
      try (InputStream in = new FileInputStream(externalConfig)) {
        props.load(in);
        logger.info("DatabaseConfig: read config from external file: {}",
            externalConfig.getAbsolutePath());
      } catch (Exception e) {
        logger.error("DatabaseConfig: read external config failed: {}", e.getMessage());
      }
    } else {
      // Ưu tiên 2: đọc config.properties bên TRONG jar
      try (InputStream in = DatabaseConfig.class.getResourceAsStream(
          "/config.properties")) {
        if (in != null) {
          props.load(in);
          logger.info("DatabaseConfig: read config from internal file");
        }
      } catch (Exception e) {
        logger.error(
            "DatabaseConfig: can not read internal config, use default config: local host");
      }
    }

    DB_NAME = props.getProperty("db.name", "auction_system.db").trim();

        /*
          ghép các biến thành một đường link kết nối chuẩn JDBC cho SQLite
          SQLite sử dụng file-based database
         */
    String DB_URL = "jdbc:sqlite:" + DB_NAME;

    logger.info("DatabaseConfig -> SQLite database: {}", DB_NAME);

    HikariConfig config = getHikariConfig(DB_URL);

    jdbcDataSource = new HikariDataSource(config);
    logger.info("DatabaseConfig: HikariCP connection pool initialized");
    initializeDatabase();
  }

  private static HikariConfig getHikariConfig(String DB_URL) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(DB_URL);
    config.setDriverClassName("org.sqlite.JDBC");
    config.setMaximumPoolSize(20);
    config.setMinimumIdle(2);
    config.setConnectionTimeout(30000);
    config.setIdleTimeout(600000);
    config.setMaxLifetime(1800000);
    config.setPoolName("AuctionSystemHikariPool");
    config.addDataSourceProperty("journal_mode", "WAL");
    config.addDataSourceProperty("synchronous", "NORMAL");
    config.addDataSourceProperty("busy_timeout", "30000");
    return config;
  }

  /**
   * ================================================================ KHỞI TẠO DATABASE TỰ ĐỘNG
   * ================================================================ - Nếu file .db chưa tồn tại →
   * chạy auction_system.sql để tạo bảng + data - Ưu tiên 1: đọc auction_system.sql bên NGOÀI jar
   * (cùng thư mục với .jar) - Ưu tiên 2: đọc auction_system.sql bên TRONG jar (trong resources) -
   * Nếu file .db đã tồn tại → bỏ qua, giữ nguyên dữ liệu cũ
   * ================================================================
   */
  private void initializeDatabase() {
    try (Connection conn = jdbcDataSource.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='users'")) {
      if (rs.next()) {
        logger.info("DatabaseConfig: database already initialized, skipping");
        return;
      }
    } catch (SQLException e) {
      logger.info("DatabaseConfig: checking tables, will initialize...");
    }
    logger.info("DatabaseConfig: database file not found, initializing from auction_system.sql...");

    String sql = null;
    File externalSql = new File("auction_system.sql");
    if (externalSql.exists()) {
      try (InputStream in = new FileInputStream(externalSql)) {
        sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        logger.info("DatabaseConfig: read SQL from external file: {}",
            externalSql.getAbsolutePath());
      } catch (Exception e) {
        logger.error("DatabaseConfig: failed to read external SQL file: {}", e.getMessage());
      }
    }

    // Ưu tiên 2: đọc auction_system.sql bên trong jar (resources)
    if (sql == null) {
      try (InputStream in = DatabaseConfig.class.getResourceAsStream("/auction_system.sql")) {
        if (in != null) {
          sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
          logger.info("DatabaseConfig: read SQL from internal resources");
        }
      } catch (Exception e) {
        logger.error("DatabaseConfig: failed to read internal SQL file: {}", e.getMessage());
      }
    }

    if (sql == null) {
      logger.warn("DatabaseConfig: auction_system.sql not found, database will be empty");
      return;
    }

    // Chạy từng câu SQL (tách theo dấu ";")
    try (Connection conn = jdbcDataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      String[] lines = sql.split("\n");
      StringBuilder cleanSql = new StringBuilder();
      for (String line : lines) {
        String trimmedLine = line.trim();
        if (!trimmedLine.startsWith("--")) {
          cleanSql.append(line).append("\n");
        }
      }

      String[] statements = cleanSql.toString().split(";");
      for (String s : statements) {
        String trimmed = s.trim();
        if (!trimmed.isEmpty()) {
          stmt.execute(trimmed);
        }
      }
      logger.info("DatabaseConfig: database initialized successfully from auction_system.sql");
    } catch (SQLException e) {
      logger.error("DatabaseConfig: failed to initialize database: {}", e.getMessage());
    }
  }


  public static DatabaseConfig getInstance() {
    if (instance == null) {
      synchronized (lock) {
        if (instance == null) {
          instance = new DatabaseConfig();
        }
      }
    }
    return instance;
  }

  public static Connection getConnection() throws SQLException {
    return getInstance().jdbcDataSource.getConnection();
  }
}
