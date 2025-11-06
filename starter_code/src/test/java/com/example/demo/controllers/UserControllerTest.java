package com.example.demo.controllers;

import com.example.demo.TestUtils;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserControllerTest {

    private UserController userController;
    private UserRepository userRepo = mock(UserRepository.class);
    private CartRepository cartRepo = mock(CartRepository.class);
    private PasswordEncoder encoder = mock(PasswordEncoder.class);
    

    @BeforeEach
    public void setUp() {
        userController = new UserController();
        TestUtils.injectObjects(userController, "userRepository", userRepo);
        TestUtils.injectObjects(userController, "cartRepository", cartRepo);
        TestUtils.injectObjects(userController, "bCryptPasswordEncoder", encoder);

        // Create a mocked User object for the find tests
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        
        // Configure mock repository responses
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.findByUsername("testUser")).thenReturn(user);
        when(userRepo.findByUsername("notFound")).thenReturn(null);
        when(userRepo.findById(2L)).thenReturn(Optional.empty()); // For not found by ID
    }

    // -------------------------------------------------------------------------
    // TEST: findById
    // -------------------------------------------------------------------------

    @Test
    public void find_by_id_happy_path() {
        // Call the method
        final ResponseEntity<User> response = userController.findById(1L);

        // Assertions
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
        assertEquals("testUser", response.getBody().getUsername());
    }

    @Test
    public void find_by_id_not_found() {
        final ResponseEntity<User> response = userController.findById(2L);
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // TEST: findByUserName
    // -------------------------------------------------------------------------

    @Test
    public void find_by_username_happy_path() {
        final ResponseEntity<User> response = userController.findByUserName("testUser");
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("testUser", response.getBody().getUsername());
    }

    @Test
    public void find_by_username_not_found() {
        final ResponseEntity<User> response = userController.findByUserName("notFound");
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
 
    // -------------------------------------------------------------------------
    // TEST: createUser (Happy Path)
    // -------------------------------------------------------------------------

    @Test
    public void create_user_happy_path() throws Exception {
        // Mock the encoder behavior
        when(encoder.encode("testPassword")).thenReturn("thisIsHashed");
        
        // Mock the save behavior to return a User with a generated ID (1L)
        when(userRepo.save(Mockito.any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L); // Simulate the database generated ID
            return u;
        });

        CreateUserRequest userReq = new CreateUserRequest();
        userReq.setUsername("testCreate");
        userReq.setPassword("testPassword");
        userReq.setConfirmPassword("testPassword");

        final ResponseEntity<User> response = userController.createUser(userReq);

        // Assertions
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        User user = response.getBody();
        assertNotNull(user);
        
        // Verify the set values, including the mocked ID
        assertEquals(1L, user.getId()); 
        assertEquals("testCreate", user.getUsername());
        assertEquals("thisIsHashed", user.getPassword());
    }

    // -------------------------------------------------------------------------
    // TEST: createUser (Negative Paths)
    // -------------------------------------------------------------------------

    @Test
    public void create_user_short_password() {
        CreateUserRequest userReq = new CreateUserRequest();
        userReq.setUsername("shortPwd");
        userReq.setPassword("short"); // Less than 7 characters
        userReq.setConfirmPassword("short");

        final ResponseEntity<User> response = userController.createUser(userReq);

        // Assertion: Expect 400 Bad Request
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void create_user_password_mismatch() {
        CreateUserRequest userReq = new CreateUserRequest();
        userReq.setUsername("mismatch");
        userReq.setPassword("longEnough");
        userReq.setConfirmPassword("notMatching"); // Mismatch

        final ResponseEntity<User> response = userController.createUser(userReq);

        // Assertion: Expect 400 Bad Request
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
