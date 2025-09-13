#!/bin/bash

# Test script to verify duplicate prevention functionality
echo "🧪 Testing duplicate prevention functionality..."

# Start the application (assuming it's already running)
echo "📡 Testing application startup behavior..."

# First, check if the application is running
if ! curl -s http://localhost:8081/actuator/health > /dev/null; then
    echo "❌ Application is not running. Please start it first with: mvn spring-boot:run"
    exit 1
fi

echo "✅ Application is running"

# Test 1: Check initial ingestion logs
echo ""
echo "🔍 Test 1: Checking initial ingestion behavior..."
echo "Look for these log messages in your application console:"
echo "  - 'Found X existing document chunks in database'"
echo "  - 'Ingested X new document chunks from Y files (skipped Z duplicates)'"
echo "  - 'No new documents to ingest (all X chunks already exist)'"

# Test 2: Test manual re-ingestion endpoint
echo ""
echo "🔍 Test 2: Testing manual re-ingestion endpoint..."
echo "Calling POST /review/admin/reingest..."

response=$(curl -s -X POST http://localhost:8081/review/admin/reingest)
echo "Response: $response"

if [[ $response == *"successfully"* ]]; then
    echo "✅ Manual re-ingestion endpoint works"
else
    echo "❌ Manual re-ingestion endpoint failed"
fi

# Test 3: Test duplicate prevention by calling the endpoint again
echo ""
echo "🔍 Test 3: Testing duplicate prevention by calling re-ingestion again..."
echo "This should show that no new documents are ingested..."

response2=$(curl -s -X POST http://localhost:8081/review/admin/reingest)
echo "Response: $response2"

echo ""
echo "🎯 Test Summary:"
echo "1. Check your application logs for duplicate prevention messages"
echo "2. The first startup should show 'Found 0 existing document chunks'"
echo "3. Subsequent startups should show 'Found X existing document chunks' and skip duplicates"
echo "4. Manual re-ingestion should work via POST /review/admin/reingest"
echo ""
echo "💡 To see detailed logs, run: mvn spring-boot:run and watch the console output"
