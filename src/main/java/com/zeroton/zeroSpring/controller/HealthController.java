package com.zeroton.zeroSpring.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {
    @GetMapping("/health/check")
    public ResponseEntity<Object> health() {
        return new ResponseEntity<>(HttpStatus.OK);
    }
}