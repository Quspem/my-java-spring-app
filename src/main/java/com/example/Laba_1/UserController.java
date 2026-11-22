package com.example.Laba_1;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService service;

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Group", "аалн-01-23");
        return headers;
    }

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "id") String sort) {
        Sort sortObj = sort.startsWith("-") ? Sort.by(sort.substring(1)).descending() : Sort.by(sort).ascending();
        Page<User> result = service.findAll(PageRequest.of(page - 1, limit, sortObj));

        return ResponseEntity.ok().headers(getHeaders()).body(Map.of(
                "data", result.getContent(),
                "pagination", Map.of("total", result.getTotalElements(), "page", page, "limit", limit, "pages", result.getTotalPages())
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q, @RequestParam(defaultValue = "1") int page) {
        Page<User> result = service.search(q, PageRequest.of(page - 1, 10));
        return ResponseEntity.ok().headers(getHeaders()).body(Map.of("found", result.getTotalElements(), "data", result.getContent()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable String id) {
        return ResponseEntity.ok().headers(getHeaders()).body(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody User user) {
        return ResponseEntity.ok().headers(getHeaders()).body(Map.of("data", service.create(user)));
    }
}