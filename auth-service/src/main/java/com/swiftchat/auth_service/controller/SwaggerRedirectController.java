package com.swiftchat.auth_service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to redirect the root URL to Swagger UI.
 */
@Controller
public class SwaggerRedirectController {

    /**
     * Redirects the root path to Swagger UI for API documentation.
     * 
     * @return Redirect URL to Swagger UI
     */
    @GetMapping("/")
    public String redirect() {
        return "redirect:/swagger-ui.html";
    }
}
