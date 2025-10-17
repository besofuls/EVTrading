package com.evtrading.swp391.controller;

import com.evtrading.swp391.entity.User;
import com.evtrading.swp391.service.UserService;
import com.evtrading.swp391.dto.LoginRequestDTO;
import com.evtrading.swp391.dto.RegisterRequestDTO;
import com.evtrading.swp391.entity.Role;
import com.evtrading.swp391.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // Lấy danh sách tất cả user
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // Lấy thông tin user theo id
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Integer id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Tạo mới user
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(201).body(createdUser); // Sử dụng 201 Created
    }

    // Cập nhật user
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody User userDetails) {
        User updated = userService.updateUser(id, userDetails);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    // Xóa user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // API công khai cho user

    // API login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        return userService.login(loginRequestDTO.getUsername(), loginRequestDTO.getPassword())
                .map(user -> {
                    String token = jwtUtil.generateToken(user.getUsername());
                    Map<String, Object> body = new HashMap<>();
                    body.put("token", token);
                    body.put("user", user);
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    // API register
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequestDTO registerRequestDTO) {
        User user = userService.register(registerRequestDTO);
        return user != null ? ResponseEntity.status(201).body(user) : ResponseEntity.badRequest().build();
    }

    // API admin

    // Disable user by id (chỉ cho Member và Moderator)
    @PostMapping("/{id}/disable")
    public ResponseEntity<User> disableUser(@PathVariable Integer id) {
        Optional<User> opt = userService.getUserById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = opt.get();
        Role role = user.getRole();
        String roleName = role != null ? role.getRoleName() : null;

        if (roleName == null || (!roleName.equalsIgnoreCase("Member") && !roleName.equalsIgnoreCase("Moderator"))) {
            return ResponseEntity.status(403).build();
        }

        user.setStatus("Disabled");
        User saved = userService.updateUser(id, user); // Sử dụng updateUser thay vì createUser
        return ResponseEntity.ok(saved);
    }

    // Approve user by id (chỉ cho Pending và role Member)
    @PostMapping("/{id}/approve")
    public ResponseEntity<User> approveUser(@PathVariable Integer id) {
        Optional<User> opt = userService.getUserById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = opt.get();

        if (!"Pending".equalsIgnoreCase(user.getStatus()) || user.getRole() == null || !user.getRole().getRoleName().equalsIgnoreCase("Member")) {
            return ResponseEntity.badRequest().build();
        }

        user.setStatus("Active");
        User saved = userService.updateUser(id, user); // Sử dụng updateUser thay vì createUser
        return ResponseEntity.ok(saved);
    }
}