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
import org.springframework.security.core.Authentication; // Added for Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Added for mock roles
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Added for mock Authentication
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserControllerTest {

    private UserController userController;
    private UserRepository userRepo = mock(UserRepository.class);
    private CartRepository cartRepo = mock(CartRepository.class);
    private PasswordEncoder encoder = mock(PasswordEncoder.class);
    
    // Mock user objects for setup
    private User testUser;
    private User otherUser;
    private User adminUser;

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
        userController = new UserController();
        TestUtils.injectObjects(userController, "userRepository", userRepo);
        TestUtils.injectObjects(userController, "cartRepository", cartRepo);
        TestUtils.injectObjects(userController, "bCryptPasswordEncoder", encoder);

        // --- Setup Mock Users ---
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");

        otherUser = new User();
        otherUser.setId(3L);
        otherUser.setUsername("otherUser");

        adminUser = new User();
        adminUser.setId(4L);
        adminUser.setUsername("adminUser");
        
        // --- Configure mock repository responses ---
        // Find by ID
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepo.findById(3L)).thenReturn(Optional.of(otherUser));
        when(userRepo.findById(4L)).thenReturn(Optional.of(adminUser));
        when(userRepo.findById(99L)).thenReturn(Optional.empty()); // For not found by ID

        // Find by Username
        when(userRepo.findByUsername("testUser")).thenReturn(testUser);
        when(userRepo.findByUsername("otherUser")).thenReturn(otherUser);
        when(userRepo.findByUsername("adminUser")).thenReturn(adminUser);
        when(userRepo.findByUsername("notFound")).thenReturn(null);
    }

    // -------------------------------------------------------------------------
    // TEST: findById (Authorization Checks)
    // -------------------------------------------------------------------------

    @Test
    public void find_by_id_happy_path_self_authorized() {
        // Authenticate as the target user ("testUser")
        Authentication auth = createAuth("testUser");

        // Call the method to find testUser (ID 1)
        final ResponseEntity<User> response = userController.findById(1L, auth);

        // Assertions
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    public void find_by_id_unauthorized_user_forbidden() {
        // Authenticate as a different user ("otherUser")
        Authentication unauthorizedAuth = createAuth("otherUser");

        // Attempt to find testUser (ID 1)
        final ResponseEntity<User> response = userController.findById(1L, unauthorizedAuth);

        // Assertion: Expect 403 Forbidden
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
    
    @Test
    public void find_by_id_admin_authorized() {
        // Authenticate as admin user
        Authentication adminAuth = createAuth("adminUser", "ROLE_ADMIN");

        // Admin attempts to find testUser (ID 1)
        final ResponseEntity<User> response = userController.findById(1L, adminAuth);

        // Assertion: Expect 200 OK
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    public void find_by_id_not_found() {
        // Use a valid authentication context to ensure we reach the NOT_FOUND logic
        Authentication auth = createAuth("testUser");

        // Search for non-existent ID (99L)
        final ResponseEntity<User> response = userController.findById(99L, auth);

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
