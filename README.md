# quartz-retry
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

This is a small library written in Java that allows you to automate the re-execution of quartz jobs after the last execution was completed with an error.

### Requirements:
| Java  | Quartz-retry | Spring & Spring Boot | RDBMS |
| ------------- | ------------- | ------------- | ------------- |
| 11+  | 1.0.0-SNAPSHOT  | Spring 5 <br> Spring Boot 2.7 | Postgres 9+ |

### Project modules:
- quartz-retry-core - uses Quartz as the main dependency and provides a set of components that implements the main mechanics and api, thereby forming a core that can be used in any project where Quartz is connected, regardless of the IoC container you use.
- spring-quartz-retry - is a set of Spring application-specific infrastructure components that allow you to automate the customization of your Spring application.
- spring-boot-starter-quartz-retry - is a Spring Boot starter that will auto-configure the library as soon as the library is included in your application's dependency list.

### Application configuration:
#### quartz-retry-core:
...Currently WIP...

#### spring-quartz-retry
To use the library without a Spring Boot starter, after connecting the appropriate dependency to your application, 
you need to enable the quartz-retry auto-configuration using the `@EnableQuartzRetries` annotation, which, like any 
other Enable-like annotation, can be used in the configuration classes of your application, or above the class itself your Spring Boot application.

#### spring-boot-quartz-retry
To use the auto-configuration library as a starter, you don't need to do any extra work, 
because the starter will do everything for you. Just include the spring-boot-starter-quartz-retry 
dependency into the POM file of your project

### Autoconfiguration of spring-quartz-retry under the hood:
After connecting the library, the spring-quartz-retry infrastructure components will configure the BeanFactory, 
as well as the DataSource for the library to work correctly with Quartz and Spring.
It is important to note that spring-quartz-retry includes the ``QuartzRetryDataSourceScriptDatabaseInitializer`` 
infrastructure component, which is designed to customize the DataSource by executing a DDL script for your RDBMS.
DataSource autoconfiguration is possible only if the following conditions are met:
1. Application env property ``spring.quartz.jdbc.initialize-schema`` must have one of the following values: ``always, embedded``
2. Application env property ``spring.quartz.job-store-type`` must be set to ``jdbc`` value.

If one of these two conditions is not met, autoconfiguration will not initialize the script for your RDBMS, 
but if this is still your case, then you need to apply the script manually, or by using the migration library your app use.

### Using the provided API:
In order to use the retry mechanism for a quartz job, you must use the ```@RetryableJob``` annotation on the specific job class. 
The following properties are available within the annotation:
- ``cron`` - a required property that defines the schedule of retries execution (must be used in the quartz cron expression format).
- ``maxAttempts`` - an optional property that defines the maximum number of attempts to execute a particular retry. 
The default value is "-1", which means an infinite number of attempts. The retries themselves will be executed until 
the moment of a successful execution result, or until the limits are exhausted (whichever comes first).

Both of these properties can also be exposed in the application configuration using Spring placeholders, 
for example: ``${my.awesome.property:defaultValue}``.

### Building:
In the near future you will not need to build from source to use quartz-retry library.
To get started, please make sure that the development kits you are using are the correct ones.
```
$ ./mvnw clean install
```
If you want to build with the regular `mvn` command, you will need [Maven v3.5.0 or above](https://maven.apache.org/run-maven/index.html).

### Contributing:
Anyone can participate in the development of the project by providing all possible assistance in the following areas:
- Submitting bug reports (patches are welcome!).
- Writing documentation (especially based on common issues).

We appreciate any help!