-- V8: Add granular pipeline stage and error message tracking for tenders and vendor submissions

ALTER TABLE tenders 
ADD COLUMN IF NOT EXISTS processing_stage VARCHAR(100) DEFAULT 'IDLE',
ADD COLUMN IF NOT EXISTS error_message TEXT;

ALTER TABLE vendor_submissions 
ADD COLUMN IF NOT EXISTS processing_stage VARCHAR(100) DEFAULT 'QUEUED',
ADD COLUMN IF NOT EXISTS error_message TEXT;
