package com.brainway.stomp.demo.controller;

import com.brainway.stomp.demo.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend access
public class LoginController {

    private final JwtService jwtService;

    public LoginController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String token = jwtService.generateToken(username);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
