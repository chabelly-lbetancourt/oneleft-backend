-- HU-033 · Blocks. A copy of the blocks of the social service, kept from its user.blocked and user.unblocked events:
-- neither person sees the plans of the other one, nor joins them, nor appears among the free people of the other one
CREATE TABLE block
(
    blocker_id uuid                     NOT NULL,
    blocked_id uuid                     NOT NULL,
    blocked_at timestamp with time zone NOT NULL,
    PRIMARY KEY (blocker_id, blocked_id)
);

-- The other direction of "between": who blocked me
CREATE INDEX block_blocked ON block (blocked_id, blocker_id);
