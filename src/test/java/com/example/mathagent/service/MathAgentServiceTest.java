package com.example.mathagent.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.ai.chat.prompt.Prompt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;

import com.example.mathagent.model.MathResponse;

class MathAgentServiceTest {

    private MathAgentService mathAgentService;
    private ChatModel chatModel;
    
    @BeforeEach
    void setUp() {
        // Mock ChatModel
        chatModel = mock(ChatModel.class);
        mathAgentService = new MathAgentService(chatModel);
    }
    
    private void mockAIResponse(String responseJson) {
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage message = new AssistantMessage(responseJson);
        
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
    }
    
    /**
     * Test basic addition operation
     */
    @Test
    void testAddition() {
        // Mock AI response
        mockAIResponse("[{\"operation\":\"ADD\",\"operands\":[5,3]}]");
        
        // Execute service
        MathResponse response = mathAgentService.processMathQuery("add 5 and 3");
        
        // Verify result
        assertEquals(8.0, response.getResult());
        assertEquals(1, response.getSteps().size());
        assertFalse(response.getSteps().get(0).isEmpty());
    }
    
    /**
     * Test basic subtraction operation
     */
    @Test
    void testSubtraction() {
        // Mock AI response
        mockAIResponse("[{\"operation\":\"SUBTRACT\",\"operands\":[10,3]}]");
        
        // Execute service
        MathResponse response = mathAgentService.processMathQuery("subtract 3 from 10");
        
        // Verify result
        assertEquals(7.0, response.getResult());
        assertEquals(1, response.getSteps().size());
        assertFalse(response.getSteps().get(0).isEmpty());
    }
    
    /**
     * Test basic multiplication operation
     */
    @Test
    void testMultiplication() {
        // Mock AI response
        mockAIResponse("[{\"operation\":\"MULTIPLY\",\"operands\":[4,5]}]");
        
        // Execute service
        MathResponse response = mathAgentService.processMathQuery("multiply 4 by 5");
        
        // Verify result
        assertEquals(20.0, response.getResult());
        assertEquals(1, response.getSteps().size());
        assertFalse(response.getSteps().get(0).isEmpty());
    }
    
    /**
     * Test basic division operation
     */
    @Test
    void testDivision() {
        // Mock AI response
        mockAIResponse("[{\"operation\":\"DIVIDE\",\"operands\":[20,4]}]");
        
        // Execute service
        MathResponse response = mathAgentService.processMathQuery("divide 20 by 4");
        
        // Verify result
        assertEquals(5.0, response.getResult());
        assertEquals(1, response.getSteps().size());
        assertFalse(response.getSteps().get(0).isEmpty());
    }
    
    /**
     * Test division by zero error handling
     */
    @Test
    void testDivisionByZero() {
        // Mock AI response
        mockAIResponse("[{\"operation\":\"DIVIDE\",\"operands\":[10,0]}]");
        
        // Verify that the appropriate exception is thrown
        assertThrows(ArithmeticException.class, () -> {
            mathAgentService.processMathQuery("divide 10 by 0");
        });
    }
    
    /**
     * Test chained operations
     */
    @Test
    void testChainedOperations() {
        // Mock AI response for chained operations
        mockAIResponse("[{\"operation\":\"ADD\",\"operands\":[5,10]},{\"operation\":\"MULTIPLY\",\"operands\":[2]}]");
        
        // Execute service
        MathResponse response = mathAgentService.processMathQuery("add 5 and 10, then multiply by 2");
        
        // Verify result (5 + 10 = 15, then 15 * 2 = 30)
        assertEquals(30.0, response.getResult());
        assertEquals(2, response.getSteps().size());
    }
    
    /**
     * Test the exact example query from the requirements
     */
    @Test
    void testSpecificExample() {
        // Mock AI response for complex query
        mockAIResponse("[{\"operation\":\"MULTIPLY\",\"operands\":[10,5]},{\"operation\":\"ADD\",\"operands\":[10]},{\"operation\":\"SUBTRACT\",\"operands\":[5]}]");
        
        // Execute service
        MathResponse response = mathAgentService.processMathQuery("multiply 10 by 5, add 10, subtract 5");
        
        // Verify all steps occurred correctly
        assertEquals(55.0, response.getResult());
        assertEquals(3, response.getSteps().size());
    }
    
    /**
     * Test invalid input handling
     */
    @Test
    void testInvalidInput() {
        // Mock AI response with empty array
        mockAIResponse("[]");
        
        // Verify that appropriate exception is thrown
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            mathAgentService.processMathQuery("what is the meaning of life?");
        });
        
        assertTrue(exception.getMessage().contains("No valid math operations"));
    }
    
    /**
     * Test fallback to regex parsing when AI fails
     */
    @Test
    void testFallbackToRegexParsing() {
        // Mock AI to throw exception to trigger regex fallback
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("AI unavailable"));
        
        // Execute service - should fallback to regex
        MathResponse response = mathAgentService.processMathQuery("add 5 and 7");
        
        // Verify result was calculated using regex fallback
        assertEquals(12.0, response.getResult());
    }
    
    /**
     * Test complex query with multiple operations and different phrasing
     */
    @Test
    void testComplexQueryWithDifferentPhrasing() {
        // Mock AI response
        mockAIResponse("[{\"operation\":\"MULTIPLY\",\"operands\":[6,7]},{\"operation\":\"DIVIDE\",\"operands\":[2]},{\"operation\":\"ADD\",\"operands\":[10]}]");
        
        // Execute service
        MathResponse response = mathAgentService.processMathQuery("multiply 6 and 7, divide by 2, add 10");
        
        // Verify result (6 * 7 = 42, then 42 / 2 = 21, then 21 + 10 = 31)
        assertEquals(31.0, response.getResult());
        assertEquals(3, response.getSteps().size());
    }
}

