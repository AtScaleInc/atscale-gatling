# Runtime Profile Configuration

Load simulation configuration from external properties files at runtime, enabling flexible deployment without code changes.

## Available Executors

| Executor                 | Description       | Simulation Class                              |
|--------------------------|-------------------|-----------------------------------------------|
| `JdbcSimulationExecutor` | Runs JDBC queries | `AtScaleClosedInjectionStepSimulation`        |
| `XmlaSimulationExecutor` | Runs XMLA queries | `AtScaleXmlaClosedInjectionStepSimulation`    |

## How It Works

1. Set the properties file at runtime: `-Datscale.profileFile=my_config.properties`
2. The file is loaded from **filesystem first**, then **classpath** if not found
3. Task settings (model, runId, etc.) are read from the properties file

## Usage Examples

### Example 1: Run JDBC Simulation with Properties File

```bash
mvn exec:java -Dexec.mainClass="executors.JdbcSimulationExecutor" \
    -Datscale.profileFile=example_jdbc_profile.properties
```

### Example 2: Run XMLA Simulation with Properties File

```bash
mvn exec:java -Dexec.mainClass="executors.XmlaSimulationExecutor" \
    -Datscale.profileFile=example_xmla_profile.properties
```

### Example 3: Use Absolute Path to Properties File

```bash
mvn exec:java -Dexec.mainClass="executors.JdbcSimulationExecutor" \
    -Datscale.profileFile=/path/to/production_jdbc.properties
```

This allows loading configuration from outside the project (e.g., secure config directories).

### Example 4: Override a Single Property at Runtime

```bash
mvn exec:java -Dexec.mainClass="executors.JdbcSimulationExecutor" \
    -Datscale.profileFile=example_jdbc_profile.properties \
    -Datscale.task.runId=custom-run-001
```

System properties (`-D` flags) override values from the properties file.

### Example 5: Set Properties File in systems.properties

Instead of passing the properties file via `-D`, you can set it in `systems.properties`:

```properties
atscale.profileFile=example_jdbc_profile.properties
```

Then run without specifying the file:

```bash
mvn exec:java -Dexec.mainClass="executors.JdbcSimulationExecutor"
```

## Creating Properties Files

Create a properties file for each scenario. Files can be placed anywhere on the filesystem or in `src/main/resources/` for classpath access.

### example_jdbc_profile.properties

```properties
# Example JDBC Profile Properties File
# Usage: -Datscale.profileFile=example_jdbc_profile.properties

# JDBC connection settings
atscale.jdbc.url=jdbc:atscale://host:10502/database
atscale.jdbc.username=user
atscale.jdbc.password=password
atscale.jdbc.maxPoolSize=10
atscale.jdbc.log.resultset.rows=false

# Task settings
atscale.task.name=JDBC Simulation
atscale.task.model=TPC-DS Benchmark Model
atscale.task.runId=jdbc-simulation-run
atscale.task.runLogFileName=jdbc_simulation.log
atscale.task.runDescription=JDBC Simulation Test
```

### example_xmla_profile.properties

```properties
# Example XMLA Profile Properties File
# Usage: -Datscale.profileFile=example_xmla_profile.properties

# XMLA connection settings
atscale.xmla.url=https://host/engine/xmla/token
atscale.xmla.cube=MyCube
atscale.xmla.catalog=MyCatalog
atscale.xmla.log.responsebody=false

# Task settings
atscale.task.name=XMLA Simulation
atscale.task.model=TPC-DS Benchmark Model
atscale.task.runId=xmla-simulation-run
atscale.task.runLogFileName=xmla_simulation.log
atscale.task.runDescription=XMLA Simulation Test
```

### production_jdbc.properties (External File Example)

```properties
# Production JDBC Configuration
# Store this file outside the project in a secure location

# Production JDBC connection
atscale.jdbc.url=jdbc:atscale://prod-host:10502/prod_db
atscale.jdbc.username=prod_user
atscale.jdbc.password=prod_pass
atscale.jdbc.maxPoolSize=50

# Task settings
atscale.task.name=Production JDBC Load Test
atscale.task.model=Production Model
atscale.task.runId=prod-jdbc-001
atscale.task.runLogFileName=production_jdbc.log
atscale.task.runDescription=Production JDBC Load Test
```

## Task Properties

Each executor requires these properties in the properties file:

| Property                      | Description                      |
|-------------------------------|----------------------------------|
| `atscale.task.name`           | Task name displayed in logs      |
| `atscale.task.model`          | Model name for the simulation    |
| `atscale.task.runId`          | Unique identifier for the run    |
| `atscale.task.runLogFileName` | Log file name                    |
| `atscale.task.runDescription` | Description of the run           |

## Property Lookup Order

Properties are resolved in this order (first match wins):

1. **System properties** (`-D` flags) - highest priority
2. **Properties file** (specified via `atscale.profileFile`)
3. **Main properties file** (`systems.properties` via PropertiesManager)
4. **Exception** thrown if property not found in any source

## File Loading Order

The properties file is loaded using this order:

1. **Filesystem** - checks if file exists at the specified path (supports absolute and relative paths)
2. **Classpath** - falls back to classpath resources if not found on filesystem

This allows:

- External configuration files for production deployments
- Bundled default configurations in `src/main/resources/`
- Easy local overrides without rebuilding

## Quick Reference

| Executor | Example Command                                                                                                                                       |
|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| JDBC     | `mvn exec:java -Dexec.mainClass="executors.JdbcSimulationExecutor" -Datscale.profileFile=example_jdbc_profile.properties`       |
| XMLA     | `mvn exec:java -Dexec.mainClass="executors.XmlaSimulationExecutor" -Datscale.profileFile=example_xmla_profile.properties`       |
