============================== Spring Integration =============================
To check out the project and build from source, do the following:

 git clone git://git.springsource.org/spring-integration/spring-integration.git
 cd spring-integration
 ./gradlew build

-------------------------------------------------------------------------------
To generate Eclipse metadata (.classpath and .project files), do the following:

 ./gradlew eclipse

Once complete, you may then import the projects into Eclipse as usual:

 File -> Import -> Existing projects into workspace

Browse to the 'spring-integration' root directory. All projects should import
free of errors.

-------------------------------------------------------------------------------
To generate IDEA metadata (.iml and .ipr files), do the following:

 ./gradlew idea

-------------------------------------------------------------------------------
To build the JavaDoc, do the following from within the root directory:

 ./gradlew :docs:api

The result will be available in 'docs/build/api'.
===============================================================================