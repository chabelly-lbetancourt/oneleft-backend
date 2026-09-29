-- HU-023 · Leave a plan and waiting list
CREATE TABLE plan_waitlist (
    plan_id   UUID        NOT NULL REFERENCES plan (id) ON DELETE CASCADE,
    user_id   UUID        NOT NULL,
    name      VARCHAR(50) NOT NULL,
    -- Order of arrival: the first one takes the next freed spot
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (plan_id, user_id)
);
