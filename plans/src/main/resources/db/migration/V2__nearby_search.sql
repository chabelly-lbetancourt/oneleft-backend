-- HU-004 · See nearby plans
-- The search filters by distance in metres with ST_DWithin on geography (the Earth's sphere). A GiST index on the
-- geometry column does not serve that expression, so it is replaced by an expression index.
DROP INDEX plan_location;
CREATE INDEX plan_location_geography ON plan USING GIST ((location::geography));
