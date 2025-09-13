-- Populate content_hash for existing documents
-- This migration calculates content hashes for documents that were created before the hash column was added

-- Enable pgcrypto extension if not already enabled
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Update existing documents with null content_hash using pgcrypto
UPDATE ai_documents 
SET content_hash = encode(digest(COALESCE(metadata->>'source', 'unknown') || '|' || content, 'sha256'), 'hex')
WHERE content_hash IS NULL;

-- Verify the update
-- This will show how many documents now have content hashes
SELECT 
    COUNT(*) as total_documents,
    COUNT(content_hash) as documents_with_hash,
    COUNT(*) - COUNT(content_hash) as documents_without_hash
FROM ai_documents;
