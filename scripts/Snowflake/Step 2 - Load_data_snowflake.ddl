-- Use these scripts to load data in your staged file into the Gatling
-- results tables

-- If you are using SnowSQL, upload your file like this (run from your terminal, not in the worksheet):
-- snowsql -q "PUT file:///path/to/your/logs.csv @DAVE.TPCDS_RESULTS.GATLING_LOGS_STAGE AUTO_COMPRESS=TRUE OVERWRITE=TRUE"

-- If you use Snowsight UI, you can also right-click the stage and "Upload" the file.

copy into gatling_raw_sql_logs (raw_line, src_filename, src_row_number)
    from (
    select
    $1 as raw_line,
    metadata$filename as src_filename,
    metadata$file_row_number as src_row_number
    from @gatling_logs_stage
    )
    file_format = (format_name = gatling_whole_line_fmt)
    on_error = 'continue';

create or replace table gatling_sql_logs (
  ts                timestamp_ntz,
  level             string,
  logger            string,
  message_kind      string,
  gatling_run_id    string,
  status            string,
  gatling_session_id number,
  model             string,
  query_name        string,
  query_hash        string,
  start_ms          number,
  end_ms            number,
  duration_ms       number,
  rows_returned     number,
  rownumber         number,
  row_map_raw       string,   -- keep the raw Map(...) text
  row_hash          string,
  src_filename      string,
  src_row_number    number,
  raw_line          string
);

insert into gatling_sql_logs
select
    /* ts */
    to_timestamp_ntz(regexp_substr(raw_line, '^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}'))                                       as ts,

    /* level */
    regexp_substr(raw_line, '^[^ ]+ [^ ]+ ([A-Z]+)', 1, 1, 'e', 1)                                                                           as level,

    /* logger (single cleaned value) */
    regexp_replace(regexp_substr(raw_line, ' [A-Za-z0-9_\\.]+:', 1, 1), '[: ]', '')                                                          as logger,

    /* message_kind */
    regexp_substr(raw_line, '- ([A-Za-z0-9_]+)', 1, 1, 'e', 1)                                                                               as message_kind,

    /* key/value pairs */
    regexp_substr(raw_line, 'gatlingRunId=''([^'']*)''', 1, 1, 'e', 1)                                                                       as gatling_run_id,
    regexp_substr(raw_line, 'status=''([^'']*)''', 1, 1, 'e', 1)                                                                       as status,
    try_to_number(regexp_substr(raw_line, 'gatlingSessionId=([0-9]+)', 1, 1, 'e', 1))                                                        as gatling_session_id,
    regexp_substr(raw_line, 'model=''([^'']*)''', 1, 1, 'e', 1)                                                                              as model,
    regexp_substr(raw_line, 'queryName=''([^'']*)''', 1, 1, 'e', 1)                                                                          as query_name,
    regexp_substr(raw_line, 'inboundTextAsMd5Hash=''([^'']*)''', 1, 1, 'e', 1)                                                                          as query_hash,

    try_to_number(regexp_substr(raw_line, 'start=([0-9]+)',    1, 1, 'e', 1))                                                                as start_ms,
    try_to_number(regexp_substr(raw_line, 'end=([0-9]+)',      1, 1, 'e', 1))                                                                as end_ms,
    try_to_number(regexp_substr(raw_line, 'duration=([0-9]+)', 1, 1, 'e', 1))                                                                as duration_ms,
    try_to_number(regexp_substr(raw_line, 'rows=([0-9]+)',     1, 1, 'e', 1))                                                                as rows_returned,

    /* optional fields */
    try_to_number(regexp_substr(raw_line, 'rownumber=([0-9]+)', 1, 1, 'e', 1))                                                               as rownumber,
    regexp_substr(raw_line, 'row=Map\\((.*?)\\)', 1, 1, 'e', 1)                                                                              as row_map_raw,
    regexp_substr(raw_line, 'rowhash=([a-f0-9]+)', 1, 1, 'e', 1)                                                                             as row_hash,

    /* lineage + raw */
    src_filename,
    src_row_number,
    raw_line
from gatling_raw_sql_logs;

-- 1) Header / Detail tables
create or replace table gatling_sql_headers as
select
    /* stable key built from your join columns */
    hash(gatling_run_id, gatling_session_id, model, query_hash) as run_key,

    ts,
    level,
    logger,
    message_kind,
    gatling_run_id,
    status,
    gatling_session_id,
    model,
    query_name,
    query_hash,
    start_ms,
    end_ms,
    duration_ms,
    rows_returned,

    /* lineage + raw */
    src_filename,
    src_row_number,
    raw_line
from gatling_sql_logs
where rownumber is null;

create or replace table gatling_sql_details as
select
    /* same stable key for easy joins */
    hash(gatling_run_id, gatling_session_id, model, query_hash) as run_key,

    ts,
    level,
    logger,
    message_kind,
    gatling_run_id,
    status,
    gatling_session_id,
    model,
    query_name,
    query_hash,

    /* detail-specific fields */
    rownumber,
    row_map_raw,
    row_hash,

    /* optional metrics appear on some detail lines in some log formats;
       keep them in case they show up */
    start_ms,
    end_ms,
    duration_ms,
    rows_returned,

    /* lineage + raw */
    src_filename,
    src_row_number,
    raw_line
from gatling_sql_logs
where rownumber is not null;

-- 2) Helpful clustering (optional)
alter table gatling_sql_headers cluster by (run_key, ts);
alter table gatling_sql_details cluster by (run_key, rownumber);

-- 3) Join view using exactly your specified join keys
create or replace view v_gatling_joined as
select
    h.run_key,
    trim(split_part(h.gatling_run_id, '|', 1)) as test_name,
    try_to_number(regexp_substr(split_part(h.gatling_run_id, '|', 2), '[0-9]+')) as concurrent_users,
    trim(split_part(h.gatling_run_id, '|', 3)) as test_run_time,
    h.ts                     as header_ts,
    h.level                  as header_level,
    h.logger                 as header_logger,
    h.message_kind           as header_message_kind,
    h.gatling_run_id,
    h.status,
    h.gatling_session_id,
    h.model,
    h.query_name,
    h.query_hash,
    h.start_ms               as header_start_ms,
    h.end_ms                 as header_end_ms,
    h.duration_ms            as header_duration_ms,
    h.rows_returned          as header_rows_returned,
    h.src_filename           as header_src_filename,
    h.src_row_number         as header_src_row_number,

    d.ts                     as detail_ts,
    d.rownumber,
    d.row_map_raw,
    d.row_hash,
    d.src_filename           as detail_src_filename,
    d.src_row_number         as detail_src_row_number,
    d.raw_line               as detail_raw_line
from gatling_sql_headers h
         join gatling_sql_details d
              on h.gatling_run_id     = d.gatling_run_id
                  and h.gatling_session_id = d.gatling_session_id
                  and h.model              = d.model
                  and h.query_hash         = d.query_hash;