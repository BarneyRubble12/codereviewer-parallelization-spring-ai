# API Testing Examples

This file contains ready-to-use curl commands for testing the Code Reviewer API.

## Prerequisites

Make sure the application is running on `http://localhost:8081`:
```bash
mvn spring-boot:run
```

## 1. Health Check

```bash
curl http://localhost:8081/actuator/health
```

## 2. Sequential Diff Review

```bash
curl -X POST http://localhost:8081/review/diff \
  -H "Content-Type: application/json" \
  -d '{
    "patch": "diff --git a/Service.java b/Service.java\nindex 1234567..abcdefg 100644\n--- a/Service.java\n+++ b/Service.java\n@@ -1,3 +1,5 @@\n public class Service {\n+    private String password = \"secret123\";\n+    private List<String> items = new ArrayList<>();\n+    \n     public void doSomething() {\n         // implementation\n     }\n }",
    "parallel": false
  }'
```

## 3. Parallel Diff Review

```bash
curl -X POST http://localhost:8081/review/diff \
  -H "Content-Type: application/json" \
  -d '{
    "patch": "diff --git a/Service.java b/Service.java\nindex 1234567..abcdefg 100644\n--- a/Service.java\n+++ b/Service.java\n@@ -1,3 +1,5 @@\n public class Service {\n+    private String password = \"secret123\";\n+    private List<String> items = new ArrayList<>();\n+    \n     public void doSomething() {\n         // implementation\n     }\n }",
    "parallel": true
  }'
```

## 4. GitHub PR Review (Replace with actual repo/PR)

```bash
curl -X POST http://localhost:8081/review/pr \
  -H "Content-Type: application/json" \
  -d '{
    "repo": "spring-projects/spring-boot",
    "prNumber": 12345,
    "parallel": true
  }'
```

## 5. Complex Diff with Multiple Issues

```bash
curl -X POST http://localhost:8081/review/diff \
  -H "Content-Type: application/json" \
  -d '{
    "patch": "diff --git a/UserService.java b/UserService.java\nindex 1234567..abcdefg 100644\n--- a/UserService.java\n+++ b/UserService.java\n@@ -1,10 +1,15 @@\n public class UserService {\n+    private String dbPassword = \"admin123\";\n+    \n     public List<User> getUsers() {\n-        return userRepository.findAll();\n+        List<User> users = new ArrayList<>();\n+        for (int i = 0; i < 1000; i++) {\n+            users.add(userRepository.findById(i));\n+        }\n+        return users;\n     }\n+    \n+    public void processUser(String userId) {\n+        // TODO: implement this method\n+    }\n }",
    "parallel": true
  }'
```

## 6. Performance Comparison Script

Save this as `performance-test.sh`:

```bash
#!/bin/bash

echo "🚀 Performance Comparison Test"
echo "=============================="

# Test Sequential
echo "Testing sequential execution..."
SEQUENTIAL_START=$(date +%s%3N)
curl -s -X POST http://localhost:8081/review/diff \
  -H "Content-Type: application/json" \
  -d '{"patch": "diff --git a/Test.java b/Test.java\nindex 123..456 100644\n--- a/Test.java\n+++ b/Test.java\n@@ -1,3 +1,4 @@\n public class Test {\n+    String password = \"secret\";\n     public void method() {\n     }\n }", "parallel": false}' > /dev/null
SEQUENTIAL_END=$(date +%s%3N)
SEQUENTIAL_DURATION=$((SEQUENTIAL_END - SEQUENTIAL_START))

# Test Parallel
echo "Testing parallel execution..."
PARALLEL_START=$(date +%s%3N)
curl -s -X POST http://localhost:8081/review/diff \
  -H "Content-Type: application/json" \
  -d '{"patch": "diff --git a/Test.java b/Test.java\nindex 123..456 100644\n--- a/Test.java\n+++ b/Test.java\n@@ -1,3 +1,4 @@\n public class Test {\n+    String password = \"secret\";\n     public void method() {\n     }\n }", "parallel": true}' > /dev/null
PARALLEL_END=$(date +%s%3N)
PARALLEL_DURATION=$((PARALLEL_END - PARALLEL_START))

echo ""
echo "📊 Results:"
echo "Sequential: ${SEQUENTIAL_DURATION}ms"
echo "Parallel:   ${PARALLEL_DURATION}ms"
if [ $SEQUENTIAL_DURATION -gt 0 ] && [ $PARALLEL_DURATION -gt 0 ]; then
    IMPROVEMENT=$(( (SEQUENTIAL_DURATION - PARALLEL_DURATION) * 100 / SEQUENTIAL_DURATION ))
    echo "Improvement: ${IMPROVEMENT}% faster with parallel execution"
fi
```

## Expected Response Format

```json
{
  "findings": [
    {
      "filePath": "Service.java",
      "lineStart": 2,
      "lineEnd": 2,
      "title": "Hardcoded password detected",
      "rationale": "Passwords should not be hardcoded in source code as this creates a security vulnerability",
      "suggestion": "Use environment variables or a secure configuration management system",
      "severity": "HIGH",
      "reviewer": "SECURITY"
    },
    {
      "filePath": "Service.java",
      "lineStart": 3,
      "lineEnd": 3,
      "title": "Inefficient collection initialization",
      "rationale": "ArrayList should be initialized with proper capacity if size is known",
      "suggestion": "Use new ArrayList<>(expectedSize) or consider using Arrays.asList() for fixed collections",
      "severity": "MEDIUM",
      "reviewer": "PERFORMANCE"
    }
  ],
  "summary": "Findings: 2 (BLOCKER=0, HIGH=1, MEDIUM=1, LOW=0)"
}
```

## Tips for Demo

1. **Watch the logs** - The application provides detailed logging with emojis
2. **Compare timing** - Run both sequential and parallel versions to see the difference
3. **Use jq** - Install jq for pretty JSON formatting: `brew install jq` (macOS) or `apt-get install jq` (Ubuntu)
4. **Monitor performance** - Check the application logs for detailed timing information

## Troubleshooting

- **Connection refused**: Make sure the application is running on port 8081
- **Database errors**: Ensure PostgreSQL is running via `docker-compose up -d`
- **OpenAI errors**: Verify your `OPENAI_API_KEY` environment variable is set
- **No findings**: Try with a more complex diff that contains potential issues
