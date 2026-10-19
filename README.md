<p align="center">
  <img src="assets/logo.png" alt="Springai Math Test logo" width="180">
</p>

# Math Agent with Spring AI and Ollama

Agentic Math Service that uses Spring AI with local LLM (Llama via Ollama) to parse natural language math queries.

## Prerequisites

1. **Java 21** - The project is configured to use Java 21
2. **Maven 3.9+**
3. **Ollama** - For running local LLM models

## Setup

### 1. Install and Run Ollama

```bash
# Install Ollama (macOS)
brew install ollama

# Or download from https://ollama.ai

# Start Ollama server
ollama serve

# In another terminal, pull a model (e.g., Llama 3.2)
ollama pull llama3.2

# Or try other models like:
# ollama pull llama3.1
# ollama pull mistral
```

### 2. Configure the Application

The application is pre-configured to use Ollama in `src/main/resources/application.properties`:

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=llama3.2
```

You can change the model by editing this file.

### 3. Build and Run

```bash
# Build the project (uses Java 21 automatically)
./build.sh

# Or manually with:
export JAVA_HOME=$(/usr/libexec/java_home -v 21) && mvn clean package

# Run the application
java -jar target/math-agent-0.0.1-SNAPSHOT.jar
```

## Usage

Once the application is running, you can send math queries to it:

```bash
curl -X POST http://localhost:8080/api/math \
  -H "Content-Type: application/json" \
  -d '{"query": "multiply 10 by 5, then add 7, then subtract 3"}'
```

Expected response:
```json
{
  "result": 54.0,
  "steps": [
    "Multiplying 10 by 5 = 50.0",
    "Adding 7 to 50.0 = 57.0",
    "Subtracting 3 from 57.0 = 54.0"
  ],
  "explanation": "I performed the following steps: Multiplying 10 by 5 = 50.0. Then, Adding 7 to 50.0 = 57.0. Then, Subtracting 3 from 57.0 = 54.0"
}
```

### Example Queries

```bash
# Simple addition
curl -X POST http://localhost:8080/api/math \
  -H "Content-Type: application/json" \
  -d '{"query": "add 5 and 3"}'

# Division
curl -X POST http://localhost:8080/api/math \
  -H "Content-Type: application/json" \
  -d '{"query": "divide 20 by 4"}'

# Complex chained operations
curl -X POST http://localhost:8080/api/math \
  -H "Content-Type: application/json" \
  -d '{"query": "multiply 6 and 7, divide by 2, add 10"}'
```

## How It Works

1. **Spring AI Integration**: The service uses Spring AI's `ChatModel` interface to communicate with Ollama
2. **LLM Parsing**: Natural language queries are sent to the local LLM (Llama) which extracts math operations in JSON format
3. **Fallback**: If the LLM is unavailable or returns invalid data, the system falls back to regex-based parsing
4. **Execution**: Operations are executed sequentially, maintaining state between steps

## Architecture

- **MathAgentService**: Core service that integrates with Spring AI
  - Sends prompts to the LLM via ChatModel
  - Parses JSON responses from the LLM
  - Falls back to regex if LLM fails
  
- **MathOperationType**: Enum defining supported operations (ADD, SUBTRACT, MULTIPLY, DIVIDE)

- **MathState**: Maintains current value and operation history

## Testing

The project includes comprehensive unit tests with mocked LLM responses:

```bash
./build.sh test

# Or manually:
export JAVA_HOME=$(/usr/libexec/java_home -v 21) && mvn test
```

All tests use Mockito to mock the `ChatModel`, so they don't require a running Ollama instance.

## Configuration Options

Edit `src/main/resources/application.properties` to customize:

```properties
# Change the Ollama endpoint
spring.ai.ollama.base-url=http://localhost:11434

# Change the model
spring.ai.ollama.chat.model=llama3.2  # or mistral, llama3.1, etc.

# Adjust creativity (0.0 = deterministic, 1.0 = creative)
spring.ai.ollama.chat.options.temperature=0.1

# Adjust response diversity
spring.ai.ollama.chat.options.top-p=0.9

# Change server port
server.port=8080
```

## Troubleshooting

### Ollama not running
```
Error: Connection refused to localhost:11434
```
**Solution**: Start Ollama with `ollama serve`

### Model not found
```
Error: model 'llama3.2' not found
```
**Solution**: Pull the model with `ollama pull llama3.2`

### Java version mismatch
```
Error: Unsupported class file major version
```
**Solution**: Make sure you're using Java 21. The `build.sh` script handles this automatically.

## License

This is a demonstration project for showcasing Spring AI integration with local LLMs.
