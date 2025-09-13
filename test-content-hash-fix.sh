#!/bin/bash

# Test script to verify content hash population
echo "🔧 Testing content hash population fix..."

# Check if the application is running
if ! curl -s http://localhost:8081/actuator/health > /dev/null; then
    echo "❌ Application is not running. Please start it first with: mvn spring-boot:run"
    exit 1
fi

echo "✅ Application is running"

# Test 1: Check database migration
echo ""
echo "🔍 Test 1: Checking if migration ran successfully..."
echo "The V3 migration should have populated content_hash values for existing documents."
echo "Look for this in your application startup logs:"
echo "  - Flyway migration messages"
echo "  - 'Retrieved X existing content hashes from database'"

# Test 2: Test re-ingestion to see if duplicates are now properly detected
echo ""
echo "🔍 Test 2: Testing duplicate detection with populated hashes..."
echo "Calling POST /review/admin/reingest to test duplicate detection..."

response=$(curl -s -X POST http://localhost:8081/review/admin/reingest)
echo "Response: $response"

if [[ $response == *"successfully"* ]]; then
    echo "✅ Re-ingestion completed successfully"
else
    echo "❌ Re-ingestion failed: $response"
fi

# Test 3: Test normal ingestion (should skip duplicates)
echo ""
echo "🔍 Test 3: Testing normal ingestion (should skip duplicates)..."
echo "Restart the application to see if it now properly detects existing documents."
echo "You should see logs like:"
echo "  - 'Found X existing document chunks in database'"
echo "  - 'No new documents to ingest (all X chunks already exist)'"

echo ""
echo "🎯 Next Steps:"
echo "1. Check your application logs for migration messages"
echo "2. Look for 'Retrieved X existing content hashes from database' in the logs"
echo "3. Restart the application to see duplicate detection in action"
echo "4. If you still see null values, the migration might not have run - check Flyway logs"
