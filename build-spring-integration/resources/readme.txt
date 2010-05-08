SPRING INTEGRATION 2.0.0 Milestone 4 (May 8, 2010)
--------------------------------------------------

To find out what has changed since version 1.0.4 or 2.0 M3, see 'changelog.txt'

Please consult the documentation located within the 'docs/reference' directory of this
release  and also visit the official Spring Integration home at:
http://www.springsource.org/spring-integration

There you will find links to the forum, issue tracker, and several other resources.

To build and run the sample applications that are included with this distribution,
view the README.txt file in the 'samples' directory.

To checkout the project from the SVN head and build from source, do the following
(NOTE: this requires Ant 1.7.1):

    svn co https://src.springsource.org/svn/spring-integration/trunk .
    cd build-spring-integration
    ant jar test package

The result is available as a zip file in "build-spring-integration/target/artifacts"
An expanded version is also available in "build-spring-integration/target/package-expanded"

To build the JavaDoc, run 'ant javadoc-api' from within 'build-spring-integration'. The
result will be available in "build-spring-integration/target/javadoc-api".

The projects are Maven enabled, so you should be able to import them into any IDE that
has support for Maven (2.0.9 or greater).
