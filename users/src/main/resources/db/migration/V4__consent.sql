-- HU-045 · The privacy text each person accepted: its version and when. One per person; erasing the account removes it.
CREATE TABLE consent
(
    user_id     uuid PRIMARY KEY,
    version     varchar(20)              NOT NULL,
    accepted_at timestamp with time zone NOT NULL
);
