package executors;

import com.atscale.java.utils.PropertiesManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.nio.file.Path;
import java.sql.*;
import utils.RunLogUtils;

public class ArchiveJdbcToSnowflakeExecutor extends com.atscale.java.executors.ArchiveJdbcToSnowflakeExecutor{
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveJdbcToSnowflakeExecutor.class);

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
