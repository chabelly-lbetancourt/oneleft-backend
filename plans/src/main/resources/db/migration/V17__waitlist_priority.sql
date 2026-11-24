-- HU-063 · Perks of the reputation. Who goes first on a waiting list: the priority that the reputation of each person
-- gave them when they joined it (0 for everyone until now), and the order of arrival among equals
ALTER TABLE plan_waitlist
    ADD COLUMN priority integer NOT NULL DEFAULT 0;
