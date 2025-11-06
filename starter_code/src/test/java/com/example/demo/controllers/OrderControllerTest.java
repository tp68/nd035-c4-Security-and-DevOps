package com.example.demo.controllers;

import com.example.demo.TestUtils;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderControllerTest {

    private OrderController orderController;
    private UserRepository userRepo = mock(UserRepository.class);
    private OrderRepository orderRepo = mock(OrderRepository.class);
    private CartRepository cartRepo = mock(CartRepository.class);

    private User testUser;
    private User emptyCartUser; // New user setup for empty cart test
    private UserOrder mockOrder;
    private Cart initialCart;

    // Helper method to create a user with a pre-filled cart for submission testing
    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");

        Item item = new Item();
        item.setId(1L);
        item.setPrice(BigDecimal.valueOf(5.00));
        
        Cart cart = new Cart();
        // Initialize cart's internal state with two items
        TestUtils.injectObjects(cart, "items", new ArrayList<>(Arrays.asList(item, item)));
        cart.setTotal(BigDecimal.valueOf(10.00));
        TestUtils.injectObjects(cart, "id", 1L); 

        user.setCart(cart);
        
        return user;
    }
    
    // Helper method to create a user with an empty cart
    private User createEmptyCartUser() {
        User user = new User();
        user.setId(2L);
        user.setUsername("emptyUser");

        Cart cart = new Cart();
        // Initialize cart's internal state as empty
        TestUtils.injectObjects(cart, "items", new ArrayList<>());
        cart.setTotal(BigDecimal.ZERO);
        TestUtils.injectObjects(cart, "id", 2L); 

        user.setCart(cart);
        return user;
    }
    
    // Helper method to create a mock UserOrder for repository returns
    private UserOrder createMockOrder(User user, Cart cart) {
        UserOrder order = new UserOrder();
        order.setId(5L);
        order.setUser(user);
        order.setItems(cart.getItems());
        order.setTotal(cart.getTotal());
        return order;
    }

    @BeforeEach
    public void setUp() {
        orderController = new OrderController();
        
        // Inject mocks into the controller
        TestUtils.injectObjects(orderController, "userRepository", userRepo);
        TestUtils.injectObjects(orderController, "orderRepository", orderRepo);
        TestUtils.injectObjects(orderController, "cartRepository", cartRepo);

        // Setup test data
        testUser = createTestUser();
        emptyCartUser = createEmptyCartUser(); // Initialize empty cart user
        initialCart = testUser.getCart();
        mockOrder = createMockOrder(testUser, initialCart);

        // Common Mock Setup
        when(userRepo.findByUsername("testUser")).thenReturn(testUser);
        when(userRepo.findByUsername("emptyUser")).thenReturn(emptyCartUser); // Mock empty cart user
        when(userRepo.findByUsername("nonExistentUser")).thenReturn(null);

        // Mock saving the generated order (we assume UserOrder.createFromCart works)
        when(orderRepo.save(Mockito.any(UserOrder.class))).thenReturn(mockOrder);
    }

    // -------------------------------------------------------------------------
    // TEST: submit
    // -------------------------------------------------------------------------

    @Test
    public void submit_happy_path() {
        // Capture the cart state before submission for verification
        int initialItemCount = initialCart.getItems().size();
        BigDecimal initialTotal = initialCart.getTotal();
        
        // Execute
        final ResponseEntity<UserOrder> response = orderController.submit("testUser");

        // Verify response status
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verification 1: Order repository received a save call
        UserOrder returnedOrder = response.getBody();
        verify(orderRepo, Mockito.times(1)).save(returnedOrder);

        // Verification 2: Cart repository received a save call and capture the saved Cart object
        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepo, Mockito.times(1)).save(cartCaptor.capture());
        
        // Verification 3: Check that the saved cart is now empty and total is reset
        Cart savedCart = cartCaptor.getValue();
        assertTrue(savedCart.getItems().isEmpty(), "Cart items list should be empty after submission.");
        assertEquals(BigDecimal.ZERO, savedCart.getTotal(), "Cart total should be zero after submission.");
        
        // Verification 4: Check that the returned order reflects the *initial* cart state
        assertEquals(initialItemCount, returnedOrder.getItems().size());
        assertEquals(initialTotal, returnedOrder.getTotal());
    }
    
    @Test
    public void submit_empty_cart_fails() {
        // Execute submit with a user whose cart is initialized to be empty
        final ResponseEntity<UserOrder> response = orderController.submit("emptyUser");

        // Verify response status is 400 Bad Request
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Should return 400 Bad Request for empty cart.");

        // Verify no persistence calls were made
        verify(orderRepo, Mockito.times(0)).save(Mockito.any(UserOrder.class));
        verify(cartRepo, Mockito.times(0)).save(Mockito.any(Cart.class));
    }

    @Test
    public void submit_user_not_found() {
        // Execute
        final ResponseEntity<UserOrder> response = orderController.submit("nonExistentUser");

        // Verify response status is 404
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        // Verify no persistence calls were made
        verify(orderRepo, Mockito.times(0)).save(Mockito.any(UserOrder.class));
        verify(cartRepo, Mockito.times(0)).save(Mockito.any(Cart.class));
    }

    // -------------------------------------------------------------------------
    // TEST: getOrdersForUser
    // -------------------------------------------------------------------------

    @Test
    public void get_orders_happy_path() {
        // Setup mock order history
        List<UserOrder> history = Arrays.asList(mockOrder, mockOrder);
        when(orderRepo.findByUser(testUser)).thenReturn(history);
        
        // Execute
        final ResponseEntity<List<UserOrder>> response = orderController.getOrdersForUser("testUser");

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        
        // Verify repository interaction
        verify(orderRepo, Mockito.times(1)).findByUser(testUser);
    }

    @Test
    public void get_orders_user_not_found() {
        // Execute
        final ResponseEntity<List<UserOrder>> response = orderController.getOrdersForUser("nonExistentUser");

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        // Verify no order lookup was attempted
        verify(orderRepo, Mockito.times(0)).findByUser(Mockito.any(User.class));
    }

    @Test
    public void get_orders_empty_history() {
        // Setup mock to return an empty list
        when(orderRepo.findByUser(testUser)).thenReturn(Collections.emptyList());
        
        // Execute
        final ResponseEntity<List<UserOrder>> response = orderController.getOrdersForUser("testUser");

        // Verify
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty(), "Response body should be an empty list.");
    }
}
