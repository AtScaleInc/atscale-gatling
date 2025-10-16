-- Regression Test 2: Compare results for exact matching
--
-- Set the two run keys you want to compare
set gatling_run_id_a = 'TPC-DS JDBC Model Tests | 1 Users | 2025-10-16-09:01:06';
set gatling_run_id_b = 'TPC-DS JDBC Model Tests | 1 Users | 2025-10-16-09:01:06';

with
    a as (
        select
            test_name,
            model as model_name,
            query_name,
            query_hash,
            status,
            concurrent_users,
            rownumber,
            header_rows_returned as rows_returned,
            row_hash
        from v_gatling_joined
        where gatling_run_id = $gatling_run_id_a
    ),
    b as (
        select
            test_name,
            model as model_name,
            query_name,
            query_hash,
            status,
            concurrent_users,
            rownumber,
            header_rows_returned as rows_returned,
            row_hash
        from v_gatling_joined
        where gatling_run_id = $gatling_run_id_b
    )
select
    a.test_name,
    a.model_name,
    a.query_name,
    a.query_hash,
    a.status,
    a.concurrent_users,
    a.rownumber,
    a.rows_returned as rows_returned_a,
    b.rows_returned as rows_returned_b,
    a.row_hash     as row_hash_a,
    b.row_hash     as row_hash_b
from a
         join b
              on a.test_name        = b.test_name
                  and a.model_name       = b.model_name
                  and a.query_name       = b.query_name
                  and a.concurrent_users = b.concurrent_users
                  and a.rownumber        = b.rownumber
where a.rows_returned <> b.rows_returned
   or a.row_hash <> b.row_hash
order by a.query_name, a.rownumber;