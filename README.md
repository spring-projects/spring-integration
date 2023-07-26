<img align="right" width="250" height="250" src="https://spring.io/img/projects/spring-integration.svg?v=2">

# Spring Integration

[<img src="https://build.spring.io/plugins/servlet/wittified/build-status/INT-MAIN">](https://build.spring.io/browse/INT-MAIN) [![Join the chat at https://gitter.im/spring-projects/spring-integration](https://badges.gitter.im/spring-projects/spring-integration.svg)](https://gitter.im/spring-projects/spring-integration?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.spring.io/scans?search.rootProjectNames=spring-integration)

Extends the Spring programming model to support the well-known Enterprise Integration Patterns. 
Spring Integration enables lightweight messaging within Spring-based applications and supports integration with external systems via declarative adapters. 
Those adapters provide a higher-level of abstraction over Spring’s support for remoting, messaging, and scheduling. 
Spring Integration’s primary goal is to provide a simple model for building enterprise integration solutions while maintaining the separation of concerns that is essential for producing maintainable, testable code.

Using the Spring Framework encourages developers to code using interfaces and use dependency injection (DI) to provide a Plain Old Java Object (POJO) with the dependencies it needs to perform its tasks. 
Spring Integration takes this concept one step further, where POJOs are wired together using a messaging paradigm and individual components may not be aware of other components in the application. 
Such an application is built by assembling fine-grained reusable components to form a higher level of functionality. 
With careful design, these flows can be modularized and also reused at an even higher level.

In addition to wiring together fine-grained components, Spring Integration provides a wide selection of channel adapters and gateways to communicate with external systems. 
Channel Adapters are used for one-way integration (send or receive); gateways are used for request/reply scenarios (inbound or outbound). 

# Installation and Getting Started

First, you need dependencies in your POM/Gradle:

```xml
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-core</artifactId>
</dependency>
```

which is also pulled transitively if you deal with target protocol channel adapters.
For example for Apache Kafka support you need just this:

```xml
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-kafka</artifactId>
</dependency>
```

For annotations or Java DSL configuration you need to *enable* Spring Integration in the application context:

```java
@EnableIntegration
@Configuration
public class ExampleConfiguration {
    
}
```

# Code of Conduct

Please see our [Code of conduct](https://github.com/spring-projects/.github/blob/main/CODE_OF_CONDUCT.md).

# Reporting Security Vulnerabilities

Please see our [Security policy](https://github.com/spring-projects/spring-integration/security/policy).

# Documentation

The Spring Integration maintains reference documentation ([published](https://docs.spring.io/spring-integration/docs/current/reference/html/) and [source](src/reference/asciidoc)), GitHub [wiki pages](https://github.com/spring-projects/spring-integration/wiki), and an [API reference](https://docs.spring.io/spring-integration/docs/current/api/). 
There are also [guides and tutorials](https://spring.io/guides) across Spring projects.


# Checking out and Building

To check out the project and build from the source, do the following:

    git clone git://github.com/spring-projects/spring-integration.git
    cd spring-integration
    ./gradlew clean test

    or

    ./gradlew clean testAll

The latter runs additional tests (those annotated with `@LongRunningIntegrationTest`); it is a more thorough test but takes quite a lot longer to run.

The test results are captured in `build/reports/tests/test` (or `.../testAll`) under each module (in HTML format).

Add `--continue` to the command to perform a complete build, even if there are failing tests in some modules; otherwise the build will stop after the current module(s) being built are completed.

**NOTE:** While Spring Integration runs with Java SE 17 or higher, a Java 17 compiler is required to build the project.

To build and install jars into your local Maven cache:

    ./gradlew publishToMavenLocal

To build api Javadoc (results will be in `build/api`):

    ./gradlew api

To build the reference documentation (results will be in `build/docs/asciidoc` and `build/docs/asciidocPdf`):

    ./gradlew reference

To build complete distribution including `-dist`, `-docs`, and `-schema` zip files (results will be in `build/distributions`):

    ./gradlew dist

# Using Eclipse or Spring Tool Suite (with BuildShip Plugin)

If you have the BuildShip plugin installed,

*File -> Import -> Gradle -> Existing Gradle Project*

# Using Eclipse or Spring Tool Suite (when the BuildShip Plugin is not installed)

To generate Eclipse metadata (.classpath and .project files, etc), do the following:

    ./gradlew eclipse

Once complete, you may then import the projects into Eclipse as usual:

 *File -> Import -> General -> Existing projects into workspace*

Browse to the *'spring-integration'* root directory. All projects should import
free of errors.

# Using IntelliJ IDEA

To import the project into IntelliJ IDEA:

File -> Open... -> and select build.gradle from spring-integration project root directory

# Guidelines

See also [Contributor Guidelines](https://github.com/spring-projects/spring-integration/blob/main/CONTRIBUTING.adoc).

# Resources

For more information, please visit the Spring Integration website at:
[https://spring.io/projects/spring-integration](https://spring.io/projects/spring-integration/)
