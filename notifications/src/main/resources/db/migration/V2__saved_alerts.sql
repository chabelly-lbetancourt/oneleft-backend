-- HU-036: saved searches. When a plan like one of them is published, its owner gets a notice (HU-006)
create table saved_alert
(
    id            uuid primary key,
    user_id       uuid             not null,
    name          varchar(40)      not null,
    -- null: any level
    level         varchar(20),
    -- Centre of the search, rounded to 2 decimals (about 1 km)
    latitude      double precision not null,
    longitude     double precision not null,
    radius_meters integer          not null,
    -- Hours of the day of the start; both null for any time
    time_from     time,
    time_to       time,
    created_at    timestamp with time zone not null,
    constraint saved_alert_hours check ((time_from is null) = (time_to is null)
        and (time_from is null or time_from < time_to))
);

create index saved_alert_user on saved_alert (user_id);

-- Activities of the search; none means every activity
create table saved_alert_activity
(
    alert_id uuid        not null references saved_alert (id) on delete cascade,
    activity varchar(20) not null,
    primary key (alert_id, activity)
);

-- Days of the week of the start; none means every day
create table saved_alert_day
(
    alert_id    uuid        not null references saved_alert (id) on delete cascade,
    day_of_week varchar(10) not null,
    primary key (alert_id, day_of_week)
);
