-- HU-051 · Activity feed. What each person did in the plans, from the events of the plans service: published a plan,
-- joined one or completed it (arrived at it). Once per person, kind and plan: a repeated event changes nothing.
-- No foreign key to person: the activity of someone nobody follows yet is kept for whoever follows them later
CREATE TABLE activity
(
    id            bigserial PRIMARY KEY,
    actor_id      uuid                     NOT NULL,
    type          varchar(10)              NOT NULL CHECK (type IN ('PUBLISHED', 'JOINED', 'COMPLETED')),
    plan_id       uuid                     NOT NULL,
    plan_title    varchar(80)              NOT NULL,
    plan_activity varchar(30)              NOT NULL,
    starts_at     timestamp with time zone NOT NULL,
    occurred_at   timestamp with time zone NOT NULL,
    UNIQUE (actor_id, type, plan_id)
);

-- The feed is read by the people followed, the latest first (fan-out on read)
CREATE INDEX activity_actor ON activity (actor_id, occurred_at DESC);
-- A cancelled plan takes its activity away
CREATE INDEX activity_plan ON activity (plan_id);
