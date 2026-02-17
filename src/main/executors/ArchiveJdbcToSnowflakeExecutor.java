package executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.nio.file.Path;
import java.sql.*;
import com.atscale.java.utils.Log4jShutdown;

public class ArchiveJdbcToSnowflakeExecutor extends com.atscale.java.executors.ArchiveJdbcToSnowflakeExecutor{
    static {
        Log4jShutdown.installHook();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveJdbcToSnowflakeExecutor.class);

    public static void main(String[] args) {
        LOGGER.info("ArchiveJdbcToSnowflakeExecutor started.");
        try {
            Map<String, String> arguments = parseArgs(args);
            Path dataFile = Path.of(arguments.get("data_file"));

            executors.ArchiveJdbcToSnowflakeExecutor executor = new executors.ArchiveJdbcToSnowflakeExecutor();
            executor.initAdditionalProperties();
            executor.execute(dataFile);
        } catch (Exception e) {
            LOGGER.error("Error during ArchiveJdbcToSnowflakeExecutor execution", e);
            throw new RuntimeException("ArchiveJdbcToSnowflakeExecutor failed", e);
        }
        LOGGER.info("ArchiveJdbcToSnowflakeExecutor completed.");
        try{
            Thread.sleep(java.time.Duration.ofSeconds(10).toMillis());
        }catch(InterruptedException ie){
            Thread.currentThread().interrupt();
        }
    }
}
