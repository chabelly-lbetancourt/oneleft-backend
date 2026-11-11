-- HU-070: for whom a plan is. ALL (the default, so that every existing plan stays as it was), WOMAN or MAN, and an optional
-- minimum and maximum age. People who are non-binary or prefer not to say fit the plans for ALL only.
ALTER TABLE plan
    ADD COLUMN audience_gender VARCHAR(5) NOT NULL DEFAULT 'ALL',
    ADD COLUMN min_age INTEGER,
    ADD COLUMN max_age INTEGER;
