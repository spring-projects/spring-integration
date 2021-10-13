Spring Integration [<img src="https://build.spring.io/plugins/servlet/wittified/build-status/INT-MAIN">](https://build.spring.io/browse/INT-MAIN) [![Join the chat at https://gitter.im/spring-projects/spring-integration](https://badges.gitter.im/spring-projects/spring-integration.svg)](https://gitter.im/spring-projects/spring-integration?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
==================

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
