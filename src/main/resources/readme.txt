SPRING INTEGRATION 2.0.0 Milestone 7 (Sept 03, 2010)
----------------------------------------------------

To find out what has changed since version 1.0.x or 2.0 M6, see 'changelog.txt'

Please consult the documentation located within the 'docs/reference' directory of this
release and also visit the official Spring Integration home at:
http://www.springsource.org/spring-integration

There you will find links to the forum, issue tracker, and several other resources.

To build and run the sample applications that are included with this distribution,
view the README.txt file in the 'samples' directory.

To checkout the project from the SVN head and build from source, do the following
(NOTE: this requires Maven 2.2.x):

    svn co https://src.springsource.org/svn/spring-integration/trunk .
    mvn clean install

To build the JavaDoc, run `mvn javadoc:aggregate` from within the root directory. The
result will be available in 'target/site/apidocs'.

The projects are Maven enabled, so you should be able to import them into any IDE that
has support for Maven (2.2 or greater). The SpringSource Tool Suite (STS) ships with
support for Maven projects, is free-of-charge and is the recommended IDE for use with
Spring Integration (http://springsource.com/products/sts).
