-- HU-046: what makes the place of a plan accessible, as its organizer declares it (WHEELCHAIR_ACCESSIBLE,
-- STEP_FREE_ACCESS, ACCESSIBLE_TOILET or ACCESSIBLE_PARKING). One row per feature: the plans from before have none, as
-- a place nobody said anything about. It is about the place, never about the people who come.
CREATE TABLE plan_accessibility (
    plan_id UUID        NOT NULL REFERENCES plan (id) ON DELETE CASCADE,
    feature VARCHAR(30) NOT NULL,
    -- Also the index of the nearby search, which looks for the features of each plan
    PRIMARY KEY (plan_id, feature)
);
