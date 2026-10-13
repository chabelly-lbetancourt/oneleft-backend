-- Messages of the chat of each plan (HU-008). Only the people of the plan read and write them; they are deleted a
-- while after the plan ends, so the end of the plan travels with each message.
CREATE TABLE chat_message
(
    id           UUID PRIMARY KEY,
    plan_id      UUID         NOT NULL,
    author_id    UUID         NOT NULL,
    author_name  VARCHAR(100) NOT NULL,
    text         VARCHAR(500) NOT NULL,
    sent_at      TIMESTAMPTZ  NOT NULL,
    plan_ends_at TIMESTAMPTZ  NOT NULL
);

-- The history of a plan, newest first, page by page
CREATE INDEX chat_message_plan_sent ON chat_message (plan_id, sent_at DESC);
-- The hourly purge of the plans that ended
CREATE INDEX chat_message_plan_ends ON chat_message (plan_ends_at);
