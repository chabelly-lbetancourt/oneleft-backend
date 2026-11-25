-- HU-061 · Endorsements of the level of someone in an activity. One per endorser, receiver and activity, with the
-- level that the endorser vouches for; the points go to the ledger, whose entry has the id of the endorsement as its
-- source. Nobody endorses themselves. The accounts live in another service: no foreign keys
CREATE TABLE endorsement
(
    endorser_id uuid                     NOT NULL,
    receiver_id uuid                     NOT NULL,
    activity    varchar(30)              NOT NULL,
    level       varchar(20)              NOT NULL
        CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    given_at    timestamp with time zone NOT NULL,
    PRIMARY KEY (endorser_id, receiver_id, activity),
    CHECK (endorser_id <> receiver_id)
);

-- The endorsements someone received, by activity and level, in their profile
CREATE INDEX endorsement_receiver ON endorsement (receiver_id);
