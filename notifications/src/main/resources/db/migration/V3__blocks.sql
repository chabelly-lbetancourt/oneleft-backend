-- HU-033 · Blocks. A copy of the blocks of the social service, kept from its user.blocked and user.unblocked events:
-- nobody hears about a plan of someone they blocked or who blocked them
CREATE TABLE block
(
    blocker_id uuid                     NOT NULL,
    blocked_id uuid                     NOT NULL,
    blocked_at timestamp with time zone NOT NULL,
    PRIMARY KEY (blocker_id, blocked_id)
);

-- The other direction of "between": who blocked me
CREATE INDEX block_blocked ON block (blocked_id, blocker_id);
