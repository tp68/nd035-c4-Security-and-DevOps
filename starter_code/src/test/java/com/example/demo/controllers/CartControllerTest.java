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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

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


    private User createTestUser(Long id, String username, String password, Cart cart) {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setCart(cart);
        cart.setUser(user);
        return user;
    }

    private Cart createCart() {
        Cart cart = new Cart();        
        // Initialize cart's internal list for state tracking
        TestUtils.injectObjects(cart, "items", new ArrayList<Item>());
        TestUtils.injectObjects(cart, "total", BigDecimal.ZERO);
        return cart;
    }

    private Item createTestItem() {
        Item item = new Item();
        item.setId(1L);
        item.setName("Round Widget");
        item.setPrice(BigDecimal.valueOf(2.99));
        return item;
    }

    /**
     * Helper to create a mock Authentication object.
     *
     * @param username The authenticated user's name.
     * @param roles Varargs array of roles (e.g., "ROLE_USER", "ROLE_ADMIN").
     * @return Authentication object.
     */
    private Authentication createAuth(String username, String... roles) {
        return new UsernamePasswordAuthenticationToken(
            username,
            "password",
            Arrays.stream(roles)
                  .map(SimpleGrantedAuthority::new)
                  .collect(Collectors.toList())
        );
    }

    @BeforeEach
    public void setUp() {
        cartController = new CartController();
        
        // Inject mocks into the controller
        TestUtils.injectObjects(cartController, "userRepository", userRepo);
        TestUtils.injectObjects(cartController, "cartRepository", cartRepo);
        TestUtils.injectObjects(cartController, "itemRepository", itemRepo);

        // Setup test data
        testUser = createTestUser(1L, "testUser", "hashedPass", createCart());
        testItem = createTestItem();

        User adminUser = createTestUser(2L, "adminUser", "hashedPass", new Cart());
        User otherUser = createTestUser(3L, "otherUser", "hashedPass", new Cart());

        // Common Mock Setup
        when(userRepo.findByUsername("testUser")).thenReturn(testUser);
        when(userRepo.findByUsername("adminUser")).thenReturn(adminUser);
        when(userRepo.findByUsername("otherUser")).thenReturn(otherUser);
        when(userRepo.findByUsername("nonExistentUser")).thenReturn(null);
        when(itemRepo.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepo.findById(99L)).thenReturn(Optional.empty());
    }

    // --- addToCart Tests (Authentication Checks) ---

    @Test
    public void add_to_cart_happy_path_self_authorized() {
        // Setup request for self-modification
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("testUser");
        request.setItemId(1L);
        request.setQuantity(3); 

        // Authenticate as the target user ("testUser")
        Authentication auth = createAuth("testUser");
        int initialSize = testUser.getCart().getItems().size();

        // Execute
        final ResponseEntity<Cart> response = cartController.addTocart(request, auth);

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify cart state mutation: size should increase by 3
        assertEquals(initialSize + 3, response.getBody().getItems().size());
        
        // Verify persistence interaction
        verify(cartRepo, Mockito.times(1)).save(testUser.getCart());
    }

    @Test
    public void add_to_cart_unauthorized_user_forbidden() {
        // Setup request to modify "testUser"'s cart
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("testUser");
        request.setItemId(1L);
        request.setQuantity(1);

        // Authenticate as a different user ("otherUser")
        Authentication unauthorizedAuth = createAuth("otherUser"); 

        // Execute
        final ResponseEntity<Cart> response = cartController.addTocart(request, unauthorizedAuth);

        // Verify Forbidden status
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        // Verify no persistence interaction occurred
        verify(cartRepo, Mockito.never()).save(Mockito.any(Cart.class));
    }

    @Test
    public void add_to_cart_admin_authorized() {
        // Setup request to modify "testUser"'s cart
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("testUser");
        request.setItemId(1L);
        request.setQuantity(1);

        // Authenticate as Admin user
        Authentication adminAuth = createAuth("adminUser", "ROLE_ADMIN"); 
        int initialSize = testUser.getCart().getItems().size();

        // Execute
        final ResponseEntity<Cart> response = cartController.addTocart(request, adminAuth);

        // Verify OK status
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify cart state mutation
        assertEquals(initialSize + 1, response.getBody().getItems().size());
        
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

        // Must be authorized to reach the business logic (404)
        Authentication auth = createAuth("nonExistentUser"); 

        // Execute
        final ResponseEntity<Cart> response = cartController.addTocart(request, auth);

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

        // Must be authorized to reach the business logic (404)
        Authentication auth = createAuth("testUser"); 

        // Execute
        final ResponseEntity<Cart> response = cartController.addTocart(request, auth);

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
    
    // --- removeFromCart Tests (Authentication Checks) ---
    
    @Test
    public void remove_from_cart_happy_path_self_authorized() {
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

        // Authenticate as the target user ("testUser")
        Authentication auth = createAuth("testUser");

        // Execute
        final ResponseEntity<Cart> response = cartController.removeFromcart(request, auth);

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify cart state mutation: size should decrease by 2
        assertEquals(3, response.getBody().getItems().size());
        
        // Verify persistence interaction
        verify(cartRepo, Mockito.times(1)).save(testUser.getCart());
    }

    @Test
    public void remove_from_cart_unauthorized_user_forbidden() {
        // Setup request to modify "testUser"'s cart
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("testUser");
        request.setItemId(1L);
        request.setQuantity(1);

        // Authenticate as a different user ("otherUser")
        Authentication unauthorizedAuth = createAuth("otherUser"); 

        // Execute
        final ResponseEntity<Cart> response = cartController.removeFromcart(request, unauthorizedAuth);

        // Verify Forbidden status
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        // Verify no persistence interaction occurred
        verify(cartRepo, Mockito.never()).save(Mockito.any(Cart.class));
    }

    @Test
    public void remove_from_cart_admin_authorized() {
        // 1. Pre-fill the cart with 5 items to allow removal
        for (int i = 0; i < 5; i++) {
            testUser.getCart().addItem(testItem);
        }
        
        // 2. Setup removal request
        ModifyCartRequest request = new ModifyCartRequest();
        request.setUsername("testUser");
        request.setItemId(1L);
        request.setQuantity(1);

        // Authenticate as Admin user
        Authentication adminAuth = createAuth("adminUser", "ROLE_ADMIN"); 

        // Execute
        final ResponseEntity<Cart> response = cartController.removeFromcart(request, adminAuth);

        // Verify OK status
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify cart state mutation: size should decrease by 1 (5 -> 4)
        assertEquals(4, response.getBody().getItems().size());
        
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

        // Must be authorized (as the nonExistentUser) to reach the business logic (404)
        Authentication auth = createAuth("nonExistentUser");

        // Execute
        final ResponseEntity<Cart> response = cartController.removeFromcart(request, auth);

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

        // Must be authorized (as testUser) to reach the business logic (404)
        Authentication auth = createAuth("testUser");

        // Execute
        final ResponseEntity<Cart> response = cartController.removeFromcart(request, auth);

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
