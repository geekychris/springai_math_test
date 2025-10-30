package com.example.mathagent.model;

/**
 * Enum representing the types of mathematical operations supported by the application.
 */
public enum MathOperationType {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE;
    
    /**
     * Executes the mathematical operation on the given operands
     * 
     * @param a The first operand
     * @param b The second operand
     * @return The result of the operation
     * @throws ArithmeticException if division by zero is attempted
     */
    public double execute(double a, double b) {
        return switch (this) {
            case ADD -> a + b;
            case SUBTRACT -> a - b;
            case MULTIPLY -> a * b;
            case DIVIDE -> {
                if (b == 0) {
                    throw new ArithmeticException("Division by zero is not allowed");
                }
                yield a / b;
            }
        };
    }
    
    /**
     * Gets a human-readable description of the operation
     * 
     * @param a The first operand
     * @param b The second operand
     * @return A string describing the operation
     */
    public String getDescription(double a, double b) {
        return switch (this) {
            case ADD -> String.format("Adding %s to %s", b, a);
            case SUBTRACT -> String.format("Subtracting %s from %s", b, a);
            case MULTIPLY -> String.format("Multiplying %s by %s", a, b);
            case DIVIDE -> String.format("Dividing %s by %s", a, b);
        };
    }
}

