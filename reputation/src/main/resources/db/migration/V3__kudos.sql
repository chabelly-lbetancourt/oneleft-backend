-- HU-060 · Kudos between the people of a plan. One per giver, receiver and plan; the points go to the ledger, whose
-- entry has the id of the kudos as its source. The plans and the accounts live in other services: no foreign keys
CREATE TABLE kudos
(
    giver_id    uuid                     NOT NULL,
    receiver_id uuid                     NOT NULL,
    plan_id     uuid                     NOT NULL,
    kind        varchar(20)              NOT NULL
        CHECK (kind IN ('PUNCTUAL', 'GOOD_VIBES', 'GREAT_LEVEL', 'GREAT_ORGANIZER')),
    given_at    timestamp with time zone NOT NULL,
    PRIMARY KEY (giver_id, receiver_id, plan_id)
);

-- The kudos someone received, by kind, in their profile
CREATE INDEX kudos_receiver ON kudos (receiver_id);

-- The people of a plan, to give kudos to the others who arrived
CREATE INDEX track_record_plan ON track_record (plan_id);
