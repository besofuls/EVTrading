package com.evtrading.swp391.controller;

import com.evtrading.swp391.entity.User;
import com.evtrading.swp391.service.UserService;
import com.evtrading.swp391.dto.LoginRequestDTO;
import com.evtrading.swp391.dto.RegisterRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

import com.evtrading.swp391.entity.Role;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;
    
    @Autowired
    private com.evtrading.swp391.security.JwtUtil jwtUtil;

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
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    // Cập nhật user
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody User userDetails) {
        User updated = userService.updateUser(id, userDetails);
        if (updated != null)
            return ResponseEntity.ok(updated);
        return ResponseEntity.notFound().build();
    }

    // Xóa user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    ///////////////////////////////////////////////////////////////////////////////////

    // public api for user

    //api login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        return userService.login(loginRequestDTO.getUsername(), loginRequestDTO.getPassword())
                .map(user -> {
                    String token = jwtUtil.generateToken(user.getUsername());
                    java.util.Map<String, Object> body = new java.util.HashMap<>();
                    body.put("token", token);
                    body.put("user", user);
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    //api register
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequestDTO registerRequestDTO) {
        User user = userService.register(registerRequestDTO);
        if (user != null) {
            return ResponseEntity.status(201).body(user);
            //đăng ký thành công trả về 201 Created
        }
        return ResponseEntity.badRequest().build();
        //đăng ký thất bại trả về 400 Bad Request
    }


    //admin api
    // Disable user by id (chỉ các member và moderator)
    @PostMapping("/{id}/disable")
    public ResponseEntity<User> disableUser(@PathVariable Integer id) {
        Optional<User> opt = userService.getUserById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = opt.get();
        Role role = user.getRole();
        String roleName = role != null ? role.getRoleName() : null;
        if (roleName == null) {
            return ResponseEntity.status(403).build();
            //role null trả về 403 Forbidden
        }
        // Only allow disabling users with role Member or Moderator
        if (!roleName.equalsIgnoreCase("Member") && !roleName.equalsIgnoreCase("Moderator")) {
            return ResponseEntity.status(403).build();
            //role khác Member và Moderator trả về 403 Forbidden
        }
        user.setStatus("Disabled");
        User saved = userService.createUser(user);
        return ResponseEntity.ok(saved);
    }

    // Approve user by id (only if user is Pending and role is Member) 
    @PostMapping("/{id}/approve")
    public ResponseEntity<User> approveUser(@PathVariable Integer id) {
        Optional<User> opt = userService.getUserById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = opt.get();
        if (!"Pending".equalsIgnoreCase(user.getStatus())) {
            // Can't approve a user that's not pending
            return ResponseEntity.badRequest().build();
        }
        Role role = user.getRole();
        String roleName = role != null ? role.getRoleName() : null;
        if (roleName == null) {
            return ResponseEntity.status(403).build();
            //role null trả về 403 Forbidden
        }
        if (!roleName.equalsIgnoreCase("Member") ) {
            return ResponseEntity.status(403).build();
            //role khác Member trả về 403 Forbidden
        }
        user.setStatus("Active");
        User saved = userService.createUser(user);
        return ResponseEntity.ok(saved);
    }
     

}
