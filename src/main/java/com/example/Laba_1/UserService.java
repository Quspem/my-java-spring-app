package com.example.Laba_1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    @Autowired
    private UserRepository repository;

    public Page<User> findAll(Pageable pageable) { return repository.findAll(pageable); }

    public Page<User> search(String q, Pageable pageable) { return repository.searchByNameOrEmail(q, pageable); }

    public User findById(String id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User create(User user) {
        if (repository.findByEmail(user.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        System.out.println("[SERVICE] Создание пользователя: " + user.getEmail());
        return repository.save(user);
    }
}