package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UserController.
 *
 * Unlike UserControllerTest (unit test), this starts the FULL Spring Boot application
 * on a random port and makes REAL HTTP requests against it.
 * The "test" profile activates application-test.properties which configures
 * an H2 in-memory database — no external MySQL needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Clean the database before each test to ensure test isolation
        userRepository.deleteAll();
    }

    @Test
    void testCreateUser() {
        // Given: a new user to create
        User newUser = new User();
        newUser.setName("Alice");
        newUser.setEmail("alice@example.com");

        // When: we POST to /users
        ResponseEntity<User> response = restTemplate.postForEntity("/users", newUser, User.class);

        // Then: the user is created successfully
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Alice");
        assertThat(response.getBody().getEmail()).isEqualTo("alice@example.com");

        // And: it is persisted in the database
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void testGetAllUsers() {
        // Given: two users exist in the database
        userRepository.save(new User(null, "Alice", "alice@example.com"));
        userRepository.save(new User(null, "Bob", "bob@example.com"));

        // When: we GET /users
        ResponseEntity<User[]> response = restTemplate.getForEntity("/users", User[].class);

        // Then: both users are returned
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void testGetUserById() {
        // Given: a user exists in the database
        User saved = userRepository.save(new User(null, "Alice", "alice@example.com"));

        // When: we GET /users/{id}
        ResponseEntity<User> response = restTemplate.getForEntity("/users/" + saved.getId(), User.class);

        // Then: the correct user is returned
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Alice");
        assertThat(response.getBody().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void testDeleteUser() {
        // Given: a user exists in the database
        User saved = userRepository.save(new User(null, "Alice", "alice@example.com"));
        assertThat(userRepository.count()).isEqualTo(1);

        // When: we DELETE /users/{id}
        ResponseEntity<Void> response = restTemplate.exchange(
                "/users/" + saved.getId(),
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
        );

        // Then: the user is deleted
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    void testFullCrudLifecycle() {
        // 1. Create a user
        User newUser = new User();
        newUser.setName("Charlie");
        newUser.setEmail("charlie@example.com");

        ResponseEntity<User> createResponse = restTemplate.postForEntity("/users", newUser, User.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long userId = createResponse.getBody().getId();
        assertThat(userId).isNotNull();

        // 2. Read the user back
        ResponseEntity<User> getResponse = restTemplate.getForEntity("/users/" + userId, User.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getName()).isEqualTo("Charlie");

        // 3. Verify it appears in the list
        ResponseEntity<User[]> listResponse = restTemplate.getForEntity("/users", User[].class);
        assertThat(listResponse.getBody()).hasSize(1);

        // 4. Delete the user
        restTemplate.delete("/users/" + userId);

        // 5. Verify the list is now empty
        ResponseEntity<User[]> emptyListResponse = restTemplate.getForEntity("/users", User[].class);
        assertThat(emptyListResponse.getBody()).hasSize(0);
    }
}
