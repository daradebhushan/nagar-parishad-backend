package com.nagar.parishad.backend.controller;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/public/test")

public class TestController {

    public TestController() {
        System.err.println("TestController: INSTANTIATED");
    }

    @GetMapping
    public String testGet() {
        System.err.println("TestController: GET HIT");
        return "GET OK";
    }

    @PostMapping
    public String testPost(HttpServletRequest request) {
        System.err.println("TestController: POST HIT");
        return "POST OK";
    }
}
