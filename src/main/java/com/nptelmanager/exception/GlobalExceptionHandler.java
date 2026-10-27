package com.nptelmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleRuntimeException(RuntimeException ex, Model model) {
        model.addAttribute("errorTitle", "Something went wrong");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        model.addAttribute("errorTitle", "Invalid Request");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(Model model) {
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage", "You don't have permission to access this page.");
        return "error/error";
    }
}
