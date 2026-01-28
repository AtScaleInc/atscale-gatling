package executors;

import com.atscale.java.executors.MavenTaskDto;
import com.atscale.java.executors.SequentialSimulationExecutor;
import com.atscale.java.injectionsteps.ClosedStep;
import com.atscale.java.injectionsteps.ConstantConcurrentUsersClosedInjectionStep;
import com.atscale.java.injectionsteps.IncrementConcurrentUsersClosedInjectionStep;
import com.atscale.java.utils.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.IOException;
import java.util.*;

public class ClosedStepSequentialSimulationExecutor extends SequentialSimulationExecutor<ClosedStep> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClosedStepSequentialSimulationExecutor.class);
    private Properties profileProperties;

    public static void main(String[] args) {
        LOGGER.info("SequentialSimulationExecutor started.");

        ClosedStepSequentialSimulationExecutor executor = new ClosedStepSequentialSimulationExecutor();
        executor.execute();
        LOGGER.info("SequentialSimulationExecutor completed.");
    }

    @Override
    protected List<MavenTaskDto<ClosedStep>> getSimulationTasks() {
        List<MavenTaskDto<ClosedStep>> tasks = new ArrayList<>();

        List<ClosedStep> t1InjectionSteps = new ArrayList<>();
        t1InjectionSteps.add(new IncrementConcurrentUsersClosedInjectionStep(1, 3, 1,1,1));

        List<ClosedStep> t2InjectionSteps = new ArrayList<>();
        t2InjectionSteps.add(new IncrementConcurrentUsersClosedInjectionStep(1, 3,1,1, 1));

        List<ClosedStep> t3InjectionSteps = new ArrayList<>();
        t3InjectionSteps.add(new IncrementConcurrentUsersClosedInjectionStep(1, 3, 1, 1, 1));

        List<ClosedStep> constantUsersInjectionSteps = new ArrayList<>();
        constantUsersInjectionSteps.add(new ConstantConcurrentUsersClosedInjectionStep(1,1));

        // Profile property determines which properties file to load
        // Usage: -Datscale.closedsimulation.profile=internet_sales
        // Loads: internet_sales_systems.properties
        String profile = getProfileProperty("atscale.closedsimulation.profile");
        String propsFile = profile + "_systems.properties";
        loadProfileProperties(propsFile);
        LOGGER.info("Using profile '{}', loading properties from: {}", profile, propsFile);

        // Task properties are read from the profile's properties file
        // Pattern: atscale.closedsimulation.<taskN>.<property>
        String taskPrefix = "atscale.closedsimulation.";

        MavenTaskDto<ClosedStep> task1 = new MavenTaskDto<>("Internet Sales XMLA Stepped User Simulation");
        tasks.add(task1);
        task1.setMavenCommand("gatling:test");
        task1.setModel(getProperty(taskPrefix + "task1.model"));
        task1.setAlternatePropertiesFileName(propsFile);
        task1.setInjectionSteps(t1InjectionSteps);
        task1.setRunId(getProperty(taskPrefix + "task1.runId"));
        task1.setRunLogFileName(getProperty(taskPrefix + "task1.runLogFileName"));
        task1.setLoggingAsAppend(true);
        task1.setRunDescription(getProperty(taskPrefix + "task1.runDescription"));
        task1.setSimulationClass("com.atscale.java.xmla.simulations.AtScaleXmlaClosedInjectionStepSimulation");

        MavenTaskDto<ClosedStep> task2 = new MavenTaskDto<>("Internet Sales JDBC User Simulation");
        tasks.add(task2);
        task2.setMavenCommand("gatling:test");
        task2.setModel(getProperty(taskPrefix + "task2.model"));
        task2.setAlternatePropertiesFileName(propsFile);
        task2.setInjectionSteps(t2InjectionSteps);
        task2.setRunId(getProperty(taskPrefix + "task2.runId"));
        task2.setRunLogFileName(getProperty(taskPrefix + "task2.runLogFileName"));
        task2.setLoggingAsAppend(false);
        task2.setRunDescription(getProperty(taskPrefix + "task2.runDescription"));
        task2.setSimulationClass("com.atscale.java.jdbc.simulations.AtScaleClosedInjectionStepSimulation");

        MavenTaskDto<ClosedStep> task3 = new MavenTaskDto<>("TPC-DS JDBC Stepped User Simulation");
        tasks.add(task3);
        task3.setMavenCommand("gatling:test");
        task3.setModel(getProperty(taskPrefix + "task3.model"));
        task3.setAlternatePropertiesFileName(propsFile);
        task3.setInjectionSteps(t3InjectionSteps);
        task3.setRunId(getProperty(taskPrefix + "task3.runId"));
        task3.setRunLogFileName(getProperty(taskPrefix + "task3.runLogFileName"));
        task3.setLoggingAsAppend(true);
        task3.setRunDescription(getProperty(taskPrefix + "task3.runDescription"));
        task3.setSimulationClass("com.atscale.java.jdbc.simulations.AtScaleClosedInjectionStepSimulation");

        // Two example tasks for the Installer Version. Exclude by removing tasks.add as needed.
        MavenTaskDto<ClosedStep> task4 = new MavenTaskDto<>("Installer TPC-DS JDBC Simulation");
        //tasks.add(task4);
        task4.setMavenCommand("gatling:test");
        task4.setModel(getProperty(taskPrefix + "task4.model"));
        task4.setAlternatePropertiesFileName(propsFile);
        task4.setInjectionSteps(constantUsersInjectionSteps);
        task4.setRunId(getProperty(taskPrefix + "task4.runId"));
        task4.setRunLogFileName(getProperty(taskPrefix + "task4.runLogFileName"));
        task4.setLoggingAsAppend(false);
        task4.setRunDescription(getProperty(taskPrefix + "task4.runDescription"));
        task4.setSimulationClass("com.atscale.java.jdbc.simulations.AtScaleClosedInjectionStepSimulation");

        MavenTaskDto<ClosedStep> task5 = new MavenTaskDto<>("Installer TPC-DS XMLA Simulation");
        //tasks.add(task5);
        task5.setMavenCommand("gatling:test");
        task5.setModel(getProperty(taskPrefix + "task5.model"));
        task5.setAlternatePropertiesFileName(propsFile);
        task5.setInjectionSteps(constantUsersInjectionSteps);
        task5.setRunId(getProperty(taskPrefix + "task5.runId"));
        task5.setRunLogFileName(getProperty(taskPrefix + "task5.runLogFileName"));
        task5.setLoggingAsAppend(false);
        task5.setRunDescription(getProperty(taskPrefix + "task5.runDescription"));
        task5.setSimulationClass("com.atscale.java.xmla.simulations.AtScaleXmlaClosedInjectionStepSimulation");

        return withAdditionalProperties(tasks);
    }

    /**
     * Loads the profile properties file.
     */
    private void loadProfileProperties(String propsFile) {
        profileProperties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(propsFile)) {
            if (input == null) {
                throw new RuntimeException("Profile properties file not found: " + propsFile);
            }
            profileProperties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load profile properties file: " + propsFile, e);
        }
    }

    /**
     * Gets the profile property. Checks system properties first, then PropertiesManager (systems.properties).
     * Throws exception if not found.
     */
    private String getProfileProperty(String key) {
        String value = System.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        if (PropertiesManager.hasProperty(key)) {
            return PropertiesManager.getCustomProperty(key);
        }
        throw new RuntimeException("Required property not found: " + key);
    }

    /**
     * Gets a property value with layered lookup:
     * 1. System properties (-D flags)
     * 2. Profile properties file (e.g., internet_sales_systems.properties)
     * 3. Main properties file (systems.properties via PropertiesManager)
     * Throws exception if not found in any source.
     */
    private String getProperty(String key) {
        // System property takes precedence (for -D flags)
        String value = System.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        // Check profile properties file
        if (profileProperties != null && profileProperties.containsKey(key)) {
            return profileProperties.getProperty(key).trim();
        }
        // Fall back to main properties file
        if (PropertiesManager.hasProperty(key)) {
            return PropertiesManager.getCustomProperty(key);
        }
        throw new RuntimeException("Required property not found: " + key);
    }

    /**
     * Default implementation.
     *<p/>
     * <p>Loads additional properties from AWS Secrets Manager if configured.</p>
     * <p>Custom implementations: Change the implementation for other secret management systems as needed.
     * You may override the {@code createSecretsManager} method in the parent class
     * to provide a different SecretsManager implementation, or override the
     * {@code additionalProperties} method to change how properties are loaded.</p>
     */
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
}
