package vn.edu.vnu.auction.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EntityTest {

  // Concrete dummy subclass to test the abstract Entity class
  private static class DummyEntity extends Entity {

    public DummyEntity() {
      super();
    }

    public DummyEntity(int id) {
      super(id);
    }
  }

  @Test
  void testDefaultConstructor() {
    // Verify that the default constructor initializes the ID to -1
    Entity entity = new DummyEntity();
    assertEquals(-1, entity.getId(), "The default ID should be -1");
  }

  @Test
  void testConstructorWithId() {
    // Verify that the parameterized constructor assigns the ID correctly
    Entity entity = new DummyEntity(99);
    assertEquals(99, entity.getId(), "The ID should match the value passed in the constructor");
  }

  @Test
  void testSetId() {
    // Verify that the setId() method updates the ID correctly
    Entity entity = new DummyEntity();
    entity.setId(500);

    assertEquals(500, entity.getId(), "The ID should be updated to 500");
  }
}