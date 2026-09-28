-- HU-005 · Join a plan
CREATE TABLE plan_participant (
    plan_id   UUID        NOT NULL REFERENCES plan (id) ON DELETE CASCADE,
    user_id   UUID        NOT NULL,
    name      VARCHAR(50) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    -- The same person cannot take two spots of the same plan
    PRIMARY KEY (plan_id, user_id)
);

-- Plans a user has joined (for their list of upcoming plans)
CREATE INDEX plan_participant_user ON plan_participant (user_id);
