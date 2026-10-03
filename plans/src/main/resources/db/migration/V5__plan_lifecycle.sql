-- HU-007 · Automatic expiry of plans and reminder before they start
ALTER TABLE plan ADD COLUMN reminded_at TIMESTAMP WITH TIME ZONE;

-- Plans published before this version: those already started or about to start get no late reminder
UPDATE plan SET reminded_at = published_at WHERE starts_at <= now() + INTERVAL '30 minutes';

-- The lifecycle task looks for due plans every minute: only the ones still moving (not finished or cancelled)
CREATE INDEX plan_lifecycle ON plan (starts_at) WHERE status IN ('OPEN', 'FULL', 'IN_PROGRESS');
