-- HU-070: date of birth (18+) and gender, so that a plan can say for whom it is. The exact date never leaves this service:
-- other people only get the age band, and the plans service only gets gender and age.
CREATE TABLE demographics (
    user_id    UUID        PRIMARY KEY,
    birth_date DATE        NOT NULL,
    gender     VARCHAR(20) NOT NULL,
    -- How many times the date of birth was changed after it was first set: limited, to avoid abuse
    changes    INTEGER     NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
