-- HU-066 · Anti-cheating of reputation. An entry that the rules cut is still kept, with the points it really gave and
-- why: the daily cap of its reason, kudos from the same person lately, or a pair that keeps rating each other
ALTER TABLE reputation_entry
    ADD COLUMN limited_by varchar(20)
        CHECK (limited_by IN ('DAILY_CAP', 'SAME_PERSON', 'MUTUAL_PAIR'));

-- The entries of a person and reason in one day, for the daily caps
CREATE INDEX reputation_entry_user_reason ON reputation_entry (user_id, reason, occurred_at);
