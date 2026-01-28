# Runtime Profile Configuration

A single profile property determines which properties file to load for all tasks.

## How It Works

1. Set the profile at runtime: `-Datscale.closedsimulation.profile=internet_sales`
2. This loads the properties file: `internet_sales_systems.properties`
3. All tasks read their settings (model, runId, etc.) from that file

## Usage Examples

### Example 1: Run with Internet Sales Profile

```bash
mvn exec:java -Dexec.mainClass="executors.ClosedStepSequentialSimulationExecutor" \
    -Datscale.closedsimulation.profile=internet_sales
```

This loads `internet_sales_systems.properties` and runs all tasks against the Internet Sales model.

### Example 2: Run with Production Profile

```bash
mvn exec:java -Dexec.mainClass="executors.ClosedStepSequentialSimulationExecutor" \
    -Datscale.closedsimulation.profile=production
```

This loads `production_systems.properties` for production load testing.

### Example 3: Run with TPC-DS Benchmark Profile

```bash
mvn exec:java -Dexec.mainClass="executors.ClosedStepSequentialSimulationExecutor" \
    -Datscale.closedsimulation.profile=tpcds_benchmark
```

This loads `tpcds_benchmark_systems.properties` for TPC-DS benchmark tests.

### Example 4: Override a Single Property at Runtime

```bash
mvn exec:java -Dexec.mainClass="executors.ClosedStepSequentialSimulationExecutor" \
    -Datscale.closedsimulation.profile=internet_sales \
    -Datscale.closedsimulation.task1.runId=custom-run-001
```

System properties (`-D` flags) override values from the profile file.

### Example 5: Set Profile in systems.properties

Instead of passing the profile via `-D`, you can set it in `systems.properties`:

```properties
atscale.closedsimulation.profile=internet_sales
```

Then run without specifying the profile:

```bash
mvn exec:java -Dexec.mainClass="executors.ClosedStepSequentialSimulationExecutor"
```

## Creating Profile Properties Files

Create a properties file for each profile/scenario. The file name must follow the pattern: `<profile>_systems.properties`

### internet_sales_systems.properties

```properties
# Model connection settings
atscale.internet_sales.jdbc.url=jdbc:atscale://dev-host:10502/internet_sales_db
atscale.internet_sales.jdbc.username=dev_user
atscale.internet_sales.jdbc.password=dev_pass
atscale.internet_sales.jdbc.maxPoolSize=10

atscale.internet_sales.xmla.url=https://dev-host/engine/xmla/token123
atscale.internet_sales.xmla.cube=InternetSalesCube
atscale.internet_sales.xmla.catalog=InternetSalesCatalog

# Task settings - all tasks use internet_sales model
atscale.closedsimulation.task1.model=internet_sales
atscale.closedsimulation.task1.runId=Helter Skelter
atscale.closedsimulation.task1.runLogFileName=a.log
atscale.closedsimulation.task1.runDescription=Internet Sales XMLA Model Tests

atscale.closedsimulation.task2.model=internet_sales
atscale.closedsimulation.task2.runId=Gimme Shelter
atscale.closedsimulation.task2.runLogFileName=b.log
atscale.closedsimulation.task2.runDescription=Internet Sales JDBC Model Tests

atscale.closedsimulation.task3.model=internet_sales
atscale.closedsimulation.task3.runId=task3-run
atscale.closedsimulation.task3.runLogFileName=c.log
atscale.closedsimulation.task3.runDescription=Internet Sales JDBC Stepped Tests

atscale.closedsimulation.task4.model=internet_sales
atscale.closedsimulation.task4.runId=task4-run
atscale.closedsimulation.task4.runLogFileName=d.log
atscale.closedsimulation.task4.runDescription=Internet Sales JDBC Constant Tests

atscale.closedsimulation.task5.model=internet_sales
atscale.closedsimulation.task5.runId=task5-run
atscale.closedsimulation.task5.runLogFileName=e.log
atscale.closedsimulation.task5.runDescription=Internet Sales XMLA Constant Tests
```

### production_systems.properties

```properties
# Production connection settings
atscale.production.jdbc.url=jdbc:atscale://prod-host:10502/production_db
atscale.production.jdbc.username=prod_user
atscale.production.jdbc.password=prod_pass
atscale.production.jdbc.maxPoolSize=50

atscale.production.xmla.url=https://prod-host/engine/xmla/prod_token
atscale.production.xmla.cube=ProductionCube
atscale.production.xmla.catalog=ProductionCatalog

# Task settings - all tasks use production model
atscale.closedsimulation.task1.model=production
atscale.closedsimulation.task1.runId=prod-xmla-001
atscale.closedsimulation.task1.runLogFileName=prod_xmla.log
atscale.closedsimulation.task1.runDescription=Production XMLA Load Test

atscale.closedsimulation.task2.model=production
atscale.closedsimulation.task2.runId=prod-jdbc-001
atscale.closedsimulation.task2.runLogFileName=prod_jdbc.log
atscale.closedsimulation.task2.runDescription=Production JDBC Load Test

atscale.closedsimulation.task3.model=production
atscale.closedsimulation.task3.runId=prod-jdbc-002
atscale.closedsimulation.task3.runLogFileName=prod_jdbc_stepped.log
atscale.closedsimulation.task3.runDescription=Production JDBC Stepped Test

atscale.closedsimulation.task4.model=production
atscale.closedsimulation.task4.runId=prod-jdbc-003
atscale.closedsimulation.task4.runLogFileName=prod_jdbc_constant.log
atscale.closedsimulation.task4.runDescription=Production JDBC Constant Test

atscale.closedsimulation.task5.model=production
atscale.closedsimulation.task5.runId=prod-xmla-002
atscale.closedsimulation.task5.runLogFileName=prod_xmla_constant.log
atscale.closedsimulation.task5.runDescription=Production XMLA Constant Test
```

### tpcds_benchmark_systems.properties

```properties
# TPC-DS Benchmark connection settings
atscale.tpcds_benchmark_model.jdbc.url=jdbc:atscale://benchmark-host:10502/tpcds_db
atscale.tpcds_benchmark_model.jdbc.username=benchmark_user
atscale.tpcds_benchmark_model.jdbc.password=benchmark_pass
atscale.tpcds_benchmark_model.jdbc.maxPoolSize=20

atscale.tpcds_benchmark_model.xmla.url=https://benchmark-host/engine/xmla/tpcds_token
atscale.tpcds_benchmark_model.xmla.cube=TPCDSCube
atscale.tpcds_benchmark_model.xmla.catalog=TPCDSCatalog

# Task settings - all tasks use tpcds_benchmark_model
atscale.closedsimulation.task1.model=tpcds_benchmark_model
atscale.closedsimulation.task1.runId=tpcds-xmla-001
atscale.closedsimulation.task1.runLogFileName=tpcds_xmla.log
atscale.closedsimulation.task1.runDescription=TPC-DS XMLA Benchmark Test

atscale.closedsimulation.task2.model=tpcds_benchmark_model
atscale.closedsimulation.task2.runId=tpcds-jdbc-001
atscale.closedsimulation.task2.runLogFileName=tpcds_jdbc.log
atscale.closedsimulation.task2.runDescription=TPC-DS JDBC Benchmark Test

atscale.closedsimulation.task3.model=tpcds_benchmark_model
atscale.closedsimulation.task3.runId=tpcds-jdbc-002
atscale.closedsimulation.task3.runLogFileName=tpcds_jdbc_stepped.log
atscale.closedsimulation.task3.runDescription=TPC-DS JDBC Stepped Benchmark

atscale.closedsimulation.task4.model=tpcds_benchmark_model
atscale.closedsimulation.task4.runId=tpcds-jdbc-003
atscale.closedsimulation.task4.runLogFileName=tpcds_jdbc_constant.log
atscale.closedsimulation.task4.runDescription=TPC-DS JDBC Constant Benchmark

atscale.closedsimulation.task5.model=tpcds_benchmark_model
atscale.closedsimulation.task5.runId=tpcds-xmla-002
atscale.closedsimulation.task5.runLogFileName=tpcds_xmla_constant.log
atscale.closedsimulation.task5.runDescription=TPC-DS XMLA Constant Benchmark
```

## Task Properties

Each task requires these properties in the profile file:

| Property | Description |
|----------|-------------|
| `atscale.closedsimulation.taskN.model` | Model name (determines connection settings prefix) |
| `atscale.closedsimulation.taskN.runId` | Unique identifier for the run |
| `atscale.closedsimulation.taskN.runLogFileName` | Log file name |
| `atscale.closedsimulation.taskN.runDescription` | Description of the run |

## Property Lookup Order

Properties are resolved in this order (first match wins):

1. **System properties** (`-D` flags) - highest priority
2. **Profile properties file** (e.g., `internet_sales_systems.properties`)
3. **Main properties file** (`systems.properties`)
4. **Exception** thrown if property not found in any source

## Quick Reference

| Profile | Properties File | Command |
|---------|-----------------|---------|
| internet_sales | `internet_sales_systems.properties` | `-Datscale.closedsimulation.profile=internet_sales` |
| production | `production_systems.properties` | `-Datscale.closedsimulation.profile=production` |
| tpcds_benchmark | `tpcds_benchmark_systems.properties` | `-Datscale.closedsimulation.profile=tpcds_benchmark` |
