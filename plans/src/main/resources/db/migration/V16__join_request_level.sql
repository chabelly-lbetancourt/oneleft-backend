-- HU-070: the organizer also sees the level of who asks in the activity of the plan, as it was when they asked.
ALTER TABLE plan_join_request
    ADD COLUMN level VARCHAR(20);
