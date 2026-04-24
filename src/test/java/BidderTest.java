import me.user.AuthenticationException;
import me.user.Bidder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class BidderTest {
    private Bidder bidder;

    @BeforeEach
    void setUp() {
        // Khởi tạo đối tượng Admin trước mỗi bài test
        bidder = new Bidder("duong", "AD01", "123456", "admin@gmail.com", "Nguyen Van Admin");
    }

    @Test
    void testLogInSuccess() {
        // Kiểm tra đăng nhập đúng
        assertDoesNotThrow(() -> {

            bidder.logIn("duong", "123456");
        });
    }

    @Test
    void testLogInFail() {
        // Kiểm tra đăng nhập sai username hoặc password sẽ ném ra Exception
        assertThrows(AuthenticationException.class, () -> {
            bidder.logIn("admin1", "wrong_pass");
        });
    }}

