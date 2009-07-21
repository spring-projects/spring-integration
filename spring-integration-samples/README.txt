Instructions for building the samples using Maven (http://maven.apache.org)
NOTE: this requires Maven 2.0.9 or later
-------------------------------------------------------------------------------

1. Open a command prompt and navigate to the 'samples' directory within the
root of the project distribution:

	#> cd samples
	
2. Build the samples and install them into your local Maven repository:

	#> mvn install
	
You should see output similar to the following:
. . . .
[INFO] Reactor Summary:
[INFO] ------------------------------------------------------------------------
[INFO] Spring Integration Samples ............................ SUCCESS [2.910s]
[INFO] Unnamed - cafe:cafe:jar:1.0.3 ......................... SUCCESS [2.750s]
[INFO] Unnamed - errorhandling:errorhandling:jar:1.0.3 ....... SUCCESS [1.427s]
[INFO] Unnamed - filecopy:filecopy:jar:1.0.3 ................. SUCCESS [1.257s]
[INFO] Unnamed - helloworld:helloworld:jar:1.0.3 ............. SUCCESS [0.943s]
[INFO] Unnamed - jms:jms:jar:1.0.3 ........................... SUCCESS [0.801s]
[INFO] Unnamed - oddeven:oddeven:jar:1.0.3 ................... SUCCESS [1.009s]
[INFO] Unnamed - quote:quote:jar:1.0.3 ....................... SUCCESS [0.697s]
[INFO] Unnamed - ws:ws:jar:1.0.3 ............................. SUCCESS [0.714s]
[INFO] Unnamed - xml:xml:jar:1.0.3 ........................... SUCCESS [0.690s]
[INFO] ------------------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESSFUL
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 13 seconds
. . . .

Once you see BUILD SUCCESSFUL, you are ready to start working with samples.

You can do so in one of two ways:

1. If your Java IDE is integrated with Maven (e.g., SpringSource Tool Suite or
Eclipse with a Maven plug-in is highly recommended), then you can import the
projects into a workspace as Maven projects and start using them by following
the documentation in the javadoc of the various *Demo.java files.

For example:
    File -> Import -> General -> Existing Projects into Workspace
    Browse to the 'samples' directory or a specific project's sub-directory

2. If you prefer to configure the projects on your own and simply need the
right JARs on the class path, you can build the samples using a pre-configured
Maven profile which will copy all project dependencies (JARs) in the 'lib'
directory of each project:

	#> mvn install -P classpath
	
Open any project and you will see the 'lib' directory with all of the JARs
required to run the demos. Now you can configure your IDE to point to those
directories.

===============================================================================
OSGi Samples
------------

The Hello World and Cafe samples listed above are both OSGi-enabled. Instead
of running those as standalone applications via their main() methods, you can
deploy them to an OSGi runtime and rely on the included BundleActivators.

Before running those or any other OSGi-based applications that rely on Spring
Integration, you will need to deploy the necessary Spring Integration bundles
into the OSGi deployment environment (you can just copy all of the JARs within
the 'dist' directory of the distribution). You may also need some of the other
dependencies that are located within the 'lib' directory of the distribution.
For the detailed instructions, refer to the "Samples" Appendix of the Spring
Integration Reference Manual.

This release also includes two new samples that are dedicated to showcasing
capabilities of the Spring Integration and OSGi combination. They form a
producer/consumer pair: osgi-inbound and osgi-outbound. Unlike the others,
they are not Maven-enabled, but instead they are ready to run directly within
SpringSource dm Server, and with the SpringSource Tool Suite support, this
requires only an import, adding "OSGi Bundle Project Nature" via the 'Spring
Tools' context menu, and deploying to a dm server instance as the project's
targeted runtime. A comprehensive overview of those OSGi samples is included
in the "Samples" Appendix of the Spring Integration Reference Manual.

Happy integration :-)
