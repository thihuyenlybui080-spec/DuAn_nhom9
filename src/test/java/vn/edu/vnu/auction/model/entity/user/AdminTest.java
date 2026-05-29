package vn.edu.vnu.auction.model.entity.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.auction.common.exception.AuthenticationException;
import static org.junit.jupiter.api.Assertions.*;

class AdminTest {

    private Admin admin;

    @BeforeEach
    void setUp() {
        // Initialize the Admin object before each test
        admin = new Admin("adminRoot", "admin123", "admin@vnu.edu.vn", "Nguyen Van Admin");
    }

    @Test
    void testGetRole() {
        // Verify that the role is exactly "Admin"
        assertEquals("Admin", admin.getRole(), "The role of the object must be 'Admin'");
    }

    @Test
    void testConstructorWithoutId() {
        // Verify inherited fields and default ID
        assertEquals(-1, admin.getId(), "The default ID from Entity should be -1");
        assertEquals("adminRoot", admin.getName());
        assertEquals("admin123", admin.getPassword());
        assertEquals("admin@vnu.edu.vn", admin.getEmail());
        assertEquals("Nguyen Van Admin", admin.getFullName());

        // Verify default status
        assertTrue(admin.isActive(), "The default initialized account must be in the Active state");
    }

    @Test
    void testConstructorWithId() {
        // Verify the constructor that includes an ID
        Admin adminWithId = new Admin(99, "superAdmin", "pass", "super@vnu.edu.vn", "Super Admin");

        assertEquals(99, adminWithId.getId(), "The ID should match the value passed in the constructor");
        assertEquals("superAdmin", adminWithId.getName());
        assertEquals("super@vnu.edu.vn", adminWithId.getEmail());
    }

    @Test
    void testLoginSuccess() {
        // Test logging in with correct credentials
        assertDoesNotThrow(() -> admin.logIn("adminRoot", "admin123"),
                "Logging in with the correct username and password must not throw an error");
    }

    @Test
    void testLoginFail_WrongPassword() {
        // Test logging in with a wrong password
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> admin.logIn("adminRoot", "wrongPassword"));

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void testLoginFail_WrongUsername() {
        // Test logging in with a wrong username
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> admin.logIn("fakeAdmin", "admin123"));

        assertEquals("Invalid username or password", exception.getMessage());
    }
}