package executors;

import com.atscale.java.dao.AtScalePostgresDao;
import com.atscale.java.utils.PropertiesManager;
import com.atscale.java.utils.QueryHistoryFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class InstallerVerQueryExtractExecutor {
    // Disable Log4j's automatic shutdown hook early (must run before any LoggerContext initializes)
    static {
        System.setProperty("log4j.shutdownHookEnabled", "false");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallerVerQueryExtractExecutor.class);

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    try { Class<?> ctxClass = Class.forName("org.apache.logging.log4j.core.LoggerContext");
                        Object ctx = org.apache.logging.log4j.LogManager.getContext(false);
                        if (ctxClass.isInstance(ctx)) {
                            ((org.apache.logging.log4j.core.LoggerContext) ctx).stop();
                        } else {
                            org.apache.logging.log4j.LogManager.shutdown(); }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // log4j-core not present in this classloader — nothing to do
                    } catch (Throwable t) {
                        // swallow to avoid throwing during shutdown
                    } }
                )
        );

        InstallerVerQueryExtractExecutor executor = new InstallerVerQueryExtractExecutor();
        executor.initAdditionalProperties();
        executor.execute();
    }

    protected void execute() {
        LOGGER.info("QueryExtractExecutor started.");
        List<String> models = PropertiesManager.getAtScaleModels();

        for(String model : models) {
            LOGGER.info("Processing model: {}", model);
            cacheJdbcQueries(model);
            cacheXmlaQueries(model);
        }

        LOGGER.info("QueryExtractExecutor finished.");
        // Do NOT shut down Log4j here; the JVM shutdown hook will perform shutdown when the process exits.
    }

    private void cacheJdbcQueries(String model) {
        // Implement the logic to cache JDBC queries
        // This method should be overridden in subclasses if needed
        LOGGER.info("Caching JDBC queries...");

        final String query = """
             SELECT
                q.service,
                q.query_language,
                q.query_text as inbound_text,
                MAX(q.query_id::text) as atscale_query_id,
                MAX(s.subquery_text) as outbound_text,
                p.cube_name,
                p.project_id,
                case when MAX(s.subquery_text) like '%as_agg_%' then true else false end as used_agg,
                COUNT(*) as num_times,
                extract(EPOCH from AVG(r.finished - p.planning_started)) AS elapsed_time_in_seconds,
                AVG(r.result_size) as avg_result_size
            FROM
                atscale.queries q
            INNER JOIN
                atscale.query_results r
            ON
                q.query_id=r.query_id
            INNER JOIN
                atscale.queries_planned p
            ON
                q.query_id=p.query_id
            INNER JOIN
                atscale.subqueries s
            ON
                q.query_id=s.query_id
            WHERE
                q.query_language = ?
                AND p.planning_started > current_timestamp - interval '60' DAY
                AND p.cube_name = ?
                AND q.service = 'user-query'
                AND r.succeeded = true
                AND LENGTH(q.query_text) > 100
                AND q.query_text NOT LIKE '/* Virtual query to get the members of a level */%'
                AND q.query_text NOT LIKE '-- statement does not return rows%'
            GROUP BY
                1,
                2,
                3,
                6,
                7
            HAVING COUNT(*) >= 1
            ORDER BY 3
        """;

        AtScalePostgresDao dao = AtScalePostgresDao.getInstance();
        QueryHistoryFileUtil queryHistoryFileUtil = new QueryHistoryFileUtil(dao);
        queryHistoryFileUtil.cacheJdbcQueries(model, query, AtScalePostgresDao.QueryLanguage.INSTALLER_SQL.getValue(), model);
    }

    private void cacheXmlaQueries(String model) {
        // Implement the logic to cache XMLA queries
        // This method should be overridden in subclasses if needed
        LOGGER.info("Caching XMLA queries...");

        final String query = """
                SELECT
                q.service,
                q.query_language,
                q.query_text as inbound_text,
                MAX(q.query_id::text) as atscale_query_id,
                MAX(s.subquery_text) as outbound_text,
                p.cube_name,
                p.project_id,
                case when MAX(s.subquery_text) like '%as_agg_%' then true else false end as used_agg,
                COUNT(*) as num_times,
                extract(EPOCH from AVG(r.finished - p.planning_started)) AS elapsed_time_in_seconds,
                AVG(r.result_size) as avg_result_size
            FROM
                atscale.queries q
            INNER JOIN
                atscale.query_results r
            ON
                q.query_id=r.query_id
            INNER JOIN
                atscale.queries_planned p
            ON
                q.query_id=p.query_id
            INNER JOIN
                atscale.subqueries s
            ON
                q.query_id=s.query_id
            WHERE
                q.query_language = ?
                AND p.planning_started > current_timestamp - interval '60' DAY
                AND p.cube_name = ?
                AND q.service = 'user-query'
                AND r.succeeded = true
                AND LENGTH(q.query_text) > 100
                AND q.query_text NOT LIKE '/* Virtual query to get the members of a level */%'
                AND q.query_text NOT LIKE '-- statement does not return rows%'
            GROUP BY
                1,
                2,
                3,
                6,
                7
            HAVING COUNT(*) >= 1
            ORDER BY 3
        """;


        AtScalePostgresDao dao = AtScalePostgresDao.getInstance();
        QueryHistoryFileUtil queryHistoryFileUtil = new QueryHistoryFileUtil(dao);
        queryHistoryFileUtil.cacheXmlaQueries(model, query, AtScalePostgresDao.QueryLanguage.XMLA.getValue(), model);
    }

    protected void initAdditionalProperties() {
        AdditionalPropertiesLoader loader = new AdditionalPropertiesLoader();
        PropertiesManager.setCustomProperties(loader.fetchAdditionalProperties(AdditionalPropertiesLoader.SecretsManagerType.AWS));
    }
}
