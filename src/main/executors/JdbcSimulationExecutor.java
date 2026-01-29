package executors;

import com.atscale.java.executors.MavenTaskDto;
import com.atscale.java.executors.SequentialSimulationExecutor;
import com.atscale.java.injectionsteps.ClosedStep;
import com.atscale.java.injectionsteps.ConstantConcurrentUsersClosedInjectionStep;
import com.atscale.java.utils.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ProfilePropertyLoader;

import java.util.*;

/**
 * Executor that runs JDBC queries (equivalent to task4 from ClosedStepSequentialSimulationExecutor).
 * Uses the AtScaleClosedInjectionStepSimulation with constant concurrent users.
 *
 * <p>Configuration: Set the properties file via system property or systems.properties:</p>
 * <pre>-Datscale.profileFile=my_config.properties</pre>
 */
public class JdbcSimulationExecutor extends SequentialSimulationExecutor<ClosedStep> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSimulationExecutor.class);
    private static final String PROPERTIES_FILE_KEY = "atscale.profileFile";

    public static void main(String[] args) {
        LOGGER.info("JdbcSimulationExecutor started.");

        JdbcSimulationExecutor executor = new JdbcSimulationExecutor();
        executor.execute();
        LOGGER.info("JdbcSimulationExecutor completed.");
    }

    @Override
    protected List<MavenTaskDto<ClosedStep>> getSimulationTasks() {
        List<MavenTaskDto<ClosedStep>> tasks = new ArrayList<>();

        List<ClosedStep> constantUsersInjectionSteps = new ArrayList<>();
        constantUsersInjectionSteps.add(new ConstantConcurrentUsersClosedInjectionStep(1,1));

        String propsFile = getRequiredProperty(PROPERTIES_FILE_KEY);
        ProfilePropertyLoader propertyLoader = new ProfilePropertyLoader(propsFile);

        MavenTaskDto<ClosedStep> task = new MavenTaskDto<>(propertyLoader.getProperty("atscale.task.name"));
        tasks.add(task);
        task.setMavenCommand("gatling:test");
        task.setModel(propertyLoader.getProperty("atscale.task.model"));
        task.setAlternatePropertiesFileName(propertyLoader.getPropertiesFileName());
        task.setInjectionSteps(constantUsersInjectionSteps);
        task.setRunId(propertyLoader.getProperty("atscale.task.runId"));
        task.setRunLogFileName(propertyLoader.getProperty("atscale.task.runLogFileName"));
        task.setLoggingAsAppend(false);
        task.setRunDescription(propertyLoader.getProperty("atscale.task.runDescription"));
        task.setSimulationClass("com.atscale.java.jdbc.simulations.AtScaleClosedInjectionStepSimulation");

        return withAdditionalProperties(tasks);
    }

    private Map<String, String> getAdditionalProperties() {
        AdditionalPropertiesLoader loader = new AdditionalPropertiesLoader();
        return loader.fetchAdditionalProperties(AdditionalPropertiesLoader.SecretsManagerType.AWS);
    }

    private List<MavenTaskDto<ClosedStep>> withAdditionalProperties(List<MavenTaskDto<ClosedStep>> tasks) {
        Map<String, String> additionalProperties = getAdditionalProperties();
        for(MavenTaskDto<ClosedStep> task : tasks) {
            task.setAdditionalProperties(additionalProperties);
        }
        return tasks;
    }

    private String getRequiredProperty(String key) {
        String value = System.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        if (PropertiesManager.hasProperty(key)) {
            return PropertiesManager.getCustomProperty(key);
        }
        throw new RuntimeException("Required property not found: " + key);
    }
}
