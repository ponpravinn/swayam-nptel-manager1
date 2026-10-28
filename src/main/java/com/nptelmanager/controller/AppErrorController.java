package com.nptelmanager.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = status != null ? Integer.parseInt(status.toString()) : 500;

        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            model.addAttribute("errorTitle", "404 - Page Not Found");
            model.addAttribute("errorMessage", "The page or resource you are looking for does not exist or has been moved.");
        } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
            model.addAttribute("errorTitle", "403 - Access Denied");
            model.addAttribute("errorMessage", "You do not have permission to access this resource.");
        } else {
            Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
            model.addAttribute("errorTitle", "Something went wrong (" + statusCode + ")");
            model.addAttribute("errorMessage", message != null && !message.toString().isBlank()
                ? message.toString()
                : "An unexpected error occurred. Please try again.");
        }
        return "error/error";
    }
}
