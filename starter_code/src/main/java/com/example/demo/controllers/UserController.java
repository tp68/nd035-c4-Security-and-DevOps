package com.example.demo.controllers;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.security.AuthenticationUtil;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/user")
@EnableMethodSecurity 
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PasswordEncoder bCryptPasswordEncoder;

    @GetMapping("/id/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id, Authentication authentication) {
        log.info("Attempting to find user by ID: {}", id);

        Optional<User> user = userRepository.findById(id);

        if (user.isPresent()) {
            log.info("Successfully retrieved user ID: {}", id);

            // Authorization check
            if (!AuthenticationUtil.isAuthorized(user.get().getUsername(), authentication)) {
                log.warn("findById failed: User {} not authorized to read user with id {}.", authentication.getName(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            else {
                return ResponseEntity.ok(user.get());
            }
        }
        else {
            log.warn("User with ID {} not found.", id);
            return ResponseEntity.notFound().build();
        }


    }

    @GetMapping("/{username}")
    @PreAuthorize("isAuthenticated() and (#username == authentication.name or hasRole('ROLE_ADMIN'))")
    public ResponseEntity<User> findByUserName(@PathVariable String username) {

        log.info("Attempting to find user by username: {}", username);
        User user = userRepository.findByUsername(username);
        if (user == null) {
            log.warn("User with username {} not found.", username);
            return ResponseEntity.notFound().build();
        }
		else {
			log.info("Successfully retrieved user: {}", username);
			return ResponseEntity.ok(user);
		}
    }

    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest createUserRequest) {
        String username = createUserRequest.getUsername();
        log.info("Attempting to create new user: {}", username);

        if (userRepository.findByUsername(username) != null) {
            log.warn("Attempt to create duplicate user: {}", username);
            // 409 Conflict: The requested username is already in use.
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null); 
        }

        String password = createUserRequest.getPassword();
		if (!password.equals(createUserRequest.getConfirmPassword())) {
            log.warn("Failed user creation for {}: Password and confirmation password do not match.", username);
            return ResponseEntity.badRequest().build();
        }

        if (password.length() < 7) {
            log.warn("Failed user creation for {}: Password length is less than 7 characters.", username);
            return ResponseEntity.badRequest().build();
        }
        
        User user = new User();
        user.setUsername(username);
        
        // Set the initial cart for the user
		Cart cart = new Cart();
		cartRepository.save(cart);		
        user.setCart(cart);

        String hashedPassword = bCryptPasswordEncoder.encode(password);
        user.setPassword(hashedPassword);

        userRepository.save(user);

        log.info("Successfully created user with ID {} and username {}.", user.getId(), username);
        return new ResponseEntity<User>(user, HttpStatus.OK);
    }
}
