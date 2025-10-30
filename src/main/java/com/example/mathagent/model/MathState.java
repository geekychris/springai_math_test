package com.example.mathagent.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the current state of a math calculation session.
 * Maintains the current value and history of operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MathState {
    /** The current calculated value */
    private double currentValue = 0.0;
    
    /** List of operations performed during the calculation session */
    private List<String> operations = new ArrayList<>();
    
    /**
     * Adds an operation to the history with current value and the operation performed
     * @param operation The operation description to add
     */
    public void addOperation(String operation) {
        operations.add(operation);
    }
    
    /**
     * Updates the current value and adds an operation to history
     * @param newValue The new calculated value
     * @param operation The operation description
     */
    public void updateState(double newValue, String operation) {
        this.currentValue = newValue;
        addOperation(operation);
    }
    
    /**
     * Creates a new MathState with an initial value
     * @param initialValue The starting value
     * @return A new MathState instance
     */
    public static MathState withInitialValue(double initialValue) {
        MathState state = new MathState();
        state.setCurrentValue(initialValue);
        state.addOperation("Started with initial value: " + initialValue);
        return state;
    }
}

