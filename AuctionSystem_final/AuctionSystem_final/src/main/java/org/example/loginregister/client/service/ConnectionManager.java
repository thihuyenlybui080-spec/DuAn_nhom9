package org.example.loginregister.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * Quản lý kết nối Socket giữa Client và Server
 * dÙNG singletom để đảm bảo toàn bộ web chỉ có một kết nối duy nhất
 *
 */
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class.getName());

    private String serverHost = "localhost";
    private int serverPort = 8080;

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static ConnectionManager instance;
    public static ConnectionManager getInstance(){
        if(instance == null){
            instance = new ConnectionManager();
        }
        return instance;
    }
    private ConnectionManager(){
        loadConfig();
    }
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private boolean connected = false;

    /**
     * đọc cấu hình hệ thống của Server
     * Đầu tiên kiểm tra thư mục bên ngoài, sau đó vào trong phần mềm trong resources
     */
    private void loadConfig() {
        Yaml yaml = new Yaml();
        InputStream input = null;
        boolean isExternalFile = false;
        
        try {
            // First, try to load from external file (working directory)
            File externalFile = new File("application.yaml");
            if (externalFile.exists() && externalFile.isFile()) {
                input = new FileInputStream(externalFile);
                isExternalFile = true;
                logger.info("Loading configuration from external file: {}", externalFile.getAbsolutePath());
            } else {
                // Fallback to resources
                input = getClass().getClassLoader().getResourceAsStream("application.yaml");
                if (input == null) {
                    logger.warn("application.yaml not found in external location or resources, using default values");
                    return;
                }
                logger.info("Loading configuration from resources");
            }
            
            // Parse YAML file
            Map<String, Object> config = yaml.load(input);
            
            if (config != null && config.containsKey("server")) {
                Map<String, Object> serverConfig = (Map<String, Object>) config.get("server");
                
                if (serverConfig != null) {
                    // Extract server.ip
                    Object ipObj = serverConfig.get("ip");
                    if (ipObj != null) {
                        String ip = ipObj.toString().trim();
                        if (!ip.isEmpty()) {
                            this.serverHost = ip;
                        }
                    }
                    
                    // Extract server.port
                    Object portObj = serverConfig.get("port");
                    if (portObj != null) {
                        try {
                            this.serverPort = Integer.parseInt(portObj.toString().trim());
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid server port: {}, using default 8080", portObj);
                        }
                    }
                }
            }
            
            logger.info("Loaded config - Server IP: {}, Port: {}", this.serverHost, this.serverPort);
            
        } catch (IOException e) {
            logger.warn("Failed to load configuration, using default values", e);
        } catch (Exception e) {
            logger.warn("Error parsing YAML configuration, using default values", e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.warn("Error closing input stream", e);
                }
            }
        }
    }

    /**
     * Mở kết nối đến server
     * @return true nếu kết nối thành công
     */
    public boolean connect(){
        try{
            socket = new Socket(serverHost, serverPort);
            socket.setSoTimeout(0);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());
            connected = true;
            logger.info("Connected to server {} : {}", serverHost, serverPort );
            return true;
        } catch (IOException e){
            logger.warn("Cannot connect to server", e);
            connected = false;
            return false;
        }
    }

    /**
     * Đóng kết nối
     * Gọi khi app tắt hoặc user sign out
     */
    public  void disconnect(){
        connected = false;
        try{
            if(inputStream != null) inputStream.close();
            if(outputStream != null) outputStream.close();
            if(socket != null && !socket.isClosed()) socket.close();
            logger.info("Disconnected from server.");
        } catch (IOException e){
            logger.warn("Error during diconnect: {}", e.getMessage());
        }
    }

    /**
     * Kiểm tra kết nối
     * nếu mất kết nối thì tự dộng reconnect.
     * @return
     */
    public boolean isConnected(){
        return  connected && socket != null
                && !socket.isClosed() && socket.isConnected();
    }

    /**
     * Thử kết nối lại nếu mất kêt nối
     * @return true nếu kết nối thành công
     */
    public boolean reconnect(){
        logger.info("Attempting to reconnect");
        disconnect();
        return connect();
    }

    public ObjectOutputStream getOutputStream(){
        return outputStream;
    }
    public  ObjectInputStream getInputStream(){
        return  inputStream;
    }

}
