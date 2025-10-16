-- Run these scripts once to create all the Snowflake assets to load the Gatling test results
-- from the run_logs directory
create stage if not exists gatling_logs_stage file_format = (type = csv field_delimiter = '\t');

create or replace file format gatling_whole_line_fmt
  type = 'CSV'
  field_delimiter = '\t'
  skip_header = 0
  trim_space = false
  field_optionally_enclosed_by = none
  empty_field_as_null = false
  null_if = ();

create or replace table gatling_raw_sql_logs (
  raw_line string,
  src_filename string,
  src_row_number number
);