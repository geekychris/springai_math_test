package com.example.mathagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a natural language math query.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MathQuery {
    private String query;
}

