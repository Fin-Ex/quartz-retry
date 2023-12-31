create table if not exists qrtz_retry_cron_triggers
(
  sched_name varchar(120) not null,
  trigger_name varchar(200) not null,
  trigger_group varchar(200) not null,
  cron_expression varchar(120) not null,
  time_zone_id varchar(80),
  retries_count int,
  max_attempts int,
  primary key (sched_name, trigger_name, trigger_group),
  foreign key (sched_name, trigger_name, trigger_group)
  references qrtz_triggers (sched_name, trigger_name, trigger_group)
);
