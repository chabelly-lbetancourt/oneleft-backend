-- HU-047 · Following people. The plans and the accounts live in other services: no foreign keys to them.

-- Everyone who uses the social service or is followed: the name of their account (a copy, refreshed when they use the
-- service) and who may follow them
CREATE TABLE person
(
    user_id    uuid PRIMARY KEY,
    name       varchar(50) NOT NULL,
    visibility varchar(10) NOT NULL CHECK (visibility IN ('PUBLIC', 'PRIVATE'))
);

-- Who follows whom. A request to a private profile is PENDING until its owner accepts it
CREATE TABLE follow
(
    follower_id  uuid                     NOT NULL REFERENCES person (user_id) ON DELETE CASCADE,
    followee_id  uuid                     NOT NULL REFERENCES person (user_id) ON DELETE CASCADE,
    status       varchar(10)              NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED')),
    requested_at timestamp with time zone NOT NULL,
    accepted_at  timestamp with time zone,
    PRIMARY KEY (follower_id, followee_id),
    CHECK (follower_id <> followee_id),
    CHECK ((status = 'ACCEPTED') = (accepted_at IS NOT NULL))
);

-- My followers and the requests waiting for me (the primary key already serves the people I follow)
CREATE INDEX follow_followee ON follow (followee_id, status);
