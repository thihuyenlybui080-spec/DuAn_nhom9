package org.example.loginregister.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ================================================================
 *  CẤU HÌNH KẾT NỐI DATABASE
 * ================================================================
 *
 *  Đọc cấu hình từ file config.propertie đặt cùng thư mục với .jar
 *  Nếu không có file ngoài thì đọc file mặc định bên trong jar.
 * tạo một bể chứa kết nối, chứa sẵn 20 kết nối, mỗi khi gọi => bốc một kết nối có sẵn
 *  Nội dung config.properties:
 *    db.host=localhost
 *    db.port=3306
 *    db.name=loginregister
 *    db.user=root
 *    db.pass=root
 * ================================================================
 */
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private static DatabaseConfig instance;
    private static final Object lock = new Object();

    private final String DB_HOST;
    private final String DB_PORT;
    private final String DB_NAME;
    private final String DB_USER;
    private final String DB_PASS;
    private final String DB_URL;
    private DataSource jdbcDataSource;

    private DatabaseConfig() {
        Properties props = new Properties();
        File externalConfig = new File("config.properties");
        if (externalConfig.exists()) {
            try (InputStream in = new FileInputStream(externalConfig)) {
                props.load(in);
                logger.info("DatabaseConfig: read config from external file: {}", externalConfig.getAbsolutePath());
            } catch (Exception e) {
                logger.error("DatabaseConfig: read external config failed: {}", e.getMessage());
            }
        } else {
            try (InputStream in = DatabaseConfig.class.getResourceAsStream(
                    "/org/example/loginregister/config.properties")) {
                if (in != null) {
                    props.load(in);
                    logger.info("DatabaseConfig: read config from internal file");
                }
            } catch (Exception e) {
                logger.error("DatabaseConfig: can not read internal config, use default config: local host");
            }
        }

        DB_HOST = props.getProperty("db.host", "localhost").trim();
        DB_PORT = props.getProperty("db.port", "3306").trim();
        DB_NAME = props.getProperty("db.name", "loginregister").trim();
        DB_USER = props.getProperty("db.user", "root").trim();
        DB_PASS = props.getProperty("db.pass", "root").trim();

        /**
         * ghép các biến thành một đường link kết nối chuẩn JDBC cho MySQL
         * tắt mã hóa SSL, cho phép lấy khóa bảo mật, cài múi giờ Việt Nam
         * bật hỗ trợ tiếng việt có dấu
         */
        DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
                + "&useUnicode=true&characterEncoding=UTF-8";

        logger.info("DatabaseConfig -> {}:{}:{}", DB_HOST, DB_PORT, DB_NAME);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASS);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("AuctionSystemHikariPool");

        jdbcDataSource = new HikariDataSource(config);
        logger.info("DatabaseConfig: HikariCP connection pool initialized");
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

    public static String getServerInfo() {
        return getInstance().DB_HOST + ":" + getInstance().DB_PORT + "/" + getInstance().DB_NAME;
    }
}
