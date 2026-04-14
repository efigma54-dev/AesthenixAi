package com.aicode.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "AESTHENIXAI Backend is running 🚀 — /api/health | /api/ping";
    }
}
