-- HU-040 · «On my way» and «running late (N min)» of the group of a plan, until it starts
CREATE TABLE plan_arrival
(
    plan_id      uuid                     NOT NULL REFERENCES plan (id) ON DELETE CASCADE,
    user_id      uuid                     NOT NULL,
    name         varchar(50)              NOT NULL,
    status       varchar(20)              NOT NULL,
    minutes_late integer,
    announced_at timestamp with time zone NOT NULL,
    PRIMARY KEY (plan_id, user_id),
    CONSTRAINT plan_arrival_late CHECK ((status = 'LATE') = (minutes_late IS NOT NULL))
);
