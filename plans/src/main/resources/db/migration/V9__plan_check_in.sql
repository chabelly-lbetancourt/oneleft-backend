-- HU-025 · Check-in at the meeting point: the attendance of each person to each plan, for the reputation (HU-009).
-- Where the device was is not kept: only who and when.
CREATE TABLE plan_check_in
(
    plan_id       uuid                     NOT NULL REFERENCES plan (id) ON DELETE CASCADE,
    user_id       uuid                     NOT NULL,
    name          varchar(50)              NOT NULL,
    checked_in_at timestamp with time zone NOT NULL,
    PRIMARY KEY (plan_id, user_id)
);

-- The attendance of a person, for the reputation
CREATE INDEX plan_check_in_user ON plan_check_in (user_id);
