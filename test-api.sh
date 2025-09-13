#!/bin/bash

# Code Reviewer API Test Script
# This script demonstrates the parallelization workflow

BASE_URL="http://localhost:8081"

echo "🎯 Code Reviewer API Test Script"
echo "================================="
echo ""

# Test 1: Health Check
echo "1. Testing health endpoint..."
curl -s "$BASE_URL/actuator/health" | jq '.' 2>/dev/null || echo "Health check response received"
echo ""

# Test 2: Sequential Execution
echo "2. Testing SEQUENTIAL execution..."
echo "📋 Sending diff review request (sequential)..."
SEQUENTIAL_START=$(date +%s%3N)
curl -X POST "$BASE_URL/review/diff" \
  -H "Content-Type: application/json" \
  -d '{
    "patch": "diff --git a/Service.java b/Service.java\nindex 1234567..abcdefg 100644\n--- a/Service.java\n+++ b/Service.java\n@@ -1,3 +1,5 @@\n public class Service {\n+    private String password = \"secret123\";\n+    private List<String> items = new ArrayList<>();\n+    \n     public void doSomething() {\n         // implementation\n     }\n }",
    "parallel": false
  }' | jq '.' 2>/dev/null || echo "Sequential review completed"
SEQUENTIAL_END=$(date +%s%3N)
SEQUENTIAL_DURATION=$((SEQUENTIAL_END - SEQUENTIAL_START))
echo "⏱️  Sequential execution took: ${SEQUENTIAL_DURATION}ms"
echo ""

# Wait a moment between tests
sleep 2

# Test 3: Parallel Execution
echo "3. Testing PARALLEL execution..."
echo "⚡ Sending diff review request (parallel)..."
PARALLEL_START=$(date +%s%3N)
curl -X POST "$BASE_URL/review/diff" \
  -H "Content-Type: application/json" \
  -d '{
    "patch": "diff --git a/Service.java b/Service.java\nindex 1234567..abcdefg 100644\n--- a/Service.java\n+++ b/Service.java\n@@ -1,3 +1,5 @@\n public class Service {\n+    private String password = \"secret123\";\n+    private List<String> items = new ArrayList<>();\n+    \n     public void doSomething() {\n         // implementation\n     }\n }",
    "parallel": true
  }' | jq '.' 2>/dev/null || echo "Parallel review completed"
PARALLEL_END=$(date +%s%3N)
PARALLEL_DURATION=$((PARALLEL_END - PARALLEL_START))
echo "⏱️  Parallel execution took: ${PARALLEL_DURATION}ms"
echo ""

# Performance Comparison
echo "📊 Performance Comparison:"
echo "=========================="
echo "Sequential: ${SEQUENTIAL_DURATION}ms"
echo "Parallel:   ${PARALLEL_DURATION}ms"
if [ $SEQUENTIAL_DURATION -gt 0 ] && [ $PARALLEL_DURATION -gt 0 ]; then
    IMPROVEMENT=$(( (SEQUENTIAL_DURATION - PARALLEL_DURATION) * 100 / SEQUENTIAL_DURATION ))
    echo "Improvement: ${IMPROVEMENT}% faster with parallel execution"
fi
echo ""

# Test 4: GitHub PR Review (if you have a test repo)
echo "4. Testing GitHub PR review (optional)..."
echo "🔗 To test GitHub PR review, uncomment and modify the following:"
echo ""
echo "# curl -X POST \"$BASE_URL/review/pr\" \\"
echo "#   -H \"Content-Type: application/json\" \\"
echo "#   -d '{"
echo "#     \"repo\": \"your-username/your-repo\","
echo "#     \"prNumber\": 1,"
echo "#     \"parallel\": true"
echo "#   }' | jq '.'"
echo ""

echo "✅ Test script completed!"
echo "Check the application logs to see detailed execution flow with emojis."
