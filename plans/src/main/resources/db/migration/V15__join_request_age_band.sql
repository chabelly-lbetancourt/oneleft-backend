-- HU-070: the organizer sees the age band of who asks for a spot (never the date of birth), as it was when they asked.
ALTER TABLE plan_join_request
    ADD COLUMN age_band VARCHAR(10);
