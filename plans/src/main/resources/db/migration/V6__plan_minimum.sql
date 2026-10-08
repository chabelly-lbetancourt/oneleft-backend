-- HU-039 · Minimum of participants: if fewer people have joined by the deadline, the plan is cancelled
ALTER TABLE plan ADD COLUMN min_participants INTEGER;
ALTER TABLE plan ADD COLUMN minimum_deadline TIMESTAMP WITH TIME ZONE;
ALTER TABLE plan ADD COLUMN confirmed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE plan ADD CONSTRAINT plan_minimum CHECK (
    (min_participants IS NULL AND minimum_deadline IS NULL AND confirmed_at IS NULL)
    OR (min_participants BETWEEN 1 AND spots AND minimum_deadline IS NOT NULL AND minimum_deadline <= starts_at));

-- The lifecycle task also looks every minute for the minimums whose deadline has come: only the pending ones
CREATE INDEX plan_minimum_pending ON plan (minimum_deadline)
    WHERE min_participants IS NOT NULL AND confirmed_at IS NULL AND status IN ('OPEN', 'FULL');
