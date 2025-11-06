package com.example.demo.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.repositories.ItemRepository;

@RestController
@RequestMapping("/api/item")
public class ItemController {

    private static final Logger log = LoggerFactory.getLogger(ItemController.class);

    @Autowired
    private ItemRepository itemRepository;
    
    @GetMapping
    public ResponseEntity<List<Item>> getItems() {
        log.info("Fetching all items.");
        List<Item> items = itemRepository.findAll();
        log.info("Found {} items.", items.size());
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        log.info("Fetching item by ID: {}", id);
        return itemRepository.findById(id)
                .map(item -> {
                    log.info("Successfully retrieved item ID: {}", id);
                    return ResponseEntity.ok(item);
                })
                .orElseGet(() -> {
                    log.warn("Item ID {} not found.", id);
                    return ResponseEntity.notFound().build();
                });
    }
    
    @GetMapping("/name/{name}")
    public ResponseEntity<List<Item>> getItemsByName(@PathVariable String name) {
        log.info("Fetching items by name: {}", name);
        List<Item> items = itemRepository.findByName(name);
        if (items == null || items.isEmpty()) {
            log.warn("No items found with name: {}", name);
            return ResponseEntity.notFound().build();
        } else {
            log.info("Found {} items with name: {}", items.size(), name);
            return ResponseEntity.ok(items);
        }
    }
    
}
