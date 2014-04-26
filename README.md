Spring Integration <img src="https://build.spring.io/plugins/servlet/buildStatusImage/INT-B40X">
==================

# Checking out and Building

To check out the project and build from source, do the following:

    git clone git://github.com/spring-projects/spring-integration.git
    cd spring-integration
    ./gradlew build

If you encounter out of memory errors during the build, increase available heap and permgen for Gradle:

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
[http://projects.spring.io/spring-integration](http://projects.spring.io/spring-integration/)
