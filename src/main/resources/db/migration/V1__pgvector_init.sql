-- Requires superuser or a role with CREATE on database
CREATE EXTENSION IF NOT EXISTS vector;

-- Drop existing table if it exists (to fix schema mismatch)
DROP TABLE IF EXISTS ai_documents;

CREATE TABLE ai_documents (
  id         UUID PRIMARY KEY,
  content    TEXT NOT NULL,
  metadata   JSONB,
  embedding  VECTOR(1536) NOT NULL   -- matches text-embedding-3-small
);

CREATE INDEX IF NOT EXISTS ai_documents_embedding_idx
  ON ai_documents
  USING ivfflat (embedding vector_cosine_ops)
  WITH (lists = 100);
