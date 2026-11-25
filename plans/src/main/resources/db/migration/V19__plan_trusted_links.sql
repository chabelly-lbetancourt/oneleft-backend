-- HU-034 · Links a person of a plan gives to someone they trust, to tell them where, when and with whom they meet.
-- Only the SHA-256 of the token is kept: whoever reads this table cannot open any link. A link goes when it is
-- revoked (its row is deleted), with the plan, and with the account of whoever created it (HU-045).
CREATE TABLE plan_trusted_link
(
    id         uuid                     NOT NULL PRIMARY KEY,
    plan_id    uuid                     NOT NULL REFERENCES plan (id) ON DELETE CASCADE,
    user_id    uuid                     NOT NULL,
    token_hash varchar(64)              NOT NULL UNIQUE,
    created_at timestamp with time zone NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    CHECK (expires_at > created_at)
);

-- The links of a person for a plan (their list, their limit) and for their data and the deletion of their account
CREATE INDEX plan_trusted_link_owner ON plan_trusted_link (user_id, plan_id);
-- The expired ones, to remove them
CREATE INDEX plan_trusted_link_expiry ON plan_trusted_link (expires_at);
