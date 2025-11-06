package com.example.demo.model.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CartTest {

    private Cart cart;
    private Item testItem1;
    private Item testItem2;
    private User testUser;

    @BeforeEach
    public void setUp() {
        cart = new Cart();

        // Setup mock Item 1
        testItem1 = new Item();
        testItem1.setId(1L);
        testItem1.setName("Widget");
        testItem1.setPrice(new BigDecimal("10.00"));

        // Setup mock Item 2 (different price)
        testItem2 = new Item();
        testItem2.setId(2L);
        testItem2.setName("Gadget");
        testItem2.setPrice(new BigDecimal("25.50"));
        
        // Setup mock User
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
    }

    // -------------------------------------------------------------------------
    // TEST: Constructors and Basic Properties
    // -------------------------------------------------------------------------
    
    /**
     * Tests the default constructor and the initial state of fields.
     */
    @Test
    public void test_default_constructor_and_initial_state() {
        assertNotNull(cart, "Cart instance should not be null.");
        assertNull(cart.getId(), "ID should be initially null.");
        assertNull(cart.getUser(), "User should be initially null.");
        assertNull(cart.getTotal(), "Total should be initially null.");
        assertNull(cart.getItems(), "Items list should be initially null.");
    }

    /**
     * Tests the functionality of all getters and setters.
     */
    @Test
    public void test_all_getters_and_setters() {
        Long testId = 5L;
        List<Item> initialItems = Arrays.asList(testItem1);
        BigDecimal initialTotal = new BigDecimal("10.00");

        // Set values
        cart.setId(testId);
        cart.setUser(testUser);
        cart.setItems(initialItems);
        cart.setTotal(initialTotal);

        // Verify values
        assertEquals(testId, cart.getId(), "ID getter/setter failed.");
        assertEquals(testUser, cart.getUser(), "User getter/setter failed.");
        assertEquals(initialItems, cart.getItems(), "Items getter/setter failed.");
        assertEquals(initialTotal, cart.getTotal(), "Total getter/setter failed.");
    }

    // -------------------------------------------------------------------------
    // TEST: addItem(Item) Logic
    // -------------------------------------------------------------------------

    /**
     * Tests adding an item when the cart is initially null (list and total).
     * This verifies the internal initialization logic in addItem.
     */
    @Test
    public void test_add_item_with_null_initial_state() {
        // Assert pre-conditions (items and total are null)
        assertNull(cart.getItems());
        assertNull(cart.getTotal());

        cart.addItem(testItem1);

        // Assert post-conditions
        assertNotNull(cart.getItems(), "Items list should be initialized after first addition.");
        assertEquals(1, cart.getItems().size(), "One item should have been added.");
        assertEquals(testItem1.getPrice(), cart.getTotal(), "Total should match the price of the first item.");
        assertTrue(cart.getItems().contains(testItem1), "List should contain the added item.");
    }
    
    /**
     * Tests adding multiple items to a non-empty cart.
     */
    @Test
    public void test_add_multiple_items() {
        // Setup state (initialized cart)
        cart.addItem(testItem1); 
        
        // Add second item
        cart.addItem(testItem2); 
        
        BigDecimal expectedTotal = testItem1.getPrice().add(testItem2.getPrice());
        
        // Assert post-conditions
        assertEquals(2, cart.getItems().size(), "Two items should have been added.");
        assertEquals(expectedTotal, cart.getTotal(), "Total should be the sum of both item prices.");
        assertTrue(cart.getItems().contains(testItem2), "List should contain the second added item.");
    }
    
    // -------------------------------------------------------------------------
    // TEST: removeItem(Item) Logic
    // -------------------------------------------------------------------------
    
    /**
     * Tests removing an existing item from a filled cart.
     */
    @Test
    public void test_remove_item_happy_path() {
        // Setup initial cart state: Add item1, Add item2, Add item1 again
        cart.addItem(testItem1); 
        cart.addItem(testItem2); 
        cart.addItem(testItem1);
        
        // Initial state check
        assertEquals(3, cart.getItems().size());
        BigDecimal initialTotal = new BigDecimal("45.50"); // 10.00 + 25.50 + 10.00
        assertEquals(initialTotal, cart.getTotal());
        
        // Remove one instance of Item 1
        cart.removeItem(testItem1);
        
        BigDecimal expectedTotal = initialTotal.subtract(testItem1.getPrice()); // 45.50 - 10.00 = 35.50
        
        // Assert post-conditions
        assertEquals(2, cart.getItems().size(), "One item should have been removed.");
        // Ensure the remaining Item 1 and Item 2 are still present
        assertEquals(1, cart.getItems().stream().filter(i -> i.getId().equals(1L)).count(), "One instance of Item 1 should remain.");
        assertEquals(expectedTotal, cart.getTotal(), "Total should be reduced by Item 1's price.");
    }
    
    /**
     * Tests removing the last item, resulting in a total of zero.
     */
    @Test
    public void test_remove_last_item_total_zero() {
        // Setup initial cart state: two Item 1's
        cart.addItem(testItem1); 
        cart.addItem(testItem1); 
        
        // Remove the first instance
        cart.removeItem(testItem1); 
        
        // Remove the last instance
        cart.removeItem(testItem1);
        
        // Assert post-conditions
        assertEquals(0, cart.getItems().size(), "Items list should be empty.");
        // We use setScale(2) for precise BigDecimal comparison against zero
        assertEquals(BigDecimal.ZERO.setScale(2), cart.getTotal(), "Total should be exactly zero after removing the last item.");
    }

    /**
     * Tests behavior when attempting to remove an item that is not in the cart.
     * The cart list will remain unchanged, but the total logic should be checked.
     */
    @Test
    public void test_remove_nonexistent_item_from_initialized_cart() {
        Item notInCart = new Item();
        notInCart.setPrice(new BigDecimal("50.00")); 

        // Setup cart with initial state
        cart.addItem(testItem1);
        BigDecimal initialTotal = cart.getTotal(); // 10.00
        int initialSize = cart.getItems().size(); // 1
        
        // Attempt removal of non-existent item: List.remove fails, but total is subtracted
        cart.removeItem(notInCart); 
        
        // Assert post-conditions
        assertEquals(initialSize, cart.getItems().size(), "Size should remain the same (removal failed).");
        
        // Total should be (10.00 - 50.00) = -40.00 due to subtraction logic after removal attempt
        BigDecimal expectedTotal = initialTotal.subtract(notInCart.getPrice());
        assertEquals(expectedTotal, cart.getTotal(), "Total should be negative due to subtraction logic after removal attempt.");
    }

    /**
     * Tests removing an item when the cart is completely null initially.
     * This exposes and tests the implemented behavior where the total goes negative.
     */
    @Test
    public void test_remove_item_with_null_initial_state() {
        // Assert pre-conditions (items and total are null)
        assertNull(cart.getItems());
        assertNull(cart.getTotal());

        // Attempt removal
        cart.removeItem(testItem1);

        // Expected behavior based on implementation:
        // 1. items is null -> items = new ArrayList<>()
        // 2. List.remove(item) -> fails silently
        // 3. total is null -> total = new BigDecimal(0)
        // 4. total = total.subtract(item.getPrice()) -> total = -10.00
        
        assertNotNull(cart.getItems(), "Items list should be initialized after removal attempt.");
        assertEquals(0, cart.getItems().size(), "List should be empty.");
        
        BigDecimal expectedTotal = BigDecimal.ZERO.subtract(testItem1.getPrice());
        assertEquals(expectedTotal, cart.getTotal(), "Total should be negative due to subtraction logic on an empty cart/null total.");
    }
}
