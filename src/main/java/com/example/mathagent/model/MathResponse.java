package com.example.mathagent.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response object containing the results of a math operation sequence.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MathResponse {
    /** The final calculated result */
    private double result;
    
    /** List of steps executed during calculation */
    private List<String> steps = new ArrayList<>();
    
    /** Natural language explanation of the operations performed */
    private String explanation;
}

