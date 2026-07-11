package com.bugtracker.controller;

import com.bugtracker.entity.Role;
import com.bugtracker.entity.User;
import com.bugtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // ---- List all users: Admin only ----
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(this::toUserMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    // ---- List all developers (used when assigning bugs) ----
    @GetMapping("/developers")
    public ResponseEntity<List<Map<String, Object>>> getAllDevelopers() {
        List<Map<String, Object>> developers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.DEVELOPER)
                .map(this::toUserMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(developers);
    }

    private Map<String, Object> toUserMap(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "role", user.getRole().name()
        );
    }
}
