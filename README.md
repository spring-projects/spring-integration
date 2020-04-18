Spring Integration [<img src="https://build.spring.io/plugins/servlet/wittified/build-status/INT-MASTER">](https://build.spring.io/browse/INT-MASTER) [![Join the chat at https://gitter.im/spring-projects/spring-integration](https://badges.gitter.im/spring-projects/spring-integration.svg)](https://gitter.im/spring-projects/spring-integration?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
==================

# Code of Conduct

Please see our [Code of conduct](https://github.com/spring-projects/.github/blob/master/CODE_OF_CONDUCT.md).

# Reporting Security Vulnerabilities

Please see our [Security policy](https://github.com/spring-projects/spring-integration/security/policy).


# Checking out and Building

To check out the project and build from the source, do the following:

    git clone git://github.com/spring-projects/spring-integration.git
    cd spring-integration
    ./gradlew build

**NOTE:** While Spring Integration runs with Java SE 6 or higher, a Java 8 compiler is required to build the project.

If you encounter out of memory errors during the build, increase an available heap and permgen for Gradle:

    GRADLE_OPTS='-XX:MaxPermSize=1024m -Xmx1024m'

To build and install jars into your local Maven cache:

    ./gradlew install

To build api Javadoc (results will be in `build/api`):

    ./gradlew api

To build reference documentation (results will be in `build/reference`):

    ./gradlew reference

To build complete distribution including `-dist`, `-docs`, and `-schema` zip files (results will be in `build/distributions`)

    ./gradlew dist

# Using Eclipse

To generate Eclipse metadata (.classpath and .project files), do the following:

    ./gradlew eclipse

Once complete, you may then import the projects into Eclipse as usual:

 *File -> Import -> Existing projects into workspace*

Browse to the *'spring-integration'* root directory. All projects should import
free of errors.

# Using IntelliJ IDEA

To generate IDEA metadata (.iml and .ipr files), do the following:

    ./gradlew idea

# Resources

For more information, please visit the Spring Integration website at:
[https://projects.spring.io/spring-integration](https://projects.spring.io/spring-integration/)
