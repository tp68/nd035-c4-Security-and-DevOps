package com.example.demo.controllers;

import java.util.Optional;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.ModifyCartRequest;
import com.example.demo.security.AuthenticationUtil;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CartRepository cartRepository;
    
    @Autowired
    private ItemRepository itemRepository;

    
    @PostMapping("/addToCart")
    public ResponseEntity<Cart> addTocart(@RequestBody ModifyCartRequest request, Authentication authentication) {
        log.info("Received addToCart request for user: {} and item ID: {}", request.getUsername(), request.getItemId());

        // Authorization check
        if (!AuthenticationUtil.isAuthorized(request.getUsername(), authentication)) {
            log.warn("addToCart failed: User {} not authorized to modify cart for target user {}.", authentication.getName(), request.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userRepository.findByUsername(request.getUsername());
        if(user == null) {
            log.warn("addToCart failed: User {} not found.", request.getUsername());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Optional<Item> item = itemRepository.findById(request.getItemId());
        if(!item.isPresent()) {
            log.warn("addToCart failed: Item ID {} not found.", request.getItemId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Cart cart = user.getCart();
        IntStream.range(0, request.getQuantity())
            .forEach(i -> cart.addItem(item.get()));
        cartRepository.save(cart);
        log.info("Successfully added {} of item {} to cart for user {}. New total: {}", 
                  request.getQuantity(), request.getItemId(), request.getUsername(), cart.getTotal());
        return ResponseEntity.ok(cart);
    }
    
    @PostMapping("/removeFromCart")
    public ResponseEntity<Cart> removeFromcart(@RequestBody ModifyCartRequest request, Authentication authentication) {
        log.info("Received removeFromCart request for user: {} and item ID: {}", request.getUsername(), request.getItemId());

        // Authorization check
        if (!AuthenticationUtil.isAuthorized(request.getUsername(), authentication)) {
            log.warn("removeFromCart failed: User {} not authorized to modify cart for target user {}.", authentication.getName(), request.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userRepository.findByUsername(request.getUsername());
        if(user == null) {
            log.warn("removeFromCart failed: User {} not found.", request.getUsername());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Optional<Item> item = itemRepository.findById(request.getItemId());
        if(!item.isPresent()) {
            log.warn("removeFromCart failed: Item ID {} not found.", request.getItemId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Cart cart = user.getCart();
        IntStream.range(0, request.getQuantity())
            .forEach(i -> cart.removeItem(item.get()));
        cartRepository.save(cart);
        log.info("Successfully removed {} of item {} from cart for user {}. New total: {}", 
                  request.getQuantity(), request.getItemId(), request.getUsername(), cart.getTotal());
        return ResponseEntity.ok(cart);
    }
        
}
