-- HU-033 · Blocking and reporting people.

-- Who blocked whom. It is private: nothing tells the blocked person. The plans and notifications services keep their
-- own copy from the user.blocked and user.unblocked events
CREATE TABLE block
(
    blocker_id uuid                     NOT NULL REFERENCES person (user_id) ON DELETE CASCADE,
    blocked_id uuid                     NOT NULL REFERENCES person (user_id) ON DELETE CASCADE,
    blocked_at timestamp with time zone NOT NULL,
    PRIMARY KEY (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);

-- Who blocked me (the primary key already serves the people I blocked)
CREATE INDEX block_blocked ON block (blocked_id);

-- Reports of a person or a plan, for the moderation (HU-011) and the administration panel (HU-013). The plans live in
-- another service: no foreign keys to them. reported_user_id is the person, or the organizer of the plan
CREATE TABLE report
(
    id               uuid PRIMARY KEY,
    reporter_id      uuid                     NOT NULL REFERENCES person (user_id) ON DELETE CASCADE,
    target_type      varchar(10)              NOT NULL CHECK (target_type IN ('PERSON', 'PLAN')),
    target_id        uuid                     NOT NULL,
    reported_user_id uuid                     NOT NULL,
    reason           varchar(20)              NOT NULL
        CHECK (reason IN ('SPAM', 'HARASSMENT', 'INAPPROPRIATE', 'FAKE', 'DANGEROUS', 'OTHER')),
    comment          varchar(500),
    status           varchar(10)              NOT NULL CHECK (status IN ('OPEN')),
    reported_at      timestamp with time zone NOT NULL,
    CHECK (reporter_id <> reported_user_id),
    CHECK (reason <> 'OTHER' OR comment IS NOT NULL)
);

-- One open report of each person about the same person or plan
CREATE UNIQUE INDEX report_open_once ON report (reporter_id, target_type, target_id) WHERE status = 'OPEN';

-- The open reports, the oldest first, for the moderation
CREATE INDEX report_open ON report (reported_at) WHERE status = 'OPEN';
