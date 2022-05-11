<img align="right" width="350" height="350" src="https://user-images.githubusercontent.com/96301480/153731014-72229c13-cea9-4da1-85cd-bcf445e358bf.svg">


Spring Integration [<img src="https://build.spring.io/plugins/servlet/wittified/build-status/INT-MAIN">](https://build.spring.io/browse/INT-MAIN) [![Join the chat at https://gitter.im/spring-projects/spring-integration](https://badges.gitter.im/spring-projects/spring-integration.svg)](https://gitter.im/spring-projects/spring-integration?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
==================

Extends the Spring programming model to support the well-known Enterprise Integration Patterns. Spring Integration enables lightweight messaging within Spring-based applications and supports integration with external systems via declarative adapters. Those adapters provide a higher-level of abstraction over Spring’s support for remoting, messaging, and scheduling. Spring Integration’s primary goal is to provide a simple model for building enterprise integration solutions while maintaining the separation of concerns that is essential for producing maintainable, testable code.

Using the Spring Framework encourages developers to code using interfaces and use dependency injection (DI) to provide a Plain Old Java Object (POJO) with the dependencies it needs to perform its tasks. Spring Integration takes this concept one step further, where POJOs are wired together using a messaging paradigm and individual components may not be aware of other components in the application. Such an application is built by assembling fine-grained reusable components to form a higher level of functionality. WIth careful design, these flows can be modularized and also reused at an even higher level.

In addition to wiring together fine-grained components, Spring Integration provides a wide selection of channel adapters and gateways to communicate with external systems. Channel Adapters are used for one-way integration (send or receive); gateways are used for request/reply scenarios (inbound or outbound). For a full list of adapters and gateways, refer to the reference documentation.

# Installation and Getting Started

Here is a quick teaser of a complete Spring Integration application in Java:

```java
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.*;

@EnableIntegration
@SpringBootApplication
public class Example {
    public static void main(String[] args) {
        SpringApplication.run(Example.class, args);
    }

    @Bean
    public AtomicInteger integerSource() {
        return new AtomicInteger();
    }

    @Bean
    public IntegrationFlow buildFlow() {
        return IntegrationFlows.fromSupplier(integerSource()::getAndIncrement,
                                         c -> c.poller(Pollers.fixedRate(100)))
                    .channel("inputChannel")
                    .filter((Integer p) -> p > 0)
                    .transform(Object::toString)
                    .handle(...)
                    .get();
    }
}
```
Or with Project Reactor as the processing engine:
```java
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.*;
import org.springframework.messaging.support.GenericMessage;
import reactor.core.publisher.Flux;

@EnableIntegration
@SpringBootApplication
public class Example {
    public static void main(String[] args) {
        SpringApplication.run(Example.class, args);
    }

    @Bean
    public AtomicInteger integerSource() {
        return new AtomicInteger();
    }
    
    @Bean
    protected IntegrationFlow buildFlow() {
        return IntegrationFlows.from(Flux.range(1, 50)
                    .map(GenericMessage::new))
                .filter((Integer p) -> p > 10)
                .transform(Object::toString)
                .handle(m -> System.out.println(m.getPayload()))
                .get();
    }
}
```

# Code of Conduct

Please see our [Code of conduct](https://github.com/spring-projects/.github/blob/main/CODE_OF_CONDUCT.md).

# Reporting Security Vulnerabilities

Please see our [Security policy](https://github.com/spring-projects/spring-integration/security/policy).


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

**NOTE:** While Spring Integration runs with Java SE 8 or higher, a Java 11 compiler is required to build the project.

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
