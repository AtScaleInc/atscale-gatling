-- Regression Test 3: Compare performance within a threshold
--
-- Set the two run_keys you want to compare
set run_key_a = 'INSERT RUN KEY A HERE';
set run_key_b = 'INSERT RUN KEY B HERE';

-- Set your performance threshold as a percent (e.g. 10 = 10%)
set perf_threshold_pct = 10;

with
    a as (
        select
            run_key,
            test_name,
            model as model_name,
            query_name,
            concurrent_users,
            header_duration_ms as duration_ms
        from v_gatling_joined
        where run_key = $run_key_a
    qualify row_number() over (partition by test_name, model, query_name, concurrent_users order by header_ts desc) = 1
    ),
    b as (
select
    run_key,
    test_name,
    model as model_name,
    query_name,
    concurrent_users,
    header_duration_ms as duration_ms
from v_gatling_joined
where run_key = $run_key_b
    qualify row_number() over (partition by test_name, model, query_name, concurrent_users order by header_ts desc) = 1
    ),

    joined as (
select
    a.test_name,
    a.model_name,
    a.query_name,
    a.concurrent_users,
    a.duration_ms as duration_a,
    b.duration_ms as duration_b,
    round(((b.duration_ms - a.duration_ms) / nullif(a.duration_ms, 0)) * 100, 2) as pct_diff
from a
    join b
on a.test_name        = b.test_name
    and a.model_name       = b.model_name
    and a.query_name       = b.query_name
    and a.concurrent_users = b.concurrent_users
    )

select
    test_name,
    model_name,
    query_name,
    concurrent_users,
    duration_a,
    duration_b,
    pct_diff,
    case
        when pct_diff > 0 then 'SLOWER'
        when pct_diff < 0 then 'FASTER'
        else 'SAME'
        end as perf_change
from joined
where abs(pct_diff) >= $perf_threshold_pct
order by abs(pct_diff) desc, test_name, query_name;