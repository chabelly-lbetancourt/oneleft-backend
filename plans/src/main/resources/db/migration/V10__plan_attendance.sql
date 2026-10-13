-- HU-009 · When the attendance of a plan was closed (its check-in closes 30 minutes after the start): who arrived and
-- who did not is published once per plan.
ALTER TABLE plan ADD COLUMN attendance_closed_at timestamp with time zone;

-- The plans already held count as closed: the check-in did not exist for most of them, so nobody is judged by them
UPDATE plan SET attendance_closed_at = now() WHERE starts_at + interval '30 minutes' <= now();

-- The plans whose attendance is still open
CREATE INDEX plan_attendance_open ON plan (starts_at) WHERE attendance_closed_at IS NULL;
