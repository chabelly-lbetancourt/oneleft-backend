-- HU-059 · Privacy of the feed. Who sees my activity in their feeds: my followers or nobody. The default of the
-- column only fills the people that already exist; the social service chooses it for new ones (followers)
ALTER TABLE person
    ADD COLUMN feed_audience varchar(10) NOT NULL DEFAULT 'FOLLOWERS'
        CHECK (feed_audience IN ('FOLLOWERS', 'NOBODY'));

-- Plans someone keeps out of their activity. A filter when the feeds are read: showing the plan again brings its
-- activity back. No foreign key to person: anyone in a plan can hide it, even before using the social service
CREATE TABLE hidden_plan
(
    user_id   uuid                     NOT NULL,
    plan_id   uuid                     NOT NULL,
    hidden_at timestamp with time zone NOT NULL,
    PRIMARY KEY (user_id, plan_id)
);
