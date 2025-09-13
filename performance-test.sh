#!/bin/bash

# Performance Test Script for Code Reviewer API
# This script compares sequential vs parallel execution performance

BASE_URL="http://localhost:8081"
TEST_DIFF='{"patch": "diff --git a/Service.java b/Service.java\nindex 1234567..abcdefg 100644\n--- a/Service.java\n+++ b/Service.java\n@@ -1,3 +1,6 @@\n public class Service {\n+    private String password = \"secret123\";\n+    private List<String> items = new ArrayList<>();\n+    private Map<String, Object> cache = new HashMap<>();\n+    \n     public void doSomething() {\n         // implementation\n     }\n }", "parallel": %s}'

echo "🚀 Code Reviewer Performance Test"
echo "================================="
echo ""

# Check if application is running
echo "🔍 Checking if application is running..."
if ! curl -s "$BASE_URL/actuator/health" > /dev/null; then
    echo "❌ Application is not running on $BASE_URL"
    echo "Please start the application with: mvn spring-boot:run"
    exit 1
fi
echo "✅ Application is running"
echo ""

# Test Sequential Execution
echo "📋 Testing SEQUENTIAL execution..."
SEQUENTIAL_START=$(date +%s%3N)
curl -s -X POST "$BASE_URL/review/diff" \
  -H "Content-Type: application/json" \
  -d "$(printf "$TEST_DIFF" "false")" > /dev/null
SEQUENTIAL_END=$(date +%s%3N)
SEQUENTIAL_DURATION=$((SEQUENTIAL_END - SEQUENTIAL_START))
echo "⏱️  Sequential execution: ${SEQUENTIAL_DURATION}ms"
echo ""

# Wait between tests
sleep 2

# Test Parallel Execution
echo "⚡ Testing PARALLEL execution..."
PARALLEL_START=$(date +%s%3N)
curl -s -X POST "$BASE_URL/review/diff" \
  -H "Content-Type: application/json" \
  -d "$(printf "$TEST_DIFF" "true")" > /dev/null
PARALLEL_END=$(date +%s%3N)
PARALLEL_DURATION=$((PARALLEL_END - PARALLEL_START))
echo "⏱️  Parallel execution: ${PARALLEL_DURATION}ms"
echo ""

# Calculate improvement
echo "📊 Performance Comparison:"
echo "=========================="
echo "Sequential: ${SEQUENTIAL_DURATION}ms"
echo "Parallel:   ${PARALLEL_DURATION}ms"

if [ $SEQUENTIAL_DURATION -gt 0 ] && [ $PARALLEL_DURATION -gt 0 ]; then
    IMPROVEMENT=$(( (SEQUENTIAL_DURATION - PARALLEL_DURATION) * 100 / SEQUENTIAL_DURATION ))
    TIME_SAVED=$((SEQUENTIAL_DURATION - PARALLEL_DURATION))
    
    echo "Time saved: ${TIME_SAVED}ms"
    echo "Improvement: ${IMPROVEMENT}% faster with parallel execution"
    
    if [ $IMPROVEMENT -gt 0 ]; then
        echo "🎉 Parallel execution is ${IMPROVEMENT}% faster!"
    else
        echo "⚠️  Sequential execution was faster (this might be due to overhead for small workloads)"
    fi
else
    echo "⚠️  Could not calculate improvement (invalid timing data)"
fi

echo ""
echo "💡 Tip: Check the application logs to see detailed execution flow with emojis!"
echo "💡 Tip: For more accurate results, run this test multiple times and average the results."
