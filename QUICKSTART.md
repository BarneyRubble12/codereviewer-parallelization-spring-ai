# Quick Start Guide

Get the Code Reviewer application running in 5 minutes!

## 🚀 Prerequisites

- Java 24+
- Maven 3.6+
- Docker & Docker Compose
- OpenAI API Key

## ⚡ Quick Setup

### 1. Set Environment Variable
```bash
export OPENAI_API_KEY="your-openai-api-key-here"
```

### 2. Start Database
```bash
docker-compose up -d
```

### 3. Run Application
```bash
mvn spring-boot:run
```

### 4. Test API
```bash
# Make the test script executable
chmod +x test-api.sh

# Run the test
./test-api.sh
```

## 🎯 Demo Commands

### Test Sequential vs Parallel
```bash
# Sequential (slower)
curl -X POST http://localhost:8081/review/diff \
  -H "Content-Type: application/json" \
  -d '{"patch": "diff --git a/Test.java b/Test.java\nindex 123..456 100644\n--- a/Test.java\n+++ b/Test.java\n@@ -1,3 +1,4 @@\n public class Test {\n+    String password = \"secret\";\n     public void method() {\n     }\n }", "parallel": false}'

# Parallel (faster)
curl -X POST http://localhost:8081/review/diff \
  -H "Content-Type: application/json" \
  -d '{"patch": "diff --git a/Test.java b/Test.java\nindex 123..456 100644\n--- a/Test.java\n+++ b/Test.java\n@@ -1,3 +1,4 @@\n public class Test {\n+    String password = \"secret\";\n     public void method() {\n     }\n }", "parallel": true}'
```

## 📊 What to Watch

1. **Application Logs**: Look for emoji indicators showing the workflow
2. **Timing**: Compare execution times between sequential and parallel
3. **Findings**: See what issues the AI reviewers detect

## 🔧 Troubleshooting

- **Port 8081 in use**: Change `server.port` in `application.properties`
- **Database issues**: Run `docker-compose logs pgvector`
- **OpenAI errors**: Check your API key and credits

## 📚 Next Steps

- Read the full [README.md](README.md) for detailed documentation
- Check [curl-examples.md](curl-examples.md) for more API examples
- Explore the code to understand the parallelization pattern

---

**Ready to demo! 🎉**
