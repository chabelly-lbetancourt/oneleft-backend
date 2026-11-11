-- HU-069: plans whose organizer chooses who comes. Nobody joins directly: people ask for a spot and the organizer decides.
ALTER TABLE plan
    ADD COLUMN requires_approval BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE plan_join_request (
    plan_id      UUID                     NOT NULL REFERENCES plan (id) ON DELETE CASCADE,
    user_id      UUID                     NOT NULL,
    name         VARCHAR(50)              NOT NULL,
    -- A short optional message: no contact data
    message      VARCHAR(140),
    -- PENDING, ACCEPTED, REJECTED or WITHDRAWN; a pending request expires on its own when the plan starts
    status       VARCHAR(10)              NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_at   TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (plan_id, user_id)
);
