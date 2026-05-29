package vn.edu.vnu.auction.model.entity.user;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class UserStatusRecordTest {

    @Test
    void testConstructorAndGetters() {
        // Step 1: Create a mock Admin object
        // We only care about testing UserStatusRecord, so a mock Admin is perfect
        Admin mockAdmin = Mockito.mock(Admin.class);

        // Step 2: Create a new UserStatusRecord with a specific status and the mock Admin
        UserStatusRecord record = new UserStatusRecord(UserStatus.BANNED, mockAdmin);

        // Step 3: Verify that the getters return the exact data we passed in
        assertEquals(UserStatus.BANNED, record.getStatus(), "The status should be BANNED");
        assertEquals(mockAdmin, record.getChangedBy(), "The changedBy field should match the injected mock Admin");
    }

    @Test
    void testDefaultActive() {
        // Step 1: Call the static factory method
        UserStatusRecord defaultRecord = UserStatusRecord.defaultActive();

        // Step 2: Verify the default values based on the logic in the class
        assertEquals(UserStatus.ACTIVE, defaultRecord.getStatus(), "The default status must be ACTIVE");
        assertNull(defaultRecord.getChangedBy(), "The default changedBy admin must be null initially");
    }
}