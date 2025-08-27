# atscale-gatling
Overview

Gatling test harness for AtScale. Uses the atscale-gatling-core library to run regression, load, and performance tests.

This project is intended to be a simple way to run Gatling tests against AtScale.  It uses the atscale-gatling-core library to do the heavy lifting: https://github.com/AtScaleInc/atscale-gatling-core  This project is much simpler than atscale-gatling-core and is easier to extend and modify.  It is intended to be a starting point for users who want to build their own Gatling test harness for AtScale.

Let us put this into context.  A simplified AtScale architecture is shown below:

![img.png](img.png)

Our approach is simple.  We impersonate BI Tools by leveraging Gatling to simulate user activity.  Since the atscale database contains a history of the BI queries run by the engine against the Data Warehouse, we are able to extract the queries executed against AtSale by the BI Tools and run them using automation.

![img_1.png](img_1.png)

This is incredibly powerful.  Not only can we run regression tests, but we can easily emulate various user loads by shaping our simulations.

To get started create a properties file named system.properties in the src/test/resources directory.  The file should be modeled after the example_system.properties file in the same directory.  The properties file should contain the following properties:
1. A list of models
2. The JDBC URL to connect to the Atscale Postgres Database where system configuration data and query data is stored.  The database is named atscale.
3. The username and password to connect to the Atscale Postgres Database
4. A set of properties to connect to the AtScale JDBC endpoint for each model.  AtScale emulates Postgres so the JDBC URL looks similar to the AtScale database where system configuration is stored.
5. A set of properties to connect to the AtScale XMLA endpoint for each model.

```
atscale.models=model1,model2,model3
atscale.jdbc.url=jdbc:postgresql://your_host:your_port/atscale
atscale.jdbc.username=your_username
atscale.jdbc.password=your_password
atscale.model1.jdbc.url=jdbc:postgresql://your_host:your_port/your_catalog
atscale.model1.jdbc.username=your_keycloak_username
atscale.model1.jdbc.password=your_keycloak_password
atscale.model1.jdbc.maxPoolSize=10
atscale.model2.jdbc.url=jdbc:postgresql://your_host:your_port/your_catalog
atscale.model2.jdbc.username=your_keycloak_username
atscale.model2.jdbc.password=your_keycloak_password
atscale.model2.jdbc.maxPoolSize=10
atscale.model3.jdbc.url=jdbc:postgresql://your_host:your_port/your_catalog
atscale.model3.jdbc.username=your_keycloak_username
atscale.model3.jdbc.password=yuor_keycloak_password
atscale.model3.jdbc.maxPoolSize=10
atscale.model1.xmla.url=your_atscale_xmla_url_with_access_token
atscale.model1.xmla.cube=cube_name_for_model1
atscale.model1.xmla.catalog=catalog_name_for_model1
```  

Run this command to extract queries from the Atscale database into a files:
```shell
 ./mvnw clean compile exec:java -Dexec.mainClass="executors.QueryExtractExecutor"
```
There is also a maven goal defined in the pom.xml file.  The same command can be run using:
```shell
 ./mvnw clean compile exec:java@query-extract
```
where query-extract is the id of the execution to be run.

For details refer to the pom.xml file and look for:  <artifactId>exec-maven-plugin</artifactId>

If run successfully, there will be two files created in the directory /queries for each model defined in the atscale.models property

Once we have extracted the queries we can run Gatling Scenario Simulations to execute the queries against the Atscale Engine.  In the src/main/executors directory there are two example executors: OpenStepSimulationExecutor and ClosedStepSimulationExecutor.  These executors run Gatling simulations using open steps and closed steps respectively.  You can create your own executors by modeling them after these examples.  OpenStep and ClosedStep simulations are defined in Gatling.  We simply leverage those constructs.

Our example executors extend the base SimulationExecutor class, implementing the Decorator design pattern to more tightly define it as either a ClosedStep or OpenStep Simulation.  Implementations are simple.  Just define implementations  for the two abstract methods of the abstract base class.
```
protected abstract List<MavenTaskDto> getSimulationTasks(); 
protected abstract String injectionStepsAsJson(List<T> injectionSteps);
```

The getSimulationTasks() method returns a list of MavenTaskDto objects.  These data transfer objects define the simulation class to be run, the AtScale model to be used, and a list of injection steps.  Injection steps define the type of user load we want to run against the AtScale Engine.  Injection steps are defined as a list.  Each injection step is an adaptor between a JSON form that can be passed to the test framework and a Gatling injection step.  See the toGatlingStep() method in these objects.  Gatling will run the simulation defined in the task passing the list of injection steps to the simulation.  Injections steps will be executed sequentially.  The tasks, or more specifically the simulations, are run in parallel on separate JVMs to simulate real world load on the XMLA and JDBC AtScale Engine endpoints.

There are two types of injection steps: Open Steps and Closed Steps.  Open Steps define user load in terms of users per second.  Closed Steps define user load in terms of a fixed number of users.  For more information refer to the Gatling documentation.
                        
Open Steps include:
```
AtOnceUsersOpenInjectionStep: Injects a specified number of users at once.
ConstantUsersPerSecOpenInjectionStep: Injects users at a constant rate per second for a given duration.
NothingForOpenInjectionStep: Pauses injection for a specified duration.  
RampUsersOpenInjectionStep: Gradually increases the number of users from 0 to a target over a specified duration.
RampUsersPerSecOpenInjectionStep: Ramps the user injection rate per second between two values over a duration.
StressPeakUsersOpenInjectionStep:  Models a stress peak scenario, where a specified number of users are injected at the same time, repeatedly, to simulate a sudden spike in load.
```

Clsosed Steps include:
```
ConstantConcurrentUsersClosedInjectionStep: Maintains a constant number of users for a period of time.
IncrementConcurrentUsersClosedInjectionStep: Gradually increases the number of users in increments over a specified duration.
RampConcurrentUsersClosedInjectionStep: Starts with a specified number of users and ramps up to a target number over a given duration.
```

In the pom.xml file we have defined maven executors for the simulations to make them easy to run from anywhere       
```shell                                       
./mvnw clean compile exec:java@closedstep-simulation-executor
```

``` shell
./mvnw clean compile exec:java@openstep-simulation-executor  
```


