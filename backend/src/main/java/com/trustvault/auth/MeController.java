package com.trustvault.auth;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/auth/me")
    public Map<String, Object> me(@AuthenticationPrincipal AuthUser user) {
        return Map.of("id", user.id(), "role", user.role());
    }
}