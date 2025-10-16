--
-- Regression Test 1: Look for failure in the current run
--
-- Set the two run keys you want to compare
set gatling_run_id_a = 'TPC-DS JDBC Model Tests | 1 Users | 2025-10-16-09:01:06';

select * from gatling_sql_headers
where gatling_run_id = $gatling_run_id_a
  and status <> 'SUCCEEDED';
