package org.example.loginregister.server.model.entity.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserStatusRecordTest {

    // ==========================================
    // 1. TEST KHỞI TẠO BẰNG CONSTRUCTOR
    // ==========================================
    @Test
    void testConstructorAndGetters_SetsValuesCorrectly() {
        // Chuẩn bị một Admin đóng vai trò người thực hiện thay đổi trạng thái
        Admin testAdmin = new Admin("admin1", "pass123", "admin@email.com", "System Admin");

        // Khởi tạo Record với trạng thái BANNED (Giả sử enum của bạn có giá trị này)
        UserStatusRecord record = new UserStatusRecord(UserStatus.BANNED, testAdmin);

        // Kiểm chứng các hàm Getter
        assertEquals(UserStatus.BANNED, record.getStatus(), "Trạng thái phải khớp với lúc khởi tạo");
        assertEquals(testAdmin, record.getChangedBy(), "Admin thực hiện thay đổi phải khớp");
    }

    // ==========================================
    // 2. TEST HÀM TẠO MẶC ĐỊNH (DEFAULT ACTIVE)
    // ==========================================
    @Test
    void testDefaultActive_ReturnsActiveStatusWithNullAdmin() {
        // Gọi hàm static
        UserStatusRecord defaultRecord = UserStatusRecord.defaultActive();

        // Kiểm chứng
        assertNotNull(defaultRecord, "Record mặc định sinh ra không được null");
        assertEquals(UserStatus.ACTIVE, defaultRecord.getStatus(), "Trạng thái mặc định bắt buộc phải là ACTIVE");

        // Vì là hệ thống tự tạo lúc user mới đăng ký, người thay đổi (Admin) phải là null
        assertNull(defaultRecord.getChangedBy(), "Không có Admin nào tác động lúc mới tạo, changedBy phải là null");
    }
}