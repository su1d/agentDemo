package com.agent.controller;

import com.agent.entity.User;
import com.agent.repository.UserRepository;
import com.agent.config.JwtUtil;
import com.agent.service.RedisTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          RedisTokenService redisTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTokenService = redisTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and password cannot be empty"));
        }

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "username already exists"));
        }

        User user = new User(username, passwordEncoder.encode(password));
        user.setDisplayName(body.getOrDefault("displayName", username));
        userRepository.save(user);

        String token = jwtUtil.generateToken(username, user.getRole());
        redisTokenService.saveUserToken(username, token, user.getRole());
        return ResponseEntity.ok(Map.of("token", token, "username", username, "role", user.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and password cannot be empty"));
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "wrong username or password"));
        }

        String token = jwtUtil.generateToken(username, user.getRole());
        redisTokenService.saveUserToken(username, token, user.getRole());
        return ResponseEntity.ok(Map.of("token", token, "username", username, "role", user.getRole(), "displayName", user.getDisplayName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid token"));
        }
        String token = authHeader.substring(7);
        // Remove token and user session from Redis
        String username = jwtUtil.getUsernameFromTokenIgnoringExpiration(token);
        redisTokenService.removeTokenAndSession(token, username);
        return ResponseEntity.ok(Map.of("message", "logout success"));
    }
}
