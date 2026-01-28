package executors;

import com.atscale.java.utils.PropertiesManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.RunLogUtils;
import java.nio.file.Path;
import java.sql.*;
import java.time.Duration;
import java.util.*;

public class ArchiveXmlaToSnowflakeExecutor extends com.atscale.java.executors.ArchiveXmlaToSnowflakeExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveXmlaToSnowflakeExecutor.class);

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