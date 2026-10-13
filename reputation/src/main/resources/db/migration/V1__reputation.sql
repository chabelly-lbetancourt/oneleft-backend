-- HU-009 · Reputation from events. The plans, the reports and the accounts live in other services: no foreign keys.

-- The ledger: every change of reputation with its reason, points, cause (plan or report) and time. An event delivered
-- twice gives the same entry once (unique person, reason and source)
CREATE TABLE reputation_entry
(
    id          bigserial PRIMARY KEY,
    user_id     uuid                     NOT NULL,
    reason      varchar(30)              NOT NULL,
    points      integer                  NOT NULL,
    source_id   uuid                     NOT NULL,
    plan_title  varchar(80),
    occurred_at timestamp with time zone NOT NULL,
    recorded_at timestamp with time zone NOT NULL DEFAULT now(),
    UNIQUE (user_id, reason, source_id)
);

-- The entries of a person, the latest first
CREATE INDEX reputation_entry_user ON reputation_entry (user_id, occurred_at DESC, id DESC);

-- The ledger is immutable: entries are never changed. They are only deleted on purpose (the right to erasure, or the
-- tests cleaning up after themselves), with the setting oneleft.ledger_erasure turned on in that transaction
CREATE FUNCTION reputation_entry_immutable() RETURNS trigger AS
$$
BEGIN
    IF TG_OP = 'DELETE' AND current_setting('oneleft.ledger_erasure', true) = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'The reputation ledger is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER reputation_entry_immutable
    BEFORE UPDATE OR DELETE
    ON reputation_entry
    FOR EACH ROW
EXECUTE FUNCTION reputation_entry_immutable();

-- The projection of the ledger: the score of each person, the sum of their entries
CREATE TABLE reputation_score
(
    user_id uuid PRIMARY KEY,
    score   integer NOT NULL
);
