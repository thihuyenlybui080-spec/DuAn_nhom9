package org.example.loginregister.server.model.entity.user;
import org.example.loginregister.common.exception.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class SellerTest {
    private Seller seller;

    @BeforeEach
    void setUp() {
        // Khởi tạo đối tượng Admin trước mỗi bài test
        seller = new Seller("AD01", "duong", "123456", "admin@gmail.com", "Nguyen Van Admin");
    }

    @Test
    void testLogInSuccess() {
        // Kiểm tra đăng nhập đúng
        assertDoesNotThrow(() -> {

            seller.logIn("duong", "123456");
        });
    }

    @Test
    void testLogInFail() {
        // Kiểm tra đăng nhập sai username hoặc password sẽ ném ra Exception
        assertThrows(AuthenticationException.class, () -> {
            seller.logIn("admin1", "wrong_pass");
        });
    }}
