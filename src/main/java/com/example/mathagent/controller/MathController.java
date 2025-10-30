package com.example.mathagent.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.mathagent.model.MathQuery;
import com.example.mathagent.model.MathResponse;
import com.example.mathagent.service.MathAgentService;

import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for handling mathematical operations expressed in natural language.
 */
@RestController
@RequestMapping("/api/math")
@Slf4j
public class MathController {

    private final MathAgentService mathAgentService;

    @Autowired
    public MathController(MathAgentService mathAgentService) {
        this.mathAgentService = mathAgentService;
    }

    /**
     * Processes a natural language math query and returns the result with explanation.
     *
     * @param query The natural language math query
     * @return Response entity containing the math result and explanation
     */
    @PostMapping
    public ResponseEntity<MathResponse> processMathQuery(@RequestBody MathQuery query) {
        log.info("Received math query: {}", query.getQuery());
        
        if (query.getQuery() == null || query.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        MathResponse response = mathAgentService.processMathQuery(query.getQuery());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Handles arithmetic exceptions such as division by zero.
     */
    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<String> handleArithmeticException(ArithmeticException ex) {
        log.error("Arithmetic error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
    
    /**
     * Handles all other exceptions that might occur during processing.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        log.error("Error processing math query", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while processing your math query: " + ex.getMessage());
    }
}

