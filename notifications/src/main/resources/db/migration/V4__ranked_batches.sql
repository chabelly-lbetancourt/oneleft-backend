-- HU-012: notices ranked by how likely each person is to take a spot, sent in batches, and what each notice led to.
alter table sent_notice
    add column accepted_at timestamp with time zone,
    -- How the person was chosen: model, distance, fallback (the model was down), alert, or legacy (before HU-012)
    add column ranking varchar(10) not null default 'legacy',
    add column batch   integer     not null default 0;

-- Plans with people still waiting to be notified, and when the next batch is due
create table notice_batch_plan
(
    plan_id       uuid primary key,
    organizer_id  uuid                     not null,
    activity      varchar(20)              not null,
    title         varchar(80)              not null,
    place_name    varchar(100)             not null,
    latitude      double precision         not null,
    longitude     double precision         not null,
    starts_at     timestamp with time zone not null,
    free_spots    integer                  not null,
    level         varchar(20),
    ranking       varchar(10)              not null,
    next_batch    integer                  not null,
    next_batch_at timestamp with time zone not null
);

create index notice_batch_plan_due on notice_batch_plan (next_batch_at);

-- The people of each plan still to be notified, best first
create table pending_notice
(
    plan_id  uuid             not null references notice_batch_plan (plan_id) on delete cascade,
    user_id  uuid             not null,
    position integer          not null,
    score    double precision not null,
    primary key (plan_id, user_id)
);

create index pending_notice_order on pending_notice (plan_id, position);
create index pending_notice_user on pending_notice (user_id);
