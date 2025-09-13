-- Add content hash column for duplicate detection and change tracking
ALTER TABLE ai_documents ADD COLUMN content_hash VARCHAR(64);

-- Create index for fast content hash lookups
CREATE INDEX IF NOT EXISTS ai_documents_content_hash_idx ON ai_documents(content_hash);

-- Create index for source file lookups
CREATE INDEX IF NOT EXISTS ai_documents_source_idx ON ai_documents((metadata->>'source'));
