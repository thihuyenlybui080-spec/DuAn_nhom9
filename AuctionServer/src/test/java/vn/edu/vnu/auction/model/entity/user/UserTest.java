package vn.edu.vnu.auction.model.entity.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.common.exception.AuthenticationException;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    // Concrete dummy subclass to test the abstract User class
    private static class DummyUser extends User {
        public DummyUser(String userName, String password, String email, String fullName) {
            super(userName, password, email, fullName);
        }

        public DummyUser(int id, String userName, String password, String email, String fullName) {
            super(id, userName, password, email, fullName);
        }

        @Override
        public String getRole() {
            return "DummyRole";
        }
    }

    private User user;

    @BeforeEach
    void setUp() {
        // Initialize the dummy user object before each test
        user = new DummyUser("testUser", "password123", "test@vnu.edu.vn", "Test Full Name");
    }

    @Test
    void testConstructorWithoutId() {
        // Verify default ID and fields are assigned correctly
        assertEquals(-1, user.getId(), "Default ID should be -1 from Entity");
        assertEquals("testUser", user.getName());
        assertEquals("password123", user.getPassword());
        assertEquals("test@vnu.edu.vn", user.getEmail());
        assertEquals("Test Full Name", user.getFullName());

        // Verify the user is active by default
        assertTrue(user.isActive(), "User must be active by default upon creation");
    }

    @Test
    void testConstructorWithId() {
        User userWithId = new DummyUser(5, "user5", "pass5", "user5@vnu.edu.vn", "User Five");
        assertEquals(5, userWithId.getId());
        assertEquals("user5", userWithId.getName());
    }

    @Test
    void testSettersAndGetters() {
        // Test updating user information
        user.setName("newName");
        user.setPassword("newPass");
        user.setEmail("new@vnu.edu.vn");
        user.setFullName("New Full Name");

        assertEquals("newName", user.getName());
        assertEquals("newPass", user.getPassword());
        assertEquals("new@vnu.edu.vn", user.getEmail());
        assertEquals("New Full Name", user.getFullName());
    }

    @Test
    void testLogIn_Success() {
        // Should not throw any exception when credentials match and account is active
        assertDoesNotThrow(() -> user.logIn("testUser", "password123"),
                "Login should succeed with correct credentials");
    }

    @Test
    void testLogIn_Fail_WrongPassword() {
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> user.logIn("testUser", "wrongPass"));

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void testLogIn_Fail_WrongUsername() {
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> user.logIn("wrongUser", "password123"));

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void testLogIn_Fail_AccountBanned() {
        // Step 1: Create a mock UserStatus showing the account is NOT active (Banned)
        UserStatus mockStatus = Mockito.mock(UserStatus.class);
        Mockito.when(mockStatus.isActive()).thenReturn(false);

        // Step 2: Create a mock UserStatusRecord that holds the banned status
        UserStatusRecord mockRecord = Mockito.mock(UserStatusRecord.class);
        Mockito.when(mockRecord.getStatus()).thenReturn(mockStatus);

        // Step 3: Apply the banned record to our user
        user.updateStatus(mockRecord);

        // Step 4: Verify that login throws an exception because the account is banned
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> user.logIn("testUser", "password123"));

        assertEquals("Account is banned", exception.getMessage(),
                "An exception must be thrown if the account is not active");
    }
}