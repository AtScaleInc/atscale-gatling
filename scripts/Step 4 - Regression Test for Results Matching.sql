-- Regression Test 2: Compare results for exact matching
--
-- Set the two run keys you want to compare
set run_key_a = 'INSERT RUN KEY A HERE';
set run_key_b = 'INSERT RUN KEY B HERE';

with
    a as (
        select
            test_name,
            model as model_name,
            query_name,
            concurrent_users,
            rownumber,
            header_rows_returned as rows_returned,
            row_hash
        from v_gatling_joined
        where run_key = $run_key_a
    ),
    b as (
        select
            test_name,
            model as model_name,
            query_name,
            concurrent_users,
            rownumber,
            header_rows_returned as rows_returned,
            row_hash
        from v_gatling_joined
        where run_key = $run_key_b
    )
select
    a.test_name,
    a.model_name,
    a.query_name,
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