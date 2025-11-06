package com.example.demo.model.persistence;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class ItemTest {

    /**
     * Tests the default constructor and the initial null instantiation.
     */
    @Test
    public void test_default_constructor_and_initial_state() {
        Item item = new Item();
        assertNotNull(item, "Item should not be null.");
        assertNull(item.getId(), "ID should be initially null.");
        assertNull(item.getName(), "Name should be initially null.");
        assertNull(item.getPrice(), "Price should be initially null.");
        assertNull(item.getDescription(), "Description should be initially null.");
    }

    /**
     * Tests the functionality of all getters and setters.
     */
    @Test
    public void test_all_getters_and_setters() {
        Item item = new Item();
        
        // 1. Setup test values
        Long id = 10L;
        String name = "Laptop X";
        BigDecimal price = new BigDecimal("1250.99");
        String description = "High performance computing device.";

        // 2. Setting the values
        item.setId(id);
        item.setName(name);
        item.setPrice(price);
        item.setDescription(description);

        // 3. Verifying the values with getters
        assertEquals(id, item.getId(), "ID was not set/retrieved correctly.");
        assertEquals(name, item.getName(), "Name was not set/retrieved correctly.");
        assertEquals(price, item.getPrice(), "Price was not set/retrieved correctly.");
        assertEquals(description, item.getDescription(), "Description was not set/retrieved correctly.");
    }
    
    /**
     * Tests the equals and hashCode methods, which are based only on the ID.
     */
    @Test
    public void test_equals_and_hashcode_based_on_id() {
        BigDecimal price1 = new BigDecimal("10.00");
        BigDecimal price2 = new BigDecimal("20.00");

        // Item 1: Full data
        Item item1 = new Item();
        item1.setId(1L);
        item1.setName("Apple");
        item1.setPrice(price1);
        item1.setDescription("Red apple");

        // Item 2: Same ID, but different other fields
        Item item2 = new Item();
        item2.setId(1L);
        item2.setName("Pear");
        item2.setPrice(price2);
        item2.setDescription("Green pear");

        // Item 3: Different ID
        Item item3 = new Item();
        item3.setId(2L);
        item3.setName("Apple");
        item3.setPrice(price1);

        // Test 1: Objects with the same ID should be equal
        assertEquals(item1, item2, "Objects with the same ID should be considered equal.");
        assertEquals(item1.hashCode(), item2.hashCode(), "HashCodes should be equal for equal objects.");
        
        // Test 2: Objects with different IDs should be unequal
        assertNotEquals(item1, item3, "Objects with different IDs should be unequal.");
        
        // Test 3: Test with null
        assertNotEquals(item1, null, "Comparison with null should return false.");

        // Test 4: Test with different class
        assertNotEquals(item1, "String", "Comparison with a different class should return false.");

        // Test 5: Reflexivity (object is equal to itself)
        assertEquals(item1, item1, "Object should be equal to itself.");
    }

    /**
     * Tests the special case of equals/hashCode when IDs are null.
     */
    @Test
    public void test_equals_with_null_ids() {
        Item itemA = new Item(); // ID = null
        Item itemB = new Item(); // ID = null
        Item itemC = new Item();
        itemC.setId(3L);         // ID = 3L

        // Two items with null ID are equal
        assertEquals(itemA, itemB, "Two items with null ID should be equal.");
        assertEquals(itemA.hashCode(), itemB.hashCode(), "HashCodes should be equal for null IDs.");

        // Item with null ID is unequal to Item with set ID
        assertNotEquals(itemA, itemC, "Items with null and set ID should be unequal.");
    }
}

