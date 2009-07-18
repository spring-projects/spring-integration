SPRING INTEGRATION 1.0.3 (Jul 18, 2009)
---------------------------------------

To find out what has changed since version 1.0.2, see 'changelog.txt'

Please consult the documentation located within the 'docs/reference' directory of this
release  and also visit the official Spring Integration home at:
http://www.springsource.org/spring-integration

There you will find links to the forum, issue tracker, and several other resources.

To build and run the sample applications that are included with this distribution,
view the README.txt file in the 'samples' directory.

To checkout the project from the SVN head and build from source, do the following:

    svn co https://src.springsource.org/svn/spring-integration/trunk .
    cd build-spring-integration
    ant jar test package

The result is available as a zip file in "build-spring-integration/target/artifacts"
An expanded version is also available in "build-spring-integration/target/package-expanded"

To build the JavaDoc, run 'ant javadoc-api' from within 'build-spring-integration'. The
result will be available in "build-spring-integration/target/javadoc-api".

To run the code within Eclipse, do the following:

   Import... > General > Existing Projects into Workspace
   Browse to the directory where you checked out the project
   Select each module that begins with "org.springframework.integration"
   Define a Classpath Variable named IVY_CACHE under "Preferences > Java > Build Path"
   Its value should be: <checkout-dir>/ivy-cache/repository

To run the code within Idea, do the following:

   Import the existing Eclipse projects:
         (File > New Project... > Import project from external model > Eclipse)
   Define a Path Variable named IVY_CACHE (IDE Settings > Path Variables)
   Its value should be: <checkout-dir>/ivy-cache/repository
