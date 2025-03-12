package com.example.friend_microservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

@org.springframework.web.bind.annotation.ControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handlerForIllegalArguments(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.toString());
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handlerForIllegalArguments(IllegalStateException exception) {
        return ResponseEntity.badRequest().body(exception.toString());
    }



}
