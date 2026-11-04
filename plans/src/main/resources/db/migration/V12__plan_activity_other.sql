-- HU-068: the name of the activity of a plan whose activity is «other». The activities themselves are stored as text, so
-- the new codes of the catalog need no change of the schema.
ALTER TABLE plan
    ADD COLUMN activity_other VARCHAR(30);

COMMENT ON COLUMN plan.activity_other IS 'Name of the activity when activity = OTHER; null for any other activity';
