package com.example.demo.controllers;

import com.example.demo.TestUtils;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.ModifyCartRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CartControllerTest {

    private CartController cartController;
    private UserRepository userRepo = mock(UserRepository.class);
    private CartRepository cartRepo = mock(CartRepository.class);
    private ItemRepository itemRepo = mock(ItemRepository.class);

    private User testUser;
    private Item testItem;

    // Helper method to create a stateful User object for testing.
    // This allows us to track changes made by cart.addItem/removeItem.
    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");

        Cart cart = new Cart();
        // Initialize cart's internal list for state tracking
        TestUtils.injectObjects(cart, "items", new ArrayList<Item>());
        TestUtils.injectObjects(cart, "total", BigDecimal.ZERO);

        user.setCart(cart);
        return user;
    }

    private Item createTestItem() {
        Item item = new Item();
        item.setId(1L);
        item.setName("Round Widget");
        item.setPrice(BigDecimal.valueOf(2.99));
        return item;
    }

    @BeforeEach
    public void setUp() {
        cartController = new CartController();
        
        // Inject mocks into the controller
        TestUtils.injectObjects(cartController, "userRepository", userRepo);
        TestUtils.injectObjects(cartController, "cartRepository", cartRepo);
        TestUtils.injectObjects(cartController, "itemRepository", itemRepo);

        // Setup test data
        testUser = createTestUser();
        testItem = createTestItem();

        // Common Mock Setup
        when(userRepo.findByUsername("testUser")).thenReturn(testUser);
        when(userRepo.findByUsername("nonExistentUser")).thenReturn(null);
        when(itemRepo.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepo.findById(99L)).thenReturn(Optional.empty());        
    }

    // --- addToCart Tests ---

    @Test
    public void add_to_cart_happy_path() {
        // Setup request
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("testUser");
        request.setItemId(1L);
        request.setQuantity(3); // Add 3 items

        int initialSize = testUser.getCart().getItems().size();

        // Execute
        final ResponseEntity<Cart> response = cartController.addTocart(request);

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify cart state mutation: size should increase by 3
        assertEquals(initialSize + 3, response.getBody().getItems().size());
        
        // Verify persistence interaction
        verify(cartRepo, Mockito.times(1)).save(testUser.getCart());
    }

    @Test
    public void add_to_cart_user_not_found() {
        // Setup request for non-existent user
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("nonExistentUser");
        request.setItemId(1L);
        request.setQuantity(1);

        // Execute
        final ResponseEntity<Cart> response = cartController.addTocart(request);

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void add_to_cart_item_not_found() {
        // Setup request for non-existent item
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("testUser");
        request.setItemId(99L); // Non-existent ID
        request.setQuantity(1);

        // Execute
        final ResponseEntity<Cart> response = cartController.addTocart(request);

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
    
    // --- removeFromCart Tests ---
    
    @Test
    public void remove_from_cart_happy_path() {
        // 1. Pre-fill the cart with 5 items to allow removal
        for (int i = 0; i < 5; i++) {
            testUser.getCart().addItem(testItem);
        }
        assertEquals(5, testUser.getCart().getItems().size());
        
        // 2. Setup removal request
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("testUser");
        request.setItemId(1L);
        request.setQuantity(2); // Remove 2 items

        // Execute
        final ResponseEntity<Cart> response = cartController.removeFromcart(request);

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify cart state mutation: size should decrease by 2
        assertEquals(3, response.getBody().getItems().size());
        
        // Verify persistence interaction
        verify(cartRepo, Mockito.times(1)).save(testUser.getCart());
    }

    @Test
    public void remove_from_cart_user_not_found() {
        // Setup request for non-existent user
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("nonExistentUser");
        request.setItemId(1L);
        request.setQuantity(1);

        // Execute
        final ResponseEntity<Cart> response = cartController.removeFromcart(request);

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void remove_from_cart_item_not_found() {
        // Setup request for non-existent item
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("testUser");
        request.setItemId(99L); // Non-existent ID
        request.setQuantity(1);

        // Execute
        final ResponseEntity<Cart> response = cartController.removeFromcart(request);

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
