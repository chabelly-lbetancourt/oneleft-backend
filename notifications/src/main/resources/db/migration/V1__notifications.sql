-- HU-006: notices of nearby plans. Each person chooses what to hear about; nothing is sent until they turn it on.
create table notification_preferences
(
    user_id       uuid primary key,
    enabled       boolean          not null,
    -- Approximate zone (rounded to 2 decimals, about 1 km), never an exact address
    latitude      double precision,
    longitude     double precision,
    radius_meters integer          not null,
    quiet_start   time,
    quiet_end     time,
    max_per_day   integer          not null,
    updated_at    timestamp with time zone not null
);

create index notification_preferences_enabled on notification_preferences (enabled) where enabled;

-- Activities of interest; none means every activity
create table notification_preference_activity
(
    user_id  uuid        not null references notification_preferences (user_id) on delete cascade,
    activity varchar(20) not null,
    primary key (user_id, activity)
);

-- Browsers subscribed to Web Push
create table push_subscription
(
    endpoint   varchar(1024) primary key,
    user_id    uuid          not null,
    p256dh     varchar(120)  not null,
    auth       varchar(40)   not null,
    language   varchar(2)    not null,
    created_at timestamp with time zone not null
);

create index push_subscription_user on push_subscription (user_id);

-- Notices already sent: once per person and plan, and the count for the daily limit
create table sent_notice
(
    user_id uuid                     not null,
    plan_id uuid                     not null,
    sent_at timestamp with time zone not null,
    primary key (user_id, plan_id)
);

create index sent_notice_user_time on sent_notice (user_id, sent_at);
