package org.example.loginregister.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
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
 *
 *  Nội dung config.propertie:
 *    db.host=localhost
 *    db.port=3306
 *    db.name=loginregister
 *    db.user=root
 *    db.pass=root
 * ================================================================
 */
public class DatabaseConfig {

    private static final String DB_HOST;
    private static final String DB_PORT;
    private static final String DB_NAME;
    private static final String DB_USER;
    private static final String DB_PASS;
    private static final String DB_URL;

    static {
        Properties props = new Properties();

        // Ưu tiên 1: đọc config.properties bên NGOÀI jar (cùng thư mục với .jar)
        File externalConfig = new File("config.properties");
        if (externalConfig.exists()) {
            try (InputStream in = new FileInputStream(externalConfig)) {
                props.load(in);
                System.out.println("DatabaseConfig: doc config tu file ngoai: " + externalConfig.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("DatabaseConfig: loi doc config ngoai: " + e.getMessage());
            }
        } else {
            // Ưu tiên 2: đọc config.properties bên TRONG jar
            try (InputStream in = DatabaseConfig.class.getResourceAsStream(
                    "/org/example/loginregister/config.properties")) {
                if (in != null) {
                    props.load(in);
                    System.out.println("DatabaseConfig: doc config tu trong jar");
                }
            } catch (Exception e) {
                System.err.println("DatabaseConfig: khong doc duoc config, dung mac dinh localhost");
            }
        }

        DB_HOST = props.getProperty("db.host", "localhost").trim();
        DB_PORT = props.getProperty("db.port", "3306").trim();
        DB_NAME = props.getProperty("db.name", "loginregister").trim();
        DB_USER = props.getProperty("db.user", "root").trim();
        DB_PASS = props.getProperty("db.pass", "root").trim();

        DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
                + "&useUnicode=true&characterEncoding=UTF-8";

        System.out.println("DatabaseConfig -> " + DB_HOST + ":" + DB_PORT + "/" + DB_NAME);

        // Initialize HikariCP connection pool
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
        System.out.println("DatabaseConfig: HikariCP connection pool initialized");
    }

    public static DataSource jdbcDataSource;

    public static Connection getConnection() throws SQLException {
        return jdbcDataSource.getConnection();
    }

    public static String getServerInfo() {
        return DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    }
}
