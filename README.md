# Code Reviewer - Parallelization Workflow with Spring AI

A Spring Boot application that demonstrates the **Parallelization Workflow pattern** using Spring AI for automated code review. The application can analyze code changes either from raw unified diffs or GitHub pull requests using multiple AI-powered reviewers running in parallel.

## 🎯 Overview

This application showcases how to implement parallel processing in Spring Boot applications using **virtual threads** (Project Loom) to achieve significant performance improvements. It uses multiple specialized AI reviewers that analyze code changes simultaneously, then aggregates their findings into a comprehensive report.

### Key Features

- **🚀 Parallel Processing**: Uses virtual threads for concurrent code review execution
- **🤖 AI-Powered Reviews**: Multiple specialized reviewers (Security, Performance, Clean Code)
- **📊 Performance Comparison**: Built-in timing to compare parallel vs sequential execution
- **🔍 Standards-Based**: Uses vector search to ground AI reviews with internal standards
- **🌐 GitHub Integration**: Can review pull requests directly from GitHub
- **📈 Comprehensive Logging**: Detailed logging with emojis for demo visibility
- **🔄 Smart Document Ingestion**: Prevents duplicate document ingestion with content hashing

## 🏗️ Architecture

### Core Components

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   REST API      │    │  Parallel        │    │   AI Reviewers  │
│   Controller    │───▶│  Workflow        │───▶│                 │
│                 │    │  Service         │    │ • Security      │
└─────────────────┘    └──────────────────┘    │ • Performance   │
         │                       │             │ • Clean Code    │
         ▼                       ▼             └─────────────────┘
┌─────────────────┐    ┌──────────────────┐              │
│   Diff Service  │    │   Aggregator     │              ▼
│                 │    │   Service        │    ┌─────────────────┐
└─────────────────┘    └──────────────────┘    │  Vector Store   │
         │                       │             │  (pgvector)     │
         ▼                       ▼             └─────────────────┘
┌─────────────────┐    ┌──────────────────┐
│  GitHub Client  │    │   Standards      │
│                 │    │   Retriever      │
└─────────────────┘    └──────────────────┘
```

### Technology Stack

- **Framework**: Spring Boot 3.5.5
- **Java Version**: Java 24
- **AI Integration**: Spring AI 1.0.1 with OpenAI
- **Database**: PostgreSQL with pgvector extension
- **Migration**: Flyway
- **Concurrency**: Virtual Threads (Project Loom)
- **Logging**: SLF4J with Lombok
- **Build Tool**: Maven

## 🔄 Parallelization Workflow

### Sequential vs Parallel Execution

The application demonstrates two execution modes:

#### Sequential Execution (`parallel: false`)
```
Request → Security Review → Performance Review → Clean Code Review → Aggregate → Response
         (2.5s)           (2.5s)              (2.5s)              (0.1s)     (7.6s total)
```

#### Parallel Execution (`parallel: true`)
```
Request → [Security Review] → Aggregate → Response
         [Performance Review] → (0.1s)   (2.6s total)
         [Clean Code Review] →
         (2.5s concurrent)
```

### Performance Benefits

- **Sequential**: ~7.6 seconds for 3 reviewers
- **Parallel**: ~2.6 seconds for 3 reviewers
- **Improvement**: ~65% faster execution

## 🚀 Getting Started

### Prerequisites

- Java 24+
- Maven 3.6+
- Docker & Docker Compose
- OpenAI API Key

### 1. Environment Setup

Set your OpenAI API key:
```bash
export OPENAI_API_KEY="your-openai-api-key-here"
```

### 2. Start the Database

```bash
docker-compose up -d
```

This starts a PostgreSQL database with pgvector extension on port 5432.

### 3. Build and Run

```bash
mvn clean compile
mvn spring-boot:run
```

The application will start on `http://localhost:8081`

### 4. Verify Setup

Check the health endpoint:
```bash
curl http://localhost:8081/actuator/health
```

## 📡 API Endpoints

### 1. Review Raw Diff

**POST** `/review/diff`

Review a unified diff directly:

```bash
curl -X POST http://localhost:8081/review/diff \
  -H "Content-Type: application/json" \
  -d '{
    "patch": "diff --git a/Service.java b/Service.java\nindex 1234567..abcdefg 100644\n--- a/Service.java\n+++ b/Service.java\n@@ -1,3 +1,5 @@\n public class Service {\n+    private String password = \"secret123\";\n+    \n     public void doSomething() {\n         // implementation\n     }\n }",
    "parallel": true
  }'
```

### 2. Review GitHub Pull Request

**POST** `/review/pr`

Review a GitHub pull request:

```bash
curl -X POST http://localhost:8081/review/pr \
  -H "Content-Type: application/json" \
  -d '{
    "repo": "owner/repository",
    "prNumber": 123,
    "parallel": true
  }'
```

### Request/Response Format

#### Request
```json
{
  "patch": "unified diff content",  // For /review/diff
  "repo": "owner/repo",            // For /review/pr
  "prNumber": 123,                 // For /review/pr
  "parallel": true                 // Enable parallel execution
}
```

#### Response
```json
{
  "findings": [
    {
      "filePath": "src/main/java/Service.java",
      "lineStart": 2,
      "lineEnd": 2,
      "title": "Hardcoded password detected",
      "rationale": "Passwords should not be hardcoded in source code",
      "suggestion": "Use environment variables or secure configuration",
      "severity": "HIGH",
      "reviewer": "SECURITY"
    }
  ],
  "summary": "Findings: 3 (BLOCKER=0, HIGH=1, MEDIUM=1, LOW=1)"
}
```

## 🔍 AI Reviewers

### Security Reviewer 🔒
- **Focus**: Security vulnerabilities, authentication, authorization
- **Standards**: Security best practices, OWASP guidelines
- **Examples**: Hardcoded secrets, SQL injection, XSS vulnerabilities

### Performance Reviewer ⚡
- **Focus**: Performance bottlenecks, memory usage, scalability
- **Standards**: Performance optimization guidelines
- **Examples**: N+1 queries, memory leaks, inefficient algorithms

### Clean Code Reviewer 🧹
- **Focus**: Code quality, maintainability, readability
- **Standards**: Clean code principles, SOLID principles
- **Examples**: Code duplication, complex methods, poor naming

## 🔄 Smart Document Ingestion

The application includes intelligent document ingestion that prevents duplicate documents from being stored in the vector database.

### How It Works

1. **Content Hashing**: Each document chunk is hashed using SHA-256 based on content + source filename
2. **Duplicate Detection**: Before ingestion, the system checks existing content hashes in the database
3. **Selective Ingestion**: Only new or changed documents are ingested, skipping duplicates
4. **Change Tracking**: The system tracks content changes and updates only when necessary

### Database Schema

The `ai_documents` table includes a `content_hash` column for efficient duplicate detection:

```sql
ALTER TABLE ai_documents ADD COLUMN content_hash VARCHAR(64);
CREATE INDEX ai_documents_content_hash_idx ON ai_documents(content_hash);
```

### Admin Endpoints

- **POST `/review/admin/reingest`**: Force complete re-ingestion of all standards documents
  - Useful for development or when standards files have been updated
  - Clears all existing documents and re-ingests everything

### Testing Duplicate Prevention

Run the test script to verify the functionality:

```bash
./test-duplicate-prevention.sh
```

This script will:
1. Check if the application is running
2. Test the manual re-ingestion endpoint
3. Verify duplicate prevention behavior

### Fixing Existing Data

If you have existing documents with null `content_hash` values, you can fix them by:

1. **Automatic Fix**: Restart the application - Flyway will run the V3 migration
2. **Manual Fix**: Run the SQL script directly:
   ```bash
   psql -h localhost -U codereviewer -d codereviewer -f fix-content-hash.sql
   ```
3. **Test the Fix**: Run the test script:
   ```bash
   ./test-content-hash-fix.sh
   ```

### Troubleshooting Migration Issues

If you encounter migration errors related to the `digest` function:

1. **Reset Database**: Use the reset script to start fresh:
   ```bash
   ./reset-database.sh
   ```
   This will:
   - Stop the application and database
   - Remove the database volume
   - Start fresh with proper extensions (pgcrypto)
   - Run all migrations automatically

2. **Manual Database Reset**: If the script doesn't work:
   ```bash
   docker-compose down -v
   docker volume rm codereviewer-parallelization-spring-ai_pgdata
   docker-compose up -d
   mvn spring-boot:run
   ```

### Log Messages

Watch for these log messages to understand the ingestion behavior:

- `Found X existing document chunks in database` - Shows how many documents already exist
- `Ingested X new document chunks from Y files (skipped Z duplicates)` - Shows ingestion results
- `No new documents to ingest (all X chunks already exist)` - All documents are up-to-date

## 📊 Monitoring and Logging

The application provides comprehensive logging with visual indicators:

### Log Levels
- **INFO**: Main workflow steps, timing information
- **DEBUG**: Detailed processing steps, individual operations
- **WARN**: Non-critical issues, fallback behaviors
- **ERROR**: Exceptions and failures

### Sample Log Output
```
🎯 ===== NEW PR REVIEW REQUEST =====
📋 Request details: repo=owner/repo, pr=123, parallel=true
🔗 Fetching PR patch from GitHub: owner/repo/123
📡 GitHub API response: 200 in 450ms, body size: 2048 characters
📄 Starting diff parsing - patch size: 2048 characters
📁 Found 3 files in diff
🚀 Starting code review workflow - 6 hunks, parallel execution: true
⚡ Executing PARALLEL workflow with 3 reviewers using virtual threads
🔒 Starting SECURITY review for 6 hunks
⚡ Starting PERFORMANCE review for 6 hunks
🧹 Starting CLEAN CODE review for 6 hunks
✅ SECURITY review completed in 1250ms - 3 findings
✅ PERFORMANCE review completed in 1180ms - 2 findings
✅ CLEAN CODE review completed in 1320ms - 4 findings
📊 Parallel execution completed in 1320ms
🔗 Starting aggregation of 3 review results
📋 Total findings before deduplication: 9
🔄 Deduplication complete: 7 unique findings (removed 2 duplicates)
📈 Final aggregated result: 7 findings - Findings: 7 (BLOCKER=0, HIGH=2)
🏁 PR REVIEW COMPLETE: 7 findings in 1800ms
🎯 ===== END PR REVIEW REQUEST =====
```

## 🛠️ Development

### Project Structure
```
src/main/java/com/hrpd/codereview/
├── config/                 # Spring configuration
├── controller/             # REST endpoints
├── model/                  # Data models and DTOs
├── reviewer/               # AI reviewer implementations
├── service/                # Business logic services
└── utils/                  # Utility classes

src/main/resources/
├── application.properties  # Application configuration
├── db/migration/          # Database migrations
└── standards/             # AI review standards
```

### Key Design Patterns

1. **Strategy Pattern**: Different reviewer implementations
2. **Template Method**: Common review workflow structure
3. **Dependency Injection**: Spring's IoC container
4. **Parallelization Workflow**: Concurrent execution pattern

### Adding New Reviewers

1. Implement the `Reviewer` interface:
```java
@Slf4j
@RequiredArgsConstructor
public class CustomReviewer implements Reviewer {
    private final ChatClient chat;
    private final StandardsRetrieverService retriever;
    
    @Override
    public ReviewerType type() { return ReviewerType.CUSTOM; }
    
    @Override
    public ReviewResult review(List<DiffHunk> hunks) {
        // Implementation
    }
}
```

2. Add bean configuration in `AppConfig.java`
3. The reviewer will automatically be included in parallel execution

## 🧪 Testing

### Manual Testing

1. **Start the application**:
```bash
mvn spring-boot:run
```

2. **Test with a simple diff**:
```bash
curl -X POST http://localhost:8081/review/diff \
  -H "Content-Type: application/json" \
  -d '{
    "patch": "diff --git a/Test.java b/Test.java\nindex 123..456 100644\n--- a/Test.java\n+++ b/Test.java\n@@ -1,3 +1,4 @@\n public class Test {\n+    String password = \"secret\";\n     public void method() {\n     }\n }",
    "parallel": true
  }'
```

3. **Compare parallel vs sequential**:
```bash
# Parallel execution
curl -X POST http://localhost:8081/review/diff \
  -H "Content-Type: application/json" \
  -d '{"patch": "...", "parallel": true}'

# Sequential execution  
curl -X POST http://localhost:8081/review/diff \
  -H "Content-Type: application/json" \
  -d '{"patch": "...", "parallel": false}'
```

### Performance Testing

Monitor the logs to see timing differences:
- Look for `📊 Parallel execution completed in Xms`
- Look for `📊 Sequential execution completed in Xms`
- Compare total execution times

## 🔧 Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# Server
server.port=8081

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/codereviewer
spring.datasource.username=codereviewer
spring.datasource.password=codereviewer

# OpenAI
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.2

# Vector Store
spring.ai.vectorstore.pgvector.initialize-schema=false
spring.ai.vectorstore.pgvector.table-name=ai_documents

# Logging
logging.level.com.hrpd.reviewer=INFO
```

### Environment Variables

- `OPENAI_API_KEY`: Your OpenAI API key (required)
- `GITHUB_TOKEN`: Optional GitHub token for private repositories
- `GITHUB_BASE_URL`: Optional custom GitHub API URL

## 🚨 Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Ensure Docker is running
   - Check if PostgreSQL container is healthy: `docker ps`
   - Verify database credentials in `application.properties`

2. **OpenAI API Errors**
   - Verify `OPENAI_API_KEY` is set correctly
   - Check API key has sufficient credits
   - Ensure network connectivity to OpenAI

3. **No Standards Found**
   - Check if standards files exist in `src/main/resources/standards/`
   - Verify Flyway migration ran successfully
   - Check application logs for ingestion errors

4. **Performance Issues**
   - Monitor virtual thread usage in logs
   - Check database connection pool settings
   - Verify OpenAI API rate limits

### Debug Mode

Enable debug logging:
```properties
logging.level.com.hrpd.codereview=DEBUG
```

## 📈 Performance Optimization

### Virtual Threads
The application uses Java 24's virtual threads for optimal concurrency:
- Lightweight thread creation
- Efficient I/O handling
- Automatic scaling

### Database Optimization
- pgvector extension for fast similarity search
- Proper indexing on embeddings
- Connection pooling

### AI Model Optimization
- Uses `gpt-4o-mini` for cost efficiency
- Low temperature (0.2) for consistent results
- Efficient embedding model (`text-embedding-3-small`)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Spring AI team for the excellent AI integration framework
- OpenAI for providing the AI models
- PostgreSQL team for pgvector extension
- Project Loom team for virtual threads

---

**Happy Coding! 🚀**
