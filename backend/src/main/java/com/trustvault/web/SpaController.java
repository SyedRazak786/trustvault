package com.trustvault.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Lets website pages load when opened directly, such as /signup. The browser app then shows the right page.
// API paths (/api/...) are not listed here, so they still reach the backend.
@Controller
public class SpaController {

    @GetMapping({"/welcome", "/login", "/signup", "/forgot-password", "/how-it-works", "/about",
                 "/security", "/contact", "/help", "/privacy", "/terms", "/shared", "/profile",
                 "/admin", "/admin/documents", "/documents/{id}"})
    public String app() {
        return "forward:/index.html";
    }
}