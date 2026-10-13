-- HU-044 · Badges for milestones.

-- The track record: each person in each plan whose check-in closed, with its activity, whether they organized it,
-- whether they arrived and whether the plan was held (someone of the group checked in). Once per person and plan
CREATE TABLE track_record
(
    user_id   uuid                     NOT NULL,
    plan_id   uuid                     NOT NULL,
    activity  varchar(30)              NOT NULL,
    organizer boolean                  NOT NULL,
    arrived   boolean                  NOT NULL,
    held      boolean                  NOT NULL,
    closed_at timestamp with time zone NOT NULL,
    PRIMARY KEY (user_id, plan_id)
);

-- The plans of a person, the latest first (for the streak)
CREATE INDEX track_record_user ON track_record (user_id, closed_at DESC);

-- The badges earned: once each, for good
CREATE TABLE badge
(
    user_id   uuid                     NOT NULL,
    badge     varchar(30)              NOT NULL,
    earned_at timestamp with time zone NOT NULL,
    PRIMARY KEY (user_id, badge)
);
