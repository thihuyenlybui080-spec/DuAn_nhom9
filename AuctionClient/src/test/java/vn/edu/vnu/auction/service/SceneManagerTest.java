package vn.edu.vnu.auction.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử (Unit Test) cho {@link SceneManager}.
 */
class SceneManagerTest {

    private SceneManager sceneManager;

    @BeforeEach
    void setUp() {
        sceneManager = new SceneManager(SceneManagerTest.class);
    }

    /**
     * Test nhánh: Load FXML không tồn tại.
     * Vì FXMLLoader sẽ ném IllegalStateException khi đường dẫn null (file không tồn tại),
     * và SceneManager không catch lỗi này, ta cần assertThrows để test pass.
     */
    @Test
    void testSwitchScene_ThrowsIllegalStateExceptionForMissingFile() {
        // Kỳ vọng một lỗi IllegalStateException sẽ văng ra khi không tìm thấy file FXML
        assertThrows(IllegalStateException.class, () -> {
            sceneManager.switchSceneAndGetController(null, "fake_file.fxml", "Title");
        }, "FXMLLoader should throw IllegalStateException when the FXML file is missing and location is null.");
    }

    @Test
    void testConstructorInitialization() {
        assertNotNull(sceneManager, "SceneManager should be initialized correctly.");
    }
}