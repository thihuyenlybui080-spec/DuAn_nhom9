package vn.edu.vnu.auction.common.network;
import java.io.Serializable;

/**
 * Đại diện cho một phản hồi từ Server trả về Client
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private Object data;
    private String requestId;

    private Response(boolean success, String message, Object data, String requestId){
        this.success = success;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
    }

    /**
     * Tạo phản hồi thành công kèm dữ liệu
     * @param data dữ liệu phản hồi
     * @return trả về một phản hồi thành công
     */
    public static Response ok(Object data){
        return new Response(true, "OK", data, null);
    }

    /**
     * Tạo phản hồi thành công và tùy chỉnh tin nhắn
     * @param message tin nhắn tự tùy chỉnh
     * @param data dữ liệu phản hồi
     * @return một phản hồi
     */
    public static Response ok(String message, Object data){
        return new Response(true, message, data, null);
    }
    /**
     * Tạo phản hồi thành công kèm dữ liệu và requestId
     * @param data dữ liệu phản hồi
     * @param requestId ID của request tương ứng
     * @return trả về một phản hồi thành công
     */
    public static Response ok(Object data, String requestId){
        return new Response(true, "OK", data, requestId);
    }

    /**
     * Tạo phản hồi thành công và tùy chỉnh tin nhắn kèm requestId
     * @param message tin nhắn tự tùy chỉnh
     * @param data dữ liệu phản hồi
     * @param requestId ID của request tương ứng
     * @return một phản hồi
     */
    public static Response ok(String message, Object data, String requestId){
        return new Response(true, message, data, requestId);
    }

    /**
     * Tạo một phản hồi thất bại kèm thông báo lỗi và requestId.
     * @param message thông báo lỗi
     * @param requestId ID của request tương ứng
     * @return trả về phản hồi thất bại
     */
    public static final Response error(String message, String requestId){
        return new Response(false, message, null, requestId);
    }

    /**
     * Tạo một phản hồi thất bại kèm thông báo lỗi.
     * @param message thông báo lỗi
     * @return trả về phản hồi thất bại
     */
    public static final Response error(String message){
        return new Response(false, message, null,null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
    public void setRequestId(String requestId){
        this.requestId = requestId;
    }
    public String getRequestId(){
        return requestId;
    }

    @Override
    public String toString(){
        return "Response{success=" + success + ", message='" + message + "', data=" + data + "}";
    }
}
