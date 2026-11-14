-- HU-070 · Audience of the plans. A copy of the gender and the age (never the date of birth) of the people, kept from
-- the demographics.changed events of the users service: the notices of nearby plans leave out who the plan is not for
CREATE TABLE member_audience
(
    user_id    uuid PRIMARY KEY,
    gender     varchar(20)              NOT NULL,
    age        integer                  NOT NULL,
    updated_at timestamp with time zone NOT NULL
);
