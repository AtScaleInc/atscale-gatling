package executors;

import com.atscale.java.utils.Log4jShutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.sql.*;
import java.time.Duration;
import java.util.*;

public class ArchiveXmlaToSnowflakeExecutor extends com.atscale.java.executors.ArchiveXmlaToSnowflakeExecutor {
    static {
        Log4jShutdown.installHook();
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveXmlaToSnowflakeExecutor.class);

    public static void main(String[] args) {
        LOGGER.info("ArchiveXmlaToSnowflakeExecutor started.");
        try {
            Map<String, String> arguments = parseArgs(args);
            Path dataFile = Path.of(arguments.get("data_file"));

            executors.ArchiveXmlaToSnowflakeExecutor executor = new executors.ArchiveXmlaToSnowflakeExecutor();
            executor.initAdditionalProperties();
            executor.execute(dataFile);
        } catch (Exception e) {
            LOGGER.error("Error during ArchiveXmlaToSnowflakeExecutor execution", e);
            throw new RuntimeException("ArchiveXmlaToSnowflakeExecutor failed", e);
        }
        LOGGER.info("ArchiveXmlaToSnowflakeExecutor completed.");
        try {
            Thread.sleep(Duration.ofSeconds(10).toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}