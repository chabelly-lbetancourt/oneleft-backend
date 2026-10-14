-- HU-020 · Profile photo. The photo is in the object storage (S3 or compatible): here only where it is.
-- No foreign key to profile: a person may upload a photo before their profile is created
CREATE TABLE profile_photo (
    user_id    UUID PRIMARY KEY,
    object_key VARCHAR(120)             NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
