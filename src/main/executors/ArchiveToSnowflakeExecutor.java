package executors;

import com.atscale.java.utils.PropertiesManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Properties;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Path;
import java.sql.*;

public class ArchiveToSnowflakeExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveToSnowflakeExecutor.class);
    private static final String STAGE = "GATLING_LOGS_STAGE";
    private static final String RAW_TABLE = "GATLING_RAW_SQL_LOGS";



    public static void main(String[] args) {
        LOGGER.info("ArchiveToSnowflakeExecutor started.");
        try {
            Map<String, String> arguments = parseArgs(args);
            Path dataFile = Path.of(arguments.get("data_file"));

            ArchiveToSnowflakeExecutor executor = new ArchiveToSnowflakeExecutor();
            executor.execute(dataFile);
        } catch (Exception e) {
            LOGGER.error("Error during ArchiveToSnowflakeExecutor execution", e);
            throw new RuntimeException("ArchiveToSnowflakeExecutor failed", e);
        }
        LOGGER.info("ArchiveToSnowflakeExecutor completed.");
    }

    protected void execute(Path dataFile) {
        initAdditionalProperties();
        String jdbcUrl = getSnowflakeURL();
        Properties connectionProps = getConnectionProperties();

        LOGGER.info("Connecting to Snowflake with URL: {}", jdbcUrl);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, connectionProps)) {
            LOGGER.info("Connected to Snowflake successfully.");

            // 1) Ensure required objects exist
            createIfNotExistsObjects(conn);

            // 2) Upload local file to stage
            String fileUri = dataFile.toUri().toString();
            exec(conn, "PUT '" + fileUri.replace("'", "''") + "' @" + STAGE + " AUTO_COMPRESS=TRUE OVERWRITE=TRUE");
            LOGGER.info("Uploaded file {} to stage {}", fileUri, STAGE);

            // 3) Copy staged file into GATLING_RAW_SQL_LOGS
            exec(conn, """
                COPY INTO GATLING_RAW_SQL_LOGS (RAW_LINE, SRC_FILENAME, SRC_ROW_NUMBER)
                FROM (
                  SELECT
                    $1 AS RAW_LINE,
                    METADATA$FILENAME AS SRC_FILENAME,
                    METADATA$FILE_ROW_NUMBER AS SRC_ROW_NUMBER
                  FROM @GATLING_LOGS_STAGE
                )
                FILE_FORMAT = (FORMAT_NAME = GATLING_WHOLE_LINE_FMT)
                ON_ERROR = 'CONTINUE';
                """);
            LOGGER.info("Copied data from stage {} into table {}", STAGE, RAW_TABLE);

            // 4) Insert parsed rows into GATLING_SQL_LOGS
            exec(conn, getInsertIntoSqlLogsSql());
            LOGGER.info("Inserted parsed rows into GATLING_SQL_LOGS from {}", "GATLING_RAW_SQL_LOGS");

            // 5) Insert header rows (ROWNUMBER is NULL) into GATLING_SQL_HEADERS
            exec(conn, getInsertIntoHeadersSql());
            LOGGER.info("Inserted header rows into GATLING_SQL_HEADERS from gatling_sql_logs");

            // 6) Insert detail rows (ROWNUMBER is NOT NULL) into GATLING_SQL_DETAILS
            exec(conn, getInsertIntoDetailsSql());
            LOGGER.info("Inserted detail rows into GATLING_SQL_DETAILS from gatling_sql_logs");

            LOGGER.info("✅ Load complete: RAW -> SQL_LOGS -> (HEADERS, DETAILS).");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute Snowflake operations", e);
        }
    }

    /** Create all tables, view, stage, and file format if not already present. */
    private static void createIfNotExistsObjects(Connection conn) throws SQLException {
        System.out.println("Ensuring all required Snowflake objects exist...");

        exec(conn, """
            CREATE STAGE IF NOT EXISTS GATLING_LOGS_STAGE
              FILE_FORMAT = (TYPE = CSV FIELD_DELIMITER = '\\t');
            """);

        exec(conn, """
            CREATE FILE FORMAT IF NOT EXISTS GATLING_WHOLE_LINE_FMT
              TYPE = 'CSV'
              FIELD_DELIMITER = '\\t'
              SKIP_HEADER = 0
              TRIM_SPACE = FALSE
              FIELD_OPTIONALLY_ENCLOSED_BY = NONE
              EMPTY_FIELD_AS_NULL = FALSE
              NULL_IF = ();
            """);

        exec(conn, """
            CREATE TABLE IF NOT EXISTS GATLING_RAW_SQL_LOGS (
              RAW_LINE VARCHAR(16777216),
              SRC_FILENAME VARCHAR(16777216),
              SRC_ROW_NUMBER NUMBER(38,0)
            );
            """);

        exec(conn, """
            CREATE TABLE IF NOT EXISTS GATLING_SQL_LOGS (
              TS TIMESTAMP_NTZ(9),
              LEVEL VARCHAR(16777216),
              LOGGER VARCHAR(16777216),
              MESSAGE_KIND VARCHAR(16777216),
              GATLING_RUN_ID VARCHAR(16777216),
              STATUS VARCHAR(16777216),
              GATLING_SESSION_ID NUMBER(38,0),
              MODEL VARCHAR(16777216),
              QUERY_NAME VARCHAR(16777216),
              QUERY_HASH VARCHAR(16777216),
              START_MS NUMBER(38,0),
              END_MS NUMBER(38,0),
              DURATION_MS NUMBER(38,0),
              ROWS_RETURNED NUMBER(38,0),
              ROWNUMBER NUMBER(38,0),
              ROW_MAP_RAW VARCHAR(16777216),
              ROW_HASH VARCHAR(16777216),
              SRC_FILENAME VARCHAR(16777216),
              SRC_ROW_NUMBER NUMBER(38,0),
              RAW_LINE VARCHAR(16777216)
            );
            """);

        exec(conn, """
            CREATE TABLE IF NOT EXISTS GATLING_SQL_DETAILS CLUSTER BY (RUN_KEY, ROWNUMBER) (
              RUN_KEY NUMBER(19,0),
              TS TIMESTAMP_NTZ(9),
              LEVEL VARCHAR(16777216),
              LOGGER VARCHAR(16777216),
              MESSAGE_KIND VARCHAR(16777216),
              GATLING_RUN_ID VARCHAR(16777216),
              STATUS VARCHAR(16777216),
              GATLING_SESSION_ID NUMBER(38,0),
              MODEL VARCHAR(16777216),
              QUERY_NAME VARCHAR(16777216),
              QUERY_HASH VARCHAR(16777216),
              ROWNUMBER NUMBER(38,0),
              ROW_MAP_RAW VARCHAR(16777216),
              ROW_HASH VARCHAR(16777216),
              START_MS NUMBER(38,0),
              END_MS NUMBER(38,0),
              DURATION_MS NUMBER(38,0),
              ROWS_RETURNED NUMBER(38,0),
              SRC_FILENAME VARCHAR(16777216),
              SRC_ROW_NUMBER NUMBER(38,0),
              RAW_LINE VARCHAR(16777216)
            );
            """);

        exec(conn, """
            CREATE TABLE IF NOT EXISTS GATLING_SQL_HEADERS CLUSTER BY (RUN_KEY, TS) (
              RUN_KEY NUMBER(19,0),
              TS TIMESTAMP_NTZ(9),
              LEVEL VARCHAR(16777216),
              LOGGER VARCHAR(16777216),
              MESSAGE_KIND VARCHAR(16777216),
              GATLING_RUN_ID VARCHAR(16777216),
              STATUS VARCHAR(16777216),
              GATLING_SESSION_ID NUMBER(38,0),
              MODEL VARCHAR(16777216),
              QUERY_NAME VARCHAR(16777216),
              QUERY_HASH VARCHAR(16777216),
              START_MS NUMBER(38,0),
              END_MS NUMBER(38,0),
              DURATION_MS NUMBER(38,0),
              ROWS_RETURNED NUMBER(38,0),
              SRC_FILENAME VARCHAR(16777216),
              SRC_ROW_NUMBER NUMBER(38,0),
              RAW_LINE VARCHAR(16777216)
            );
            """);

        exec(conn, """
            CREATE OR REPLACE VIEW V_GATLING_JOINED AS
            SELECT
                h.run_key,
                TRIM(SPLIT_PART(h.gatling_run_id, '|', 1)) AS test_name,
                TRY_TO_NUMBER(REGEXP_SUBSTR(SPLIT_PART(h.gatling_run_id, '|', 2), '[0-9]+')) AS concurrent_users,
                TRIM(SPLIT_PART(h.gatling_run_id, '|', 3)) AS test_run_time,
                h.ts AS header_ts,
                h.level AS header_level,
                h.logger AS header_logger,
                h.message_kind AS header_message_kind,
                h.gatling_run_id,
                h.status,
                h.gatling_session_id,
                h.model,
                h.query_name,
                h.query_hash,
                h.start_ms AS header_start_ms,
                h.end_ms AS header_end_ms,
                h.duration_ms AS header_duration_ms,
                h.rows_returned AS header_rows_returned,
                h.src_filename AS header_src_filename,
                h.src_row_number AS header_src_row_number,
                d.ts AS detail_ts,
                d.rownumber,
                d.row_map_raw,
                d.row_hash,
                d.src_filename AS detail_src_filename,
                d.src_row_number AS detail_src_row_number,
                d.raw_line AS detail_raw_line
            FROM gatling_sql_headers h
            JOIN gatling_sql_details d
              ON h.gatling_run_id = d.gatling_run_id
             AND h.gatling_session_id = d.gatling_session_id
             AND h.model = d.model
             AND h.query_hash = d.query_hash;
            """);

        LOGGER.info("✅ All required Snowflake schema objects verified.");
    }

    private static void exec(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    private Properties getConnectionProperties() {
        String user = PropertiesManager.getCustomProperty("snowflake.archive.username");
        String password = PropertiesManager.getCustomProperty("snowflake.archive.password");
        String warehouse = PropertiesManager.getCustomProperty("snowflake.archive.warehouse");
        String database = PropertiesManager.getCustomProperty("snowflake.archive.database");
        String schema = PropertiesManager.getCustomProperty("snowflake.archive.schema");
        String role = null;

        if (PropertiesManager.hasProperty("snowflake.archive.role")) {
            role = PropertiesManager.getCustomProperty("snowflake.archive.role");
        }

        // Defensive: trim values read from properties to remove accidental whitespace/newlines
        if (user != null) user = user.trim();
        if (password != null) password = password.trim();

        Properties props = new Properties();
        props.put("user", user);
        props.put("password", password);
        props.put("warehouse", warehouse);
        props.put("db", database);
        props.put("schema", schema);
        if (StringUtils.isNotBlank(role)) {
            props.put("role", role);
        }

        // Debug: log username and masked password info (do NOT log secrets)
        if (LOGGER.isDebugEnabled()) {
            int pwLen = password == null ? 0 : password.length();
            String masked = pwLen > 0 ? ("***" + pwLen + "chars**") : "(empty)";
            LOGGER.debug("Snowflake connection properties: user='{}', password={}", user, masked);
        }

        return props;
    }

    /** INSERT from RAW -> SQL_LOGS (parsing by tab-delimited fields). Adjust positions if needed. */
    private static String getInsertIntoSqlLogsSql() {
        return """
            INSERT INTO GATLING_SQL_LOGS (
                TS, LEVEL, LOGGER, MESSAGE_KIND, GATLING_RUN_ID, STATUS,
                GATLING_SESSION_ID, MODEL, QUERY_NAME, QUERY_HASH,
                START_MS, END_MS, DURATION_MS, ROWS_RETURNED,
                ROWNUMBER, ROW_MAP_RAW, ROW_HASH,
                SRC_FILENAME, SRC_ROW_NUMBER, RAW_LINE
            )
            SELECT
                /* ts */
                to_timestamp_ntz(regexp_substr(raw_line, '^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}'))                                       as ts,

                /* level */
                regexp_substr(raw_line, '^[^ ]+ [^ ]+ ([A-Z]+)', 1, 1, 'e', 1)                                                                           as level,

                /* logger (single cleaned value) */
                regexp_replace(regexp_substr(raw_line, ' [A-Za-z0-9_\\\\.]+:', 1, 1), '[: ]', '')                                                          as logger,

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
                regexp_substr(raw_line, 'row=Map\\\\((.*?)\\\\)', 1, 1, 'e', 1)                                                                              as row_map_raw,
                regexp_substr(raw_line, 'rowhash=([a-f0-9]+)', 1, 1, 'e', 1)                                                                             as row_hash,

                /* lineage + raw */
                src_filename,
                src_row_number,
                raw_line            FROM GATLING_RAW_SQL_LOGS;
            """;
    }

    /** Step 5: INSERT headers (ROWNUMBER IS NULL) into GATLING_SQL_HEADERS. */
    private static String getInsertIntoHeadersSql() {
        return """
            INSERT INTO GATLING_SQL_HEADERS
            SELECT
                /* stable key built from your join columns */
                HASH(gatling_run_id, gatling_session_id, model, query_hash) AS run_key,

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
            FROM gatling_sql_logs
            WHERE rownumber IS NULL;
            """;
    }

    /** Step 6: INSERT details (ROWNUMBER IS NOT NULL) into GATLING_SQL_DETAILS. */
    private static String getInsertIntoDetailsSql() {
        return """
            INSERT INTO GATLING_SQL_DETAILS
            SELECT
                /* same stable key for easy joins */
                HASH(gatling_run_id, gatling_session_id, model, query_hash) AS run_key,

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
            FROM gatling_sql_logs
            WHERE rownumber IS NOT NULL;
            """;
    }


    private String getSnowflakeURL() {
        String account = PropertiesManager.getCustomProperty("snowflake.archive.account");
        return String.format("jdbc:snowflake://%s.snowflakecomputing.com/", account);
    }

    protected void initAdditionalProperties() {
        AdditionalPropertiesLoader loader = new AdditionalPropertiesLoader();
        PropertiesManager.setCustomProperties(loader.fetchAdditionalProperties(AdditionalPropertiesLoader.SecretsManagerType.AWS));
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (String a : args) {
            if (a.startsWith("--") && a.contains("=")) {
                int i = a.indexOf('=');
                m.put(a.substring(2, i).toLowerCase(Locale.ROOT), a.substring(i + 1));
            }
        }
        return m;
    }
}