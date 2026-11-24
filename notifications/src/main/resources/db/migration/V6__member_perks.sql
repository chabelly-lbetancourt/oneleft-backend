-- HU-063 · Perks of the reputation. A copy of what the level of each person is worth here (the weight of their
-- notices and the alerts they may keep), kept from the reputation.perks-changed events of the reputation service:
-- people without a row have the perks of a new person
CREATE TABLE member_perks
(
    user_id          uuid PRIMARY KEY,
    level            varchar(30)              NOT NULL,
    notice_weight    double precision         NOT NULL CHECK (notice_weight > 0),
    max_saved_alerts integer                  NOT NULL CHECK (max_saved_alerts >= 0),
    -- When the reputation service said it: an older event never replaces a newer one
    occurred_at      timestamp with time zone NOT NULL
);
