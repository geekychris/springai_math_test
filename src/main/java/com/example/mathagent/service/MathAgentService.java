package com.example.mathagent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.mathagent.model.MathOperationType;
import com.example.mathagent.model.MathResponse;
import com.example.mathagent.model.MathState;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for processing natural language math queries using Spring AI and LangGraph4J.
 */
@Service
@Slf4j
public class MathAgentService {

    private final ChatModel chatModel;
    
    @Autowired
    public MathAgentService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    /**
     * Main entry point for processing a natural language math query.
     * 
     * @param query The natural language query string
     * @return A MathResponse containing the result, steps, and explanation
     */
    public MathResponse processMathQuery(String query) {
        log.info("Processing math query: {}", query);
        
        // Parse operations from the natural language query
        List<Operation> operations = parseOperations(query);
        
        if (operations.isEmpty()) {
            throw new IllegalArgumentException("No valid math operations could be identified in the query");
        }
        
        log.info("Parsed {} operations from query", operations.size());
        
        // Initialize state and execute operations
        MathState state = new MathState();
        
        // For the first operation, handle special case
        Operation firstOp = operations.get(0);
        
        // For binary operations like add/multiply, we need two values
        if (firstOp.hasMultipleOperands()) {
            // For first operation, we execute with the two operands directly
            double result = executeInitialOperation(firstOp.getType(), 
                    firstOp.getOperands().get(0), 
                    firstOp.getOperands().get(1));
            
            String description = firstOp.getType().getDescription(
                    firstOp.getOperands().get(0), 
                    firstOp.getOperands().get(1));
            
            state.updateState(result, description + " = " + result);
        } else {
            // If only one operand like "start with 5", just set the initial value
            state = MathState.withInitialValue(firstOp.getOperands().get(0));
        }
        
        // Process the remaining operations
        for (int i = 1; i < operations.size(); i++) {
            Operation operation = operations.get(i);
            double operand = operation.getOperands().get(0);
            
            executeOperation(state, operation.getType(), operand);
        }
        
        // Generate the response
        String explanation = generateExplanation(state.getOperations());
        
        return new MathResponse(
                state.getCurrentValue(),
                state.getOperations(),
                explanation
        );
    }
    
    /**
     * Execute the initial operation with two explicit operands.
     */
    private double executeInitialOperation(MathOperationType type, double a, double b) {
        return type.execute(a, b);
    }
    
    /**
     * Execute a math operation on the current state with a new operand.
     */
    private void executeOperation(MathState state, MathOperationType opType, double operand) {
        double currentValue = state.getCurrentValue();
        double result;
        
        try {
            result = opType.execute(currentValue, operand);
        } catch (ArithmeticException e) {
            log.error("Arithmetic error: {}", e.getMessage());
            throw e;
        }
        
        String description = opType.getDescription(currentValue, operand) + " = " + result;
        state.updateState(result, description);
    }
    
    /**
     * Parse natural language query into a list of operations using Spring AI.
     */
    private List<Operation> parseOperations(String query) {
        // Create a system message with clear instructions for the AI
        String systemPrompt = """
            You are a specialized math operation parser. Your task is to extract math operations from text.
            Extract ONLY these operations: ADD, SUBTRACT, MULTIPLY, DIVIDE.
            
            For each operation, extract:
            1. The operation type
            2. The number operands
                
            Format your response as a JSON array of operations. Each operation has:
            - "operation": One of "ADD", "SUBTRACT", "MULTIPLY", "DIVIDE"
            - "operands": Array of number values
            
            When a reference like "that" is used, it refers to the previous result.
            Ignore such references in the operands, just extract concrete numbers.
            
            Examples:
            - "add 5 and 3" → [{"operation":"ADD","operands":[5,3]}]
            - "multiply 10 by 5" → [{"operation":"MULTIPLY","operands":[10,5]}]
            - "subtract 7 from the result" → [{"operation":"SUBTRACT","operands":[7]}]
            - "multiply 10 by 5, then add 7" → [{"operation":"MULTIPLY","operands":[10,5]},{"operation":"ADD","operands":[7]}]
            
            ONLY include the JSON array in your response, no other text.
            """;
            
        String userPrompt = "Extract math operations from: " + query;
        
        // Create prompt with system and user messages
        Prompt prompt = new Prompt(
            List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
            )
        );

        try {
            // Send prompt to the AI model
            ChatResponse response = chatModel.call(prompt);
            String content = response.getResult().getOutput().getContent();
            
            log.info("AI response: {}", content);
            
            // Parse the JSON response into Operation objects
            List<Operation> operations = parseOperationsFromJson(content);
            
            if (operations.isEmpty()) {
                log.warn("AI returned no operations, falling back to regex");
                return parseOperationsWithRegex(query);
            }
            
            return operations;
        } catch (Exception e) {
            log.error("Error parsing operations with AI: {}", e.getMessage(), e);
            
            // Fallback to regex-based parsing if AI parsing fails
            log.info("Falling back to regex parsing");
            return parseOperationsWithRegex(query);
        }
    }
    
    /**
     * Fallback method to parse operations using regex patterns.
     */
    private List<Operation> parseOperationsWithRegex(String query) {
        List<Operation> operations = new ArrayList<>();
        
        // Split query into sentences or phrases
        String[] parts = query.split("[.,;]\\s*");
        
        for (String part : parts) {
            part = part.toLowerCase().trim();
            
            // Check for addition
            if (part.contains("add") || part.contains("plus") || part.contains("sum")) {
                List<Double> numbers = extractMultipleNumbers(part);
                if (numbers.size() >= 2) {
                    operations.add(new Operation(MathOperationType.ADD, numbers.subList(0, 2)));
                } else if (numbers.size() == 1) {
                    operations.add(new Operation(MathOperationType.ADD, numbers));
                }
            } 
            // Check for subtraction
            else if (part.contains("subtract") || part.contains("minus")) {
                List<Double> numbers = extractMultipleNumbers(part);
                if (numbers.size() >= 2) {
                    // For "subtract 3 from 10", we get [3, 10] but need [10, 3] for 10-3
                    List<Double> reversed = List.of(numbers.get(1), numbers.get(0));
                    operations.add(new Operation(MathOperationType.SUBTRACT, reversed));
                } else if (numbers.size() == 1) {
                    operations.add(new Operation(MathOperationType.SUBTRACT, numbers));
                }
            }
            // Check for multiplication
            else if (part.contains("multiply") || part.contains("times")) {
                List<Double> numbers = extractMultipleNumbers(part);
                if (numbers.size() >= 2) {
                    operations.add(new Operation(MathOperationType.MULTIPLY, numbers.subList(0, 2)));
                } else if (numbers.size() == 1) {
                    operations.add(new Operation(MathOperationType.MULTIPLY, numbers));
                }
            } 
            // Check for division
            else if (part.contains("divide") || part.contains("divided by")) {
                List<Double> numbers = extractMultipleNumbers(part);
                if (numbers.size() >= 2) {
                    // Division order is typically correct as parsed
                    operations.add(new Operation(MathOperationType.DIVIDE, numbers.subList(0, 2)));
                } else if (numbers.size() == 1) {
                    operations.add(new Operation(MathOperationType.DIVIDE, numbers));
                }
            }
        }
        
        return operations;
    }
    
    /**
     * Parse operations from AI's JSON response.
     */
    private List<Operation> parseOperationsFromJson(String json) {
        List<Operation> operations = new ArrayList<>();
        
        try {
            // Basic parsing with string manipulation
            // In a production app, use a proper JSON parser like Jackson
            json = json.trim();
            if (json.startsWith("```")) {
                json = json.substring(json.indexOf('\n') + 1);
                json = json.substring(0, json.lastIndexOf("```")).trim();
            }
            
            // Extract operation entries
            Pattern pattern = Pattern.compile("\\{\"operation\":\"([A-Z]+)\",\"operands\":\\[(\\d+(?:\\.\\d+)?(?:,\\s*\\d+(?:\\.\\d+)?)*)\\]\\}");
            Matcher matcher = pattern.matcher(json);
            
            while (matcher.find()) {
                String opType = matcher.group(1);
                String operandsStr = matcher.group(2);
                
                MathOperationType type = MathOperationType.valueOf(opType);
                List<Double> operands = new ArrayList<>();
                
                // Parse operands
                String[] operandParts = operandsStr.split(",\\s*");
                for (String op : operandParts) {
                    operands.add(Double.parseDouble(op));
                }
                
                operations.add(new Operation(type, operands));
            }
        } catch (Exception e) {
            log.error("Error parsing JSON response: {}", e.getMessage());
        }
        
        return operations;
    }
    
    /**
     * Extract a single number from a text fragment.
     */
    private double extractNumber(String text) {
        Pattern pattern = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\b");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        
        return Double.NaN;
    }
    
    /**
     * Extract multiple numbers from a text fragment.
     */
    private List<Double> extractMultipleNumbers(String text) {
        List<Double> numbers = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\b");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            numbers.add(Double.parseDouble(matcher.group(1)));
        }
        
        return numbers;
    }
    
    /**
     * Generate a natural language explanation of the steps taken.
     */
    private String generateExplanation(List<String> steps) {
        if (steps.isEmpty()) {
            return "No operations were performed.";
        }
        
        StringBuilder explanation = new StringBuilder("I performed the following steps: ");
        
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) {
                explanation.append(" Then, ");
            }
            explanation.append(steps.get(i));
            if (i < steps.size() - 1) {
                explanation.append(".");
            }
        }
        
        return explanation.toString();
    }
    
    /**
     * Internal class representing a math operation with its type and operands.
     */
    @Data
    private static class Operation {
        private final MathOperationType type;
        private final List<Double> operands;
        
        /**
         * Check if this operation has multiple operands.
         */
        public boolean hasMultipleOperands() {
            return operands.size() > 1;
        }
    }
}

