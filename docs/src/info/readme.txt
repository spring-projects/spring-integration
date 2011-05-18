SPRING INTEGRATION 2.0.4 (May 18, 2011)
-----------------------------------------------------------

To find out what has changed since version 1.0.x or any earlier 2.0.0
releases or milestones, see 'changelog.txt'.

Please consult the documentation located within the 'docs/reference' directory
of this release and also visit the official Spring Integration home at
http://www.springsource.org/spring-integration

There you will find links to the forum, issue tracker, and several other resources.

To check out the project and build from source, do the following:

    git clone --recursive git://git.springsource.org/spring-integration/spring-integration.git
    cd spring-integration
    ./gradlew build

To generate Eclipse metadata (.classpath and .project files), do the following:

    ./gradlew eclipse

Once complete, you may then import projects into Eclipse as usual:

    File->Import->Existing projects into workspace

and point to the 'spring-integration' root directory.  All projects should import
free of errors.

To generate IDEA metadata (.iml and .ipr files), do the following:

    ./gradlew idea

To build the JavaDoc, do the following from within the root directory:

    ./gradlew :docs:api

the result will be available in 'docs/build/api'.
