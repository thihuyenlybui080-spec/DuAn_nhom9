package org.example.loginregister.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Quản lý kết nối Socket giữa Client và Server
 * dÙNG singletom để đảm bảo toàn bộ web chỉ có một kết nối duy nhất
 *
 */
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class.getName());

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    private static final int CONNECT_TIMEOUT_MS = 5000;
     private static ConnectionManager instance;
     public static ConnectionManager getInstance(){
         if(instance == null){
             instance = new ConnectionManager();
         }
         return instance;
     }
     private ConnectionManager(){}
     private Socket socket;
     private ObjectOutputStream outputStream;
     private ObjectInputStream inputStream;
     private boolean connected = false;

    /**
     * Mở kết nối đến server
     * @return true nếu kết nối thành công
     */
     public boolean connect(){
         try{
             socket = new Socket(SERVER_HOST, SERVER_PORT);
             socket.setSoTimeout(CONNECT_TIMEOUT_MS);
             outputStream = new ObjectOutputStream(socket.getOutputStream());
             outputStream.flush();
             inputStream = new ObjectInputStream(socket.getInputStream());
             connected = true;
             logger.info("Connected to server {} : {}", SERVER_HOST, SERVER_PORT );
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
         if(!connected || socket == null || socket.isConnected()){
             return false;
         }
         return true;
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
