-- HU-052 · Memories of a plan: the photos its group shares once it is over. The photo itself is in the object storage;
-- here, whose it is and where. They go with the plan, and with the account of whoever uploaded them (HU-045).
CREATE TABLE plan_memory
(
    id          uuid                     NOT NULL PRIMARY KEY,
    plan_id     uuid                     NOT NULL REFERENCES plan (id) ON DELETE CASCADE,
    user_id     uuid                     NOT NULL,
    object_key  varchar(160)             NOT NULL UNIQUE,
    size_bytes  bigint                   NOT NULL CHECK (size_bytes > 0),
    uploaded_at timestamp with time zone NOT NULL
);

-- The album of a plan, newest first, and how many photos each person has in it
CREATE INDEX plan_memory_album ON plan_memory (plan_id, uploaded_at DESC, id DESC);
-- The photos of a person, for their data and the deletion of their account
CREATE INDEX plan_memory_user ON plan_memory (user_id);
