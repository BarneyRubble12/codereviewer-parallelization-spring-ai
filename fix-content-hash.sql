-- Fix content_hash for existing documents
-- Run this script directly against your database if the migration didn't work

-- Enable pgcrypto extension if not already enabled
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Check current state
SELECT 
    COUNT(*) as total_documents,
    COUNT(content_hash) as documents_with_hash,
    COUNT(*) - COUNT(content_hash) as documents_without_hash
FROM ai_documents;

-- Update existing documents with null content_hash
UPDATE ai_documents 
SET content_hash = encode(digest(COALESCE(metadata->>'source', 'unknown') || '|' || content, 'sha256'), 'hex')
WHERE content_hash IS NULL;

-- Verify the update
SELECT 
    COUNT(*) as total_documents,
    COUNT(content_hash) as documents_with_hash,
    COUNT(*) - COUNT(content_hash) as documents_without_hash
FROM ai_documents;

-- Show a few examples of the populated hashes
SELECT 
    id,
    LEFT(content, 50) as content_preview,
    metadata->>'source' as source,
    LEFT(content_hash, 16) as hash_preview
FROM ai_documents 
LIMIT 5;
