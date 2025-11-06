package com.example.demo.controllers;

import com.example.demo.TestUtils;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.repositories.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ItemControllerTest {

    private ItemController itemController;
    private ItemRepository itemRepo = mock(ItemRepository.class);

    private Item itemA;
    private Item itemB;
    private Item itemC; 
    private List<Item> itemList;

    // Helper method to create a sample item
    private Item createItem(Long id, String name, BigDecimal price) {
        Item item = new Item();
        item.setId(id);
        item.setName(name);
        item.setPrice(price);
        item.setDescription("Description for " + name);
        return item;
    }

    @BeforeEach
    public void setUp() {
        itemController = new ItemController();
        // Inject the mocked repository into the controller
        TestUtils.injectObjects(itemController, "itemRepository", itemRepo);

        // Setup test data
        itemA = createItem(1L, "Widget A", BigDecimal.valueOf(10.50));
        itemB = createItem(2L, "Widget B", BigDecimal.valueOf(20.00));
        itemC = createItem(3L, "Widget A", BigDecimal.valueOf(10.50)); // Same name as A for list test

        itemList = Arrays.asList(itemA, itemB, itemC);

        // Common Mock Setup
        // 1. findAll()
        when(itemRepo.findAll()).thenReturn(itemList);

        // 2. findById(id)
        when(itemRepo.findById(1L)).thenReturn(Optional.of(itemA));
        when(itemRepo.findById(99L)).thenReturn(Optional.empty());

        // 3. findByName(name)
        // Mock to return multiple items for "Widget A"
        when(itemRepo.findByName("Widget A")).thenReturn(Arrays.asList(itemA, itemC)); 
        // Mock to return a single item for "Widget B" 
        when(itemRepo.findByName("Widget B")).thenReturn(Collections.singletonList(itemB));
        // Mock to return empty list for not found
        when(itemRepo.findByName("NonExistent")).thenReturn(Collections.emptyList());
    }

    // -------------------------------------------------------------------------
    // TEST: getItems (findAll)
    // -------------------------------------------------------------------------

    @Test
    public void get_all_items_happy_path() {
        final ResponseEntity<List<Item>> response = itemController.getItems();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().size());
        assertEquals("Widget A", response.getBody().get(0).getName());
    }
    
    // -------------------------------------------------------------------------
    // TEST: getItemById
    // -------------------------------------------------------------------------

    @Test
    public void get_item_by_id_happy_path() {
        final ResponseEntity<Item> response = itemController.getItemById(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
        assertEquals(BigDecimal.valueOf(10.50), response.getBody().getPrice());
    }

    @Test
    public void get_item_by_id_not_found() {
        final ResponseEntity<Item> response = itemController.getItemById(99L); // Non-existent ID

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // TEST: getItemsByName
    // -------------------------------------------------------------------------

    @Test
    public void get_items_by_name_happy_path_single_item() {
        // Test where only one item is found (using "Widget B")
        final ResponseEntity<List<Item>> response = itemController.getItemsByName("Widget B");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Widget B", response.getBody().get(0).getName());
    }

    @Test
    public void get_items_by_name_multiple_items_found() {
        // Test for finding multiple items with the same name (using "Widget A")
        final ResponseEntity<List<Item>> response = itemController.getItemsByName("Widget A");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Assert that both items are returned
        assertEquals(2, response.getBody().size());
        assertEquals(1L, response.getBody().get(0).getId());
        assertEquals(3L, response.getBody().get(1).getId());
    }

    @Test
    public void get_items_by_name_not_found() {
        final ResponseEntity<List<Item>> response = itemController.getItemsByName("NonExistent");

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
