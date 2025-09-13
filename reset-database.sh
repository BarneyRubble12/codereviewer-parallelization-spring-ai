#!/bin/bash

# Script to reset the database and start fresh with proper extensions
echo "🔄 Resetting database to fix migration issues..."

# Stop the application if running
echo "🛑 Stopping application..."
pkill -f "spring-boot:run" || true

# Stop and remove the database container
echo "🗑️  Stopping and removing database container..."
docker-compose down -v

# Remove the volume to start completely fresh
echo "🧹 Removing database volume..."
docker volume rm codereviewer-parallelization-spring-ai_pgdata 2>/dev/null || true

# Start the database fresh
echo "🚀 Starting fresh database with proper extensions..."
docker-compose up -d

# Wait for database to be ready
echo "⏳ Waiting for database to be ready..."
sleep 10

# Check if database is ready
echo "🔍 Checking database connection..."
until docker exec codereview-pg pg_isready -U codereviewer -d codereviewer; do
    echo "⏳ Waiting for database..."
    sleep 2
done

echo "✅ Database is ready!"
echo ""
echo "🎯 Next steps:"
echo "1. Start your application: mvn spring-boot:run"
echo "2. The migrations will run automatically with proper extensions"
echo "3. Content hashes will be populated for existing documents"
echo "4. Duplicate prevention will work correctly"
