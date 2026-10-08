-- HU-035 · «I'm free now»: who is available for a plan, around an approximate zone, until when
CREATE TABLE availability
(
    user_id    uuid PRIMARY KEY,
    -- Zone rounded to 2 decimals (about 1 km), never the exact position
    latitude   double precision         NOT NULL,
    longitude  double precision         NOT NULL,
    location   geometry(Point, 4326)    NOT NULL,
    until      timestamp with time zone NOT NULL,
    created_at timestamp with time zone NOT NULL
);

-- Organizers look for free people around their plan (ST_DWithin on geography), and the expired ones are purged
CREATE INDEX availability_location_geography ON availability USING GIST ((location::geography));
CREATE INDEX availability_until ON availability (until);

-- Activities they would do, with their level in each (null if they did not say); none means any activity
CREATE TABLE availability_interest
(
    user_id  uuid        NOT NULL REFERENCES availability (user_id) ON DELETE CASCADE,
    activity varchar(20) NOT NULL,
    level    varchar(20),
    PRIMARY KEY (user_id, activity)
);
